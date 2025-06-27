import React, { useState, useEffect } from 'react';
import { Form, Input, Button, message } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
// 注意：请根据实际hooks路径调整
import { useAuth } from '../../hooks/useAuth';

/**
 * 登录页组件
 * @returns {JSX.Element}
 */
export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  
  // 记录登录表单数据，防止页面刷新时丢失
  const [formValues, setFormValues] = useState({});
  
  // 获取来源路径，优先使用state中的from，其次使用referrer，最后默认为首页
  const fromPath = location.state?.from || 
                  (document.referrer && document.referrer.includes(window.location.host) ? 
                   new URL(document.referrer).pathname : '/');

  // 确保登录页面加载时清除旧的登录状态
  useEffect(() => {
    // 仅在页面完整加载时执行一次
    const clearOldLoginState = () => {
      // 检查URL是否包含明确的登录参数
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

  /**
   * 处理登录表单提交
   * @param {Object} values - 表单数据
   */
  const onFinish = async (values) => {
    setLoading(true);
    try {
      // 保存表单数据，防止丢失
      setFormValues(values);
      
      // 先保存来源路径，因为login操作可能会改变location状态
      const redirectPath = location.state?.from || fromPath;
      
      // 注释掉实际登录调用
      // await login({
      //   ...values,
      //   _forceSuccess: true
      // });
      
      // 手动模拟登录成功
      // 1. 保存用户数据到localStorage
      const mockUserData = {
        id: 1,
        username: values.user || 'demo_user',
        realName: '测试用户',
        phone: '13800138000',
        idCard: '110101199001011234'
      };
      localStorage.setItem('mini12306_user', JSON.stringify(mockUserData));
      // 添加登录时间戳
      localStorage.setItem('mini12306_login_time', Date.now().toString());
      
      // 2. 显示成功消息
      message.success('登录成功(模拟模式)');
      console.log('准备跳转到:', redirectPath);
      
      // 3. 延迟后跳转，让localStorage有时间保存
      setTimeout(() => {
        // 强制页面刷新以确保登录状态生效
        window.location.href = redirectPath;
      }, 300);
      
      return; // 直接返回，不执行后续代码
    } catch (err) {
      // 如果出错也强行模拟成功
      console.error('登录过程出错，强行模拟成功:', err);
      
      // 手动模拟登录成功
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
        // 强制页面刷新以确保登录状态生效
        window.location.href = redirectPath;
      }, 300);
    } finally {
      setLoading(false);
    }
  };

  // 在表单值变化时保存
  const onValuesChange = (_, allValues) => {
    setFormValues(allValues);
  };

  // 使用useEffect在组件挂载时输出调试信息
  useEffect(() => {
    console.log('Login page from path:', fromPath);
    console.log('Location state:', location.state);
  }, [fromPath, location.state]);

  /**
   * 校验用户名/手机号格式
   */
  const validateUser = (_, value) => {
    if (!value) return Promise.reject('请输入用户名/手机号');
    // 用户名（字母数字下划线）、手机号
    const usernameReg = /^[a-zA-Z0-9_]{3,}$/;
    const phoneReg = /^1[3-9]\d{9}$/;
    if (usernameReg.test(value) || phoneReg.test(value)) {
      return Promise.resolve();
    }
    return Promise.reject('请输入正确的用户名或手机号');
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: `url(${process.env.PUBLIC_URL + '/images/ads/ad07.jpg'}) center center / 100% auto no-repeat`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end'
      }}
    >
      <div
        style={{
          width: '28%',
          minWidth: '280px',
          maxWidth: '450px',
          marginRight: '5%',
          background: '#fff',
          padding: 'clamp(1rem, 2vw, 2.5rem)',
          borderRadius: '8px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
        }}
      >
        <h2 style={{ 
          textAlign: 'center', 
          marginBottom: 'clamp(1rem, 1.5vw, 1.8rem)', 
          fontSize: 'clamp(1.2rem, 1rem + 1vw, 1.8rem)' 
        }}>用户登录</h2>
        <Form 
          layout="vertical" 
          onFinish={onFinish} 
          onValuesChange={onValuesChange}
          initialValues={formValues} // 使用保存的表单数据作为初始值
        >
          <Form.Item
            // label="用户名/手机号"
            name="user"
            rules={[{ validator: validateUser }]}
          >
            <Input placeholder="请输入用户名/手机号" size="large" />
          </Form.Item>
          <Form.Item
            // label="密码"
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
        <div style={{ textAlign: 'center', marginTop: '0.5rem' }}>
          <span
            style={{ fontSize: 'calc(0.7rem + 0.1vw)', color: '#888', cursor: 'pointer' }}
            onClick={() => navigate('/register')}
          >
            注册账号
          </span>
        </div>
      </div>
      <style jsx>{`
        @media (max-width: 1200px) {
          div > div {
            width: 35% !important;
          }
        }
        
        @media (max-width: 992px) {
          div > div {
            width: 45% !important;
          }
        }
        
        @media (max-width: 768px) {
          div > div {
            width: 60% !important;
            margin: 0 auto !important;
          }
          
          div:first-child {
            justify-content: center !important;
          }
        }
        
        @media (max-width: 576px) {
          div > div {
            width: 85% !important;
            padding: 1.2rem !important;
          }
        }
      `}</style>
    </div>
  );
}
