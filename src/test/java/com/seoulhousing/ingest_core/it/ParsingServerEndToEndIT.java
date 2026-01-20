package com.seoulhousing.ingest_core.it;

import com.seoulhousing.ingest_core.external.myhome.client.MyHomeApiClient;
import com.seoulhousing.ingest_core.external.sh.service.ShRentalNoticeCheckerService;
import com.seoulhousing.ingest_core.mainserver.client.MainServerIngestClient;
import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestItem;
import com.seoulhousing.ingest_core.mainserver.dto.AnnouncementIngestRequest;
import com.seoulhousing.ingest_core.mainserver.dto.IngestResponse;
import com.seoulhousing.ingest_core.mainserver.dto.MainServerAnnouncementSource;
import com.seoulhousing.ingest_core.mainserver.service.DefaultIngestJobService;
import com.seoulhousing.ingest_core.mainserver.service.OneShotJobRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("it")
@ActiveProfiles("local")
@SpringBootTest(classes = ParsingServerOneIntegrationIT.TestApp.class)
class ParsingServerOneIntegrationIT {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @SpringBootApplication
    @ComponentScan(
            basePackages = "com.seoulhousing.ingest_core",
            excludeFilters = {
                    // OneShotJobRunner가 System.exit()로 테스트 프로세스 kill -> 테스트에서만 제외
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OneShotJobRunner.class)
            }
    )
    static class TestApp { }

    @org.springframework.beans.factory.annotation.Autowired
    DefaultIngestJobService jobService;

    @org.springframework.beans.factory.annotation.Autowired
    RedisTemplate<String, String> redisStringTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    MyHomeApiClient myHomeApiClient;

    @org.springframework.beans.factory.annotation.Autowired
    ShRentalNoticeCheckerService shChecker;

    // Spring Boot 4: @MockBean 대신 @MockitoBean
    @MockitoBean
    MainServerIngestClient mainServerIngestClient;

    @BeforeEach
    void cleanupRedisSeenKeys() {
        deleteSeenKey("myhome", "rsdt", "seoul");
        deleteSeenKey("myhome", "ltrsdt", "seoul");
        deleteSeenKey("sh", "rental", "seoul");
    }

    private void deleteSeenKey(String source, String category, String scope) {
        String key = "seoulhousing:local:seen:"
                + norm(source) + ":" + norm(category) + ":" + norm(scope);
        redisStringTemplate.delete(key);
    }

    private static String norm(String v) {
        if (v == null) return "";
        return v.trim().toLowerCase(Locale.ROOT);
    }

    @Test
    void parsingServer_endToEnd_oneJob_noMainServer_verifyRequest_and_maskingLogic() throws Exception {
        // given: 메인서버 ingest 응답 더미
        when(mainServerIngestClient.ingest(any(AnnouncementIngestRequest.class)))
                .thenReturn(new IngestResponse(1, 1, 0, 0));

        // 1) SH RSS 실제 호출+파싱 스모크
        var shItems = shChecker.fetchAllItems();
        assertThat(shItems).isNotNull();
        shItems.stream().limit(3).forEach(it -> {
            assertThat(it.getSeq()).isNotBlank();
            assertThat(it.getLink()).isNotBlank();
        });

        // 2) 파싱서버 job 1회 실행 (메인서버는 mock이라 실제 HTTP 없음)
        jobService.runOnce();

        // 3) 메인서버로 나가는 request 최소 계약 검증
        var captor = org.mockito.ArgumentCaptor.forClass(AnnouncementIngestRequest.class);
        verify(mainServerIngestClient, atLeastOnce()).ingest(captor.capture());

        List<AnnouncementIngestRequest> sent = captor.getAllValues();
        assertThat(sent).isNotEmpty();

        for (AnnouncementIngestRequest r : sent) {
            assertThat(r).isNotNull();
            assertThat(r.category()).isNotBlank();
            assertThat(r.items()).isNotNull();
            assertThat(r.items()).isNotEmpty();

            for (AnnouncementIngestItem item : r.items()) {
                assertThat(item).isNotNull();
                assertThat(item.source()).isNotNull();
                assertThat(item.externalKey()).isNotBlank();

                if (item.source() == MainServerAnnouncementSource.MYHOME) {
                    assertThat(item.externalKey()).contains(":"); // pblancId:houseSn
                } else if (item.source() == MainServerAnnouncementSource.SH_RSS) {
                    assertThat(item.publisher()).isEqualTo("SH"); // mapper 고정
                }
            }
        }

        // 4) 민감정보 마스킹 검증 (로그가 실제로 찍히는지에 의존하지 말고, 로직 자체를 검증)
        //    - MyHomeApiClient의 private static maskServiceKey(String) 를 리플렉션으로 호출
        String raw = "/rsdtRcritNtcList?serviceKey=REAL_SECRET_ABC123&brtcCode=11&_type=json";
        String masked = invokeMaskServiceKey(raw);

        assertThat(masked).contains("serviceKey=****");
        assertThat(masked).doesNotContain("REAL_SECRET_ABC123");

        // (옵션) 실제 URI 형태에서도 동작하는지 한 번 더
        URI uri = new URI("https://apis.data.go.kr/1613000/HWSPR02" + raw);
        String masked2 = invokeMaskServiceKey(uri.getRawPath() + "?" + uri.getRawQuery());
        assertThat(masked2).contains("serviceKey=****");
        assertThat(masked2).contains("brtcCode=11");
    }

    private static String invokeMaskServiceKey(String raw) throws Exception {
        Method m = MyHomeApiClient.class.getDeclaredMethod("maskServiceKey", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, raw);
    }

    @SuppressWarnings("unused")
    private static String yyyyMM(YearMonth ym) {
        int y = ym.getYear();
        int m = ym.getMonthValue();
        return y + (m < 10 ? "0" + m : String.valueOf(m));
    }
}
