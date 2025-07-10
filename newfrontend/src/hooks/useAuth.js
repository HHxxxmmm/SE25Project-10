import { useSelector, useDispatch } from 'react-redux';
import { login, logout, register, updateUser, checkCurrentUser } from '../store/actions/authActions';
import { createContext, useContext, useMemo, useEffect, useRef, useCallback } from 'react';

// 认证上下文（用于非Redux管理的认证相关功能）
const AuthContext = createContext();

/**
 * 认证提供者组件（兼容旧版）
 * @param {Object} props - 组件属性
 * @param {ReactNode} props.children - 子组件
 * @param {Object} [props.config] - 认证配置
 */
export function AuthProvider({ children, config = {} }) {
  const authState = useSelector(state => state.auth);
  const dispatch = useDispatch();
  const lastActivityRef = useRef(Date.now());
  const sessionTimeoutRef = useRef(null);
  
  // 配置会话超时时间，在组件外部直接创建引用以避免重复计算
  const sessionTimeoutValueRef = useRef(config.sessionTimeout || 10 * 1000);
  
  // 强制退出函数 - 确保退出逻辑正确执行
  const forceLogout = async () => {
    console.log('====> [DEBUG] 强制退出会话开始 - 会话超时触发时间:', new Date().toLocaleTimeString());
    console.log('====> [DEBUG] 当前认证状态:', authState.isAuthenticated ? '已登录' : '未登录');
    console.log('====> [DEBUG] 最后活动时间:', new Date(lastActivityRef.current).toLocaleTimeString());
    
    // 确保清除任何计时器
    if (sessionTimeoutRef.current) {
      console.log('====> [DEBUG] 清除现有的会话超时定时器');
      clearTimeout(sessionTimeoutRef.current);
      sessionTimeoutRef.current = null;
    } else {
      console.log('====> [DEBUG] 没有活动的会话超时定时器需要清除');
    }
    
    // 将会话超时状态存储在 sessionStorage 中，以便登录页面可以获取并显示提示
    sessionStorage.setItem('sessionTimedOut', 'true');
    sessionStorage.setItem('sessionTimedOutAt', new Date().toISOString());
    console.log('====> [DEBUG] 已在 sessionStorage 中设置会话超时标志');
    
    try {
      console.log('====> [DEBUG] 准备调用 Redux logout action');
      // 等待Redux的logout action完成
      await dispatch(logout());
      console.log('====> [DEBUG] Redux logout action 已完成');
    } catch (error) {
      console.error('====> [DEBUG] Redux logout action 失败', error);
    } finally {
      console.log('====> [DEBUG] 准备重定向到登录页面');
      // 无论登出成功与否，都强制跳转到登录页面
      setTimeout(() => {
        console.log('====> [DEBUG] 执行重定向到登录页面');
        window.location.href = '/login';
      }, 100); // 短暂延迟确保Redux状态更新
    }
  };

  // 更新最后活动时间 - 使用useCallback记忆化函数
  const updateLastActivity = useCallback(() => {
    if (authState.isAuthenticated) {
      lastActivityRef.current = Date.now();
      console.log('活动时间已更新:', new Date(lastActivityRef.current).toLocaleTimeString());
    }
  }, [authState.isAuthenticated]);

  // 重置会话定时器 - 使用useCallback记忆化函数
  const resetSessionTimer = useCallback(() => {
    // 清除现有定时器
    if (sessionTimeoutRef.current) {
      clearTimeout(sessionTimeoutRef.current);
      sessionTimeoutRef.current = null;
    }
    
    // 只有在用户已登录的情况下才设置超时
    if (authState.isAuthenticated) {
      const timeoutValue = sessionTimeoutValueRef.current;
      console.log(`设置会话超时: ${timeoutValue/1000}秒`, new Date().toLocaleTimeString());
      
      // 设置新的定时器
      sessionTimeoutRef.current = setTimeout(forceLogout, timeoutValue);
    }
  }, [authState.isAuthenticated, forceLogout]);

  // 会话监控 - 简化实现，集中处理
  useEffect(() => {
    console.log('认证状态变更:', authState.isAuthenticated ? '已登录' : '未登录');
    
    // 如果用户已登录，设置活动监听和超时计时器
    if (authState.isAuthenticated) {
      console.log('启动会话监控');
      // 初始化活动时间
      lastActivityRef.current = Date.now();
      
      // 设置活动监听器
      const handleUserActivity = () => {
        updateLastActivity();
        resetSessionTimer();
      };
      
      // 设置多种用户活动事件
      const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];
      events.forEach(event => {
        window.addEventListener(event, handleUserActivity);
      });
      
      // 设置初始计时器
      resetSessionTimer();
      
      // 清理函数
      return () => {
        console.log('清理会话监控');
        events.forEach(event => {
          window.removeEventListener(event, handleUserActivity);
        });
        
        if (sessionTimeoutRef.current) {
          clearTimeout(sessionTimeoutRef.current);
          sessionTimeoutRef.current = null;
        }
      };
    }
  }, [authState.isAuthenticated, resetSessionTimer, updateLastActivity]);

  // 实现一个独立的会话超时检查器，不依赖于事件触发
  useEffect(() => {
    if (authState.isAuthenticated) {
      console.log('====> [DEBUG] 启动独立会话超时检查器，时间戳:', Date.now());
      console.log('====> [DEBUG] 当前超时设置为:', sessionTimeoutValueRef.current/1000, '秒');
      
      // 创建一个标志，确保forceLogout不会被多次触发
      let logoutTriggered = false;
      
      const intervalId = setInterval(() => {
        const currentTime = Date.now();
        const lastActivity = lastActivityRef.current;
        const elapsedTime = currentTime - lastActivity;
        const timeoutValue = sessionTimeoutValueRef.current;
        
        if (logoutTriggered) {
          console.log('====> [DEBUG] 已触发登出，跳过检查');
          return;
        }
        
        // 减少日志频率，只在接近超时时记录（每过1分钟或当剩余时间小于30秒时记录）
        const remainingTime = timeoutValue - elapsedTime;
        const passedMinutes = Math.floor(elapsedTime / (60 * 1000));
        const isMinuteChange = elapsedTime % (60 * 1000) < 1000; // 每分钟只打印一次
        
        if (isMinuteChange || remainingTime < 30 * 1000) {
          console.log(`====> [DEBUG] 会话状态检查: 已经过时间 ${Math.floor(elapsedTime/1000)}秒, 超时设置 ${timeoutValue/1000}秒`);
          console.log('====> [DEBUG] 当前时间:', new Date(currentTime).toLocaleTimeString(), 
                     '最后活动时间:', new Date(lastActivity).toLocaleTimeString(),
                     '剩余时间:', Math.floor(remainingTime/1000), '秒');
        }
        
        // 检测到超时就强制登出
        if (elapsedTime >= timeoutValue) {
          console.log('====> [DEBUG] 检测到会话超时，准备强制登出');
          console.log('====> [DEBUG] 超时详情 - 当前时间:', new Date(currentTime).toLocaleTimeString(), 
                     '最后活动时间:', new Date(lastActivity).toLocaleTimeString(),
                     '已经过时间(秒):', elapsedTime/1000);
          logoutTriggered = true; // 设置标志，防止重复触发
          clearInterval(intervalId);
          forceLogout();
        }
      }, 1000); // 每秒检查一次
      
      return () => {
        console.log('====> [DEBUG] 清理会话超时检查器，原因: 组件卸载或认证状态变更');
        clearInterval(intervalId);
      };
    } else {
      console.log('====> [DEBUG] 用户未认证，不启动会话超时检查器');
    }
  }, [authState.isAuthenticated, forceLogout]);

  // 记忆化上下文值 - 确保config处理正确
  const contextValue = useMemo(() => {
    // 更新会话超时引用值
    sessionTimeoutValueRef.current = config.sessionTimeout || 10 * 1000;
    console.log('====> [DEBUG] AuthProvider 接收到的会话超时配置:', sessionTimeoutValueRef.current/1000, '秒');
    
    return {
      // Redux状态
      ...authState,
      
      // 扩展功能
      config: {
        sessionTimeout: sessionTimeoutValueRef.current,
        ...config
      },
      
      // 非Redux操作方法
      checkSession: async () => {
        updateLastActivity();
        resetSessionTimer();
        try {
          const response = await dispatch(checkCurrentUser());
          return !!response;
        } catch (error) {
          console.error("会话检查错误:", error);
          return false;
        }
      },
      
      // Redux操作方法
      actions: {
        login: (credentials) => {
          lastActivityRef.current = Date.now(); // 登录时重置活动时间
          return dispatch(login(credentials));
        },
        logout: () => dispatch(logout()),
        register: (userData) => dispatch(register(userData)),
        updateUser: (newData) => {
          updateLastActivity();
          return dispatch(updateUser(newData));
        },
        checkCurrentUser: () => {
          updateLastActivity();
          return dispatch(checkCurrentUser());
        }
      },
      
      // 活动跟踪
      updateLastActivity,
      resetSessionTimer,
      forceLogout
    };
  }, [authState, config, dispatch]);

  return (
      <AuthContext.Provider value={contextValue}>
        {children}
      </AuthContext.Provider>
  );
}

/**
 * 认证钩子
 * @returns {{
 *   user: Object|null,
 *   isAuthenticated: boolean,
 *   loading: boolean,
 *   error: string|null,
 *   login: Function,
 *   logout: Function,
 *   register: Function,
 *   updateUser: Function,
 *   checkSession: Function,
 *   config: Object
 * }}
 */
export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  
  return {
    // 状态
    user: context.user,
    isAuthenticated: context.isAuthenticated,
    loading: context.loading,
    error: context.error,

    // 操作方法
    login: context.actions.login,
    logout: context.actions.logout,
    register: context.actions.register,
    updateUser: context.actions.updateUser,

    // 扩展功能
    checkSession: context.checkSession,
    checkCurrentUser: context.actions.checkCurrentUser,
    config: context.config,
    updateLastActivity: context.updateLastActivity,
    resetSessionTimer: context.resetSessionTimer,
    forceLogout: context.forceLogout
  };
}