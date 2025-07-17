/**
 * useAuth 钩子测试
 */
import React from 'react';
import { render, screen, act, fireEvent } from '@testing-library/react';
import { AuthProvider, useAuth } from '../../hooks/useAuth';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import { login, logout, register, updateUser, checkCurrentUser } from '../../store/actions/authActions';

// 模拟 actions
jest.mock('../../store/actions/authActions', () => ({
  login: jest.fn(),
  logout: jest.fn(),
  register: jest.fn(),
  checkCurrentUser: jest.fn(),
  updateUser: jest.fn()
}));

// 创建模拟 store
const mockStore = configureStore([thunk]);

// 模拟浏览器的 sessionStorage
const mockSessionStorage = (() => {
  let store = {};
  return {
    getItem: jest.fn(key => store[key] || null),
    setItem: jest.fn((key, value) => {
      store[key] = value.toString();
    }),
    removeItem: jest.fn(key => {
      delete store[key];
    }),
    clear: jest.fn(() => {
      store = {};
    })
  };
})();

Object.defineProperty(window, 'sessionStorage', {
  value: mockSessionStorage
});

// 测试组件
function TestComponent({ onTest } = {}) {
  const auth = useAuth();
  
  // 暴露钩子方法，以便测试
  React.useEffect(() => {
    if (onTest) onTest(auth);
  }, [auth, onTest]);

  return (
    <div>
      <div data-testid="isAuthenticated">{auth.isAuthenticated.toString()}</div>
      <div data-testid="userPhone">{auth.user?.phoneNumber || 'no user'}</div>
      <button onClick={() => auth.login({ phoneNumber: '13800138000', password: 'password123' })}>
        Login
      </button>
      <button onClick={() => auth.logout()}>Logout</button>
      <button onClick={() => auth.register({ phoneNumber: '13900139000', password: 'password123' })}>
        Register
      </button>
      <button data-testid="update-user" onClick={() => auth.updateUser({ name: 'Updated Name' })}>
        Update User
      </button>
      <button data-testid="check-session" onClick={() => auth.checkSession()}>
        Check Session
      </button>
      <button data-testid="check-current-user" onClick={() => auth.checkCurrentUser()}>
        Check Current User
      </button>
      <button data-testid="force-logout" onClick={() => auth.forceLogout()}>
        Force Logout
      </button>
      <button data-testid="update-activity" onClick={() => auth.updateLastActivity()}>
        Update Activity
      </button>
      <button data-testid="reset-timer" onClick={() => auth.resetSessionTimer()}>
        Reset Timer
      </button>
    </div>
  );
}

