import { useSelector, useDispatch } from 'react-redux';
import { login, logout, register, updateUser } from '../store/actions/authActions';
import { createContext, useContext, useMemo } from 'react';

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

  // 记忆化上下文值
  const contextValue = useMemo(() => ({
    // Redux状态
    ...authState,

    // 扩展功能
    config: {
      sessionTimeout: 24 * 60 * 60 * 1000, // 默认24小时
      ...config
    },

    // 非Redux操作方法
    checkSession: () => {
      const loginTime = localStorage.getItem('mini12306_login_time');
      return loginTime && Date.now() - parseInt(loginTime) < contextValue.config.sessionTimeout;
    },

    // Redux操作方法
    actions: {
      login: (credentials) => dispatch(login(credentials)),
      logout: () => dispatch(logout()),
      register: (userData) => dispatch(register(userData)),
      updateUser: (newData) => dispatch(updateUser(newData))
    }
  }), [authState, config, dispatch]);

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
    config: context.config
  };
}