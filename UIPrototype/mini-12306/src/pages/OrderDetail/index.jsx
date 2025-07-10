import React, { useState, useEffect } from 'react';
import { Card, Typography, Divider, Button, Row, message, Spin } from 'antd';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { orderAPI } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import './style.css';

const { Text } = Typography;

// 订单状态映射
const ORDER_STATUS = {
    0: { text: '待支付', colorClass: 'status-pending' },
    1: { text: '已支付', colorClass: 'status-paid' },
    2: { text: '已完成', colorClass: 'status-completed' },
    3: { text: '已取消', colorClass: 'status-cancelled' },
};

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

const OrderDetailPage = () => {
    const [orderDetail, setOrderDetail] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();

    useEffect(() => {
        const fetchOrderDetail = async () => {
            try {
                const orderId = searchParams.get('orderId');
                const currentUserId = user?.userId; // 使用当前登录用户的ID
                
                if (!orderId) {
                    message.error('订单ID不能为空');
                    navigate('/orders');
                    return;
                }

                if (!currentUserId) {
                    message.error('请先登录');
                    navigate('/login');
                    return;
                }

                console.log('获取订单详情:', { orderId, currentUserId });
                const response = await orderAPI.getOrderDetail(orderId, currentUserId);
                console.log('订单详情响应:', response);
                
                if (response && response.orderNumber) {
                    setOrderDetail(response);
                } else {
                    message.error('获取订单详情失败');
                    navigate('/orders');
                }
            } catch (error) {
                console.error('获取订单详情失败:', error);
                message.error('获取订单详情失败，请稍后重试');
                navigate('/orders');
            } finally {
                setLoading(false);
            }
        };

        fetchOrderDetail();
    }, [searchParams, navigate]);

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" />
                <div style={{ marginTop: '16px' }}>加载中...</div>
            </div>
        );
    }

    if (!orderDetail) {
        return <div>订单不存在</div>;
    }

    // 跳转处理函数
    const onTicketClick = (ticketId) => {
        navigate(`/ticket-detail?ticketId=${ticketId}`);
    };

    const onReturnTicket = () => {
        // 获取所有车票的ID
        const ticketIds = orderDetail.tickets.map(ticket => ticket.ticketId);
        const currentUserId = user?.userId; // 使用当前登录用户的ID
        
        // 从URL获取orderId参数
        const orderId = searchParams.get('orderId');
        
        // 将参数传递到退票页面
        navigate(`/return-ticket?orderId=${orderId}&ticketIds=${ticketIds.join(',')}&userId=${currentUserId}`);
    };

    const onChangeTicket = () => {
        navigate('/change-ticket');
    };

    const onCancelOrder = async () => {
        try {
            const currentUserId = user?.userId; // 使用当前登录用户的ID
            // 从URL获取orderId参数
            const orderId = searchParams.get('orderId');
            const response = await orderAPI.cancelOrder(orderId, currentUserId);
            
            if (response.status === 'SUCCESS') {
                message.success('订单取消成功');
                navigate('/orders');
            } else {
                message.error(response.message || '取消订单失败');
            }
        } catch (error) {
            console.error('取消订单失败:', error);
            message.error('取消订单失败，请稍后重试');
        }
    };

    const onPayOrder = () => {
        // 从URL获取orderId参数
        const orderId = searchParams.get('orderId');
        navigate(`/payment?orderId=${orderId}`);
    };

    return (
        <>
            <Card className="order-detail-card" bordered={false} bodyStyle={{ padding: 0 }}>
                <div className="card-title">
                    <Link to="/orders" className="back-link">返回</Link>
                    订单详情
                </div>

                <Row justify="space-between" align="middle" className="info-row">
                    <Text type="secondary" className="order-info-text">
                        订单号: {orderDetail.orderNumber}
                    </Text>
                    <Text type="secondary" className="order-info-text">
                        下单时间: {formatDateTime(orderDetail.orderTime)}
                    </Text>
                    <div className={`order-status ${ORDER_STATUS[orderDetail.orderStatus]?.colorClass || ''}`}>
                        {ORDER_STATUS[orderDetail.orderStatus]?.text || '未知状态'}
                    </div>
                </Row>

                <Divider />

                <div className="train-info-card">
                    <div className="train-date">{formatDate(orderDetail.travelDate)}</div>
                    <div className="train-route">
                        <div className="station-block">
                            <div className="station-name">{orderDetail.departureStation}</div>
                            <div className="station-time">{formatTime(orderDetail.departureTime)}开</div>
                        </div>

                        <div className="arrow-block">
                            <div className="train-number">{orderDetail.trainNumber}</div>
                            <div className="arrow">→</div>
                        </div>

                        <div className="station-block">
                            <div className="station-name">{orderDetail.arrivalStation}</div>
                            <div className="station-time">{formatTime(orderDetail.arrivalTime)}到</div>
                        </div>
                    </div>
                </div>

                <Divider />

                <div className="ticket-table-wrapper">
                    <table className="ticket-table">
                        <thead>
                        <tr>
                            <th style={{ width: '5%' }}>序号</th>
                            <th style={{ width: '12%' }}>姓名</th>
                            <th style={{ width: '22%' }}>身份证号</th>
                            <th style={{ width: '12%' }}>乘客类型</th>
                            <th style={{ width: '12%' }}>席别</th>
                            <th style={{ width: '12%' }}>票种</th>
                            <th style={{ width: '12%' }}>状态</th>
                            <th style={{ width: '14%' }}>票价</th>
                        </tr>
                        </thead>
                        <tbody>
                        {orderDetail.tickets.map((ticket, index) => (
                            <tr
                                key={ticket.ticketId}
                                style={{ cursor: 'pointer' }}
                                onClick={() => onTicketClick(ticket.ticketId)}
                                title="点击查看车票详情"
                            >
                                <td>{index + 1}</td>
                                <td>{ticket.passengerName}</td>
                                <td>{ticket.idCardNumber}</td>
                                <td>{PASSENGER_TYPE[ticket.passengerType] || '未知'}</td>
                                <td>{ticket.carriageType}</td>
                                <td>{TICKET_TYPE[ticket.ticketType] || '未知'}</td>
                                <td>{TICKET_STATUS[ticket.ticketStatus] || '未知'}</td>
                                <td>
                                    <Text className="price-text">¥{ticket.price}</Text>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                <Divider />

                <Row justify="space-between" align="middle" className="footer-row">
                    <div>订单总价: ¥{orderDetail.totalAmount}</div>
                    <div>车票数量: {orderDetail.ticketCount}张</div>
                </Row>

                <div className="button-row">
                    {orderDetail.orderStatus === 0 && (
                        <>
                            <Button type="primary" className="btn-blue" onClick={onPayOrder}>
                                去支付
                            </Button>
                            <Button className="btn-white" onClick={onCancelOrder}>
                                取消订单
                            </Button>
                        </>
                    )}
                    {orderDetail.orderStatus === 1 && (
                        <>
                            <Button type="primary" className="btn-blue" onClick={onReturnTicket}>
                                退票
                            </Button>
                            <Button className="btn-white" onClick={onChangeTicket}>
                                改签
                            </Button>
                        </>
                    )}
                    {orderDetail.orderStatus === 2 && (
                        <>
                            <Button type="primary" className="btn-blue" onClick={onReturnTicket}>
                                退票
                            </Button>
                            <Button className="btn-white" onClick={onChangeTicket}>
                                改签
                            </Button>
                        </>
                    )}
                    {/* orderStatus === 3 (已取消) 不显示按钮 */}
                </div>
            </Card>
        </>
    );
};

export default OrderDetailPage;