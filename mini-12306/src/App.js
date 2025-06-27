
import React, { useState } from 'react';
import { Layout } from 'antd';
import { AppHeader, AppSider } from './components/Layout';
import { Routes, Route } from 'react-router-dom';
import { routes } from './routes';
import './App.css';

const { Content } = Layout;

export default function App() {
    const [siderCollapsed, setSiderCollapsed] = useState(false);

    return (
        <Layout className="app-layout">
            <AppHeader />
            <Layout>
                <AppSider onCollapse={setSiderCollapsed} />
                <Content className={`app-content ${siderCollapsed ? 'collapsed' : ''}`}>
                    <Routes>
                        {routes.map((route) => (
                            <Route
                                key={route.path}
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