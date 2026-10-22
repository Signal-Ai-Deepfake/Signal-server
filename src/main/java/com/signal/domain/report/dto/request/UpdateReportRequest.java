package com.signal.domain.report.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PATCH 부분 수정 요청. 필드마다 세 가지 경우를 구분한다.
 * - 필드를 아예 보내지 않음(null): 기존 값 유지
 * - null을 보냄(Optional.empty()): 값 지우기 (목록 필드는 빈 목록이 된다, description은 지울 수 없다)
 * - 값을 보냄: 그 값으로 변경
 *
 * record로 두면 Jackson이 생성자로 바인딩하면서 누락된 필드도 Optional.empty()로 채워 두 경우가
 * 구분되지 않는다. 그래서 기본 생성자 + setter로 바인딩해, 보내지 않은 필드는 setter가 호출되지 않고
 * null로 남도록 했다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReportRequest {

    private Optional<LocalDate> incidentDate;
    private Optional<String> discoveryRoute;
    private Optional<String> damageType;
    private Optional<String> description;
    private Optional<List<String>> sourceUrls;
    private Optional<List<Long>> evidenceIds;
    private Optional<String> targetAgencyType;
    private Optional<String> additionalNotes;
}
