import React, { useState, useEffect } from 'react';
import { Card, Descriptions, Divider, Spin, Typography, Button, Modal, Form, Input, message } from 'antd';
import generatePersonData from '../../mock/Person';
import { useAuth } from '../../hooks/useAuth';
import './style.css';

const { Title } = Typography;

const ProfilePage = () => {
    const { user: authUser, updateUser } = useAuth(); // 获取当前登录的用户信息和更新函数
    const [personInfo, setPersonInfo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [form] = Form.useForm();

    useEffect(() => {
        const loadPersonData = () => {
            const mockData = generatePersonData();
            const currentUserData = mockData.news.find(
                (p) => authUser && p.account_id === authUser.id
            ) || mockData.news[0];

            setPersonInfo(currentUserData);
            setLoading(false);
            // 如果模态框可见，设置表单初始值
            if (isModalVisible && currentUserData) {
                form.setFieldsValue({
                    u_name: currentUserData.u_name,
                    u_id: currentUserData.u_id,
                    u_phone: currentUserData.u_phone,
                });
            }
        };

        loadPersonData();
    }, [authUser, isModalVisible, form]);

    const showEditModal = () => {
        setIsModalVisible(true);
        if (personInfo) {
            form.setFieldsValue({
                u_name: personInfo.u_name,
                u_id: personInfo.u_id,
                u_phone: personInfo.u_phone,
            });
        }
    };

    const handleCancel = () => {
        setIsModalVisible(false);
    };

    const handleOk = () => {
        form.validateFields()
            .then((values) => {
                // 假设这里是更新个人信息的逻辑
                // 更新模拟数据并更新AuthContext中的user状态
                const updatedPersonInfo = {
                    ...personInfo,
                    u_name: values.u_name,
                    u_id: values.u_id,
                    u_phone: values.u_phone,
                };
                setPersonInfo(updatedPersonInfo);
                updateUser({
                    username: values.u_name, // 假设用户名就是姓名
                    realName: values.u_name,
                    idCard: values.u_id,
                    phone: values.u_phone,
                });
                message.success('个人信息更新成功！');
                setIsModalVisible(false);
            })
            .catch((info) => {
                console.log('Validate Failed:', info);
                message.error('请检查输入信息。');
            });
    };

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
            <Card
                className="profile-card"
                title={<Title level={4} className="card-title">个人信息</Title>}
                extra={
                    <Button type="primary" onClick={showEditModal}>修改</Button>
                }
            >
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

            <Modal
                title="修改个人信息"
                open={isModalVisible}
                onOk={handleOk}
                onCancel={handleCancel}
                okText="保存"
                cancelText="取消"
            >
                <Form
                    form={form}
                    layout="vertical"
                    name="edit_profile_form"
                    initialValues={{
                        u_name: personInfo.u_name,
                        u_id: personInfo.u_id,
                        u_phone: personInfo.u_phone,
                    }}
                >
                    <Form.Item
                        name="u_name"
                        label="姓名"
                        rules={[{ required: true, message: '请输入姓名！' }]}
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item
                        name="u_id"
                        label="身份证号"
                        rules={[
                            { required: true, message: '请输入身份证号！' },
                            { pattern: /^(\d{15}|\d{18}|\d{17}x)$/i, message: '请输入有效的身份证号！' },
                        ]}
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item
                        name="u_phone"
                        label="手机号码"
                        rules={[
                            { required: true, message: '请输入手机号码！' },
                            { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码！' },
                        ]}
                    >
                        <Input />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default ProfilePage;