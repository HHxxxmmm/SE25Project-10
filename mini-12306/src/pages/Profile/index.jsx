import React, { useState, useEffect } from 'react';
import { Card, Descriptions, Divider, Spin, Typography, Modal, Form, Input, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import generatePersonData from '../../mock/Person';
import AddPassenger from '../AddPassenger';
import './style.css'; // 确保导入原有样式文件

const { Title } = Typography;

export default function ProfilePage() {
    const navigate = useNavigate();
    const { user: authUser, updateUser, isAuthenticated } = useAuth();
    const [personInfo, setPersonInfo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isEditModalVisible, setIsEditModalVisible] = useState(false);
    const [showAddPassengerModal, setShowAddPassengerModal] = useState(false);
    const [form] = Form.useForm();

    useEffect(() => {
        if (!isAuthenticated) {
            navigate('/login', { state: { from: '/profile' }, replace: true });
            return;
        }

        const loadPersonData = () => {
            const mockData = generatePersonData();
            const currentUserData = mockData.news.find(
                (p) => authUser && p.account_id === authUser.id
            ) || mockData.news[0];

            setPersonInfo(currentUserData);
            setLoading(false);

            if (isEditModalVisible) {
                form.setFieldsValue({
                    u_name: currentUserData.u_name,
                    u_id: currentUserData.u_id,
                    u_phone: currentUserData.u_phone,
                });
            }
        };

        loadPersonData();
    }, [isAuthenticated, navigate, authUser, isEditModalVisible, form]);

    const showEditModal = () => {
        setIsEditModalVisible(true);
        if (personInfo) {
            form.setFieldsValue({
                u_name: personInfo.u_name,
                u_id: personInfo.u_id,
                u_phone: personInfo.u_phone,
            });
        }
    };

    const handleEditSubmit = async () => {
        try {
            const values = await form.validateFields();
            const updatedPersonInfo = {
                ...personInfo,
                u_name: values.u_name,
                u_id: values.u_id,
                u_phone: values.u_phone,
            };

            setPersonInfo(updatedPersonInfo);
            await updateUser({
                realName: values.u_name,
                idCard: values.u_id,
                phone: values.u_phone
            });

            message.success('个人信息更新成功！');
            setIsEditModalVisible(false);
        } catch (error) {
            message.error('请检查输入信息。');
        }
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
            {/* 个人信息卡片 - 保持原有结构和类名 */}
            <Card
                className="profile-card"
                title={<Title level={4} className="card-title">个人信息</Title>}
                headStyle={{ padding: 0 }}
            >
                <Descriptions bordered column={1}>
                    <Descriptions.Item label="姓名">{personInfo.u_name}</Descriptions.Item>
                    <Descriptions.Item label="身份证号">{personInfo.u_id}</Descriptions.Item>
                    <Descriptions.Item label="手机号码">{personInfo.u_phone || '未提供'}</Descriptions.Item>
                </Descriptions>
                <div style={{ textAlign: 'center', marginTop: 24 }}>
                    <button className="edit-profile-button" onClick={showEditModal}>修改</button>
                </div>
            </Card>

            <Divider />

            {/* 关联乘车人卡片 - 保持原有结构和类名 */}
            <Card
                className="profile-card related-passengers-card"
                title={<Title level={4} className="card-title">关联乘车人</Title>}
                headStyle={{ padding: 0 }}
                bodyStyle={{ padding: '24px' }}
            >
                {personInfo.related_passenger_name?.length > 0 ? (
                    personInfo.related_passenger_name.map((name, index) => (
                        <Descriptions key={index} bordered column={1} style={{ marginBottom: 16 }}>
                            <Descriptions.Item label="姓名">{name}</Descriptions.Item>
                            <Descriptions.Item label="身份证号">
                                {personInfo.related_passenger_id?.[index] || '未提供'}
                            </Descriptions.Item>
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
                        onClick={() => setShowAddPassengerModal(true)}
                    >
                        <span className="plus-sign">+</span> 添加乘车人
                    </button>
                </div>
            </Card>

            {/* 编辑个人信息模态框 - 保持原有表单字段名 */}
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

            {/* 添加乘车人弹窗 - 保持原有结构 */}
            {showAddPassengerModal && (
                <div className="add-passenger-page">
                    <div
                        className="modal-wrapper"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="modal-title"
                        style={{ position: 'relative' }}
                    >
                        <AddPassenger onClose={() => setShowAddPassengerModal(false)} />
                    </div>
                </div>
            )}
        </div>
    );
}