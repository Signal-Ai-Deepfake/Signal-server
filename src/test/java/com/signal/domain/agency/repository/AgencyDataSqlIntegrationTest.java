package com.signal.domain.agency.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.agency.entity.Agency;
import com.signal.domain.agency.entity.AgencySituationType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/**
 * data.sql이 실제로 (Hibernate ddl-auto 스키마 생성 이후) 실행되는지, agency_supported_situation_types
 * 컬렉션 테이블에 대한 커스텀 @Query(MEMBER OF)가 실제 DB에서 올바르게 동작하는지 검증한다.
 * MySQL 전용 문법(INSERT IGNORE)을 그대로 검증하기 위해 H2를 MySQL 호환 모드로 띄운다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:agency_data_sql_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class AgencyDataSqlIntegrationTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Test
    void dataSql로_시드된_기관_4곳이_모두_로드된다() {
        assertThat(agencyRepository.count()).isEqualTo(4);
    }

    @Test
    void CRISIS_유형을_지원하는_기관을_조회할_수_있다() {
        List<Agency> agencies = agencyRepository.findBySituationType(AgencySituationType.CRISIS);

        assertThat(agencies).extracting(Agency::getName)
                .containsExactlyInAnyOrder("한국생명존중희망재단 자살예방상담전화", "정신건강 위기상담전화");
    }

    @Test
    void DEEPFAKE_IMAGE_유형을_지원하는_기관을_조회할_수_있다() {
        List<Agency> agencies = agencyRepository.findBySituationType(AgencySituationType.DEEPFAKE_IMAGE);

        assertThat(agencies).extracting(Agency::getName)
                .containsExactlyInAnyOrder("디지털성범죄피해자지원센터", "경찰청 사이버수사국");
    }

    @Test
    void 기관의_supportedActions가_순서대로_함께_로드된다() {
        Agency agency = agencyRepository.findBySituationType(AgencySituationType.CRISIS).stream()
                .filter(a -> a.getName().equals("한국생명존중희망재단 자살예방상담전화"))
                .findFirst()
                .orElseThrow();

        assertThat(agency.getSupportedActions()).containsExactly("위기 상담", "긴급 개입");
    }
}
