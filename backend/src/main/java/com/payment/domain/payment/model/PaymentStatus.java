package com.payment.domain.payment.model;

/**
 * 결제 상태 Enum
 * 
 * DDD 원칙에 따라 비즈니스 규칙을 엔티티에 포함합니다.
 * READY -> REQUESTED -> SUCCESS/FAIL
 * 
 * - READY: 결제 요청 준비 상태, 아직 외부 API 호출 전
 * - REQUESTED: 결제 요청을 외부 API에 보낸 상태
 * - SUCCESS: 결제 완료 상태
 * - FAIL: 결제 실패 상태
 */
public enum PaymentStatus {
    READY("준비"),
    REQUESTED("요청됨"),
    SUCCESS("성공"),
    FAIL("실패");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 다음 상태로 전환할 수 있는지 확인
     * DDD의 상태 관리: 유효한 상태 전환만 허용
     */
    public boolean canTransitionTo(PaymentStatus nextStatus) {
        return switch (this) {
            case READY -> nextStatus == REQUESTED;
            case REQUESTED -> nextStatus == SUCCESS || nextStatus == FAIL || nextStatus == READY;  // 재시도
            case SUCCESS, FAIL -> false;  // 성공/실패 상태에서는 전환 불가
        };
    }
}
