import React, { useState, useEffect } from 'react';
import { Card, Descriptions, Divider, Spin, Typography, Modal, Form, Input, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import AddPassenger from '../AddPassenger';
import { passengerAPI, profileAPI } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import './style.css'; // 确保导入原有样式文件

const { Title } = Typography;

export default function ProfilePage() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [profileData, setProfileData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isEditModalVisible, setIsEditModalVisible] = useState(false);
    const [showAddPassengerModal, setShowAddPassengerModal] = useState(false);
    const [form] = Form.useForm();

    // 加载用户个人资料
    useEffect(() => {
        const loadProfileData = async () => {
            try {
                setLoading(true);
                
                if (!user || !user.userId) {
                    message.error('请先登录');
                    navigate('/login');
            return;
        }

                const response = await profileAPI.getUserProfile(user.userId);
                
                if (response.status === 'SUCCESS') {
                    setProfileData(response.profile);
                } else {
                    message.error(response.message || '获取个人资料失败');
                }
            } catch (error) {
                console.error('获取个人资料失败:', error);
                message.error('获取个人资料失败，请稍后重试');
            } finally {
            setLoading(false);
            }
        };

        loadProfileData();
    }, [user, navigate]);

    // 当编辑模态框打开时，设置表单初始值
    useEffect(() => {
        if (isEditModalVisible && profileData) {
            form.setFieldsValue({
                realName: profileData.realName,
                phoneNumber: profileData.phoneNumber,
                email: profileData.email,
            });
        }
    }, [isEditModalVisible, profileData, form]);

    const showEditModal = () => {
        setIsEditModalVisible(true);
    };

    const handleEditSubmit = async () => {
        try {
            const values = await form.validateFields();
            
            const updateData = {
                realName: values.realName,
                phoneNumber: values.phoneNumber,
                email: values.email,
            };

            const response = await profileAPI.updateUserProfile(user.userId, updateData);
            
            if (response.status === 'SUCCESS') {
                setProfileData(response.profile);
            message.success('个人信息更新成功！');
            setIsEditModalVisible(false);
            } else {
                message.error(response.message || '更新失败');
            }
        } catch (error) {
            console.error('更新个人信息失败:', error);
            message.error('更新失败，请稍后重试');
        }
    };

    // 检查是否可以添加乘车人
    const checkCanAddPassenger = async () => {
        try {
            const response = await passengerAPI.checkCanAddPassenger(user.userId);
            
            if (response.allowed) {
                setShowAddPassengerModal(true);
            } else {
                alert(response.message || '无法添加乘车人');
                message.warning(response.message || '无法添加乘车人');
            }
        } catch (error) {
            console.error('检查添加乘车人权限失败:', error);
            alert('检查权限失败，请稍后重试');
            message.error('检查权限失败，请稍后重试');
        }
    };

    if (loading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                <Spin size="large" tip="加载中..." />
            </div>
        );
    }

    if (!profileData) {
        return (
            <div style={{ padding: '24px' }}>
                <Title level={4}>未能加载个人信息。</Title>
            </div>
        );
    }

    return (
        <div className="profile-page-container">
            {/* 个人信息卡片 */}
            <Card
                className="profile-card"
                title={<Title level={4} className="card-title">个人信息</Title>}
                headStyle={{ padding: 0 }}
            >
                <Descriptions bordered column={1}>
                    <Descriptions.Item label="姓名">{profileData.realName}</Descriptions.Item>
                    <Descriptions.Item label="手机号码">{profileData.phoneNumber || '未提供'}</Descriptions.Item>
                    <Descriptions.Item label="邮箱">{profileData.email || '未提供'}</Descriptions.Item>
                    <Descriptions.Item label="注册时间">
                        {profileData.registrationTime ? new Date(profileData.registrationTime).toLocaleString() : '未知'}
                    </Descriptions.Item>
                    <Descriptions.Item label="最后登录时间">
                        {profileData.lastLoginTime ? new Date(profileData.lastLoginTime).toLocaleString() : '未知'}
                    </Descriptions.Item>
                    <Descriptions.Item label="账户状态">
                        {profileData.accountStatus === 1 ? '正常' : '异常'}
                    </Descriptions.Item>
                </Descriptions>
                <div style={{ textAlign: 'center', marginTop: 24 }}>
                    <button className="edit-profile-button" onClick={showEditModal}>修改</button>
                </div>
            </Card>

            <Divider />

            {/* 关联乘车人卡片 */}
            <Card
                className="profile-card related-passengers-card"
                title={<Title level={4} className="card-title">关联乘车人</Title>}
                headStyle={{ padding: 0 }}
                bodyStyle={{ padding: '24px' }}
            >
                {profileData.linkedPassengers && profileData.linkedPassengers.length > 0 ? (
                    profileData.linkedPassengers.map((passenger, index) => (
                        <Descriptions key={index} bordered column={1} style={{ marginBottom: 16 }}>
                            <Descriptions.Item label="姓名">{passenger.realName}</Descriptions.Item>
                            <Descriptions.Item label="身份证号">{passenger.idCardNumber}</Descriptions.Item>
                            <Descriptions.Item label="乘客类型">{passenger.passengerTypeText}</Descriptions.Item>
                            <Descriptions.Item label="手机号码">{passenger.phoneNumber || '未提供'}</Descriptions.Item>
                            {passenger.passengerType === 3 && passenger.studentTypeLeft !== null && (
                                <Descriptions.Item label="学生票剩余次数">{passenger.studentTypeLeft}</Descriptions.Item>
                            )}
                        </Descriptions>
                    ))
                ) : (
                    <p>暂无关联乘车人</p>
                )}
                <div style={{ textAlign: 'center', marginTop: 24 }}>
                    <button
                        type="button"
                        className="add-passenger-button"
                        aria-label="添加乘车人"
                        onClick={checkCanAddPassenger}
                    >
                        <span className="plus-sign">+</span> 添加乘车人
                    </button>
                </div>
            </Card>

            {/* 编辑个人信息模态框 */}
            <Modal
                title="修改个人信息"
                open={isEditModalVisible}
                onOk={handleEditSubmit}
                onCancel={() => setIsEditModalVisible(false)}
                okText="保存"
                cancelText="取消"
            >
                <Form
                    form={form}
                    layout="vertical"
                    name="edit_profile_form"
                >
                    <Form.Item
                        name="realName"
                        label="姓名"
                        rules={[{ required: true, message: '请输入姓名！' }]}
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item
                        name="phoneNumber"
                        label="手机号码"
                        rules={[
                            { required: true, message: '请输入手机号码！' },
                            { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码！' },
                        ]}
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item
                        name="email"
                        label="邮箱"
                        rules={[
                            { type: 'email', message: '请输入有效的邮箱地址！' },
                        ]}
                    >
                        <Input />
                    </Form.Item>
                </Form>
            </Modal>

            {/* 添加乘车人模态框 */}
            {showAddPassengerModal && (
                <AddPassenger
                    visible={showAddPassengerModal}
                    onClose={() => setShowAddPassengerModal(false)}
                    onSuccess={() => {
                        setShowAddPassengerModal(false);
                        // 重新加载个人资料以显示新添加的乘车人
                        window.location.reload();
                    }}
                />
            )}
        </div>
    );
}