import React, { useCallback, useEffect, useState } from 'react';
import { Card, Tag, Pagination, message, Spin } from 'antd';
import { useNavigate } from 'react-router-dom';
import { ticketAPI } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import "./style.css";

// 车票状态映射 - 对应后端TicketStatus枚举
const TICKET_STATUS = {
    0: { text: '待支付', color: 'orange' },
    1: { text: '未使用', color: 'blue' },
    2: { text: '已使用', color: 'green' },
    3: { text: '已退票', color: 'red' },
    4: { text: '已改签', color: 'purple' },
};

// 票种映射
const TICKET_TYPE = {
    1: '成人票',
    2: '儿童票',
    3: '学生票',
    4: '残疾票',
    5: '军人票',
};

function formatDate(dateString) {
    if (!dateString) return '';
    return dateString.split('T')[0] || dateString.split(' ')[0] || '';
}

function formatTime(timeString) {
    if (!timeString) return '';
    // 处理LocalTime格式 (HH:mm:ss) 或 ISO时间格式
    const timePart = timeString.includes('T') ? 
        timeString.split('T')[1] : 
        timeString.split(' ')[1] || timeString;
    return timePart.slice(0, 5);
}

export default function MyTicketsPage() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [passengerInfo, setPassengerInfo] = useState({});
    const [tickets, setTickets] = useState([]);
    const [filteredTickets, setFilteredTickets] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [activeFilter, setActiveFilter] = useState('all');
    const [loading, setLoading] = useState(false);
    const pageSize = 10;  // 每页显示的票数

    // 获取本人车票数据
    const fetchMyTickets = useCallback(async (status = null) => {
        // 使用测试用户ID，实际项目中应该从认证状态获取
        const testUserId = 1; // 假设用户ID为1
        
        setLoading(true);
        try {
            let response;
            if (status === null) {
                response = await ticketAPI.getMyTickets(testUserId);
            } else {
                response = await ticketAPI.getMyTicketsByStatus(testUserId, status);
            }

            if (response.status === 'SUCCESS' && response.tickets) {
                setTickets(response.tickets);
                
                // 设置乘客信息（从后端返回的用户信息获取）
                if (response.userInfo) {
                    setPassengerInfo({
                        p_name: response.userInfo.passengerName || '未知',
                        p_id: response.userInfo.passengerIdCard || '未知',
                        p_phone: response.userInfo.passengerPhone || '未知',
                    });
                } else if (response.tickets.length > 0) {
                    // 如果没有用户信息，从第一张票获取（兼容旧版本）
                    const firstTicket = response.tickets[0];
                    setPassengerInfo({
                        p_name: firstTicket.passengerName || '未知',
                        p_id: firstTicket.passengerId || '未知',
                        p_phone: firstTicket.passengerPhone || '未知',
                    });
                }
            } else {
                message.error(response.message || '获取车票信息失败');
                setTickets([]);
            }
        } catch (error) {
            console.error('获取车票信息失败:', error);
            message.error('获取车票信息失败，请稍后重试');
            setTickets([]);
        } finally {
            setLoading(false);
        }
    }, []);

    // 初始化加载数据
    useEffect(() => {
        fetchMyTickets();
    }, [fetchMyTickets]);

    // 筛选车票
    const filterTickets = useCallback((filter) => {
        let filtered = [...tickets];
        
        switch (filter) {
            case 'pending':
                filtered = filtered.filter(ticket => ticket.ticketStatus === 0);
                break;
            case 'unused':
                filtered = filtered.filter(ticket => ticket.ticketStatus === 1);
                break;
            case 'used':
                filtered = filtered.filter(ticket => ticket.ticketStatus === 2);
                break;
            case 'refunded':
                filtered = filtered.filter(ticket => ticket.ticketStatus === 3);
                break;
            case 'changed':
                filtered = filtered.filter(ticket => ticket.ticketStatus === 4);
                break;
            case 'all':
            default:
                // 按状态优先级排序：待支付 > 未使用 > 已使用 > 已改签 > 已退票
                filtered.sort((a, b) => {
                    const priority = { 0: 1, 1: 2, 2: 3, 4: 4, 3: 5 };
                    return priority[a.ticketStatus] - priority[b.ticketStatus];
                });
                break;
        }
        
        setFilteredTickets(filtered);
    }, [tickets]);

    // 处理筛选变化
    useEffect(() => {
        filterTickets(activeFilter);
        setCurrentPage(1); // 切换过滤时重置页码
    }, [tickets, activeFilter, filterTickets]);

    // 处理筛选按钮点击
    const handleFilterChange = (filter) => {
        setActiveFilter(filter);
        if (filter === 'all') {
            fetchMyTickets();
        } else {
            const statusMap = {
                'pending': 0,
                'unused': 1,
                'used': 2,
                'refunded': 3,
                'changed': 4,
            };
            fetchMyTickets(statusMap[filter]);
        }
    };

    const handleTicketClick = (ticketId) => {
        navigate(`/ticket-detail?ticketId=${ticketId}`);
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

            {/* 状态筛选按钮 */}
            <div className="filter-bar">
                <button
                    className={`filter-btn ${activeFilter === 'all' ? 'active' : ''}`}
                    onClick={() => handleFilterChange('all')}
                >
                    全部
                </button>
                <button
                    className={`filter-btn ${activeFilter === 'pending' ? 'active' : ''}`}
                    onClick={() => handleFilterChange('pending')}
                >
                    待支付
                </button>
                <button
                    className={`filter-btn ${activeFilter === 'unused' ? 'active' : ''}`}
                    onClick={() => handleFilterChange('unused')}
                >
                    未使用
                </button>
                <button
                    className={`filter-btn ${activeFilter === 'used' ? 'active' : ''}`}
                    onClick={() => handleFilterChange('used')}
                >
                    已使用
                </button>
                <button
                    className={`filter-btn ${activeFilter === 'changed' ? 'active' : ''}`}
                    onClick={() => handleFilterChange('changed')}
                >
                    已改签
                </button>
                <button
                    className={`filter-btn ${activeFilter === 'refunded' ? 'active' : ''}`}
                    onClick={() => handleFilterChange('refunded')}
                >
                    已退票
                </button>
            </div>

            {loading ? (
                <div style={{ textAlign: 'center', padding: '50px' }}>
                    <Spin size="large" />
                    <div style={{ marginTop: '16px' }}>加载中...</div>
                </div>
            ) : (
                <>
                    <div className="tickets-list">
                        {currentTickets.map(ticket => (
                            <div
                                key={ticket.ticketId}
                                className="ticket-item"
                                onClick={() => handleTicketClick(ticket.ticketId)}
                                role="button"
                                tabIndex={0}
                                onKeyPress={(e) => { if (e.key === 'Enter') handleTicketClick(ticket.ticketId); }}
                            >
                                <div className="ticket-header">
                                    <div className="train-details">
                                        <span className="bold-text">{formatDate(ticket.travelDate)}</span> &nbsp;
                                        <span>{ticket.trainNumber}</span>
                                        <span className="small-text">次</span> &nbsp;
                                        {ticket.departureStationName} &nbsp;
                                        <span className="small-text">（</span>
                                        <span className="bold-text">{formatTime(ticket.departureTime)}</span>
                                        <span className="small-text">开）</span> &nbsp;
                                        — {ticket.arrivalStationName} &nbsp;
                                        <span className="small-text">（</span>
                                        <span className="bold-text">{formatTime(ticket.arrivalTime)}</span>
                                        <span className="small-text">到）</span>
                                    </div>
                                    <Tag color={TICKET_STATUS[ticket.ticketStatus]?.color}>
                                        {TICKET_STATUS[ticket.ticketStatus]?.text}
                                    </Tag>
                                </div>

                                <div className="ticket-details">
                                    <span>席别：{ticket.carriageTypeName || '未知'}</span>
                                    <span>票种：{TICKET_TYPE[ticket.ticketType] || '未知'}</span>
                                    <span>车厢座位：{ticket.carriageNumber}车{ticket.seatNumber}座</span>
                                    <span>票价：¥{ticket.price}</span>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* 分页组件 */}
                    {filteredTickets.length > 0 && (
                        <Pagination
                            current={currentPage}
                            pageSize={pageSize}
                            total={filteredTickets.length}
                            onChange={setCurrentPage}
                            showSizeChanger={false}
                            style={{ textAlign: 'center', marginTop: '16px' }}
                        />
                    )}

                    {/* 空状态 */}
                    {filteredTickets.length === 0 && !loading && (
                        <div style={{ textAlign: 'center', padding: '50px', color: '#888' }}>
                            暂无车票信息
                        </div>
                    )}
                </>
            )}

            {/* 温馨提示 */}
            <div style={{ textAlign: 'center', marginTop: '16px', color: '#888' }}>
                温馨提示：本页仅显示近一年来或近100条车票信息
            </div>
        </Card>
    );
}