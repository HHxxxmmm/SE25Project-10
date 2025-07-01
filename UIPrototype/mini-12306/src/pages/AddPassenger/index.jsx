// AddPassenger.jsx
import React, { useState } from 'react';
import './style.css';

const idTypes = [
    "居民身份证"
];

const ticketTypes = [
    "成人票",
    "学生票",
    "儿童票",
    "残军票"
];

const AddPassenger = ({ onClose }) => {  // 接收 onClose 作为 props
    const [name, setName] = useState('');
    const [idType, setIdType] = useState(idTypes[0]);
    const [ticketType, setTicketType] = useState(ticketTypes[0]);
    const [idNumber, setIdNumber] = useState('');
    const [phone, setPhone] = useState('');

    const handleSubmit = (e) => {
        e.preventDefault();
        alert(`提交成功（模拟）\n姓名：${name}\n证件类型：${idType}\n优惠类型：${ticketType}\n证件号码：${idNumber}\n手机号码：${phone}`);

        // 清空表单
        setName('');
        setIdType(idTypes[0]);
        setTicketType(ticketTypes[0]);
        setIdNumber('');
        setPhone('');

        if (onClose) onClose();  // 提交后自动关闭弹窗（可选）
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

            <label htmlFor="tickettype-select">
                优惠类型：
                <select
                    id="tickettype-select"
                    value={ticketType}
                    onChange={e => setTicketType(e.target.value)}
                    required
                >
                    {ticketTypes.map(type => (
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
                <button type="submit" className="submit-button">提交</button>
            </div>
        </form>
    );
};

export default AddPassenger;