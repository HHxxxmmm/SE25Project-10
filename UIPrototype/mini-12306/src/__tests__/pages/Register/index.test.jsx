// mock window.matchMedia，确保 matches 属性存在
window.matchMedia = function(query) {
  return {
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(), // 旧 API
    removeListener: jest.fn(),
    addEventListener: jest.fn(), // 新 API
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  };
}

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import RegisterPage from '../../../pages/Register/index';

// mock antd message
jest.mock('antd', () => {
  const origin = jest.requireActual('antd');
  return {
    ...origin,
    message: {
      success: jest.fn(),
      error: jest.fn(),
    },
    // 修正 Modal mock，渲染 title
    Modal: ({ title, children, ...props }) => (
      <div data-testid="modal" {...props}>
        {title && <div data-testid="modal-title">{title}</div>}
        {children}
      </div>
    ),
  };
});

// mock useAuth，registerMock 作用域仅在工厂内
jest.mock('../../../hooks/useAuth', () => {
  let registerMock = jest.fn();
  return {
    useAuth: () => ({ register: registerMock }),
    __esModule: true,
    _getRegisterMock: () => registerMock,
    _setRegisterMock: fn => { registerMock = fn; },
  };
});

const defaultProps = {
  visible: true,
  onCancel: jest.fn(),
};

function fillForm({ phone = '13812345678', password = 'abc123!', confirmPassword = 'abc123!', realName = '张三', idCard = '110101199003071234', email = 'test@example.com', agreement = true } = {}) {
  fireEvent.change(screen.getByPlaceholderText('请输入11位手机号码'), { target: { value: phone } });
  fireEvent.change(screen.getByPlaceholderText('6-20位密码'), { target: { value: password } });
  fireEvent.change(screen.getByPlaceholderText('请再次输入密码'), { target: { value: confirmPassword } });
  fireEvent.change(screen.getByPlaceholderText('请输入真实姓名'), { target: { value: realName } });
  fireEvent.change(screen.getByPlaceholderText('请输入18位身份证号'), { target: { value: idCard } });
  fireEvent.change(screen.getByPlaceholderText('选填项，请输入邮箱地址'), { target: { value: email } });
  if (agreement) {
    fireEvent.click(screen.getByText('我已阅读并同意相关协议'));
  }
}

describe('RegisterPage', () => {
  let registerMock;
  beforeEach(() => {
    jest.clearAllMocks();
    // 获取 mock 实例
    registerMock = require('../../../hooks/useAuth')._getRegisterMock();
  });

  it('渲染所有表单项和按钮', () => {
    render(<RegisterPage {...defaultProps} />);
    expect(screen.getByTestId('modal-title')).toHaveTextContent('用户注册');
    expect(screen.getByLabelText('手机号')).toBeInTheDocument();
    expect(screen.getByLabelText('密码')).toBeInTheDocument();
    expect(screen.getByLabelText('确认密码')).toBeInTheDocument();
    expect(screen.getByLabelText('姓名')).toBeInTheDocument();
    expect(screen.getByLabelText('身份证号')).toBeInTheDocument();
    expect(screen.getByLabelText('邮箱')).toBeInTheDocument();
    expect(screen.getByText('我已阅读并同意相关协议')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '立即注册' })).toBeInTheDocument();
  });

  it('表单校验：手机号、密码、姓名、身份证号必填', async () => {
    render(<RegisterPage {...defaultProps} />);
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('请输入手机号')).toBeInTheDocument();
      expect(screen.getByText('请输入密码')).toBeInTheDocument();
      expect(screen.getByText('请确认密码')).toBeInTheDocument();
      expect(screen.getByText('请输入真实姓名')).toBeInTheDocument();
      expect(screen.getByText('请输入身份证号')).toBeInTheDocument();
    });
  });

  it('表单校验：密码格式错误', async () => {
    render(<RegisterPage {...defaultProps} />);
    fillForm({ password: '123', confirmPassword: '123' });
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('6-20位，可含字母、数字、符号')).toBeInTheDocument();
    });
  });

  it('表单校验：手机号格式错误', async () => {
    render(<RegisterPage {...defaultProps} />);
    fillForm({ phone: '123456' });
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('请输入正确手机号')).toBeInTheDocument();
    });
  });

  it('表单校验：身份证号格式错误', async () => {
    render(<RegisterPage {...defaultProps} />);
    fillForm({ idCard: '123456789012345' });
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('请输入正确身份证号')).toBeInTheDocument();
    });
  });

  it('表单校验：邮箱格式错误', async () => {
    render(<RegisterPage {...defaultProps} />);
    fillForm({ email: 'not-an-email' });
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('请输入有效的邮箱地址')).toBeInTheDocument();
    });
  });

  it('表单校验：两次密码不一致', async () => {
    render(<RegisterPage {...defaultProps} />);
    fillForm({ password: 'abc123!', confirmPassword: 'abc1234!' });
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('两次密码不一致')).toBeInTheDocument();
    });
  });

  it('表单校验：未勾选协议', async () => {
    render(<RegisterPage {...defaultProps} />);
    fillForm({ agreement: false });
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('请同意协议')).toBeInTheDocument();
    });
  });

  it('注册成功后重置表单并关闭弹窗', async () => {
    registerMock.mockResolvedValueOnce({});
    render(<RegisterPage {...defaultProps} />);
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(registerMock).toHaveBeenCalled();
      expect(require('antd').message.success).toHaveBeenCalledWith('注册成功');
      expect(defaultProps.onCancel).toHaveBeenCalled();
    });
  });

  it('注册失败：手机号已被注册', async () => {
    registerMock.mockRejectedValueOnce({ response: { data: { message: '手机号已被注册' } } });
    render(<RegisterPage {...defaultProps} />);
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('该手机号已被注册')).toBeInTheDocument();
    });
  });

  it('注册失败：邮箱已被注册', async () => {
    registerMock.mockRejectedValueOnce({ response: { data: { message: '邮箱已被注册' } } });
    render(<RegisterPage {...defaultProps} />);
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('该邮箱已被注册')).toBeInTheDocument();
    });
  });

  it('注册失败：身份证号码不合法', async () => {
    registerMock.mockRejectedValueOnce({ response: { data: { message: '身份证号码不合法' } } });
    render(<RegisterPage {...defaultProps} />);
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(screen.getByText('身份证号码不合法')).toBeInTheDocument();
    });
  });

  it('注册失败：其他错误', async () => {
    registerMock.mockRejectedValueOnce({ message: '未知错误' });
    render(<RegisterPage {...defaultProps} />);
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '立即注册' }));
    await waitFor(() => {
      expect(require('antd').message.error).toHaveBeenCalledWith('未知错误');
    });
  });
});
