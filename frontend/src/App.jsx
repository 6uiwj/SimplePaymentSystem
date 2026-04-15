import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import PaymentForm from './components/payment/PaymentForm';
import PaymentResult from './pages/PaymentResult';
import './App.css';

/**
 * App 컴포넌트
 * 
 * 역할:
 * - 라우팅 설정
 * - 전체 애플리케이션 구조
 * 
 * 라우트:
 * - / : 결제 폼
 * - /payment/result/:paymentKey : 결제 결과
 */
function App() {
  return (
    <Router>
      <div className="app">
        <header className="app-header">
          <h1>모바일 결제 시스템</h1>
        </header>

        <main className="app-main">
          <Routes>
            <Route path="/" element={<PaymentForm />} />
            <Route path="/payment/result/:paymentKey" element={<PaymentResult />} />
          </Routes>
        </main>

        <footer className="app-footer">
          <p>&copy; 2025 Payment System. All rights reserved.</p>
        </footer>
      </div>
    </Router>
  );
}

export default App;
