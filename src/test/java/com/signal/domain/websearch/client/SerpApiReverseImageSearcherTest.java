package com.signal.domain.websearch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.domain.monitoring.entity.MatchType;
import com.signal.domain.monitoring.monitor.ReverseImageSearcher.SearchHit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SerpApiReverseImageSearcherTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 동일_이미지는_FULL_비슷한_이미지는_SIMILAR로_구분하고_중복은_하나만_남긴다() throws Exception {
        String json = """
                {"exact_matches":[{"title":"A","link":"https://a.com/1"}],
                 "visual_matches":[{"title":"A again","link":"https://a.com/1"},
                                   {"title":"B","link":"https://b.com/2","thumbnail":"https://t"}]}""";

        List<SearchHit> hits = SerpApiReverseImageSearcher.parse(mapper.readTree(json));

        assertThat(hits).extracting(SearchHit::pageUrl).containsExactly("https://a.com/1", "https://b.com/2");
        assertThat(hits).extracting(SearchHit::matchType).containsExactly(MatchType.FULL, MatchType.SIMILAR);
        assertThat(hits).extracting(SearchHit::imageUrl).containsOnlyNulls();   // 제3자 썸네일은 쓰지 않는다
    }

    @Test
    void 안전하지_않은_링크는_버리고_결과는_20건까지만() throws Exception {
        StringBuilder items = new StringBuilder("{\"link\":\"javascript:alert(1)\"}");
        for (int i = 0; i < 30; i++) {
            items.append(",{\"link\":\"https://s.com/").append(i).append("\"}");
        }

        List<SearchHit> hits = SerpApiReverseImageSearcher.parse(
                mapper.readTree("{\"visual_matches\":[" + items + "]}"));

        assertThat(hits).hasSize(20);
        assertThat(hits).extracting(SearchHit::pageUrl).noneMatch(u -> u.startsWith("javascript"));
    }

    @Test
    void 결과가_없다는_응답은_빈_목록이다() throws Exception {
        assertThat(SerpApiReverseImageSearcher.parse(
                mapper.readTree("{\"error\":\"Google hasn't returned any results for this query.\"}"))).isEmpty();
    }

    @Test
    void 그_밖의_오류_응답은_예외로_올린다() throws Exception {
        assertThatThrownBy(() -> SerpApiReverseImageSearcher.parse(
                mapper.readTree("{\"error\":\"Invalid API key.\"}")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Invalid API key");
    }

    @Test
    void 키가_없으면_기동_시점에_실패한다() {
        assertThatThrownBy(() -> new SerpApiReverseImageSearcher(RestClient.builder(), new TempImageStore(300),
                "", "https://serpapi.com", "https://api.example.com", 5))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("SERPAPI_API_KEY");
    }

    @Test
    void 서버_외부_주소가_없으면_기동_시점에_실패한다() {
        assertThatThrownBy(() -> new SerpApiReverseImageSearcher(RestClient.builder(), new TempImageStore(300),
                "key", "https://serpapi.com", "", 5))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("WEB_SEARCH_PUBLIC_BASE_URL");
    }
}
