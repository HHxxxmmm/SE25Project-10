import React, { useState } from 'react';
import { Layout, Menu, Button } from 'antd';
import {
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    HomeOutlined,
    TagsOutlined,
    OrderedListOutlined,
    FileTextOutlined,
    UserOutlined,
    SwapOutlined,
    RollbackOutlined
} from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import './style.css';

const { Sider } = Layout;

// Define an object to map paths to menu keys
const pathKeyMap = {
    '/': '1',
    '/trains': '2',
    '/orders': '3',
    '/my-tickets': '4',
    '/profile': '5',
    '/order-detail': '6',
    '/ticket-detail': '7',
    // Add more mappings if needed
};

export default function AppSider({ onCollapse }) {
    const [collapsed, setCollapsed] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    const handleCollapse = (collapsed) => {
        setCollapsed(collapsed);
        onCollapse(collapsed);
    };

    return (
        <Sider
            trigger={null}
            collapsible
            collapsed={collapsed}
            onCollapse={handleCollapse}
            className={`app-sider ${collapsed ? 'collapsed' : ''}`}
            width={200}
        >
            <div className="sider-toggle">
                <Button
                    type="text"
                    icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                    onClick={() => handleCollapse(!collapsed)}
                    style={{ width: '100%' }}
                />
            </div>
            <Menu
                theme="light"
                mode="inline"
                selectedKeys={[pathKeyMap[location.pathname] || '1']} // Use pathKeyMap to determine selected key
                onClick={({ key }) => {
                    switch(key) {
                        case '1': navigate('/'); break;
                        case '2': navigate('/trains'); break;
                        case '3': navigate('/orders'); break;
                        case '4': navigate('/my-tickets'); break;
                        case '5': navigate('/profile'); break;
                        case '6': navigate('/order-detail'); break;
                        case '7': navigate('/ticket-detail'); break;
                        default: navigate('/');
                    }
                }}
                items={[
                    { key: '1', icon: <HomeOutlined />, label: '首页' },
                    { key: '2', icon: <TagsOutlined />, label: '车票' },
                    { key: '3', icon: <OrderedListOutlined />, label: '我的订单' },
                    { key: '4', icon: <FileTextOutlined />, label: '本人车票' },
                    { key: '5', icon: <UserOutlined />, label: '个人中心' },
                    { key: '6', icon: <OrderedListOutlined />, label: '订单详情' },
                    { key: '7', icon: <FileTextOutlined />, label: '车票详情' },
                ]}
            />
        </Sider>
    );
}