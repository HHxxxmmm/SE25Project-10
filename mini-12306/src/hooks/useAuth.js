import { useState, useEffect, useContext, createContext } from 'react';

// 创建认证上下文
const AuthContext = createContext(null);

// 模拟用户数据
const mockUserData = {
  id: 1,
  username: 'demo_user',
  realName: '测试用户',
  phone: '13800138000',
  idCard: '110101199001011234'
};

// 提供者组件
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // 检查是否已保存登录状态
  useEffect(() => {
    // 不再强制设置为登录状态
    const forceAuth = false; // 改为false，不强制登录
    
    if (forceAuth) {
      // 如果没有保存的用户数据，创建一个默认用户
      let userData = null;
      try {
        const savedUser = localStorage.getItem('mini12306_user');
        if (savedUser) {
          userData = JSON.parse(savedUser);
        } else {
          // 创建默认模拟用户
          userData = { ...mockUserData };
          localStorage.setItem('mini12306_user', JSON.stringify(userData));
        }
      } catch (e) {
        console.error('Error parsing user data:', e);
        userData = { ...mockUserData };
        localStorage.setItem('mini12306_user', JSON.stringify(userData));
      }
      
      // 设置为已登录状态
      setUser(userData);
      setIsAuthenticated(true);
      console.log('已强制设置为登录状态:', userData);
      return;
    }
    
    // 正常登录状态检查逻辑
    const savedUser = localStorage.getItem('mini12306_user');
    const loginTimestamp = localStorage.getItem('mini12306_login_time'); // 检查登录时间戳
    
    if (savedUser && loginTimestamp) {
      try {
        const parsedUser = JSON.parse(savedUser);
        setUser(parsedUser);
        setIsAuthenticated(true);
      } catch (err) {
        console.error('Failed to parse saved user data:', err);
        localStorage.removeItem('mini12306_user');
        localStorage.removeItem('mini12306_login_time');
      }
    } else {
      // 确保未登录状态
      setUser(null);
      setIsAuthenticated(false);
    }
  }, []);

  // 登录逻辑
  const login = async (credentials) => {
    console.log('Login attempt with:', credentials);
    
    // 直接视为登录成功
    const userData = { ...mockUserData, username: credentials.user || mockUserData.username };
    
    // 保存到本地存储确保刷新页面后状态不丢失
    localStorage.setItem('mini12306_user', JSON.stringify(userData));
    localStorage.setItem('mini12306_login_time', Date.now().toString()); // 添加登录时间戳
    
    // 更新状态
    setUser(userData);
    setIsAuthenticated(true);
    
    return userData;
  };

  // 注册逻辑
  const register = async (userData) => {
    console.log('Register attempt with:', userData);
    // 直接视为注册成功，但不自动登录
    return { success: true };
  };

  // 登出逻辑
  const logout = () => {
    localStorage.removeItem('mini12306_user');
    localStorage.removeItem('mini12306_login_time'); // 移除登录时间戳
    setUser(null);
    setIsAuthenticated(false);
  };

  // 提供认证上下文给整个应用
  return (
    <AuthContext.Provider value={{ user, isAuthenticated, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// 自定义钩子供组件使用
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
