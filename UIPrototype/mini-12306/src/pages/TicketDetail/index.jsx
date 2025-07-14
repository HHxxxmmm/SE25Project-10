import React, { useState, useEffect } from 'react';
import { Card, Typography, Divider, message, Spin } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ticketAPI } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import './style.css'; // 样式文件

const { Text } = Typography;

// 车票状态映射
const TICKET_STATUS = {
    0: { text: '待支付', colorClass: 'status-pending' },
    1: { text: '未使用', colorClass: 'status-unused' },
    2: { text: '已使用', colorClass: 'status-used' },
    3: { text: '已退票', colorClass: 'status-refunded' },
    4: { text: '已改签', colorClass: 'status-changed' },
};

// 票种映射
const TICKET_TYPE = {
    1: '成人票',
    2: '儿童票',
    3: '学生票',
    4: '残疾票',
    5: '军人票',
};

// 乘客类型映射
const PASSENGER_TYPE = {
    1: '成人',
    2: '儿童',
    3: '学生',
    4: '残疾军人',
};

const TicketDetailPage = () => {
    const [ticket, setTicket] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();

    // 获取车票详情数据
    useEffect(() => {
        const fetchTicketDetail = async () => {
            const ticketId = searchParams.get('ticketId');
            const currentUserId = user?.userId; // 使用当前登录用户的ID

            if (!ticketId) {
                message.error('车票ID不存在');
                navigate('/my-tickets');
                return;
            }

            if (!currentUserId) {
                message.error('请先登录');
                navigate('/login');
                return;
            }

            setLoading(true);
            try {
                const response = await ticketAPI.getTicketDetail(parseInt(ticketId), currentUserId);
                
                if (response.status === 'SUCCESS' && response.ticket) {
                    setTicket(response.ticket);
                } else {
                    message.error(response.message || '获取车票详情失败');
                    navigate('/my-tickets');
                }
            } catch (error) {
                console.error('获取车票详情失败:', error);
                message.error('获取车票详情失败，请稍后重试');
                navigate('/my-tickets');
            } finally {
                setLoading(false);
            }
        };

        fetchTicketDetail();
    }, [searchParams, navigate]);

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" />
                <div style={{ marginTop: '16px' }}>加载中...</div>
            </div>
        );
    }

    if (!ticket) {
        return <div>车票信息不存在</div>;
    }

    const formatDate = (dateString) => {
        if (!dateString) return '';
        return dateString.split('T')[0] || dateString.split(' ')[0] || '';
    };

    const formatTime = (timeString) => {
        if (!timeString) return '';
        const timePart = timeString.includes('T') ? 
            timeString.split('T')[1] : 
            timeString.split(' ')[1] || timeString;
        return timePart.slice(0, 5);
    };

    const tips = [
        '请提前15分钟到达车站，避免误车。',
        '车票一经售出，退票可能会产生一定费用，请慎重考虑。',
        '乘车时请携带有效身份证件，配合工作人员查验。',
        '请妥善保管车票，遗失不补。',
        '如需改签或退票，请提前办理相关手续。',
    ];

    return (
        <Card className="ticket-detail-card" bordered={false} bodyStyle={{ padding: 0 }}>
            <div className="card-title">
                <span className="back-link" onClick={() => navigate(-1)}>返回</span>
                车票详情
                <span className={`ticket-status ${TICKET_STATUS[ticket.ticketStatus]?.colorClass || ''}`}>
                    {TICKET_STATUS[ticket.ticketStatus]?.text || '未知状态'}
                </span>
            </div>

            <Divider />

            <div className="train-info-card">
                <div className="train-date">{formatDate(ticket.travelDate)} {formatTime(ticket.departureTime)}</div>
                <div className="train-route">
                    <div className="station-block">
                        <div className="station-name">{ticket.departureStationName}</div>
                        <div className="station-time">{formatTime(ticket.departureTime)}开</div>
                    </div>

                    <div className="arrow-block">
                        <div className="train-number">{ticket.trainNumber}</div>
                        <div className="arrow">→</div>
                    </div>

                    <div className="station-block">
                        <div className="station-name">{ticket.arrivalStationName}</div>
                        <div className="station-time">{formatTime(ticket.arrivalTime)}到</div>
                    </div>
                </div>
            </div>

            <Divider />

            <div className="ticket-info">
                <div><strong>乘车人：</strong>{ticket.passengerName}</div>
                <div><strong>身份证号：</strong>{ticket.passengerIdCard}</div>
                <div><strong>手机号：</strong>{ticket.passengerPhone}</div>
                <div><strong>乘客类型：</strong>{PASSENGER_TYPE[ticket.passengerType] || '未知'}</div>
                <div><strong>席别：</strong>{ticket.carriageTypeName || '未知'}</div>
                <div><strong>票种：</strong>{TICKET_TYPE[ticket.ticketType] || '未知'}</div>
                <div><strong>车厢座位：</strong>{ticket.carriageNumber}车{ticket.seatNumber}座</div>
                <div><strong>票价：</strong>¥{ticket.price}</div>
                <div><strong>车票号：</strong>{ticket.ticketNumber}</div>
                <div><strong>订单号：</strong>{ticket.orderNumber}</div>
                <div><strong>订单状态：</strong>{ticket.orderStatusText}</div>
                <div><strong>创建时间：</strong>{ticket.createdTime}</div>
                {ticket.paymentTime && (
                    <div><strong>支付时间：</strong>{ticket.paymentTime}</div>
                )}
            </div>

            <Divider />

            <div className="warm-tips">
                <h3>温馨提示</h3>
                <ul>
                    {tips.map((tip, index) => (
                        <li key={index}>{tip}</li>
                    ))}
                </ul>
            </div>
        </Card>
    );
};

export default TicketDetailPage;