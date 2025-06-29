import React, { useCallback, useEffect, useState } from 'react';
import { Card, Tag, Pagination, Input, DatePicker, Button, Select, Space, Divider } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { SearchOutlined, ClockCircleOutlined, EnvironmentOutlined, UserOutlined, FileTextOutlined } from '@ant-design/icons';
import generateOrdersData from '../../mock/Orders'; // 请根据实际路径调整
import "./style.css";

const { Search } = Input;
const { Option } = Select;

// 订单状态映射
const ORDER_STATUS = {
    1: { text: '待支付', color: 'orange' },
    2: { text: '已支付', color: 'green' },
    3: { text: '已取消', color: 'red' },
};

// 车次类型映射
const TRAIN_TYPES = {
    'G': { text: '高铁', color: 'green' },
    'K': { text: '快速', color: 'orange' },
};

// 席别映射
const SEAT_TYPES = {
    1: { text: '头等座', color: 'gold' },
    2: { text: '商务座', color: 'purple' },
    3: { text: '二等座', color: 'blue' },
    4: { text: '无座', color: 'red' },
};

function formatDate(datetime) {
    return datetime?.split(' ')[0] || '';
}

function formatTime(datetime) {
    return datetime?.split(' ')[1]?.slice(0, 5) || '';
}

function calculateDuration(startTime, endTime) {
    if (!startTime || !endTime) return '';
    
    const start = new Date(startTime);
    const end = new Date(endTime);
    const diffMs = end - start;
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    
    return `${diffHours}小时${diffMinutes}分钟`;
}

