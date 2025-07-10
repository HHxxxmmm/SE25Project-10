import React, { useState } from 'react';
import { Form, Input, Button, Checkbox, message, Modal } from 'antd';
import { useAuth } from '../../hooks/useAuth';
import './style.css';

const validateRules = {
  password: [
    { required: true, message: '请输入密码' },
    { pattern: /^[A-Za-z0-9!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]{6,20}$/, message: '6-20位，可含字母、数字、符号' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号' },
    { pattern: /^[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/, message: '请输入正确身份证号' }
  ],
  phone: [
    { required: true, message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确手机号' }
  ],
  email: [
    { required: false },
    { type: 'email', message: '请输入有效的邮箱地址' }
  ]
};

export default function RegisterPage({ visible, onCancel }) {
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const [form] = Form.useForm();

  const [formErrors, setFormErrors] = useState({});
  
  const onFinish = async (values) => {
    setLoading(true);
    // 清除之前的错误
    setFormErrors({});
    
    try {
      // 构造注册数据
      const registerData = {
        realName: values.realName,
        phoneNumber: values.phone,
        password: values.password,
        email: values.email || '',
        idCardNumber: values.idCard
      };
      
      await register(registerData);
      message.success('注册成功');
      form.resetFields();
      onCancel?.();
    } catch (error) {
      // 处理不同类型的错误
      const errorMsg = error.response?.data?.message || error.message;
      
      if (errorMsg.includes('手机号已被注册')) {
        setFormErrors({
          phone: '该手机号已被注册'
        });
        form.setFields([
          {
            name: 'phone',
            errors: ['该手机号已被注册']
          }
        ]);
      } else if (errorMsg.includes('邮箱已被注册')) {
        setFormErrors({
          email: '该邮箱已被注册'
        });
        form.setFields([
          {
            name: 'email',
            errors: ['该邮箱已被注册']
          }
        ]);
      } else if (errorMsg.includes('身份证号码不合法')) {
        setFormErrors({
          idCard: '身份证号码不合法'
        });
        form.setFields([
          {
            name: 'idCard',
            errors: ['身份证号码不合法']
          }
        ]);
      } else {
        // 其他错误显示为全局消息
        message.error(errorMsg || '注册失败');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
      <Modal
          title={<div className="modalTitle">用户注册</div>}
          open={visible}
          onCancel={onCancel}
          footer={null}
          destroyOnClose
          centered
          width={500}
      >
        <Form form={form} onFinish={onFinish} layout="horizontal" labelCol={{ span: 6 }}>
          <Form.Item label="手机号" name="phone" rules={validateRules.phone}>
            <Input placeholder="请输入11位手机号码" />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={validateRules.password}>
            <Input.Password placeholder="6-20位密码" />
          </Form.Item>
          <Form.Item
              label="确认密码"
              name="confirmPassword"
              dependencies={['password']}
              rules={[
                { required: true, message: '请确认密码' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    return value && value === getFieldValue('password')
                        ? Promise.resolve()
                        : Promise.reject('两次密码不一致');
                  },
                }),
              ]}
          >
            <Input.Password placeholder="请再次输入密码" />
          </Form.Item>
          <Form.Item label="姓名" name="realName" rules={[{ required: true, message: '请输入真实姓名' }]}>
            <Input placeholder="请输入真实姓名" />
          </Form.Item>
          <Form.Item label="身份证号" name="idCard" rules={validateRules.idCard}>
            <Input placeholder="请输入18位身份证号" />
          </Form.Item>
          <Form.Item 
            label="邮箱" 
            name="email" 
            rules={[
              { required: false },
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input placeholder="选填项，请输入邮箱地址" />
          </Form.Item>
          <Form.Item
              name="agreement"
              valuePropName="checked"
              rules={[{ validator: (_, v) => v ? Promise.resolve() : Promise.reject('请同意协议') }]}
              wrapperCol={{ span: 24 }}
              className="form-item-center"
          >
            <Checkbox>我已阅读并同意相关协议</Checkbox>
          </Form.Item>
          <Form.Item wrapperCol={{ span: 24 }} className="form-item-center">
            <Button 
              type="primary" 
              htmlType="submit" 
              loading={loading}
              className="register-button"
            >
              立即注册
            </Button>
          </Form.Item>
        </Form>
    </Modal>
  );
}