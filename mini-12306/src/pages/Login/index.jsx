import React, { useState, useEffect } from 'react';
import { Form, Input, Button, message } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import RegisterPage from '../Register';
import './style.css';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated } = useAuth();

  const fromPath = location.state?.from ||
      (document.referrer?.includes(window.location.host) ?
          new URL(document.referrer).pathname : '/');

  useEffect(() => {
    if (isAuthenticated) {
      navigate(fromPath, { replace: true });
    }
  }, [isAuthenticated, navigate, fromPath]);

  const onFinish = async (values) => {
    setLoading(true);
    try {
      await login({
        username: values.user,
        password: values.password
      });
      message.success('登录成功');
    } catch (error) {
      message.error(error.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  const validateUser = (_, value) => {
    if (!value) return Promise.reject('请输入用户名/手机号');
    const usernameReg = /^[a-zA-Z0-9_]{3,}$/;
    const phoneReg = /^1[3-9]\d{9}$/;
    return (usernameReg.test(value) || phoneReg.test(value))
        ? Promise.resolve()
        : Promise.reject('请输入正确的用户名或手机号');
  };

  return (
      <>
        <div className="login-page"
             style={{ background: `url(${process.env.PUBLIC_URL}/images/ads/ad07.jpg) center/contain no-repeat` }}>
          <div className="login-container">
            <h2>用户登录</h2>
            <Form onFinish={onFinish} layout="vertical">
              <Form.Item name="user" rules={[{ validator: validateUser }]}>
                <Input placeholder="用户名/手机号" size="large" />
              </Form.Item>
              <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                <Input.Password placeholder="密码" size="large" />
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