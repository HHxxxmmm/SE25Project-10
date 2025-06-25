// src/App.js
import React from 'react';
import { Layout } from 'antd';
import { AppHeader, AppSider } from './components/Layout';
import { Routes, Route } from 'react-router-dom';
import { routes } from './routes';
import './App.css';

const { Content } = Layout;

export default function App() {
    return (
        <Layout className="app-layout">
            <AppHeader />
            <Layout>
                <AppSider />
                <Content className="app-content">
                    <Routes>
                        {routes.map((route, index) => (
                            <Route
                                key={route.path}  // 改用path作为key更稳定
                                path={route.path}
                                element={route.element}
                            />
                        ))}
                    </Routes>
                </Content>
            </Layout>
        </Layout>
    );
}