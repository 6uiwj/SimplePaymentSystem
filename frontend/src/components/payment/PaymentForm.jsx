import React, { useState } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { paymentApi } from '../services/api';
import { usePaymentStore, useUIStore } from '../services/store';
import '../styles/payment.css';

/**
 * PaymentForm 컴포넌트
 * 
 * 역할:
 * - 결제 정보 입력 폼
 * - 결제 프로세스 시작
 * 
 * 흐름:
 * 1. 사용자가 상품 선택 및 정보 입력
 * 2. "결제하기" 버튼 클릭
 * 3. 서버에 결제 요청 (idempotencyKey 포함)
 * 4. 서버가 결제 키 반환
 * 5. 결제 승인 페이지로 이동
 */
const PaymentForm = () => {
  const [formData, setFormData] = useState({
    orderId: `order-${Date.now()}`,
    amount: 10000,
    productName: '상품명',
    customerEmail: 'customer@example.com',
    customerName: '구매자',
    customerPhone: '01012345678',
  });

  const [isLoading, setIsLoading] = useState(false);
  const { setLoading, initializePayment, setError } = usePaymentStore();
  const { showNotification } = useUIStore();

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'amount' ? Number(value) : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setLoading(true);

    try {
      // 멱등성 키 생성 (클라이언트에서 생성)
      // 같은 orderId여도 매번 새로운 키 생성 → 중복 결제 방지
      const idempotencyKey = uuidv4();

      const response = await paymentApi.initializePayment({
        ...formData,
        idempotencyKey,
      });

      if (response.success) {
        const { paymentKey } = response.data;
        
        // 상태 저장
        initializePayment(paymentKey, 'READY', response.data);
        
        showNotification('결제 준비 완료. 결제방법을 선택해주세요.', 'success');
        
        // 결제 페이지로 이동 (실제로는 토스페이먼츠 결제 창 열림)
        // window.location.href = response.data.redirectUrl;
        
      } else {
        throw new Error(response.message || '결제 준비 실패');
      }
    } catch (error) {
      console.error('결제 준비 오류:', error);
      const errorMsg = error?.message || '결제 준비 중 오류가 발생했습니다';
      setError(errorMsg);
      showNotification(errorMsg, 'error');
    } finally {
      setIsLoading(false);
      setLoading(false);
    }
  };

  return (
    <form className="payment-form" onSubmit={handleSubmit}>
      <h2>결제 정보</h2>

      <div className="form-group">
        <label htmlFor="productName">상품명</label>
        <input
          type="text"
          id="productName"
          name="productName"
          value={formData.productName}
          onChange={handleInputChange}
          required
        />
      </div>

      <div className="form-group">
        <label htmlFor="amount">금액 (원)</label>
        <input
          type="number"
          id="amount"
          name="amount"
          value={formData.amount}
          onChange={handleInputChange}
          min="1000"
          required
        />
        <span className="amount-display">{formData.amount.toLocaleString()}원</span>
      </div>

      <div className="form-section">
        <h3>구매자 정보</h3>
        
        <div className="form-group">
          <label htmlFor="customerName">이름</label>
          <input
            type="text"
            id="customerName"
            name="customerName"
            value={formData.customerName}
            onChange={handleInputChange}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="customerEmail">이메일</label>
          <input
            type="email"
            id="customerEmail"
            name="customerEmail"
            value={formData.customerEmail}
            onChange={handleInputChange}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="customerPhone">전화번호</label>
          <input
            type="tel"
            id="customerPhone"
            name="customerPhone"
            value={formData.customerPhone}
            onChange={handleInputChange}
          />
        </div>
      </div>

      <button 
        type="submit" 
        className="btn btn-primary"
        disabled={isLoading}
      >
        {isLoading ? '처리중...' : '결제하기'}
      </button>
    </form>
  );
};

export default PaymentForm;
