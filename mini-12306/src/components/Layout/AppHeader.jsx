import React from 'react';
import { Layout } from 'antd';
import './style.css';

const { Header } = Layout;

export default function AppHeader() {
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
        </Header>
    );
}