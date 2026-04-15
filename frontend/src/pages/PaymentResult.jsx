import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { paymentApi } from '../services/api';
import { usePaymentStore, useUIStore } from '../services/store';
import '../styles/payment-result.css';

/**
 * PaymentResult 컴포넌트
 * 
 * 역할:
 * - 결제 결과 표시
 * - 결제 상태 조회 및 모니터링
 * - 재시도 옵션 제공
 * 
 * 흐름:
 * 1. 결제 완료 후 이 페이지로 이동
 * 2. 결제 상태 주기적으로 조회
 * 3. 상태에 따라 UI 업데이트
 * 4. 실패 시 재시도 버튼 제공
 */
const PaymentResult = () => {
  const { paymentKey } = useParams();
  const navigate = useNavigate();
  
  const [paymentStatus, setPaymentStatus] = useState(null);
  const [paymentDetails, setPaymentDetails] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pollCount, setPollCount] = useState(0);
  const { showNotification } = useUIStore();

  useEffect(() => {
    if (!paymentKey) {
      navigate('/');
      return;
    }

    // 결제 상태 조회
    const checkPaymentStatus = async () => {
      try {
        const response = await paymentApi.getPaymentStatus(paymentKey);
        
        if (response.success) {
          const status = response.data;
          setPaymentStatus(status.status);
          setPaymentDetails(status);
          
          // 최종 상태에 도달하면 폴링 종료
          if (['SUCCESS', 'FAIL'].includes(status.status)) {
            setIsLoading(false);
            return;
          }
        }
      } catch (error) {
        console.error('결제 상태 조회 오류:', error);
      }

      setPollCount((prev) => prev + 1);
    };

    // 초기 조회
    checkPaymentStatus();

    // 주기적 폴링 (3초마다, 최대 20회 = 1분)
    const interval = setInterval(() => {
      if (pollCount < 20) {
        checkPaymentStatus();
      } else {
        setIsLoading(false);
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [paymentKey, pollCount, navigate]);

  const handleRetry = async () => {
    // 재시도 로직
    showNotification('재시도 페이지로 이동합니다.', 'info');
    navigate(`/payment/retry/${paymentKey}`);
  };

  const handleGoHome = () => {
    navigate('/');
  };

  if (isLoading && !paymentDetails) {
    return (
      <div className="payment-result loading">
        <div className="spinner"></div>
        <p>결제 상태를 확인 중입니다...</p>
      </div>
    );
  }

  const isSuccess = paymentStatus === 'SUCCESS';
  const isFailed = paymentStatus === 'FAIL';

  return (
    <div className={`payment-result ${paymentStatus?.toLowerCase()}`}>
      <div className="result-card">
        {isSuccess ? (
          <>
            <div className="result-icon success">✓</div>
            <h1>결제 완료</h1>
            <p className="result-message">
              결제가 정상적으로 처리되었습니다.
            </p>
          </>
        ) : isFailed ? (
          <>
            <div className="result-icon failed">✗</div>
            <h1>결제 실패</h1>
            <p className="result-message">
              {paymentDetails?.failureReason || '결제 처리 중 오류가 발생했습니다.'}
            </p>
          </>
        ) : (
          <>
            <div className="result-icon pending">⏱️</div>
            <h1>결제 진행 중</h1>
            <p className="result-message">
              결제가 진행 중입니다. 잠시 기다려주세요.
            </p>
          </>
        )}

        {paymentDetails && (
          <div className="payment-details">
            <div className="detail-row">
              <span className="label">결제 키:</span>
              <span className="value">{paymentDetails.paymentKey}</span>
            </div>
            <div className="detail-row">
              <span className="label">금액:</span>
              <span className="value">{paymentDetails.amount?.toLocaleString()}원</span>
            </div>
            <div className="detail-row">
              <span className="label">상태:</span>
              <span className={`status-badge ${paymentStatus?.toLowerCase()}`}>
                {paymentStatus}
              </span>
            </div>
            <div className="detail-row">
              <span className="label">시간:</span>
              <span className="value">
                {new Date(paymentDetails.createdAt).toLocaleString('ko-KR')}
              </span>
            </div>
          </div>
        )}

        <div className="result-actions">
          {isFailed && (
            <button className="btn btn-primary" onClick={handleRetry}>
              재시도
            </button>
          )}
          <button className="btn btn-secondary" onClick={handleGoHome}>
            {isSuccess ? '쇼핑 계속하기' : '돌아가기'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentResult;
