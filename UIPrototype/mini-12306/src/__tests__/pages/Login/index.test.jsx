// mock window.matchMedia，确保 matches 属性存在
window.matchMedia = function(query) {
  return {
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  };
};

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from '../../../pages/Login/index';

// mock antd message
jest.mock('antd', () => {
  const origin = jest.requireActual('antd');
  return {
    ...origin,
    message: {
      success: jest.fn(),
      error: jest.fn(),
    },
    Modal: ({ title, children, ...props }) => (
      <div data-testid="modal" {...props}>
        {title && <div data-testid="modal-title">{title}</div>}
        {children}
      </div>
    ),
  };
});

// mock useAuth，loginMock 作用域仅在工厂内
jest.mock('../../../hooks/useAuth', () => {
  let loginMock = jest.fn();
  return {
    useAuth: () => ({ login: loginMock }),
    __esModule: true,
    _getLoginMock: () => loginMock,
    _setLoginMock: fn => { loginMock = fn; },
  };
});

// mock useNavigate
jest.mock('react-router-dom', () => {
  const origin = jest.requireActual('react-router-dom');
  // 用 global 变量暴露 mock
  global.__navigateMock = global.__navigateMock || jest.fn();
  return {
    ...origin,
    useNavigate: () => global.__navigateMock,
  };
});

const defaultProps = {
  visible: true,
  onCancel: jest.fn(),
};

function fillForm({ phone = '13812345678', password = 'abc123!', agreement = true } = {}) {
  fireEvent.change(screen.getByPlaceholderText('手机号'), { target: { value: phone } });
  fireEvent.change(screen.getByPlaceholderText('密码'), { target: { value: password } });
  if (agreement) {
    fireEvent.click(screen.getByText('我已阅读并同意相关协议'));
  }
}

describe('LoginPage', () => {
  let loginMock;
  beforeEach(() => {
    jest.clearAllMocks();
    loginMock = require('../../../hooks/useAuth')._getLoginMock();
    global.__navigateMock.mockClear();
  });

  it('渲染所有表单项和按钮', () => {
    render(
      <MemoryRouter>
        <LoginPage {...defaultProps} />
      </MemoryRouter>
    );
    expect(screen.getByRole('heading', { name: '用户登录' })).toBeInTheDocument();
    expect(screen.getByLabelText('手机号')).toBeInTheDocument();
    expect(screen.getByLabelText('密码')).toBeInTheDocument();
    expect(screen.getByText('我已阅读并同意相关协议')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '立即登录' })).toBeInTheDocument();
  });

  it('表单校验：手机号、密码必填', async () => {
    render(
      <MemoryRouter>
        <LoginPage {...defaultProps} />
      </MemoryRouter>
    );
    fireEvent.click(screen.getByRole('button', { name: '立即登录' }));
    const phoneNodes = await screen.findAllByText((_, node) => node.textContent.includes('请输入手机号'));
    expect(phoneNodes.length).toBeGreaterThan(0);
    const pwdNodes = await screen.findAllByText((_, node) => node.textContent.includes('请输入密码'));
    expect(pwdNodes.length).toBeGreaterThan(0);
  });

  it('表单校验：手机号格式错误', async () => {
    render(
      <MemoryRouter>
        <LoginPage {...defaultProps} />
      </MemoryRouter>
    );
    fillForm({ phone: '123456' });
    fireEvent.click(screen.getByRole('button', { name: '立即登录' }));
    const errorNodes = await screen.findAllByText((_, node) => node.textContent.includes('请输入正确的手机号'));
    expect(errorNodes.length).toBeGreaterThan(0);
  });

  it('表单校验：密码格式错误', async () => {
    render(
      <MemoryRouter>
        <LoginPage {...defaultProps} />
      </MemoryRouter>
    );
    fillForm({ password: '123' });
    fireEvent.click(screen.getByRole('button', { name: '立即登录' }));
    const errorNodes = await screen.findAllByText((_, node) => node.textContent.includes('密码格式错误'));
    expect(errorNodes.length).toBeGreaterThan(0);
  });

  it('登录成功后跳转来源页', async () => {
    // 动态修改 useAuth 返回 isAuthenticated: true
    const useAuthModule = require('../../../hooks/useAuth');
    useAuthModule.useAuth = () => ({ login: loginMock, isAuthenticated: true });

    loginMock.mockResolvedValueOnce({});
    render(
      <MemoryRouter>
        <LoginPage {...defaultProps} />
      </MemoryRouter>
    );
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '立即登录' }));
    await waitFor(() => {
      expect(loginMock).toHaveBeenCalled();
      expect(require('antd').message.success).toHaveBeenCalledWith('登录成功');
      expect(global.__navigateMock).toHaveBeenCalled();
    }, { timeout: 2000 });
  });

  it('登录失败：手机号未注册', async () => {
    // mock 返回 message 为 "用户不存在"
    loginMock.mockRejectedValueOnce({ response: { data: { message: '用户不存在' } } });
    render(
      <MemoryRouter>
        <LoginPage {...defaultProps} />
      </MemoryRouter>
    );
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '立即登录' }));
    const errorNodes = await screen.findAllByText((_, node) => node.textContent.includes('手机号未注册'));
    expect(errorNodes.length).toBeGreaterThan(0);
  });

  it('登录失败：密码错误', async () => {
    loginMock.mockRejectedValueOnce({ response: { data: { message: '密码错误' } } });
    render(
      <MemoryRouter>
        <LoginPage {...defaultProps} />
      </MemoryRouter>
    );
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '立即登录' }));
    const errorNodes = await screen.findAllByText((_, node) => node.textContent.includes('密码错误'));
    expect(errorNodes.length).toBeGreaterThan(0);
  });
  
});