package com.lawchat.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 외부 API 호출용 RestClient 설정.
 *
 * RestClient 는 Spring 6.1(Boot 3.2)부터 들어온 동기 HTTP 클라이언트로,
 * RestTemplate 의 후속이며 WebClient 스타일의 유연한 API를 제공한다.
 *
 * ★ 타임아웃을 반드시 지정해야 하는 이유
 *   기본값은 "무한 대기"다. 카카오 서버가 느려지면 우리 서버의 톰캣 스레드가
 *   전부 붙잡혀 서비스 전체가 멈추는 장애로 번진다.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient kakaoRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3)); // 연결 수립까지
        factory.setReadTimeout(Duration.ofSeconds(5));    // 응답 수신까지

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
