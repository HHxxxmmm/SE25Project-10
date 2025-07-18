import React, { useState, useEffect } from 'react';
import { Layout } from 'antd';
import { AppHeader, AppSider } from './components/Layout';
import { Routes, Route } from 'react-router-dom';
import { routes } from './routes';
import { useDispatch, useSelector } from 'react-redux';
import { clearChangeTicket } from './store/actions/changeTicketActions';
import './App.css';

const { Content } = Layout;

export default function App() {
    const [siderCollapsed, setSiderCollapsed] = useState(false);
    const dispatch = useDispatch();
    const changeTicketState = useSelector(state => state.changeTicket);

    // 全局监听应用关闭事件，清理改签状态
    useEffect(() => {
        const handleBeforeUnload = () => {
            if (changeTicketState.isChanging) {
                dispatch(clearChangeTicket());
            }
        };

        const handleUnload = () => {
            if (changeTicketState.isChanging) {
                dispatch(clearChangeTicket());
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        window.addEventListener('unload', handleUnload);

        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
            window.removeEventListener('unload', handleUnload);
        };
    }, [changeTicketState.isChanging, dispatch]);

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
