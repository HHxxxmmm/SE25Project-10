import React, { useState, useEffect } from 'react';
import { Form, Input, Button, message } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import RegisterPage from '../Register';
import './style.css';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const [sessionTimedOut, setSessionTimedOut] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated } = useAuth();

  const fromPath = location.state?.from ||
      (document.referrer?.includes(window.location.host) ?
          new URL(document.referrer).pathname : '/');

  useEffect(() => {
    // 检查是否因会话超时被重定向到登录页面
    const wasTimedOut = sessionStorage.getItem('sessionTimedOut') === 'true';
    if (wasTimedOut) {
      setSessionTimedOut(true);
      // 清除会话超时标志
      sessionStorage.removeItem('sessionTimedOut');
    }
    
    if (isAuthenticated) {
      navigate(fromPath, { replace: true });
    }
  }, [isAuthenticated, navigate, fromPath]);

  const [phoneError, setPhoneError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  
  const onFinish = async (values) => {
    setLoading(true);
    // 清除之前的错误信息
    setPhoneError('');
    setPasswordError('');
    
    try {
      await login({
        phoneNumber: values.phoneNumber,
        password: values.password
      });
      message.success('登录成功');
    } catch (error) {
      // 根据错误类型显示不同错误信息
      const errorMsg = error.response?.data?.message || error.message;
      
      if (errorMsg.includes('用户不存在')) {
        setPhoneError('手机号未注册');
      } else if (errorMsg.includes('密码错误')) {
        setPasswordError('密码错误');
      }
    } finally {
      setLoading(false);
    }
  };

  const validatePhoneNumber = (_, value) => {
    if (!value) return Promise.reject('请输入手机号');
    const phoneReg = /^1[3-9]\d{9}$/;
    return phoneReg.test(value)
        ? Promise.resolve()
        : Promise.reject('请输入正确的手机号');
  };

  return (
      <>
        <div className="login-page"
             style={{ background: `url(${process.env.PUBLIC_URL}/images/ads/ad07.jpg) center/contain no-repeat` }}>
          <div className="login-container">
            <h2>用户登录</h2>
            {sessionTimedOut && (
              <div style={{ color: 'red', marginBottom: '15px', textAlign: 'center' }}>
                您的会话已超时，请重新登录
              </div>
            )}
            <Form onFinish={onFinish} layout="vertical">
              <Form.Item 
                name="phoneNumber" 
                rules={[{ validator: validatePhoneNumber }]}
                validateStatus={phoneError ? "error" : ""}
                help={phoneError}
              >
                <Input placeholder="手机号" size="large" onChange={() => setPhoneError('')} />
              </Form.Item>
              <Form.Item 
                name="password" 
                rules={[{ required: true, message: '请输入密码' }]}
                validateStatus={passwordError ? "error" : ""}
                help={passwordError}
              >
                <Input.Password placeholder="密码" size="large" onChange={() => setPasswordError('')} />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" loading={loading} block size="large">
                  立即登录
                </Button>
              </Form.Item>
            </Form>
            <div className="login-register" onClick={() => setShowRegisterModal(true)}>
              注册账号
            </div>
          </div>
        </div>
        <RegisterPage visible={showRegisterModal} onCancel={() => setShowRegisterModal(false)} />
      </>
  );
}