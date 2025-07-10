import React, { useState, useEffect } from 'react';
import { Layout, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import RegisterPage from '../../pages/Register';
import './style.css';

const { Header } = Layout;

export default function AppHeader() {
    const navigate = useNavigate();
    const { user, isAuthenticated, logout, login, checkCurrentUser } = useAuth();
    const [showRegisterModal, setShowRegisterModal] = useState(false);
    const [localAuthState, setLocalAuthState] = useState({
        isAuthenticated: false,
        user: null
    });

    // 在组件挂载时检查当前用户状态
    useEffect(() => {
        const checkUserSession = async () => {
            try {
                // 从服务器获取当前会话用户
                // 注意：checkCurrentUser已在authActions中定义，需要先添加到useAuth中
                const user = await checkCurrentUser();
                
                if (user) {
                    setLocalAuthState({
                        isAuthenticated: true,
                        user: user
                    });
                }
            } catch (e) {
                console.error("认证状态检查错误:", e);
            }
        };

        checkUserSession();
        // 可以考虑添加定时检查，但对于大多数情况，这是不必要的
        // 会话验证应主要依赖服务器端控制
    }, []);

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
                                {currentAuthState.user?.realName || '用户'}
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