import React from 'react';
import { Layout, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
// import { useAuth } from '../../hooks/useAuth';
import './style.css';

const { Header } = Layout;

export default function AppHeader() {
    const navigate = useNavigate();
    
    // 临时替代方案：从localStorage直接读取用户数据
    const getUserAuthState = () => {
        try {
            const userData = localStorage.getItem('mini12306_user');
            // 检查是否有明确的登录标记
            const loginTimestamp = localStorage.getItem('mini12306_login_time');
            
            if (userData && loginTimestamp) {
                return { isAuthenticated: true, user: JSON.parse(userData) };
            }
        } catch (e) {
            console.error("Error parsing user data", e);
            // 如果出错，清除可能损坏的数据
            localStorage.removeItem('mini12306_user');
            localStorage.removeItem('mini12306_login_time');
        }
        return { isAuthenticated: false, user: null };
    };
    
    // 添加状态以确保组件在localStorage变化时重新渲染
    const [authState, setAuthState] = React.useState(getUserAuthState());
    
    // 监听localStorage变化
    React.useEffect(() => {
        const handleStorageChange = () => {
            setAuthState(getUserAuthState());
        };
        
        window.addEventListener('storage', handleStorageChange);
        // 定期检查登录状态
        const interval = setInterval(() => {
            setAuthState(getUserAuthState());
        }, 2000);
        
        return () => {
            window.removeEventListener('storage', handleStorageChange);
            clearInterval(interval);
        };
    }, []);
    
    const { isAuthenticated, user } = authState;
  
    // 处理登录按钮点击
    const handleLogin = () => {
        // 确保在跳转前清除任何旧的登录状态
        localStorage.removeItem('mini12306_user');
        localStorage.removeItem('mini12306_login_time');
        setAuthState({ isAuthenticated: false, user: null });
        navigate('/login');
    };
    
    // 处理注册按钮点击
    const handleRegister = () => {
        navigate('/register');
    };
  
    // 处理登出
    const handleLogout = () => {
        localStorage.removeItem('mini12306_user');
        localStorage.removeItem('mini12306_login_time');
        setAuthState({ isAuthenticated: false, user: null });
        navigate('/');
        window.location.reload(); // 强制刷新以确保状态更新
    };
    
    return (
        <Header className="app-header">
            <div className="header-left">
                <img
                    src={process.env.PUBLIC_URL + '/OIP.jpg'}
                    className="app-logo"
                    alt="12306 logo"
                />
                <h1 className="app-title">Mini-12306</h1>
            </div>

            <div className="header-right">
                <span className="greeting">您好，</span>
                {isAuthenticated ? (
                    // 修改登录后的显示格式：您好，用户名|退出
                    <div className="user-info">
                        <span className="user-name">{user?.username || user?.name || '用户'}</span>
                        <span className="separator" style={{ margin: '0 8px', color: '#ccc' }}>|</span>
                        <Button 
                          type="link" 
                          className="logout-btn"
                          onClick={handleLogout}
                          style={{ padding: '0', fontSize: '14px', color: '#ff4d4f' }}
                        >
                          退出
                        </Button>
                    </div>
                ) : (
                    // Not logged in state - shows login/register buttons
                    <div className="auth-buttons">
                        <span className="please-text">请</span>
                        <Button type="text" className="login-btn" onClick={handleLogin}>登录</Button>
                        <span className="divider">/</span>
                        <Button type="text" className="register-btn" onClick={handleRegister}>注册</Button>
                    </div>
                )}
            </div>
        </Header>
    );
}