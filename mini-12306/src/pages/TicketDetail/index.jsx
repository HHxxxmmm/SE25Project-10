import React, { useState, useEffect } from 'react';
import { Card, Typography, Divider } from 'antd';
import { useNavigate } from 'react-router-dom';
import './style.css'; // 样式文件
import generateMyTicketsData from '../../mock/MyTickets'; // 模拟数据函数

const { Text } = Typography;

const TicketDetailPage = () => {
    const [ticket, setTicket] = useState(null);
    const navigate = useNavigate();

    // 模拟获取车票数据
    useEffect(() => {
        const data = generateMyTicketsData();
        if (data.news.length > 0) {
            const randomIndex = Math.floor(Math.random() * data.news.length);
            setTicket(data.news[randomIndex]);
        }
    }, []);

    if (!ticket) {
        return <div>加载中...</div>;
    }

    const statusMap = {
        1: { text: '待完成', colorClass: 'status-pending' },
        2: { text: '已完成', colorClass: 'status-completed' },
    };

    const seatMap = {
        1: '头等',
        2: '商务',
        3: '二等',
        4: '无座',
    };

    const tips = [
        '请提前15分钟到达车站，避免误车。',
        '车票一经售出，退票可能会产生一定费用，请慎重考虑。',
        '乘车时请携带有效身份证件，配合工作人员查验。',
    ];

    return (
        <Card className="ticket-detail-card" bordered={false} bodyStyle={{ padding: 0 }}>
            <div className="card-title">
                <span className="back-link" onClick={() => navigate(-1)}>返回</span>
                车票详情
                <Text className={`ticket-status ${statusMap[ticket.t_status]?.colorClass || ''}`}>
                    {statusMap[ticket.t_status]?.text || '未知状态'}
                </Text>
            </div>

            <Divider />

            <div className="train-info-card">
                <div className="train-date">{ticket.t_time.slice(0, 16)}</div>
                <div className="train-route">
                    <div className="station-block">
                        <div className="station-name">{ticket.t_from_city}{ticket.t_from_station}</div>
                        <div className="station-time">{ticket.t_time.slice(11, 16)}开</div>
                    </div>

                    <div className="arrow-block">
                        <div className="train-number">{ticket.train_id}</div>
                        <div className="arrow">→</div>
                    </div>

                    <div className="station-block">
                        <div className="station-name">{ticket.t_to_city}{ticket.t_to_station}</div>
                        <div className="station-time">{ticket.t_arrival_time.slice(11, 16)}到</div>
                    </div>
                </div>
            </div>

            <Divider />

            <div className="ticket-info">
                <div><strong>乘车人：</strong>{ticket.p_name}</div>
                <div><strong>身份证号：</strong>{ticket.p_id}</div>
                <div><strong>手机号：</strong>{ticket.p_phone}</div>
                <div><strong>席别：</strong>{seatMap[ticket.t_seat] || '未知'}</div>
                <div><strong>票种：</strong>{ticket.ticket_type}</div>
                <div><strong>车厢座位：</strong>{ticket.carriage_seat}</div>
                <div><strong>票价：</strong>¥{ticket.t_price}</div>
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