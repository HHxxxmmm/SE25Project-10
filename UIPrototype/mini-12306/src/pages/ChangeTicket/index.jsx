import React, { useState, useEffect, useRef } from 'react';
import { Card, Typography, Divider, Checkbox, Button, Row, message, Spin } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { startChangeTicket } from '../../store/actions/changeTicketActions';
import { orderAPI } from '../../services/api';
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
    1: '未使用',
    2: '已使用',
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

const ChangeTicketPage = () => {
    const [orderDetail, setOrderDetail] = useState(null);
    const [selectedTickets, setSelectedTickets] = useState([]);
    const [selectedTicketIds, setSelectedTicketIds] = useState([]);
    const [selectedPassengerIds, setSelectedPassengerIds] = useState([]);
    const [loading, setLoading] = useState(true);
    const trainInfoCardRef = useRef(null);
    const navigate = useNavigate();
    const location = useLocation();
    const dispatch = useDispatch();
    const { user } = useAuth();

    useEffect(() => {
        const fetchOrderDetail = async () => {
            try {
                // 从URL参数获取订单ID
                const params = new URLSearchParams(location.search);
                const orderId = params.get('orderId');
                
                if (!orderId) {
                    message.error('缺少订单ID参数');
                    navigate('/orders');
                    return;
                }

                const currentUserId = user?.userId;
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
                    // 默认选中所有可改签的车票（状态为未使用的车票）
                    const changeableTickets = response.tickets.filter(ticket => ticket.ticketStatus === 1);
                    const changeableTicketIds = changeableTickets.map(ticket => ticket.ticketId);
                    const changeablePassengerIds = changeableTickets.map(ticket => ticket.passengerId);
                    
                    setSelectedTickets(changeableTicketIds);
                    setSelectedTicketIds(changeableTicketIds);
                    setSelectedPassengerIds(changeablePassengerIds);
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
    }, [location.search, navigate, user]);

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

    // 过滤出可改签的车票（状态为未使用的车票）
    const changeableTickets = orderDetail.tickets.filter(ticket => ticket.ticketStatus === 1);

    if (changeableTickets.length === 0) {
        return (
            <div style={{ textAlign: 'center', padding: '50px' }}>
                <div>该订单没有可改签的车票</div>
                <Button onClick={() => navigate('/orders')} style={{ marginTop: '16px' }}>
                    返回订单列表
                </Button>
            </div>
        );
    }

    const allSelected = selectedTickets.length === changeableTickets.length;
    const indeterminate = selectedTickets.length > 0 && !allSelected;

    const toggleSelect = (ticketId) => {
        const ticket = changeableTickets.find(t => t.ticketId === ticketId);
        if (!ticket) return;

        setSelectedTickets((prev) => {
            const newSelected = prev.includes(ticketId) 
                ? prev.filter(id => id !== ticketId) 
                : [...prev, ticketId];
            
            // 同步更新ticketIds和passengerIds
            const newTicketIds = changeableTickets
                .filter(t => newSelected.includes(t.ticketId))
                .map(t => t.ticketId);
            const newPassengerIds = changeableTickets
                .filter(t => newSelected.includes(t.ticketId))
                .map(t => t.passengerId);
            
            setSelectedTicketIds(newTicketIds);
            setSelectedPassengerIds(newPassengerIds);
            
            return newSelected;
        });
    };

    const onSelectAllChange = (e) => {
        if (e.target.checked) {
            const allTicketIds = changeableTickets.map(t => t.ticketId);
            const allPassengerIds = changeableTickets.map(t => t.passengerId);
            setSelectedTickets(allTicketIds);
            setSelectedTicketIds(allTicketIds);
            setSelectedPassengerIds(allPassengerIds);
        } else {
            setSelectedTickets([]);
            setSelectedTicketIds([]);
            setSelectedPassengerIds([]);
        }
    };

    const totalPrice = changeableTickets.reduce((sum, ticket) => sum + ticket.price, 0);

    // 确认改签按钮点击处理
    const handleConfirmChange = () => {
        if (selectedTickets.length === 0) {
            message.warning('请选择要改签的车票');
            return;
        }

        // 构建改签数据
        const changeTicketData = {
            originalOrderId: orderDetail.orderId,
            originalOrderNumber: orderDetail.orderNumber,
            ticketIds: selectedTicketIds,
            passengerIds: selectedPassengerIds,
            departureStation: orderDetail.departureStation,
            arrivalStation: orderDetail.arrivalStation,
            travelDate: orderDetail.travelDate,
            originalTrainInfo: {
                trainId: orderDetail.trainNumber,
                departureTime: orderDetail.departureTime,
                arrivalTime: orderDetail.arrivalTime,
                departureStation: orderDetail.departureStation,
                arrivalStation: orderDetail.arrivalStation
            }
        };

        // 设置改签状态
        dispatch(startChangeTicket(changeTicketData));

        // 跳转到trains页面，传递搜索参数
        const searchParams = new URLSearchParams({
            from: orderDetail.departureStation,
            to: orderDetail.arrivalStation,
            date: orderDetail.travelDate
        });

        navigate(`/trains?${searchParams.toString()}`);
    };

    // 取消按钮点击处理
    const handleCancel = () => {
        navigate(-1); // 返回上一级
    };

    return (
        <>
            <Card className="change-ticket-card" bordered={false} bodyStyle={{ padding: 0 }}>
                <div className="card-title">改签</div>

                <Row justify="space-between" className="info-row">
                    <Text type="secondary" className="order-info-text">
                        订单号: {orderDetail.orderNumber}
                    </Text>
                    <Text type="secondary" className="order-info-text">
                        下单时间: {formatDateTime(orderDetail.orderTime)}
                    </Text>
                </Row>

                <Divider />

                <div className="train-info-card" ref={trainInfoCardRef}>
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
                            <th style={{ width: '5%' }}>
                                <Checkbox
                                    indeterminate={indeterminate}
                                    checked={allSelected}
                                    onChange={onSelectAllChange}
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
                        {changeableTickets.map((ticket, index) => (
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
                                    <Text className="price-text">¥{ticket.price}</Text>
                                </td>
                                <td>{TICKET_STATUS[ticket.ticketStatus] || '未知'}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                <Divider />

                <Row justify="space-between" align="middle" className="footer-row">
                    <div>订单总价: ¥{totalPrice}</div>
                    <div>已选择: {selectedTickets.length}张车票</div>
                </Row>

                <div className="button-row">
                    <Button type="primary" className="btn-blue" onClick={handleConfirmChange}>
                        确认改签 ({selectedTickets.length})
                    </Button>
                    <Button className="btn-white" onClick={handleCancel}>取消</Button>
                </div>
            </Card>

            <div className="tip-wrapper">
                <div className="tip-content">
                    <p>温馨提示：</p>
                    <p>改签可能会产生一定费用，请提前了解改签政策。</p>
                    <p>确认改签后，原车票将被替换，请谨慎操作。</p>
                    <p>改签后，新订单将按原支付方式处理，差价将按实际计算。</p>
                </div>
            </div>
        </>
    );
};

export default ChangeTicketPage;