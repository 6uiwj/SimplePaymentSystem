package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * PaymentSystemApplication - Spring Boot 메인 클래스
 * 
 * 애노테이션 설명:
 * - @SpringBootApplication: Spring Boot 애플리케이션 자동 설정
 * - @EnableCaching: Redis 캐싱 활성화 (@Cacheable, @CacheEvict 등)
 * - @EnableTransactionManagement: 선언적 트랜잭션 관리 활성화 (@Transactional)
 */
@SpringBootApplication
@EnableCaching
@EnableTransactionManagement
public class PaymentSystemApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentSystemApplication.class, args);
    }
}