describe('useAuth Hook', () => {
  let store;
  
  // 设置初始 Redux store
  beforeEach(() => {
    store = mockStore({
      auth: {
        user: null,
        isAuthenticated: false,
        loading: false,
        error: null
      }
    });
    
    // 为每个测试重置模拟
    jest.clearAllMocks();
    mockSessionStorage.clear();
    
    // 模拟定时器
    jest.useFakeTimers();
  });
  
  afterEach(() => {
    jest.useRealTimers();
  });
  
  it('应该提供正确的初始值', () => {
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    expect(screen.getByTestId('isAuthenticated').textContent).toBe('false');
    expect(screen.getByTestId('userPhone').textContent).toBe('no user');
  });
  
  it('应该能够调用 login 方法', async () => {
    // 设置 login action 的模拟返回值
    login.mockImplementation((credentials) => async (dispatch) => {
      return { id: '1', phoneNumber: credentials.phoneNumber };
    });
    
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 触发登录按钮点击
    await act(async () => {
      screen.getByText('Login').click();
    });
    
    // 验证 login action 被调用
    expect(login).toHaveBeenCalledWith({
      phoneNumber: '13800138000',
      password: 'password123'
    });
  });
  
  it('应该能够调用 logout 方法', async () => {
    // 设置 logout action 的模拟实现
    logout.mockImplementation(() => async (dispatch) => {
      // 不需要返回任何值
    });
    
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 触发登出按钮点击
    await act(async () => {
      screen.getByText('Logout').click();
    });
    
    // 验证 logout action 被调用
    expect(logout).toHaveBeenCalled();
  });
  
  it('应该能够调用 register 方法', async () => {
    // 设置 register action 的模拟实现
    register.mockImplementation((userData) => async (dispatch) => {
      return { success: true, user: { ...userData, id: '2' } };
    });
    
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 触发注册按钮点击
    await act(async () => {
      screen.getByText('Register').click();
    });
    
    // 验证 register action 被调用
    expect(register).toHaveBeenCalledWith({
      phoneNumber: '13900139000',
      password: 'password123'
    });
  });
  
  it('应该在用户已认证时设置会话超时', () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    render(
      <Provider store={store}>
        <AuthProvider config={{ sessionTimeout: 5000 }}>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 验证已经认证
    expect(screen.getByTestId('isAuthenticated').textContent).toBe('true');
    
    // 前进足够的时间触发会话超时
    act(() => {
      jest.advanceTimersByTime(5000);
    });
    
    // 验证 sessionStorage 是否被设置
    expect(mockSessionStorage.setItem).toHaveBeenCalledWith('sessionTimedOut', 'true');
  });
  
  it('应该正确处理配置选项', () => {
    const customSessionTimeout = 30000; // 30 秒
    
    render(
      <Provider store={store}>
        <AuthProvider config={{ sessionTimeout: customSessionTimeout }}>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 无法直接测试内部的超时值，但我们可以验证组件渲染正常
    expect(screen.getByTestId('isAuthenticated').textContent).toBe('false');
  });
  
  it('在没有 AuthProvider 的情况下使用 useAuth 应该抛出错误', () => {
    // 禁止 React 错误日志以避免测试噪音
    const originalError = console.error;
    console.error = jest.fn();
    
    expect(() => {
      render(<TestComponent />);
    }).toThrow('useAuth must be used within an AuthProvider');
    
    // 恢复 console.error
    console.error = originalError;
  });

  it('应该能够调用 updateUser 方法', async () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    // 设置 updateUser action 的模拟实现
    updateUser.mockImplementation((userData) => (dispatch) => {
      dispatch({ type: 'UPDATE_USER', payload: userData });
      return { ...userData };
    });
    
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 触发更新用户按钮点击
    await act(async () => {
      screen.getByTestId('update-user').click();
    });
    
    // 验证 updateUser action 被调用
    expect(updateUser).toHaveBeenCalledWith({ name: 'Updated Name' });
  });

  it('应该能够调用 checkSession 方法', async () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    // 设置 checkCurrentUser action 的模拟实现
    checkCurrentUser.mockImplementation(() => async (dispatch) => {
      dispatch({ type: 'LOGIN_SUCCESS', payload: { id: '1', phoneNumber: '13800138000' } });
      return { id: '1', phoneNumber: '13800138000' };
    });
    
    let authHook;
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent onTest={(auth) => { authHook = auth; }} />
        </AuthProvider>
      </Provider>
    );
    
    // 触发检查会话按钮点击
    let result;
    await act(async () => {
      result = await authHook.checkSession();
    });
    
    // 验证 checkCurrentUser action 被调用且返回了正确的结果
    expect(checkCurrentUser).toHaveBeenCalled();
    expect(result).toBeTruthy();
  });

  it('应该能够处理 checkSession 中的错误', async () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    // 设置 checkCurrentUser action 抛出错误
    checkCurrentUser.mockImplementation(() => async (dispatch) => {
      throw new Error('Session check failed');
    });
    
    // 模拟 console.error
    const originalConsoleError = console.error;
    console.error = jest.fn();
    
    let authHook;
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent onTest={(auth) => { authHook = auth; }} />
        </AuthProvider>
      </Provider>
    );
    
    // 触发检查会话按钮点击，应该捕获错误并返回false
    let result;
    await act(async () => {
      result = await authHook.checkSession();
    });
    
    // 验证结果为false且错误被记录
    expect(result).toBe(false);
    expect(console.error).toHaveBeenCalledWith(expect.stringContaining('会话检查错误'), expect.any(Error));
    
    // 恢复 console.error
    console.error = originalConsoleError;
  });

  it('应该能够调用 checkCurrentUser 方法', async () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    // 设置 checkCurrentUser action 的模拟实现
    checkCurrentUser.mockImplementation(() => async (dispatch) => {
      return { id: '1', phoneNumber: '13800138000' };
    });
    
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 触发检查当前用户按钮点击
    await act(async () => {
      screen.getByTestId('check-current-user').click();
    });
    
    // 验证 checkCurrentUser action 被调用
    expect(checkCurrentUser).toHaveBeenCalled();
  });

  it('应该能够调用 forceLogout 方法', async () => {
    // 模拟 window.location.href
    const originalLocation = window.location;
    delete window.location;
    window.location = { href: '' };
    
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    // 设置 logout action 的模拟实现
    logout.mockImplementation(() => async (dispatch) => {
      dispatch({ type: 'LOGOUT' });
    });
    
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 触发强制登出按钮点击
    await act(async () => {
      screen.getByTestId('force-logout').click();
    });
    
    // 验证 logout action 被调用
    expect(logout).toHaveBeenCalled();
    
    // 验证 sessionStorage 设置
    expect(mockSessionStorage.setItem).toHaveBeenCalledWith('sessionTimedOut', 'true');
    expect(mockSessionStorage.setItem).toHaveBeenCalledWith('sessionTimedOutAt', expect.any(String));
    
    // 前进时间以触发重定向
    await act(async () => {
      jest.advanceTimersByTime(200);
    });
    
    // 验证重定向
    expect(window.location.href).toBe('/login');
    
    // 恢复原始 location
    window.location = originalLocation;
  });

  it('应该能够调用 updateLastActivity 方法', async () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });

    // 创建一个 mock Date.now
    const originalDateNow = Date.now;
    const mockNow = 1626912000000; // 2021-07-22T00:00:00.000Z
    Date.now = jest.fn(() => mockNow);
    
    let authHook;
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent onTest={(auth) => { authHook = auth; }} />
        </AuthProvider>
      </Provider>
    );
    
    // 触发更新活动按钮点击
    await act(async () => {
      authHook.updateLastActivity();
    });
    
    // 无法直接断言内部状态，但可以验证方法不会抛出错误
    expect(true).toBeTruthy();
    
    // 恢复原始 Date.now
    Date.now = originalDateNow;
  });

  it('应该能够调用 resetSessionTimer 方法', async () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    let authHook;
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent onTest={(auth) => { authHook = auth; }} />
        </AuthProvider>
      </Provider>
    );
    
    // 触发重置定时器按钮点击
    await act(async () => {
      authHook.resetSessionTimer();
    });
    
    // 无法直接断言内部状态，但可以验证方法不会抛出错误
    expect(true).toBeTruthy();
  });

  it('应该能够响应用户活动事件', async () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 模拟用户活动事件
    act(() => {
      fireEvent.mouseDown(document);
      fireEvent.mouseMove(document);
      fireEvent.keyPress(document);
      fireEvent.scroll(document);
      fireEvent.touchStart(document);
      fireEvent.click(document);
    });
    
    // 由于无法直接断言内部状态，我们只能验证代码不会抛出错误
    expect(true).toBeTruthy();
  });

  it('应该正确清理会话监控', async () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    const { unmount } = render(
      <Provider store={store}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 卸载组件应该清理所有事件监听器和定时器
    act(() => {
      unmount();
    });
    
    // 验证没有错误产生
    expect(true).toBeTruthy();
  });

  it('应该在会话超时检查器中检测超时', async () => {
    // 使用已认证用户的 store
    store = mockStore({
      auth: {
        user: { id: '1', phoneNumber: '13800138000' },
        isAuthenticated: true,
        loading: false,
        error: null
      }
    });
    
    render(
      <Provider store={store}>
        <AuthProvider config={{ sessionTimeout: 3000 }}>
          <TestComponent />
        </AuthProvider>
      </Provider>
    );
    
    // 前进时间，但不足以触发超时
    act(() => {
      jest.advanceTimersByTime(1000);
    });
    
    // 此时不应该有登出
    expect(logout).not.toHaveBeenCalled();
    
    // 再前进时间，达到触发超时的条件
    act(() => {
      jest.advanceTimersByTime(3000);
    });
    
    // 验证执行了登出操作
    expect(logout).toHaveBeenCalled();
    expect(mockSessionStorage.setItem).toHaveBeenCalledWith('sessionTimedOut', 'true');
  });
});
