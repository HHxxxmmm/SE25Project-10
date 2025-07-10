// AddPassenger.jsx
import React, { useState } from 'react';
import { message } from 'antd';
import { passengerAPI } from '../../services/api';
import './style.css';

const idTypes = [
    "居民身份证"
];

const AddPassenger = ({ onClose }) => {
    const [name, setName] = useState('');
    const [idType, setIdType] = useState(idTypes[0]);
    const [idNumber, setIdNumber] = useState('');
    const [phone, setPhone] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!name || !idNumber || !phone) {
            message.error('请填写完整信息');
            return;
        }

        setSubmitting(true);
        
        try {
            const userId = 1; // 使用测试用户ID
            
            // 调用后端API
            const response = await passengerAPI.addPassenger(userId, name, idNumber, phone);
            
            if (response.success) {
                message.success('乘车人添加成功！');
                // 清空表单
                setName('');
                setIdType(idTypes[0]);
                setIdNumber('');
                setPhone('');
                if (onClose) onClose();
            } else {
                // 根据不同的错误情况显示不同的提示
                let errorMessage = response.message || '添加失败，请稍后重试';
                
                if (response.message.includes('已达到最大乘车人数量限制')) {
                    errorMessage = '已达到最大乘车人数量限制（3人），无法继续添加';
                } else if (response.message.includes('该乘车人已被添加')) {
                    errorMessage = '该乘车人已被添加，请勿重复添加';
                } else if (response.message.includes('该乘车人信息无效')) {
                    errorMessage = '该乘车人信息无效，请检查姓名、身份证号和手机号是否正确';
                } else if (response.message.includes('用户不存在')) {
                    errorMessage = '用户信息错误，请重新登录';
                }
                
                message.error(errorMessage);
            }
        } catch (error) {
            console.error('添加乘车人失败:', error);
            message.error('添加失败，请检查网络连接');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <form className="modal-form" onSubmit={handleSubmit} noValidate>
            {/* 关闭按钮 */}
            <button
                type="button"
                className="modal-close-button"
                aria-label="关闭弹窗"
                onClick={onClose}
                style={{
                    position: 'absolute',
                    top: 12,
                    right: 12,
                    fontSize: 24,
                    background: 'none',
                    border: 'none',
                    color: '#999',
                    cursor: 'pointer',
                    userSelect: 'none',
                }}
            >
                ×
            </button>

            <h2 id="modal-title" className="modal-title">添加乘车人</h2>

            <label htmlFor="name-input">
                姓名：
                <input
                    id="name-input"
                    type="text"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    required
                    placeholder="请输入姓名"
                    autoComplete="name"
                />
            </label>

            <label htmlFor="idtype-select">
                证件类型：
                <select
                    id="idtype-select"
                    value={idType}
                    onChange={e => setIdType(e.target.value)}
                    required
                >
                    {idTypes.map(type => (
                        <option key={type} value={type}>{type}</option>
                    ))}
                </select>
            </label>

            <label htmlFor="idnumber-input">
                证件号码：
                <input
                    id="idnumber-input"
                    type="text"
                    value={idNumber}
                    onChange={e => setIdNumber(e.target.value)}
                    required
                    placeholder="请输入证件号码"
                    autoComplete="off"
                />
            </label>

            <label htmlFor="phone-input">
                手机号码：
                <input
                    id="phone-input"
                    type="tel"
                    value={phone}
                    onChange={e => setPhone(e.target.value)}
                    required
                    placeholder="请输入手机号码"
                    autoComplete="tel"
                    pattern="^1[3-9]\\d{9}$"
                    title="请输入有效的手机号码"
                />
            </label>

            <div className="modal-buttons">
                <button 
                    type="submit" 
                    className="submit-button"
                    disabled={submitting}
                >
                    {submitting ? '添加中...' : '添加乘车人'}
                </button>
            </div>
        </form>
    );
};

export default AddPassenger;