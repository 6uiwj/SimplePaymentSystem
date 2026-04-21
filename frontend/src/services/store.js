import { create } from "zustand";

/**
 * Payment Store - Zustand
 *
 * 역할:
 * - 전역 결제 상태 관리
 * - 컴포넌트 간 상태 공유
 *
 * 이유:
 * - Redux/Context API보다 간단 (모던 접근)
 * - 필요한 상태만 구독 (성능 최적화)
 * - Boilerplate 코드 최소화
 *
 * 상태:
 * - paymentKey: 현재 결제 고유 키
 * - paymentStatus: READY, REQUESTED, SUCCESS, FAIL
 * - error: 에러 메시지
 * - loading: 로딩 상태
 */
export const usePaymentStore = create((set) => ({
  // State
  paymentKey: null,
  paymentStatus: "READY",
  paymentInfo: null,
  error: null,
  loading: false,
  retryCount: 0,

  // Actions
  setPaymentKey: (key) => set({ paymentKey: key }),
  setPaymentStatus: (status) => set({ paymentStatus: status }),
  setPaymentInfo: (info) => set({ paymentInfo: info }),
  setError: (error) => set({ error }),
  setLoading: (loading) => set({ loading }),
  setRetryCount: (count) => set({ retryCount: count }),

  // 결제 초기화
  initializePayment: (key, status, info) =>
    set({
      paymentKey: key,
      paymentStatus: status,
      paymentInfo: info,
      error: null,
      loading: false,
    }),

  // 결제 상태 업데이트
  updatePaymentStatus: (status, info = null) =>
    set({
      paymentStatus: status,
      paymentInfo: info || undefined,
    }),

  // 에러 설정
  setPaymentError: (error) =>
    set({
      error,
      paymentStatus: "FAIL",
    }),

  // 초기화
  resetPaymentState: () =>
    set({
      paymentKey: null,
      paymentStatus: "READY",
      paymentInfo: null,
      error: null,
      loading: false,
      retryCount: 0,
    }),
}));

/**
 * UI Store - 모바일 UI 상태
 */
export const useUIStore = create((set) => ({
  // State
  showPaymentModal: false,
  showStatusModal: false,
  notification: null,

  // Actions
  openPaymentModal: () => set({ showPaymentModal: true }),
  closePaymentModal: () => set({ showPaymentModal: false }),
  openStatusModal: () => set({ showStatusModal: true }),
  closeStatusModal: () => set({ showStatusModal: false }),

  // 알림 표시 (3초 자동 닫힘)
  showNotification: (message, type = "info") => {
    set({ notification: { message, type } });
    setTimeout(() => set({ notification: null }), 3000);
  },
}));
