import React, { useState, useEffect, useRef } from 'react';
import { Card, Typography, Divider, Checkbox, Button, Row, message, Spin } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { orderAPI, ticketAPI } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import './style.css';

const { Text } = Typography;

// 乘客类型映射
const PASSENGER_TYPE = {
    1: '成人',
    2: '儿童',
    3: '学生',
    4: '残疾',
    5: '军人',
};

// 票种映射
const TICKET_TYPE = {
    1: '成人票',
    2: '儿童票',
    3: '学生票',
    4: '残疾票',
    5: '军人票',
};

// 车票状态映射
const TICKET_STATUS = {
    0: '待支付',
    1: '已支付',
    2: '已完成',
    3: '已退票',
    4: '已改签',
};

function formatDateTime(datetime) {
    if (!datetime) return '';
    const date = datetime.split('T')[0] || datetime.split(' ')[0] || '';
    const time = datetime.includes('T') ? 
        datetime.split('T')[1] : 
        datetime.split(' ')[1] || datetime;
    return `${date} ${time.slice(0, 5)}`;
}

function formatDate(date) {
    if (!date) return '';
    return date.split('T')[0] || date.split(' ')[0] || date;
}

function formatTime(time) {
    if (!time) return '';
    return time.slice(0, 5);
}

const ReturnTicketPage = () => {
    const [refundData, setRefundData] = useState(null);
    const [selectedTickets, setSelectedTickets] = useState([]);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const trainInfoCardRef = useRef(null);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();

    useEffect(() => {
        const fetchRefundData = async () => {
            try {
                const orderId = searchParams.get('orderId');
                const ticketIdsParam = searchParams.get('ticketIds');
                const currentUserId = user?.userId || searchParams.get('userId') || 1;
                
                console.log('退票页面URL参数:', { orderId, ticketIdsParam, currentUserId });
                
                if (!orderId || !ticketIdsParam) {
                    console.error('缺少必要参数:', { orderId, ticketIdsParam });
                    message.error('缺少必要参数');
                    navigate('/orders');
                    return;
                }
                
                if (!currentUserId) {
                    message.error('请先登录');
                    navigate('/login');
                    return;
                }

                // 解析车票ID列表
                const ticketIds = ticketIdsParam.split(',').map(id => parseInt(id));
                
                console.log('获取退票准备信息:', { orderId, currentUserId, ticketIds });
                // 调用退票准备阶段API
                const response = await orderAPI.getRefundPreparation(currentUserId, orderId, ticketIds);
                console.log('退票准备信息响应:', response);
                
                if (response && response.orderNumber) {
                    setRefundData(response);
                    // 默认选中所有车票
                    const allTicketIds = response.refundableTickets.map(ticket => ticket.ticketId);
                    setSelectedTickets(allTicketIds);
                } else {
                    console.error('获取退票信息失败，响应数据:', response);
                    message.error('获取退票信息失败');
                    navigate('/orders');
                }
            } catch (error) {
                console.error('获取退票信息失败:', error);
                message.error('获取退票信息失败，请稍后重试');
                navigate('/orders');
            } finally {
                setLoading(false);
            }
        };

        fetchRefundData();
    }, [searchParams, navigate]);

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" />
                <div style={{ marginTop: '16px' }}>加载中...</div>
            </div>
        );
    }

    if (!refundData) {
        return <div>退票信息不存在</div>;
    }

    const allSelected = selectedTickets.length === refundData.refundableTickets.length;
    const indeterminate = selectedTickets.length > 0 && !allSelected;

    const toggleSelect = (ticketId) => {
        setSelectedTickets((prev) =>
            prev.includes(ticketId) ? prev.filter(id => id !== ticketId) : [...prev, ticketId]
        );
    };

    const onSelectAllReturn = (e) => {
        if (e.target.checked) {
            setSelectedTickets(refundData.refundableTickets.map(ticket => ticket.ticketId));
        } else {
            setSelectedTickets([]);
        }
    };

    // 确认退票按钮点击处理函数 - 正式退票阶段
    const handleConfirmReturn = async () => {
        if (selectedTickets.length === 0) {
            message.warning('请选择要退票的车票');
            return;
        }

        setSubmitting(true);
        try {
            const currentUserId = user?.userId || searchParams.get('userId') || 1;
            const orderId = searchParams.get('orderId');
            
            console.log('提交正式退票请求:', { currentUserId, orderId, selectedTickets });
            // 调用正式退票API
            const response = await ticketAPI.refundTickets(currentUserId, orderId, selectedTickets);
            console.log('正式退票响应:', response);
            
            if (response && response.status === 'SUCCESS') {
                message.success('退票成功');
                // 跳转回订单详情页，刷新订单信息
                navigate(`/order-detail?orderId=${orderId}&userId=${currentUserId}`);
            } else {
                message.error(response.message || '退票失败');
            }
        } catch (error) {
            console.error('退票失败:', error);
            message.error('退票失败，请稍后重试');
        } finally {
            setSubmitting(false);
        }
    };

    // 取消按钮点击处理函数
    const handleCancel = () => {
        navigate(-1); // 返回上一级页面
    };

    return (
        <>
            <Card className="return-ticket-card" bordered={false} bodyStyle={{ padding: 0 }}>
                <div className="card-title">退票</div>

                <Row justify="space-between" className="info-row">
                    <Text type="secondary" className="order-info-text">
                        订单号: {refundData.orderNumber}
                    </Text>
                    <Text type="secondary" className="order-info-text">
                        下单时间: {formatDateTime(refundData.orderTime)}
                    </Text>
                </Row>

                <Divider />

                <div className="train-info-card" ref={trainInfoCardRef}>
                    <div className="train-date">{formatDate(refundData.travelDate)}</div>
                    <div className="train-route">
                        <div className="station-block">
                            <div className="station-name">{refundData.departureStation}</div>
                            <div className="station-time">{formatTime(refundData.departureTime)}开</div>
                        </div>

                        <div className="arrow-block">
                            <div className="train-number">{refundData.trainNumber}</div>
                            <div className="arrow">→</div>
                        </div>

                        <div className="station-block">
                            <div className="station-name">{refundData.arrivalStation}</div>
                            <div className="station-time">{formatTime(refundData.arrivalTime)}到</div>
                        </div>
                    </div>
                </div>

                <Divider />

                <div className="ticket-table-wrapper">
                    <table className="ticket-table">
                        <thead>
                        <tr>
                            <th style={{ width: '5%' }}>
                                <Checkbox
                                    indeterminate={indeterminate}
                                    checked={allSelected}
                                    onChange={onSelectAllReturn}
                                />
                            </th>
                            <th style={{ width: '5%' }}>序号</th>
                            <th style={{ width: '12%' }}>姓名</th>
                            <th style={{ width: '22%' }}>身份证号</th>
                            <th style={{ width: '12%' }}>席别</th>
                            <th style={{ width: '12%' }}>票种</th>
                            <th style={{ width: '12%' }}>票价</th>
                            <th style={{ width: '20%' }}>状态</th>
                        </tr>
                        </thead>
                        <tbody>
                        {refundData.refundableTickets.map((ticket, index) => (
                            <tr
                                key={ticket.ticketId}
                                className={selectedTickets.includes(ticket.ticketId) ? 'selected' : ''}
                                onClick={() => toggleSelect(ticket.ticketId)}
                                style={{ cursor: 'pointer' }}
                            >
                                <td>
                                    <Checkbox
                                        checked={selectedTickets.includes(ticket.ticketId)}
                                        onClick={e => e.stopPropagation()}
                                        onChange={() => toggleSelect(ticket.ticketId)}
                                    />
                                </td>
                                <td>{index + 1}</td>
                                <td>{ticket.passengerName}</td>
                                <td>{ticket.idCardNumber}</td>
                                <td>{ticket.carriageType}</td>
                                <td>{TICKET_TYPE[ticket.ticketType] || '未知'}</td>
                                <td>
                                    <Text className="price-text">¥{ticket.originalPrice}</Text>
                                </td>
                                <td>{TICKET_STATUS[ticket.ticketStatus] || '未知'}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                <Divider />

                <Row justify="space-between" align="middle" className="footer-row">
                    <div>订单总价: ¥{refundData.totalAmount}</div>
                    <div>已选择: {selectedTickets.length}张车票</div>
                </Row>

                <div className="button-row">
                    <Button 
                        type="primary" 
                        className="btn-blue" 
                        onClick={handleConfirmReturn}
                        disabled={selectedTickets.length === 0 || submitting}
                        loading={submitting}
                    >
                        {submitting ? '退票中...' : `确认退票 (${selectedTickets.length})`}
                    </Button>
                    <Button className="btn-white" onClick={handleCancel} disabled={submitting}>
                        取消
                    </Button>
                </div>
            </Card>

            <div className="tip-wrapper">
                <div className="tip-content">
                    <p>温馨提示：</p>
                    <p>确认退票后，车票将无法恢复，请谨慎操作。</p>
                    <p>退票后，退款将按原支付方式退回，到账时间以银行处理为准。</p>
                </div>
            </div>
        </>
    );
};

export default ReturnTicketPage;