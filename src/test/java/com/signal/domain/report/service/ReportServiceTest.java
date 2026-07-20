package com.signal.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.signal.domain.report.dto.request.CreateReportRequest;
import com.signal.domain.report.dto.request.UpdateReportRequest;
import com.signal.domain.report.entity.Report;
import com.signal.domain.report.entity.ReportStatus;
import com.signal.domain.report.entity.TimelineEvent;
import com.signal.domain.report.generator.ReportDocumentGenerator;
import com.signal.domain.report.repository.ReportRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportDocumentGenerator reportDocumentGenerator;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, reportDocumentGenerator);
    }

    private CreateReportRequest sampleRequest() {
        return new CreateReportRequest(
                LocalDate.of(2026, 7, 1),
                "지인 제보",
                "딥페이크 합성물 유포",
                "SNS에 합성 이미지가 유포되었습니다.",
                List.of("https://example.com/post/1"),
                List.of(10L, 11L),
                "경찰청");
    }

    @Test
    void 신고서를_생성하면_DRAFT_상태로_저장되고_문서URL이_생성된다() {
        when(reportDocumentGenerator.generate(ReportStatus.DRAFT)).thenReturn("https://stub/draft.pdf");
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Report report = reportService.createReport(1L, sampleRequest());

        assertThat(report.getUserId()).isEqualTo(1L);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DRAFT);
        assertThat(report.getDocumentUrl()).isEqualTo("https://stub/draft.pdf");
        assertThat(report.getTimeline()).hasSize(1);
        assertThat(report.getTimeline().get(0).getEvent()).isEqualTo(TimelineEvent.CREATED);
    }

    @Test
    void 설명이_없으면_REQUIRED_FIELD_MISSING_예외가_발생한다() {
        CreateReportRequest request = new CreateReportRequest(
                null, null, null, " ", List.of("https://example.com/post/1"), null, null);

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void sourceUrls가_없으면_REQUIRED_FIELD_MISSING_예외가_발생한다() {
        CreateReportRequest request = new CreateReportRequest(
                null, null, null, "설명입니다.", List.of(), null, null);

        assertThatThrownBy(() -> reportService.createReport(1L, request))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void 소유자가_조회하면_결과를_반환한다() {
        Report report = Report.builder()
                .userId(1L).description("설명").sourceUrls(List.of("https://a")).evidenceIds(List.of())
                .documentUrl("https://stub/draft.pdf").build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        Report result = reportService.getReport(1L, 1L);

        assertThat(result).isEqualTo(report);
    }

    @Test
    void 존재하지_않는_신고서면_예외가_발생한다() {
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReport(1L, 1L))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void 소유자가_아니면_예외가_발생한다() {
        Report report = Report.builder()
                .userId(2L).description("설명").sourceUrls(List.of("https://a")).evidenceIds(List.of())
                .documentUrl("https://stub/draft.pdf").build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.getReport(1L, 1L))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void null이_아닌_필드만_반영되고_문서URL이_재생성된다() {
        Report report = Report.builder()
                .userId(1L).discoveryRoute("지인 제보").damageType("도용").description("원본 설명")
                .sourceUrls(List.of("https://a")).evidenceIds(List.of()).targetAgencyType("경찰청")
                .documentUrl("https://stub/draft-v1.pdf").build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportDocumentGenerator.generate(ReportStatus.DRAFT)).thenReturn("https://stub/draft-v2.pdf");

        UpdateReportRequest request = new UpdateReportRequest(null, null, null, "수정된 설명", null, null, null);
        Report updated = reportService.updateReport(1L, 1L, request);

        assertThat(updated.getDescription()).isEqualTo("수정된 설명");
        assertThat(updated.getDiscoveryRoute()).isEqualTo("지인 제보");
        assertThat(updated.getDamageType()).isEqualTo("도용");
        assertThat(updated.getDocumentUrl()).isEqualTo("https://stub/draft-v2.pdf");
        assertThat(updated.getTimeline()).extracting(entry -> entry.getEvent())
                .containsExactly(TimelineEvent.CREATED, TimelineEvent.UPDATED);
    }

    @Test
    void 확정된_신고서는_수정할_수_없다() {
        Report report = Report.builder()
                .userId(1L).description("설명").sourceUrls(List.of("https://a")).evidenceIds(List.of())
                .documentUrl("https://stub/draft.pdf").build();
        report.markFinalized("https://stub/final.pdf");
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.updateReport(1L, 1L, new UpdateReportRequest(
                        null, null, null, "수정 시도", null, null, null)))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_ALREADY_FINALIZED);
    }

    @Test
    void 신고서를_확정하면_FINALIZED_상태로_전환되고_문서URL이_재생성된다() {
        Report report = Report.builder()
                .userId(1L).description("설명").sourceUrls(List.of("https://a")).evidenceIds(List.of())
                .documentUrl("https://stub/draft.pdf").build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportDocumentGenerator.generate(ReportStatus.FINALIZED)).thenReturn("https://stub/final.pdf");

        Report finalized = reportService.finalizeReport(1L, 1L);

        assertThat(finalized.getStatus()).isEqualTo(ReportStatus.FINALIZED);
        assertThat(finalized.getDocumentUrl()).isEqualTo("https://stub/final.pdf");
        assertThat(finalized.getTimeline()).extracting(entry -> entry.getEvent())
                .containsExactly(TimelineEvent.CREATED, TimelineEvent.FINALIZED);
    }

    @Test
    void 이미_확정된_신고서는_다시_확정할_수_없다() {
        Report report = Report.builder()
                .userId(1L).description("설명").sourceUrls(List.of("https://a")).evidenceIds(List.of())
                .documentUrl("https://stub/draft.pdf").build();
        report.markFinalized("https://stub/final.pdf");
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.finalizeReport(1L, 1L))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_ALREADY_FINALIZED);
    }
}
