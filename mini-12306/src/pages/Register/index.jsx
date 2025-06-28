import React, { useState } from 'react';
import { Form, Input, Button, Checkbox, message, Modal } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import './style.css';

// 用户名校验
const validateUsername = (_, value) => {
  if (!value) return Promise.reject('请输入用户名');
  const reg = /^[a-zA-Z][a-zA-Z0-9_]{5,29}$/;
  return reg.test(value)
      ? Promise.resolve()
      : Promise.reject('用户名需6-30位，字母开头，可包含字母、数字、下划线');
};

// 密码校验
const validatePassword = (_, value) => {
  if (!value) return Promise.reject('请输入密码');
  const reg = /^[A-Za-z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]{6,20}$/;
  return reg.test(value)
      ? Promise.resolve()
      : Promise.reject('密码需6-20位，可包含字母、数字、符号');
};

// 确认密码校验
const validateConfirmPassword = ({ getFieldValue }) => ({
  validator(_, value) {
    if (!value) return Promise.reject('请确认密码');
    return value === getFieldValue('password')
        ? Promise.resolve()
        : Promise.reject('两次输入的密码不一致');
  },
});

// 身份证号校验
const validateIdCard = (_, value) => {
  if (!value) return Promise.reject('请输入身份证号');
  const reg = /^[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;
  return reg.test(value)
      ? Promise.resolve()
      : Promise.reject('请输入正确的身份证号');
};

// 手机号校验
const validatePhone = (_, value) => {
  if (!value) return Promise.reject('请输入手机号');
  const reg = /^1[3-9]\d{9}$/;
  return reg.test(value)
      ? Promise.resolve()
      : Promise.reject('请输入正确的手机号');
};

export default function RegisterPage({ visible = true, onCancel }) {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { register } = useAuth();

  const onFinish = async (values) => {
    setLoading(true);
    try {
      await register(values);
      message.success('注册成功，请登录');
      if (onCancel) onCancel(); // 关闭弹窗
      // 跳转登录页面，保持原有跳转逻辑
      const referrer = document.referrer;
      const isFromLoginPage = referrer && referrer.includes('/login');
      const from = location.state?.from;
      if (isFromLoginPage || from) {
        navigate('/login', { state: { from }, replace: true });
      } else {
        navigate('/login', { state: { from: '/' }, replace: true });
      }
    } catch (err) {
      message.error('注册失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
      <Modal
          title="用户注册"
          visible={visible}
          onCancel={onCancel}
          footer={null}
          destroyOnClose
          maskClosable={false}
          centered
      >
        <Form
            layout="horizontal"
            labelCol={{ span: 6 }}
            wrapperCol={{ span: 18 }}
            onFinish={onFinish}
            style={{ maxWidth: 480, margin: '0 auto' }}
        >
          <Form.Item
              label="用户名："
              name="username"
              rules={[
                { required: true, message: '请输入用户名' },
                { validator: validateUsername },
              ]}
          >
            <Input placeholder="用户名（6-30位，字母开头）" />
          </Form.Item>
          <Form.Item
              label="登录密码："
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { validator: validatePassword },
              ]}
          >
            <Input.Password placeholder="密码（6-20位，字母/数字/符号）" />
          </Form.Item>
          <Form.Item
              label="确认密码："
              name="confirmPassword"
              dependencies={['password']}
              rules={[
                { required: true, message: '请确认密码' },
                validateConfirmPassword,
              ]}
          >
            <Input.Password placeholder="请再次输入密码" />
          </Form.Item>
          <Form.Item
              label="姓名："
              name="realName"
              rules={[{ required: true, message: '请输入姓名' }]}
          >
            <Input placeholder="姓名" />
          </Form.Item>
          <Form.Item
              label="证件号码："
              name="idCard"
              rules={[
                { required: true, message: '请输入身份证号' },
                { validator: validateIdCard },
              ]}
          >
            <Input placeholder="身份证号" />
          </Form.Item>
          <Form.Item
              label="手机号码："
              name="phone"
              rules={[
                { required: true, message: '请输入手机号' },
                { validator: validatePhone },
              ]}
          >
            <Input placeholder="手机号" />
          </Form.Item>
          <Form.Item
              wrapperCol={{ span: 24 }}
              style={{ textAlign: 'center' }}
              name="agreement"
              valuePropName="checked"
              rules={[
                {
                  validator: (_, value) =>
                      value ? Promise.resolve() : Promise.reject('请阅读并同意相关协议'),
                },
              ]}
          >
            <Checkbox>
              我已阅读并同意遵守
              <a href="#" target="_blank" rel="noopener noreferrer">
                《服务条款》
              </a>
              、
              <a href="#" target="_blank" rel="noopener noreferrer">
                《隐私权政策》
              </a>
            </Checkbox>
          </Form.Item>
          <Form.Item
              wrapperCol={{ span: 24 }}
              style={{ textAlign: 'center' }}
          >
            <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                style={{ width: 200 }}
            >
              立即注册
            </Button>
          </Form.Item>
        </Form>
      </Modal>
  );
}