import React, { useState, useEffect, useRef } from 'react';
import { Card, Typography, Divider, Checkbox, Button, Row } from 'antd';
import { useNavigate } from 'react-router-dom';
import './style.css';

// 假设 mock 数据生成函数路径和结构一致
import generateOrdersData from '../../mock/Orders';

const { Text } = Typography;

const ReturnTicketPage = () => {
    const [order, setOrder] = useState(null);
    const [selectedTickets, setSelectedTickets] = useState([]);
    const trainInfoCardRef = useRef(null);
    const navigate = useNavigate(); // 引入 useNavigate 用于导航

    useEffect(() => {
        const data = generateOrdersData();
        // 假设o_status===2是已支付订单
        const paidOrders = data.news.filter(order => order.o_status === 2);

        if (paidOrders.length > 0) {
            setOrder(paidOrders[0]);
            setSelectedTickets(paidOrders[0].passengers.map((_, idx) => idx));
        }
    }, []);

    if (!order) {
        return <div>加载中</div>;
    }

    const allSelected = selectedTickets.length === order.passengers.length;
    const indeterminate = selectedTickets.length > 0 && !allSelected;

    const toggleSelect = (index) => {
        setSelectedTickets((prev) =>
            prev.includes(index) ? prev.filter(i => i !== index) : [...prev, index]
        );
    };

    const onSelectAllReturn = (e) => {
        if (e.target.checked) {
            setSelectedTickets(order.passengers.map((_, idx) => idx));
        } else {
            setSelectedTickets([]);
        }
    };

    const totalPrice = order.passengers.reduce((sum, passenger) => sum + passenger.price, 0);

    // 确认退票按钮点击处理函数
    const handleConfirmReturn = () => {
        if (selectedTickets.length > 0) {
            navigate('/orders'); // 跳转到“我的订单”页面（假设路径为 /my-orders）
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
                        订单号: {order.order_id}
                    </Text>
                    <Text type="secondary" className="order-info-text">
                        下单时间: {order.o_time.slice(0, 16)}
                    </Text>
                </Row>

                <Divider />

                <div className="train-info-card" ref={trainInfoCardRef}>
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
                                className={selectedTickets.includes(index) ? 'selected' : ''}
                                onClick={() => toggleSelect(index)}
                            >
                                <td>
                                    <Checkbox
                                        checked={selectedTickets.includes(index)}
                                        onClick={e => e.stopPropagation()}
                                        onChange={() => toggleSelect(index)}
                                    />
                                </td>
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
                    <div>订单总价: ¥{totalPrice}</div>
                </Row>

                <div className="button-row">
                    <Button type="primary" className="btn-blue" onClick={handleConfirmReturn}>
                        确认退票 ({selectedTickets.length})
                    </Button>
                    <Button className="btn-white" onClick={handleCancel}>取消</Button>
                </div>
            </Card>

            <div className="tip-wrapper">
                <div className="tip-content">
                    <p>温馨提示：</p>
                    <p>退票可能会产生一定费用，请提前了解退票政策。</p>
                    <p>确认退票后，车票将无法恢复，请谨慎操作。</p>
                    <p>退票后，退款将按原支付方式退回，到账时间以银行处理为准。</p>
                </div>
            </div>
        </>
    );
};

export default ReturnTicketPage;