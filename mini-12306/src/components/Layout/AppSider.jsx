import React, { useState } from 'react';
import { Layout, Menu, Button } from 'antd';
import {
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    HomeOutlined,
    TagsOutlined,
    OrderedListOutlined,
    FileTextOutlined,
    UserOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import './style.css';

const { Sider } = Layout;

export default function AppSider({ onCollapse }) {
    const [collapsed, setCollapsed] = useState(false);
    const navigate = useNavigate();

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
                defaultSelectedKeys={['1']}
                onClick={({ key }) => {
                    switch(key) {
                        case '1': navigate('/'); break;
                        case '2': navigate('/trains'); break;
                        case '3': navigate('/orders'); break;
                        case '4': navigate('/my-tickets'); break;
                        case '5': navigate('/profile'); break;
                        default: navigate('/');
                    }
                }}
                items={[
                    { key: '1', icon: <HomeOutlined />, label: '首页' },
                    { key: '2', icon: <TagsOutlined />, label: '车票' },
                    { key: '3', icon: <OrderedListOutlined />, label: '我的订单' },
                    { key: '4', icon: <FileTextOutlined />, label: '本人车票' },
                    { key: '5', icon: <UserOutlined />, label: '个人中心' },
                ]}
            />
        </Sider>
    );
}