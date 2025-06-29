import React, { useState, useEffect } from 'react';
import { Card, Descriptions, Divider, Spin, Typography } from 'antd';
import generatePersonData from '../../mock/Person';
import { useAuth } from '../../hooks/useAuth';
import './style.css';

const { Title } = Typography;

const ProfilePage = () => {
    const { user: authUser } = useAuth(); // 获取当前登录的用户信息
    const [personInfo, setPersonInfo] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // 模拟数据加载
        const loadPersonData = () => {
            const mockData = generatePersonData();
            // 尝试匹配登录用户的ID，如果匹配不到则使用第一个模拟数据
            const currentUserData = mockData.news.find(
                (p) => authUser && p.account_id === authUser.id
            ) || mockData.news[0]; // Fallback to the first mock user if no match

            setPersonInfo(currentUserData);
            setLoading(false);
        };

        loadPersonData();
    }, [authUser]);

    if (loading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                <Spin size="large" tip="加载中..." />
            </div>
        );
    }

    if (!personInfo) {
        return (
            <div style={{ padding: '24px' }}>
                <Title level={4}>未能加载个人信息。</Title>
            </div>
        );
    }

    return (
        <div className="profile-page-container">
            <Card className="profile-card">
                <Title level={4} className="card-title">个人信息</Title>
                <Descriptions bordered column={1}>
                    <Descriptions.Item label="姓名">{personInfo.u_name}</Descriptions.Item>
                    <Descriptions.Item label="身份证号">{personInfo.u_id}</Descriptions.Item>
                    <Descriptions.Item label="手机号码">{personInfo.u_phone || '未提供'}</Descriptions.Item>
                </Descriptions>
            </Card>

            <Divider />

            <Card className="profile-card related-passengers-card">
                <Title level={4} className="card-title">关联乘车人</Title>
                {personInfo.related_passenger_name && personInfo.related_passenger_name.length > 0 ? (
                    personInfo.related_passenger_name.map((name, index) => (
                        <Descriptions key={index} bordered column={1} style={{ marginBottom: '16px' }}>
                            <Descriptions.Item label="姓名">{name}</Descriptions.Item>
                            <Descriptions.Item label="身份证号">
                                {personInfo.related_passenger_id && personInfo.related_passenger_id[index]
                                    ? personInfo.related_passenger_id[index]
                                    : '未提供'}
                            </Descriptions.Item>
                        </Descriptions>
                    ))
                ) : (
                    <p>暂无关联乘车人</p>
                )}
            </Card>
        </div>
    );
};

export default ProfilePage;