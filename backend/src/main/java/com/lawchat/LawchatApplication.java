package com.lawchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 애플리케이션 진입점.
 *
 * @SpringBootApplication 은 아래 3개를 합친 것이다.
 *  - @Configuration        : 이 클래스 자체가 설정 클래스
 *  - @EnableAutoConfiguration : classpath 의 라이브러리를 보고 필요한 빈을 자동 구성
 *  - @ComponentScan        : 이 클래스가 위치한 패키지(com.lawchat) 이하를 스캔해 빈 등록
 *
 * 그래서 이 파일은 반드시 최상위 패키지(com.lawchat)에 있어야
 * domain / global 하위의 @Service, @Repository, @Component 가 모두 인식된다.
 */
@SpringBootApplication
// @ConfigurationProperties 가 붙은 클래스(KakaoOAuthProperties)를 찾아 빈으로 등록해 준다.
// 이게 없으면 카카오 설정값이 주입되지 않아 NullPointerException 이 난다.
@ConfigurationPropertiesScan
// @Scheduled 배치를 활성화한다. 이게 없으면 탈퇴 회원 익명화 배치가
// 에러도 없이 조용히 실행되지 않는다.
@EnableScheduling
public class LawChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(LawChatApplication.class, args);
    }
}
