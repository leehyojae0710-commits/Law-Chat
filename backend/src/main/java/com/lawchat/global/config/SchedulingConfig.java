package com.lawchat.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 어노테이션 활성화.
 * 현재는 infra.lawapi.scheduler.PrecedentSyncScheduler(판례 정기 동기화)에서 사용한다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
