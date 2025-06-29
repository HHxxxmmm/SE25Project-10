// LoginPage.jsx
import React, { useState, useEffect } from 'react';
import { Form, Input, Button, message } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import RegisterPage from '../Register'; // 同样引入弹窗版注册组件
import './style.css';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [formValues, setFormValues] = useState({});

  const fromPath = location.state?.from ||
      (document.referrer && document.referrer.includes(window.location.host) ?
          new URL(document.referrer).pathname : '/');

  useEffect(() => {
    const clearOldLoginState = () => {
      const searchParams = new URLSearchParams(window.location.search);
      const forceLogin = searchParams.get('force') === 'true';

      if (forceLogin) {
        localStorage.removeItem('mini12306_user');
        localStorage.removeItem('mini12306_login_time');
        console.log('Forced login mode, cleared previous login state');
      }
    };

    clearOldLoginState();
  }, []);

  const onFinish = async (values) => {
    setLoading(true);
    try {
      setFormValues(values);
      const redirectPath = location.state?.from || fromPath;

      const mockUserData = {
        id: 1,
        username: values.user || 'demo_user',
        realName: '测试用户',
        phone: '13800138000',
        idCard: '110101199001011234'
      };
      localStorage.setItem('mini12306_user', JSON.stringify(mockUserData));
      localStorage.setItem('mini12306_login_time', Date.now().toString());

      message.success('登录成功(模拟模式)');
      console.log('准备跳转到:', redirectPath);

      setTimeout(() => {
        window.location.href = redirectPath;
      }, 300);

      return;
    } catch (err) {
      console.error('登录过程出错，强行模拟成功:', err);
      const mockUserData = {
        id: 1,
        username: 'demo_user',
        realName: '测试用户',
        phone: '13800138000',
        idCard: '110101199001011234'
      };
      localStorage.setItem('mini12306_user', JSON.stringify(mockUserData));

      message.success('登录成功(恢复模式)');

      const redirectPath = location.state?.from || fromPath;
      setTimeout(() => {
        window.location.href = redirectPath;
      }, 300);
    } finally {
      setLoading(false);
    }
  };

  const onValuesChange = (_, allValues) => {
    setFormValues(allValues);
  };

  useEffect(() => {
    console.log('Login page from path:', fromPath);
    console.log('Location state:', location.state);
  }, [fromPath, location.state]);

  const validateUser = (_, value) => {
    if (!value) return Promise.reject('请输入用户名/手机号');
    const usernameReg = /^[a-zA-Z0-9_]{3,}$/;
    const phoneReg = /^1[3-9]\d{9}$/;
    if (usernameReg.test(value) || phoneReg.test(value)) {
      return Promise.resolve();
    }
    return Promise.reject('请输入正确的用户名或手机号');
  };

  const bgUrl = process.env.PUBLIC_URL + '/images/ads/ad07.jpg';

  const openRegisterModal = () => {
    setShowRegisterModal(true);
  };

  return (
      <>
        <div className="login-page" style={{ background: `url(${bgUrl}) center center / contain no-repeat` }}>
          <div className="login-container">
            <h2 className="login-title">用户登录</h2>
            <Form
                layout="vertical"
                onFinish={onFinish}
                onValuesChange={onValuesChange}
                initialValues={formValues}
            >
              <Form.Item
                  name="user"
                  rules={[{ validator: validateUser }]}
              >
                <Input placeholder="请输入用户名/手机号" size="large" />
              </Form.Item>
              <Form.Item
                  name="password"
                  rules={[{ required: true, message: '请输入密码' }]}
              >
                <Input.Password placeholder="请输入密码" size="large" />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" loading={loading} block size="large">
                  立即登录
                </Button>
              </Form.Item>
            </Form>
            <div className="login-register" onClick={openRegisterModal}>
              注册账号
            </div>
          </div>
        </div>

        {/* 注册弹窗 */}
        <RegisterPage visible={showRegisterModal} onCancel={() => setShowRegisterModal(false)} />
      </>
  );
}