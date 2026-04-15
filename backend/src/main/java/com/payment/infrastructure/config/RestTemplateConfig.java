package com.payment.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 설정
 * 
 * 역할:
 * - 외부 API(토스페이먼츠) 호출을 위한 HTTP 클라이언트 설정
 * - 타임아웃, 연결 풀 등 설정
 * 
 * 이유:
 * - RestTemplate은 Spring에서 권장하는 HTTP 클라이언트
 * - 커스텀 설정으로 안정성 향상
 * - 외부 API 응답 시간 초과 처리
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * RestTemplate 빈 생성
     * 
     * 설정:
     * - Connection Timeout: 5초 (연결 수립 시간)
     * - Read Timeout: 10초 (응답 대기 시간)
     * - Buffering: 요청/응답 본문 캐싱 (로깅용)
     * - Content Negotiation: JSON 처리
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .requestFactory(this::clientHttpRequestFactory)
            .messageConverters(new MappingJackson2HttpMessageConverter())
            .build();
    }
    
    /**
     * HTTP 요청/응답 팩토리
     * Buffering 활성화: 디버깅 및 로깅 목적
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);      // 5초
        factory.setReadTimeout(10000);        // 10초
        factory.setBufferRequestBody(true);   // 요청 body 버퍼링
        
        // Buffering 래퍼 적용
        return new BufferingClientHttpRequestFactory(factory);
    }
    
    /**
     * ObjectMapper 빈
     * JSON 직렬화/역직렬화에 사용
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
