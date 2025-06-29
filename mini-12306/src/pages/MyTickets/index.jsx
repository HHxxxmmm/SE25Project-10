import React, { useCallback, useEffect, useState } from 'react';
import { Card, Tag, Pagination } from 'antd';
import { useNavigate } from 'react-router-dom';
import generateTicketsforOneData from '../../mock/TicketsforOne'; // 请根据实际路径调整
import "./style.css";

const TICKET_STATUS = {
    1: { text: '待完成', color: 'red' },
    2: { text: '已完成', color: 'blue' },
};

function formatDate(datetime) {
    return datetime?.split(' ')[0] || '';
}

function formatTime(datetime) {
    return datetime?.split(' ')[1]?.slice(0, 5) || '';
}

export default function MyTicketsPage() {
    const navigate = useNavigate();
    const [passengerInfo, setPassengerInfo] = useState({});
    const [tickets, setTickets] = useState([]);
    const [filteredTickets, setFilteredTickets] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [activeFilter, setActiveFilter] = useState('all');
    const pageSize = 10;  // 每页显示的票数

    useEffect(() => {
        const data = generateTicketsforOneData();
        if (data.news && data.news.length > 0) {
            const { p_name, p_id, p_phone } = data.news[0];
            setPassengerInfo({ p_name, p_id, p_phone });
            setTickets(data.news);
        }
    }, []);

    const filterTickets = useCallback((filter) => {
        let filtered = [...tickets];
        if (filter === 'completed') {
            filtered = filtered.filter(ticket => ticket.t_status === 2);
        } else if (filter === 'pending') {
            filtered = filtered.filter(ticket => ticket.t_status === 1);
        } else if (filter === 'all') {
            // 按待完成优先排序
            filtered.sort((a, b) => a.t_status - b.t_status);
        }
        setFilteredTickets(filtered);
    }, [tickets]);

    useEffect(() => {
        filterTickets(activeFilter);
        setCurrentPage(1); // 切换过滤时重置页码
    }, [tickets, activeFilter, filterTickets]);

    const handleTicketClick = () => {
        navigate('/ticket-detail');
    };

    const cardTitle = (
        <div className="card-title">
            本人车票
        </div>
    );

    // 分页数据
    const startIndex = (currentPage - 1) * pageSize;
    const currentTickets = filteredTickets.slice(startIndex, startIndex + pageSize);

    return (
        <Card
            title={cardTitle}
            className="my-tickets-card"
            headStyle={{ padding: 0, borderRadius: '4px 4px 0 0' }}
        >
            <div className="passenger-info">
                <div className="passenger-item">姓名：{passengerInfo.p_name}</div>
                <div className="passenger-item">身份证号：{passengerInfo.p_id}</div>
                <div className="passenger-item">手机号：{passengerInfo.p_phone}</div>
            </div>

            {/* 左对齐方块导航 */}
            <div className="filter-bar">
                <button
                    className={`filter-btn ${activeFilter === 'all' ? 'active' : ''}`}
                    onClick={() => setActiveFilter('all')}
                >
                    全部
                </button>
                <button
                    className={`filter-btn ${activeFilter === 'pending' ? 'active' : ''}`}
                    onClick={() => setActiveFilter('pending')}
                >
                    待完成
                </button>
                <button
                    className={`filter-btn ${activeFilter === 'completed' ? 'active' : ''}`}
                    onClick={() => setActiveFilter('completed')}
                >
                    已完成
                </button>
            </div>

            <div className="tickets-list">
                {currentTickets.map(ticket => (
                    <div
                        key={ticket.t_id}
                        className="ticket-item"
                        onClick={handleTicketClick}
                        role="button"
                        tabIndex={0}
                        onKeyPress={(e) => { if (e.key === 'Enter') handleTicketClick(); }}
                    >
                        <div className="ticket-header">
                            <div className="train-details">
                                <span className="bold-text">{formatDate(ticket.t_time)}</span> &nbsp;
                                <span>{ticket.train_id}</span>
                                <span className="small-text">次</span> &nbsp;
                                {ticket.t_from_city}{ticket.t_from_station} &nbsp;
                                <span className="small-text">（</span>
                                <span className="bold-text">{formatTime(ticket.t_time)}</span>
                                <span className="small-text">开）</span> &nbsp;
                                — {ticket.t_to_city}{ticket.t_to_station} &nbsp;
                                <span className="small-text">（</span>
                                <span className="bold-text">{formatTime(ticket.t_arrival_time)}</span>
                                <span className="small-text">到）</span>
                            </div>
                            <Tag color={TICKET_STATUS[ticket.t_status]?.color}>
                                {TICKET_STATUS[ticket.t_status]?.text}
                            </Tag>
                        </div>

                        <div className="ticket-details">
                            <span>席别：{ticket.t_seat}</span>
                            <span>票种：{ticket.ticket_type}</span>
                            <span>车厢座位：{ticket.carriage_seat}</span>
                            <span>票价：¥{ticket.t_price}</span>
                        </div>
                    </div>
                ))}
            </div>

            {/* 分页组件 */}
            <Pagination
                current={currentPage}
                pageSize={pageSize}
                total={filteredTickets.length}
                onChange={setCurrentPage}
                showSizeChanger={false}
                style={{ textAlign: 'center', marginTop: '16px' }}
            />

            {/* 温馨提示 */}
            <div style={{ textAlign: 'center', marginTop: '16px', color: '#888' }}>
                温馨提示：本页仅显示近一年来或近100条车票信息
            </div>
        </Card>
    );
}