import React, { useState, useEffect } from 'react';
import { Card, Typography, Divider, Button, Row } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import './style.css';
import generateOrdersData from '../../mock/Orders';

const { Text } = Typography;

const OrderDetailPage = () => {
    const [order, setOrder] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        const data = generateOrdersData();
        if (data.news.length > 0) {
            const randomIndex = Math.floor(Math.random() * data.news.length);
            setOrder(data.news[randomIndex]);
        }
    }, []);

    if (!order) {
        return <div>加载中...</div>;
    }

    const statusMap = {
        1: { text: '待支付', colorClass: 'status-pending' },
        2: { text: '已支付', colorClass: 'status-paid' },
        3: { text: '已完成', colorClass: 'status-completed' },
    };

    // 跳转处理函数
    const onTicketClick = () => {
        navigate('/ticket-detail');
    };

    const onReturnTicket = () => {
        navigate('/return-ticket');
    };

    const onChangeTicket = () => {
        navigate('/change-ticket');
    };

    const onCancelOrder = () => {
        navigate('/orders');
    };

    const onPayOrder = () => {
        navigate(`/payment?orderId=${order.order_id}`);
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
                        订单号: {order.order_id}
                    </Text>
                    <Text type="secondary" className="order-info-text">
                        下单时间: {order.o_time.slice(0, 16)}
                    </Text>
                    <div className={`order-status ${statusMap[order.o_status]?.colorClass || ''}`}>
                        {statusMap[order.o_status]?.text || '未知状态'}
                    </div>
                </Row>

                <Divider />

                <div className="train-info-card">
                    <div className="train-date">{order.t_time.slice(0, 16)}</div>
                    <div className="train-route">
                        <div className="station-block">
                            <div className="station-name">{order.t_from_city}{order.t_from_station}</div>
                            <div className="station-time">{order.t_time.slice(11, 16)}开</div>
                        </div>

                        <div className="arrow-block">
                            <div className="train-number">{order.train_id}</div>
                            <div className="arrow">→</div>
                        </div>

                        <div className="station-block">
                            <div className="station-name">{order.t_to_city}{order.t_to_station}</div>
                            <div className="station-time">{order.arrive_time.slice(11, 16)}到</div>
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
                            <th style={{ width: '18%' }}>手机号</th>
                            <th style={{ width: '12%' }}>席别</th>
                            <th style={{ width: '12%' }}>票种</th>
                            <th style={{ width: '14%' }}>票价</th>
                        </tr>
                        </thead>
                        <tbody>
                        {order.passengers.map((ticket, index) => (
                            <tr
                                key={index}
                                style={{ cursor: 'pointer' }}
                                onClick={onTicketClick}
                                title="点击查看车票详情"
                            >
                                <td>{index + 1}</td>
                                <td>{ticket.name}</td>
                                <td>{ticket.id}</td>
                                <td>{ticket.phone}</td>
                                <td>{ticket.seat === 1 ? '头等' : ticket.seat === 2 ? '商务' : ticket.seat === 3 ? '二等' : '无座'}</td>
                                <td>{ticket.ticket_type}</td>
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
                    <div>订单总价: ¥{order.passengers.reduce((sum, passenger) => sum + passenger.price, 0)}</div>
                </Row>

                <div className="button-row">
                    {order.o_status === 1 && (
                        <>
                            <Button type="primary" className="btn-blue" onClick={onPayOrder}>
                                去支付
                            </Button>
                            <Button className="btn-white" onClick={onCancelOrder}>
                                取消订单
                            </Button>
                        </>
                    )}
                    {order.o_status === 2 && (
                        <>
                            <Button type="primary" className="btn-blue" onClick={onReturnTicket}>
                                退票
                            </Button>
                            <Button className="btn-white" onClick={onChangeTicket}>
                                改签
                            </Button>
                        </>
                    )}
                    {/* o_status === 3 不显示按钮 */}
                </div>
            </Card>
        </>
    );
};

export default OrderDetailPage;