export default function OrdersPage() {
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]);
    const [filteredOrders, setFilteredOrders] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [loading, setLoading] = useState(false);
    const pageSize = 10;

    // 搜索表单状态
    const [searchForm, setSearchForm] = useState({
        orderId: '',
        trainId: '',
        status: 'all',
        dateRange: null
    });

    // 生成订单数据
    useEffect(() => {
        setLoading(true);
        setTimeout(() => {
            const data = generateOrdersData();
            if (data.news && data.news.length > 0) {
                setOrders(data.news);
                setFilteredOrders(data.news);
            }
            setLoading(false);
        }, 500);
    }, []);

    // 过滤订单
    const filterOrders = useCallback(() => {
        let filtered = [...orders];
        
        if (searchForm.orderId) {
            filtered = filtered.filter(order => 
                order.order_id.includes(searchForm.orderId)
            );
        }
        
        if (searchForm.trainId) {
            filtered = filtered.filter(order => 
                order.train_id.includes(searchForm.trainId)
            );
        }
        
        if (searchForm.status !== 'all') {
            filtered = filtered.filter(order => 
                order.o_status === parseInt(searchForm.status)
            );
        }
        
        if (searchForm.dateRange && searchForm.dateRange.length === 2) {
            const startDate = searchForm.dateRange[0];
            const endDate = searchForm.dateRange[1];
            filtered = filtered.filter(order => {
                const orderDate = new Date(order.o_time);
                return orderDate >= startDate && orderDate <= endDate;
            });
        }
        
        // 按订单时间倒序排列
        filtered.sort((a, b) => new Date(b.o_time) - new Date(a.o_time));
        
        setFilteredOrders(filtered);
        setCurrentPage(1);
    }, [orders, searchForm]);

    useEffect(() => {
        filterOrders();
    }, [filterOrders]);

    const handleSearch = () => {
        filterOrders();
    };

    const handleOrderClick = (order) => {
        navigate(`/order-detail?orderId=${order.order_id}`);
    };

    const handlePayOrder = (order, e) => {
        e.stopPropagation();
        // 这里可以添加支付逻辑
        console.log('支付订单:', order.order_id);
    };

    const handleCancelOrder = (order, e) => {
        e.stopPropagation();
        // 这里可以添加取消订单逻辑
        console.log('取消订单:', order.order_id);
    };

    const cardTitle = (
        <div className="card-title">
            <FileTextOutlined style={{ marginRight: 8 }} />
            我的订单
        </div>
    );

    // 分页数据
    const startIndex = (currentPage - 1) * pageSize;
    const currentOrders = filteredOrders.slice(startIndex, startIndex + pageSize);

    return (
        <div className="orders-page">
            <Card
                title={cardTitle}
                className="orders-card"
                headStyle={{ padding: 0, borderRadius: '4px 4px 0 0' }}
            >
                {/* 搜索表单 */}
                <div className="search-section">
                    <div className="search-form">
                        <div className="search-row">
                            <div className="search-item">
                                <label className="search-label">订单号</label>
                                <Input
                                    placeholder="请输入订单号"
                                    value={searchForm.orderId}
                                    onChange={(e) => setSearchForm(prev => ({ ...prev, orderId: e.target.value }))}
                                    className="search-input"
                                />
                            </div>
                            <div className="search-item">
                                <label className="search-label">车次号</label>
                                <Input
                                    placeholder="请输入车次号"
                                    value={searchForm.trainId}
                                    onChange={(e) => setSearchForm(prev => ({ ...prev, trainId: e.target.value }))}
                                    className="search-input"
                                />
                            </div>
                            <div className="search-item">
                                <label className="search-label">订单状态</label>
                                <Select
                                    value={searchForm.status}
                                    onChange={(value) => setSearchForm(prev => ({ ...prev, status: value }))}
                                    className="search-select"
                                >
                                    <Option value="all">全部状态</Option>
                                    <Option value="1">待支付</Option>
                                    <Option value="2">已支付</Option>
                                    <Option value="3">已取消</Option>
                                </Select>
                            </div>
                            <div className="search-item">
                                <label className="search-label">下单时间</label>
                                <DatePicker.RangePicker
                                    value={searchForm.dateRange}
                                    onChange={(dates) => setSearchForm(prev => ({ ...prev, dateRange: dates }))}
                                    className="search-date"
                                    placeholder={['开始日期', '结束日期']}
                                />
                            </div>
                            <div className="search-item">
                                <Button
                                    type="primary"
                                    icon={<SearchOutlined />}
                                    onClick={handleSearch}
                                    className="search-button"
                                    loading={loading}
                                >
                                    查询
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>

                <Divider />

                {/* 订单列表 */}
                <div className="orders-list">
                    {currentOrders.length > 0 ? (
                        currentOrders.map(order => (
                            <div
                                key={order.order_id}
                                className="order-item"
                                onClick={() => handleOrderClick(order)}
                                role="button"
                                tabIndex={0}
                                onKeyPress={(e) => { if (e.key === 'Enter') handleOrderClick(order); }}
                            >
                                <div className="order-header">
                                    <div className="order-info">
                                        <span className="order-number">订单号：{order.order_id}</span>
                                        <span className="order-time">下单时间：{formatDate(order.o_time)} {formatTime(order.o_time)}</span>
                                    </div>
                                    <Tag color={ORDER_STATUS[order.o_status]?.color}>
                                        {ORDER_STATUS[order.o_status]?.text}
                                    </Tag>
                                </div>

                                <div className="order-details">
                                    <div className="train-info">
                                        <div className="train-header">
                                            <Tag color={TRAIN_TYPES[order.train_id[0]]?.color}>
                                                {TRAIN_TYPES[order.train_id[0]]?.text}
                                            </Tag>
                                            <span className="train-number">{order.train_id}</span>
                                            <span className="train-duration">
                                                <ClockCircleOutlined style={{ marginRight: 4 }} />
                                                {calculateDuration(order.t_time, order.arrive_time)}
                                            </span>
                                        </div>
                                        
                                        <div className="route-info">
                                            <div className="departure">
                                                <div className="time">{formatTime(order.t_time)}</div>
                                                <div className="station">{order.t_from_city}{order.t_from_station}</div>
                                            </div>
                                            <div className="journey-line">
                                                <div className="line"></div>
                                                <div className="arrow">→</div>
                                            </div>
                                            <div className="arrival">
                                                <div className="time">{formatTime(order.arrive_time)}</div>
                                                <div className="station">{order.t_to_city}{order.t_to_station}</div>
                                            </div>
                                        </div>
                                    </div>

                                    <div className="passengers-info">
                                        <div className="passengers-header">
                                            <UserOutlined style={{ marginRight: 4 }} />
                                            乘车人信息
                                        </div>
                                        <div className="passengers-list">
                                            {order.passengers.map((passenger, index) => (
                                                <div key={index} className="passenger-item">
                                                    <div className="passenger-basic">
                                                        <span className="passenger-name">{passenger.name}</span>
                                                        <span className="passenger-id">{passenger.id}</span>
                                                        <span className="passenger-phone">{passenger.phone}</span>
                                                    </div>
                                                    <div className="passenger-ticket">
                                                        <Tag color={SEAT_TYPES[passenger.seat]?.color}>
                                                            {SEAT_TYPES[passenger.seat]?.text}
                                                        </Tag>
                                                        <span className="ticket-type">{passenger.ticket_type}</span>
                                                        <span className="ticket-price">¥{passenger.price}</span>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                </div>

                                <div className="order-footer">
                                    <div className="order-summary">
                                        <span>乘车人数：{order.passengers.length}人</span>
                                        <span>总金额：¥{order.passengers.reduce((sum, p) => sum + p.price, 0)}</span>
                                    </div>
                                    <div className="order-actions">
                                        {order.o_status === 1 && (
                                            <>
                                                <Button 
                                                    type="primary" 
                                                    size="small"
                                                    onClick={(e) => handlePayOrder(order, e)}
                                                >
                                                    立即支付
                                                </Button>
                                                <Button 
                                                    size="small"
                                                    onClick={(e) => handleCancelOrder(order, e)}
                                                >
                                                    取消订单
                                                </Button>
                                            </>
                                        )}
                                        {order.o_status === 2 && (
                                            <Button size="small">
                                                查看详情
                                            </Button>
                                        )}
                                        {order.o_status === 3 && (
                                            <Button size="small">
                                                重新预订
                                            </Button>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))
                    ) : (
                        <div className="no-orders">
                            <div className="no-orders-content">
                                <FileTextOutlined style={{ fontSize: 48, color: '#ccc', marginBottom: 16 }} />
                                <p>暂无符合条件的订单</p>
                                <p>请尝试调整搜索条件</p>
                            </div>
                        </div>
                    )}
                </div>

                {/* 分页组件 */}
                {filteredOrders.length > pageSize && (
                    <Pagination
                        current={currentPage}
                        pageSize={pageSize}
                        total={filteredOrders.length}
                        onChange={setCurrentPage}
                        showSizeChanger={false}
                        showQuickJumper
                        style={{ textAlign: 'center', marginTop: '24px' }}
                    />
                )}

                {/* 温馨提示 */}
                <div className="tips">
                    <p>温馨提示：</p>
                    <ul>
                        <li>待支付订单请在30分钟内完成支付，超时订单将自动取消</li>
                        <li>已支付订单可在发车前2小时申请退票</li>
                        <li>如需改签，请在发车前2小时办理</li>
                    </ul>
                </div>
            </Card>
        </div>
    );
}