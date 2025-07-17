import React, { useState, useEffect } from 'react';
import { Card, Typography, Divider, Button, Row, message, Spin } from 'antd';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { orderAPI, waitlistAPI } from '../../services/api';
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

// 候补订单状态映射
const WAITLIST_STATUS = {
    0: { text: '待支付', colorClass: 'status-pending' },
    1: { text: '待兑现', colorClass: 'status-waiting' },
    2: { text: '已兑现', colorClass: 'status-completed' },
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
    const [isWaitlist, setIsWaitlist] = useState(false);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();

    useEffect(() => {
        const fetchOrderDetail = async () => {
            try {
                const orderId = searchParams.get('orderId');
                const waitlistId = searchParams.get('waitlistId');
                const currentUserId = user?.userId;
                
                if (!orderId && !waitlistId) {
                    message.error('订单ID或候补订单ID不能为空');
                    navigate('/orders');
                    return;
                }

                if (!currentUserId) {
                    message.error('请先登录');
                    navigate('/login');
                    return;
                }

                console.log('获取订单详情:', { orderId, waitlistId, currentUserId });
                
                let response;
                if (waitlistId) {
                    // 获取候补订单详情
                    setIsWaitlist(true);
                    response = await waitlistAPI.getWaitlistOrderDetail(currentUserId, waitlistId);
                    console.log('候补订单详情响应:', response);
                    
                    if (response && response.status === 'SUCCESS' && response.waitlistOrder) {
                        setOrderDetail(response.waitlistOrder);
                    } else {
                        message.error('获取候补订单详情失败');
                        navigate('/orders');
                    }
                } else {
                    // 获取普通订单详情
                    setIsWaitlist(false);
                    response = await orderAPI.getOrderDetail(orderId, currentUserId);
                    console.log('普通订单详情响应:', response);
                
                if (response && response.orderNumber) {
                    setOrderDetail(response);
                } else {
                    message.error('获取订单详情失败');
                    navigate('/orders');
                    }
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
    }, [searchParams, navigate, user]);

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
        const ticketIds = isWaitlist ? 
            orderDetail.items.map(item => item.itemId) : 
            orderDetail.tickets.map(ticket => ticket.ticketId);
        const currentUserId = user?.userId;
        
        // 从URL获取orderId或waitlistId参数
        const orderId = searchParams.get('orderId');
        const waitlistId = searchParams.get('waitlistId');
        
        // 将参数传递到退票页面
        if (isWaitlist) {
            navigate(`/return-ticket?waitlistId=${waitlistId}&ticketIds=${ticketIds.join(',')}&userId=${currentUserId}`);
        } else {
        navigate(`/return-ticket?orderId=${orderId}&ticketIds=${ticketIds.join(',')}&userId=${currentUserId}`);
        }
    };

    const onChangeTicket = () => {
        // 从URL获取orderId或waitlistId参数
        const orderId = searchParams.get('orderId');
        const waitlistId = searchParams.get('waitlistId');
        
        if (isWaitlist) {
            navigate(`/change-ticket?waitlistId=${waitlistId}`);
        } else {
        navigate(`/change-ticket?orderId=${orderId}`);
        }
    };

    const onCancelOrder = async () => {
        try {
            const currentUserId = user?.userId;
            const orderId = searchParams.get('orderId');
            const waitlistId = searchParams.get('waitlistId');
            
            let response;
            if (isWaitlist) {
                response = await waitlistAPI.cancelWaitlistOrder(waitlistId, currentUserId);
            } else {
                response = await orderAPI.cancelOrder(orderId, currentUserId);
            }
            
            if (response.status === 'SUCCESS') {
                message.success(isWaitlist ? '候补订单取消成功' : '订单取消成功');
                navigate('/orders');
            } else {
                message.error(response.message || (isWaitlist ? '取消候补订单失败' : '取消订单失败'));
            }
        } catch (error) {
            console.error('取消订单失败:', error);
            message.error('取消订单失败，请稍后重试');
        }
    };

    const onPayOrder = () => {
        const orderId = searchParams.get('orderId');
        const waitlistId = searchParams.get('waitlistId');
        
        if (isWaitlist) {
            navigate(`/payment?waitlistId=${waitlistId}&isWaitlist=true`);
        } else {
        navigate(`/payment?orderId=${orderId}`);
        }
    };

    const onRefundWaitlistOrder = () => {
        const waitlistId = searchParams.get('waitlistId');
        const currentUserId = user?.userId;
        
        // 获取所有可退款的候补订单项ID（待兑现或已兑现状态）
        const refundableItemIds = orderDetail.items
            .filter(item => item.itemStatus === 1 || item.itemStatus === 2)
            .map(item => item.itemId);
        
        if (refundableItemIds.length === 0) {
            message.warning('没有可退款的候补订单项');
            return;
        }
        
        // 跳转到退款页面
        navigate(`/return-ticket?waitlistId=${waitlistId}&ticketIds=${refundableItemIds.join(',')}&userId=${currentUserId}`);
    };

    // 获取状态映射
    const getStatusMapping = () => {
        return isWaitlist ? WAITLIST_STATUS : ORDER_STATUS;
    };

    // 获取状态文本和样式
    const getStatusInfo = (status) => {
        const statusMapping = getStatusMapping();
        return statusMapping[status] || { text: '未知状态', colorClass: '' };
    };

    return (
        <>
            <Card className="order-detail-card" bordered={false} bodyStyle={{ padding: 0 }}>
                <div className="card-title">
                    <Link to="/orders" className="back-link">返回</Link>
                    {isWaitlist ? '候补订单详情' : '订单详情'}
                </div>

                <Row justify="space-between" align="middle" className="info-row">
                    <Text type="secondary" className="order-info-text">
                        {isWaitlist ? '候补订单号' : '订单号'}: {orderDetail.orderNumber}
                    </Text>
                    <Text type="secondary" className="order-info-text">
                        下单时间: {formatDateTime(orderDetail.orderTime)}
                    </Text>
                    <div className={`order-status ${getStatusInfo(orderDetail.orderStatus).colorClass}`}>
                        {getStatusInfo(orderDetail.orderStatus).text}
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
                        {(isWaitlist ? orderDetail.items : orderDetail.tickets) && (isWaitlist ? orderDetail.items : orderDetail.tickets).map((ticket, index) => (
                            <tr
                                key={ticket.ticketId || ticket.itemId}
                                style={{ cursor: isWaitlist ? 'default' : 'pointer' }}
                                onClick={() => !isWaitlist && onTicketClick(ticket.ticketId || ticket.itemId)}
                                title={isWaitlist ? "候补订单项，暂无可查看的车票详情" : "点击查看车票详情"}
                            >
                                <td>{index + 1}</td>
                                <td>{ticket.passengerName}</td>
                                <td>{ticket.idCardNumber}</td>
                                <td>{isWaitlist ? (ticket.passengerTypeText || PASSENGER_TYPE[ticket.passengerType] || '未知') : (PASSENGER_TYPE[ticket.passengerType] || '未知')}</td>
                                <td>{isWaitlist ? ticket.carriageTypeName : ticket.carriageType}</td>
                                <td>{isWaitlist ? (ticket.ticketTypeText || TICKET_TYPE[ticket.ticketType] || '未知') : (TICKET_TYPE[ticket.ticketType] || '未知')}</td>
                                <td>{isWaitlist ? (ticket.itemStatusText || '未知') : (TICKET_STATUS[ticket.ticketStatus] || '未知')}</td>
                                <td>
                                    <Text className="price-text">¥{ticket.price || '待定'}</Text>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                <Divider />

                <Row justify="space-between" align="middle" className="footer-row">
                    <div>订单总价: ¥{orderDetail.totalAmount}</div>
                    <div>车票数量: {isWaitlist ? (orderDetail.ticketCount || 0) : orderDetail.ticketCount}张</div>
                </Row>

                <div className="button-row">
                    {orderDetail.orderStatus === 0 && (
                        <>
                            <Button type="primary" className="btn-blue" onClick={onPayOrder}>
                                {isWaitlist ? '支付候补订单' : '去支付'}
                            </Button>
                            <Button className="btn-white" onClick={onCancelOrder}>
                                {isWaitlist ? '取消候补订单' : '取消订单'}
                            </Button>
                        </>
                    )}
                    {orderDetail.orderStatus === 1 && !isWaitlist && (
                        <>
                            <Button type="primary" className="btn-blue" onClick={onReturnTicket}>
                                退票
                            </Button>
                            <Button className="btn-white" onClick={onChangeTicket}>
                                改签
                            </Button>
                        </>
                    )}
                    {orderDetail.orderStatus === 1 && isWaitlist && (
                        <>
                            <Button type="primary" className="btn-blue" onClick={onRefundWaitlistOrder}>
                                退款
                            </Button>
                        </>
                    )}
                    {orderDetail.orderStatus === 2 && !isWaitlist && (
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