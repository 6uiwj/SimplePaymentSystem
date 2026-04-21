import axios from "axios";

/**
 * API 서비스
 *
 * 역할:
 * - 모든 백엔드 API 호출을 중앙화
 * - 에러 처리
 * - 요청/응답 인터셉팅
 *
 * 이유:
 * - API 변경 시 한 곳만 수정
 * - 공통 에러 처리 로직 분리
 * - 타임아웃, 재시도 등 정책 결정
 */

const API_BASE_URL =
  import.meta.env.VITE_API_URL || "http://localhost:8080/api";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

/**
 * 요청 인터셉터
 * - 모든 요청에 상용 헤더 추가 가능
 */
apiClient.interceptors.request.use(
  (config) => {
    // 필요시 토큰 추가: config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error),
);

/**
 * 응답 인터셉터
 * - 공통 에러 처리
 * - 토큰 갱신 등 처리
 */
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // 인증 오류: 로그인 페이지로 이동
      window.location.href = "/login";
    }
    return Promise.reject(error.response?.data || error.message);
  },
);

/**
 * Payment API 호출 모음
 */
export const paymentApi = {
  /**
   * 결제 요청 생성
   * POST /payments
   */
  initializePayment: async (request) => {
    return apiClient.post("/payments", request);
  },

  /**
   * 결제 승인
   * POST /payments/{paymentKey}/approve
   */
  approvePayment: async (paymentKey, paymentMethodKey) => {
    return apiClient.post(`/payments/${paymentKey}/approve`, null, {
      params: { paymentMethodKey },
    });
  },

  /**
   * 결제 상태 조회
   * GET /payments/{paymentKey}
   */
  getPaymentStatus: async (paymentKey) => {
    return apiClient.get(`/payments/${paymentKey}`);
  },

  /**
   * 결제 재시도
   * POST /payments/{paymentKey}/retry
   */
  retryPayment: async (paymentKey, paymentMethodKey) => {
    return apiClient.post(`/payments/${paymentKey}/retry`, null, {
      params: { paymentMethodKey },
    });
  },

  /**
   * 결제 이력 조회
   * GET /payments/{paymentKey}/history
   */
  getPaymentHistory: async (paymentKey) => {
    return apiClient.get(`/payments/${paymentKey}/history`);
  },
};

export default apiClient;
