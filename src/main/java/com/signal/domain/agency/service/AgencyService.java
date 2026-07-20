package com.signal.domain.agency.service;

import com.signal.domain.agency.entity.Agency;
import com.signal.domain.agency.entity.AgencyConnection;
import com.signal.domain.agency.entity.AgencySituationType;
import com.signal.domain.agency.entity.ConnectionType;
import com.signal.domain.agency.repository.AgencyConnectionRepository;
import com.signal.domain.agency.repository.AgencyRepository;
import com.signal.domain.report.entity.Report;
import com.signal.domain.report.service.ReportService;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final AgencyConnectionRepository agencyConnectionRepository;
    private final ReportService reportService;

    public List<Agency> getAgencies(AgencySituationType situationType) {
        if (situationType == null) {
            return agencyRepository.findAll();
        }
        return agencyRepository.findBySituationType(situationType);
    }

    @Transactional
    public AgencyConnection createConnection(Long userId, Long agencyId, Long reportId, ConnectionType connectionType) {
        if (connectionType == null) {
            throw new SignalException(ErrorCode.INVALID_INPUT);
        }

        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new SignalException(ErrorCode.NOT_FOUND));

        String attachedReportUrl = null;
        if (reportId != null) {
            Report report = reportService.getReport(userId, reportId);
            attachedReportUrl = report.getDocumentUrl();
        }

        AgencyConnection connection = AgencyConnection.builder()
                .userId(userId)
                .agencyId(agency.getId())
                .reportId(reportId)
                .connectionType(connectionType)
                .agencyName(agency.getName())
                .phone(agency.getPhone())
                .attachedReportUrl(attachedReportUrl)
                .build();

        return agencyConnectionRepository.save(connection);
    }
}
