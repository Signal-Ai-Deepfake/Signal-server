package com.signal.domain.agency.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.agency.entity.Agency;
import com.signal.domain.agency.entity.AgencySituationType;
import com.signal.domain.chat.guidance.AgencyCatalog;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void dataSql로_시드된_기관_4곳이_모두_로드된다() {
        assertThat(agencyRepository.count()).isEqualTo(4);
    }

    @Test
    void CRISIS_유형을_지원하는_기관을_조회할_수_있다() {
        List<Agency> agencies = agencyRepository.findBySituationType(AgencySituationType.CRISIS);

        assertThat(agencies).extracting(Agency::getName)
                .containsExactlyInAnyOrder("자살예방 상담전화", "정신건강 위기상담전화");
    }

    @Test
    void DEEPFAKE_IMAGE_유형을_지원하는_기관을_조회할_수_있다() {
        List<Agency> agencies = agencyRepository.findBySituationType(AgencySituationType.DEEPFAKE_IMAGE);

        assertThat(agencies).extracting(Agency::getName)
                .containsExactlyInAnyOrder("디지털성범죄피해자지원센터", "경찰 사이버범죄 신고");
    }

    @Test
    void 기관의_supportedActions가_순서대로_함께_로드된다() {
        Agency agency = agencyRepository.findBySituationType(AgencySituationType.CRISIS).stream()
                .filter(a -> a.getName().equals("자살예방 상담전화"))
                .findFirst()
                .orElseThrow();

        assertThat(agency.getSupportedActions()).containsExactly("위기 상담", "긴급 개입");
    }

    @Test
    void 이미_옛_번호로_시드된_DB에도_다시_실행하면_정정된_번호가_반영된다() {
        // 운영 DB에는 1393/182로 시드된 행이 이미 있다. INSERT IGNORE만으로는 갱신되지 않는다.
        jdbcTemplate.update("UPDATE agencies SET name = '경찰청 사이버수사국', phone = '182' WHERE id = 2");
        jdbcTemplate.update("UPDATE agencies SET name = '한국생명존중희망재단 자살예방상담전화', phone = '1393' WHERE id = 3");

        new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);

        assertThat(jdbcTemplate.queryForObject("SELECT phone FROM agencies WHERE id = 2", String.class))
                .isEqualTo("112");
        assertThat(jdbcTemplate.queryForObject("SELECT phone FROM agencies WHERE id = 3", String.class))
                .isEqualTo("109");
        assertThat(agencyRepository.count()).isEqualTo(4);
    }

    @Test
    void 기관_시드의_번호는_챗봇_기관_목록의_번호와_일치한다() {
        // 정신건강 위기상담전화(1577-0199)는 챗봇이 안내하지 않는 기관이라 비교에서 뺀다.
        Set<String> notInChatbot = Set.of("정신건강 위기상담전화");
        Set<String> chatbotNumbers = AgencyCatalog.knownNumbers();
        Pattern digits = Pattern.compile("\\d{3,}");

        for (Agency agency : agencyRepository.findAll()) {
            if (notInChatbot.contains(agency.getName())) {
                continue;
            }
            digits.matcher(agency.getPhone()).results().forEach(match ->
                    assertThat(chatbotNumbers)
                            .as("%s의 번호 %s가 챗봇 기관 목록에 없다", agency.getName(), match.group())
                            .contains(match.group()));
        }
    }

    @Test
    void 폐지되었거나_용도가_다른_번호는_시드에_없다() {
        assertThat(agencyRepository.findAll()).extracting(Agency::getPhone)
                .doesNotContain("1393", "182");
    }
}
