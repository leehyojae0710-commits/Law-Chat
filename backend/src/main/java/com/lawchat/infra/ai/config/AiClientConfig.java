package com.lawchat.infra.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * legal_chatbot_ai(FastAPI, AWS EC2) 호출용 RestClient 설정.
 *
 * application.yml 에 아래 값들을 채워야 한다:
 *
 *   ai:
 *     legal-chatbot:
 *       base-url: http://<EC2 퍼블릭IP 또는 도메인>:8000   # Elastic IP 권장 (재부팅 시 IP 변경 방지)
 *       connect-timeout-ms: 5000
 *       read-timeout-ms: 60000     # LLM 생성은 수초~수십초 걸릴 수 있어 넉넉하게
 *
 * 주의: main.py의 lifespan()이 서버 기동 시 8B 모델 2종을 로드하는 동안(콜드스타트)
 * /chat/auto가 아예 응답을 못 하거나 503을 반환할 수 있다.
 * LegalChatbotClient 쪽에서 503/커넥션 실패에 대한 재시도 정책을 함께 고려할 것.
 */
@Configuration
public class AiClientConfig {

    @Bean
    public RestClient legalChatbotRestClient(
            @Value("${ai.legal-chatbot.base-url}") String baseUrl,
            @Value("${ai.legal-chatbot.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${ai.legal-chatbot.read-timeout-ms:60000}") long readTimeoutMs
    ) {
        // ★ 기존 코드는 org.springframework.boot.web.client.ClientHttpRequestFactorySettings 와
        //   org.springframework.http.client.ClientHttpRequestFactoryBuilder 를 import 했는데,
        //   두 클래스 모두 그 패키지에 없다(Boot 3.4 이후 org.springframework.boot.http.client 로 이동,
        //   Boot 4.1 에서 다시 시그니처가 바뀜). 그래서 빨간줄이 떴다.
        //   버전에 흔들리지 않도록 RestClientConfig 와 동일하게 순수 Spring Framework API 로 통일한다.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
