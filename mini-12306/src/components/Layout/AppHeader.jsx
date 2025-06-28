// AppHeader.jsx
import React, { useState, useEffect } from 'react';
import { Layout, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import RegisterPage from '../../pages/Register'; // 这里引入你改成弹窗的RegisterPage组件
import './style.css';

const { Header } = Layout;

export default function AppHeader() {
    const navigate = useNavigate();

    // 临时替代方案：从localStorage直接读取用户数据
    const getUserAuthState = () => {
        try {
            const userData = localStorage.getItem('mini12306_user');
            const loginTimestamp = localStorage.getItem('mini12306_login_time');
            if (userData && loginTimestamp) {
                return { isAuthenticated: true, user: JSON.parse(userData) };
            }
        } catch (e) {
            console.error("Error parsing user data", e);
            localStorage.removeItem('mini12306_user');
            localStorage.removeItem('mini12306_login_time');
        }
        return { isAuthenticated: false, user: null };
    };

    const [authState, setAuthState] = useState(getUserAuthState());
    const [showRegisterModal, setShowRegisterModal] = useState(false);

    useEffect(() => {
        const handleStorageChange = () => {
            setAuthState(getUserAuthState());
        };

        window.addEventListener('storage', handleStorageChange);
        const interval = setInterval(() => {
            setAuthState(getUserAuthState());
        }, 2000);

        return () => {
            window.removeEventListener('storage', handleStorageChange);
            clearInterval(interval);
        };
    }, []);

    const { isAuthenticated, user } = authState;

    const handleLogin = () => {
        localStorage.removeItem('mini12306_user');
        localStorage.removeItem('mini12306_login_time');
        setAuthState({ isAuthenticated: false, user: null });
        navigate('/login');
    };

    const handleRegister = () => {
        setShowRegisterModal(true);
    };

    const handleLogout = () => {
        localStorage.removeItem('mini12306_user');
        localStorage.removeItem('mini12306_login_time');
        setAuthState({ isAuthenticated: false, user: null });
        navigate('/');
        window.location.reload();
    };

    return (
        <>
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
                        <div className="auth-buttons">
                            <span className="please-text">请</span>
                            <Button type="text" className="login-btn" onClick={handleLogin}>登录</Button>
                            <span className="divider">/</span>
                            <Button type="text" className="register-btn" onClick={handleRegister}>注册</Button>
                        </div>
                    )}
                </div>
            </Header>

            {/* 注册弹窗 */}
            <RegisterPage visible={showRegisterModal} onCancel={() => setShowRegisterModal(false)} />
        </>
    );
}