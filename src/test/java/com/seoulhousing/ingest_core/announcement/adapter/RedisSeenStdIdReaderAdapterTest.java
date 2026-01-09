package com.seoulhousing.ingest_core.announcement.adapter;

import com.seoulhousing.ingest_core.config.EnvProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSeenStdIdReaderAdapterTest {

    @Mock
    RedisTemplate<String, String> redisStringTemplate;

    @Mock
    EnvProvider envProvider;

    @Mock
    SetOperations<String, String> setOps;

    @InjectMocks
    RedisSeenStdIdReaderAdapter adapter;

    @Test
    void getSeenStdIds는_규칙대로_key를_만들고_set_members를_조회한다() {
        // given
        when(envProvider.envKey()).thenReturn("DEV");
        when(redisStringTemplate.opsForSet()).thenReturn(setOps);

        // 실제 Redis members 결과를 흉내
        when(setOps.members(anyString())).thenReturn(Set.of("x", "y"));

        // when
        Set<String> result = adapter.getSeenStdIds("MyHome", "RSDT", "Seoul");

        // then
        assertThat(result).containsExactlyInAnyOrder("x", "y");

        // key가 어떻게 만들어졌는지 캡쳐해서 검증
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(setOps).members(keyCaptor.capture());

        // norm()이 소문자/trim 처리하니까 예상 키도 소문자
        assertThat(keyCaptor.getValue())
                .isEqualTo("seoulhousing:dev:seen:myhome:rsdt:seoul");

        verifyNoMoreInteractions(setOps);
    }

    @Test
    void redis가_null을_주면_emptySet을_반환한다() {
        // given
        when(envProvider.envKey()).thenReturn("dev");
        when(redisStringTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(anyString())).thenReturn(null);

        // when
        Set<String> result = adapter.getSeenStdIds("myhome", "rsdt", "seoul");

        // then
        assertThat(result).isEmpty();
    }
}
