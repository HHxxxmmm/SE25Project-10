import React, { useState, useEffect } from 'react';
import { Layout, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import RegisterPage from '../../pages/Register';
import './style.css';

const { Header } = Layout;

export default function AppHeader() {
    const navigate = useNavigate();
    const { user, isAuthenticated, logout, login } = useAuth();
    const [showRegisterModal, setShowRegisterModal] = useState(false);
    const [localAuthState, setLocalAuthState] = useState({
        isAuthenticated: false,
        user: null
    });

    // 同步Redux和本地存储状态
    useEffect(() => {
        const checkAuthState = () => {
            try {
                const userData = localStorage.getItem('mini12306_user');
                const loginTime = localStorage.getItem('mini12306_login_time');

                if (userData && loginTime) {
                    const parsedUser = JSON.parse(userData);
                    const sessionValid = Date.now() - parseInt(loginTime) < 24 * 60 * 60 * 1000;

                    if (sessionValid) {
                        // 如果Redux中没有用户数据，用本地存储初始化
                        if (!isAuthenticated) {
                            login(parsedUser); // 自动登录
                        }
                        setLocalAuthState({
                            isAuthenticated: true,
                            user: parsedUser
                        });
                    } else {
                        logout(); // 会话过期
                    }
                }
            } catch (e) {
                console.error("认证状态检查错误:", e);
                logout();
            }
        };

        checkAuthState();
        const interval = setInterval(checkAuthState, 5000); // 每5秒检查一次

        return () => clearInterval(interval);
    }, [isAuthenticated, login, logout]);

    // 合并状态：优先使用Redux，没有则使用本地存储
    const currentAuthState = isAuthenticated ?
        { isAuthenticated, user } :
        localAuthState;

    const handleLogin = () => {
        navigate('/login');
    };

    const handleRegister = () => {
        setShowRegisterModal(true);
    };

    const handleLogout = () => {
        logout();
        setLocalAuthState({ isAuthenticated: false, user: null });
    };

    const handleProfileClick = () => {
        navigate('/profile');
    };

    return (
        <>
            <Header className="app-header">
                <div className="header-left">
                    <img
                        src={`${process.env.PUBLIC_URL}/OIP.jpg`}
                        className="app-logo"
                        alt="12306 logo"
                    />
                    <h1 className="app-title">Mini-12306</h1>
                </div>

                <div className="header-right">
                    <span className="greeting">您好，</span>
                    {currentAuthState.isAuthenticated ? (
                        <div className="user-info">
                            <Button
                                type="link"
                                className="user-name-btn"
                                onClick={handleProfileClick}
                                style={{ padding: 0, fontSize: '14px', color: 'rgba(0, 0, 0, 0.85)' }}
                            >
                                {currentAuthState.user?.username || currentAuthState.user?.name || '用户'}
                            </Button>
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
                            <Button
                                type="text"
                                className="login-btn"
                                onClick={handleLogin}
                            >
                                登录
                            </Button>
                            <span className="divider">/</span>
                            <Button
                                type="text"
                                className="register-btn"
                                onClick={handleRegister}
                            >
                                注册
                            </Button>
                        </div>
                    )}
                </div>
            </Header>

            <RegisterPage
                visible={showRegisterModal}
                onCancel={() => setShowRegisterModal(false)}
            />
        </>
    );
}