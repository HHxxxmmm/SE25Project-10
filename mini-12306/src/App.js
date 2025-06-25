import React from 'react';
import { Layout } from 'antd';
import { AppHeader, AppSider } from './components/Layout';
import { Routes, Route } from 'react-router-dom';
import HomePage from './pages/Home';
import TicketsPage from './pages/Tickets';
import OrdersPage from './pages/Orders';
import MyTicketsPage from './pages/MyTickets';
import ProfilePage from './pages/Profile';
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
                        <Route path="/" element={<HomePage />} />
                        <Route path="/tickets" element={<TicketsPage />} />
                        <Route path="/orders" element={<OrdersPage />} />
                        <Route path="/my-tickets" element={<MyTicketsPage />} />
                        <Route path="/profile" element={<ProfilePage />} />
                    </Routes>
                </Content>
            </Layout>
        </Layout>
    );
}