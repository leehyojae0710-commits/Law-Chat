package com.lawchat.infra.lawapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class LawOpenApiConfig {

    @Bean
    public RestClient lawOpenApiRestClient(
            @Value("${law-api.base-url:https://www.law.go.kr}") String baseUrl,
            @Value("${law-api.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${law-api.read-timeout-ms:10000}") long readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}