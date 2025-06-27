import React from 'react';
import { Layout, Button } from 'antd';
import './style.css';

const { Header } = Layout;

export default function AppHeader() {
    // For now, we'll default to not logged in state
    const isLoggedIn = false;
    // Mock user data - in a real app, this would come from state/store
    const user = {
        name: '张三',
        avatar: process.env.PUBLIC_URL + '/default-avatar.jpg'
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
                {isLoggedIn ? (
                    // Logged in state - shows name and avatar
                    <div className="user-info">
                        <span className="user-name">{user.name}</span>
                        <img
                            src={user.avatar}
                            className="user-avatar"
                            alt="User avatar"
                        />
                    </div>
                ) : (
                    // Not logged in state - shows login/register buttons
                    <div className="auth-buttons">
                        <span className="please-text">请</span>
                        <Button type="text" className="login-btn">登录</Button>
                        <span className="divider">/</span>
                        <Button type="text" className="register-btn">注册</Button>
                    </div>
                )}
            </div>
        </Header>
    );
}