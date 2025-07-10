import React, { useState } from 'react';
import { Form, Input, Button, Checkbox, message, Modal } from 'antd';
import { useAuth } from '../../hooks/useAuth';
import './style.css';

const validateRules = {
  username: [
    { required: true, message: '请输入用户名' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{5,29}$/, message: '6-30位字母开头，可含数字、下划线' }
  ],
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
  ]
};

export default function RegisterPage({ visible, onCancel }) {
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const [form] = Form.useForm();

  const onFinish = async (values) => {
    setLoading(true);
    try {
      await register(values);
      message.success('注册成功');
      form.resetFields();
      onCancel?.();
    } catch (error) {
      message.error(error.message || '注册失败');
    } finally {
      setLoading(false);
    }
  };

  return (
      <Modal
          title="用户注册"
          open={visible}
          onCancel={onCancel}
          footer={null}
          destroyOnClose
          centered
      >
        <Form form={form} onFinish={onFinish} layout="horizontal" labelCol={{ span: 6 }}>
          <Form.Item label="用户名" name="username" rules={validateRules.username}>
            <Input placeholder="6-30位字母开头" />
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
            <Input.Password />
          </Form.Item>
          <Form.Item label="姓名" name="realName" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="身份证号" name="idCard" rules={validateRules.idCard}>
            <Input />
          </Form.Item>
          <Form.Item label="手机号" name="phone" rules={validateRules.phone}>
            <Input />
          </Form.Item>
          <Form.Item
              name="agreement"
              valuePropName="checked"
              rules={[{ validator: (_, v) => v ? Promise.resolve() : Promise.reject('请同意协议') }]}
              wrapperCol={{ offset: 6 }}
          >
            <Checkbox>我已阅读并同意相关协议</Checkbox>
          </Form.Item>
          <Form.Item wrapperCol={{ offset: 6 }}>
            <Button type="primary" htmlType="submit" loading={loading} block>
              立即注册
            </Button>
          </Form.Item>
        </Form>
      </Modal>
  );
}