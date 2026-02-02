package com.nutriassistant.nutriassistant_back.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정 클래스
 *
 * 역할:
 * - Spring의 비동기 처리 기능(@Async)을 활성화합니다.
 * - @Async 어노테이션이 붙은 메서드를 별도의 스레드에서 실행합니다.
 *
 * 사용 예시:
 * - 신메뉴 AI 분석 요청 (NewMenuService.requestAnalysisAsync)
 * - 이메일 발송
 * - 푸시 알림 전송
 * - 로그 기록 등 시간이 오래 걸리는 작업
 *
 * 동작 방식:
 * 1. @Async가 붙은 메서드 호출 시 즉시 반환
 * 2. 실제 작업은 별도 스레드에서 비동기로 실행
 * 3. 호출자는 대기하지 않고 다음 로직 진행
 *
 * 주의사항:
 * - @Async 메서드는 반드시 public이어야 함
 * - 같은 클래스 내에서 호출하면 프록시를 거치지 않아 동기로 실행됨
 * - 트랜잭션과 함께 사용 시 주의 필요 (별도 트랜잭션으로 실행됨)
 *
 * 커스터마이징 (필요 시):
 * - ThreadPoolTaskExecutor Bean 등록으로 스레드 풀 설정 가능
 * - 예: 코어 스레드 수, 최대 스레드 수, 큐 크기 등
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // 기본 설정 사용 (SimpleAsyncTaskExecutor)
    // 커스텀 스레드 풀이 필요하면 아래 주석 해제 후 수정

    /*
    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // 기본 스레드 수
        executor.setMaxPoolSize(10);      // 최대 스레드 수
        executor.setQueueCapacity(25);    // 대기 큐 크기
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
    */
}
