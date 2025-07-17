import React, { useCallback, useEffect, useState } from 'react';
import { Card, Tag, Pagination, Input, DatePicker, Button, Select, Space, Divider, message } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { SearchOutlined, EnvironmentOutlined, UserOutlined, FileTextOutlined } from '@ant-design/icons';
import { orderAPI } from '../../services/api';
import "./style.css";

const { Search } = Input;
const { Option } = Select;

// 订单状态映射
const ORDER_STATUS = {
    0: { text: '待支付', color: 'orange' },
    1: { text: '已支付', color: 'green' },
    2: { text: '已支付', color: 'green' }, // 已支付状态
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
    if (!datetime) return '';
    // 处理LocalDate或LocalDateTime格式
    if (typeof datetime === 'string') {
        return datetime.split('T')[0] || datetime.split(' ')[0] || '';
    }
    return '';
}

function formatTime(datetime) {
    if (!datetime) return '';
    // 处理LocalTime或LocalDateTime格式
    if (typeof datetime === 'string') {
        const timePart = datetime.split('T')[1] || datetime.split(' ')[1] || '';
        return timePart.slice(0, 5) || '';
    }
    return '';
}

function formatDateTime(datetime) {
    if (!datetime) return '';
    if (typeof datetime === 'string') {
        const datePart = datetime.split('T')[0] || datetime.split(' ')[0] || '';
        const timePart = datetime.split('T')[1] || datetime.split(' ')[1] || '';
        const time = timePart ? timePart.slice(0, 5) : '';
        return `${datePart} ${time}`;
    }
    return '';
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
        orderNumber: '',
        trainNumber: '',
        orderStatus: 'all',
        startDate: null,
        endDate: null
    });

    // 获取订单数据
    const fetchOrders = useCallback(async () => {
        try {
            setLoading(true);
            const response = await orderAPI.getMyOrders(1); // 使用测试用户ID=1
            
            console.log('订单API响应:', response);
            
            if (response.status === 'SUCCESS') {
                console.log('订单数据:', response.orders);
                setOrders(response.orders || []);
                setFilteredOrders(response.orders || []);
            } else {
                message.error('获取订单列表失败: ' + response.message);
                setOrders([]);
                setFilteredOrders([]);
            }
        } catch (error) {
            console.error('获取订单列表失败:', error);
            message.error('获取订单列表失败，请稍后重试');
            setOrders([]);
            setFilteredOrders([]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchOrders();
    }, [fetchOrders]);

    // 过滤订单
    const filterOrders = useCallback(() => {
        console.log('开始过滤订单，原始订单数量:', orders.length);
        let filtered = [...orders];
        
        if (searchForm.orderNumber) {
            filtered = filtered.filter(order => 
                order.orderNumber && order.orderNumber.includes(searchForm.orderNumber)
            );
        }
        
        if (searchForm.trainNumber) {
            filtered = filtered.filter(order => 
                order.trainNumber && order.trainNumber.includes(searchForm.trainNumber)
            );
        }
        
        if (searchForm.orderStatus !== 'all') {
            filtered = filtered.filter(order => 
                order.orderStatus === parseInt(searchForm.orderStatus)
            );
        }
        
        if (searchForm.startDate && searchForm.endDate) {
            filtered = filtered.filter(order => {
                const orderDate = new Date(order.orderTime);
                return orderDate >= searchForm.startDate && orderDate <= searchForm.endDate;
            });
        }
        
        // 按订单时间倒序排列
        filtered.sort((a, b) => new Date(b.orderTime) - new Date(a.orderTime));
        
        console.log('过滤后订单数量:', filtered.length);
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
        navigate(`/order-detail?orderId=${order.orderId}`);
    };

    const handlePayOrder = (order, e) => {
        e.stopPropagation();
        navigate(`/payment?orderId=${order.orderId}`);
    };

    const handleCancelOrder = async (order, e) => {
        e.stopPropagation();
        try {
            const response = await orderAPI.cancelOrder(order.orderId, 1);
            if (response.status === 'SUCCESS') {
                message.success('订单取消成功');
                fetchOrders(); // 重新获取订单列表
            } else {
                message.error(response.message || '取消订单失败');
            }
        } catch (error) {
            console.error('取消订单失败:', error);
            message.error('取消订单失败，请稍后重试');
        }
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
    
    console.log('当前页面订单数量:', currentOrders.length);
    console.log('当前订单数据:', currentOrders);

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
                                    value={searchForm.orderNumber}
                                    onChange={(e) => setSearchForm(prev => ({ ...prev, orderNumber: e.target.value }))}
                                    className="search-input"
                                />
                            </div>
                            <div className="search-item">
                                <label className="search-label">车次号</label>
                                <Input
                                    placeholder="请输入车次号"
                                    value={searchForm.trainNumber}
                                    onChange={(e) => setSearchForm(prev => ({ ...prev, trainNumber: e.target.value }))}
                                    className="search-input"
                                />
                            </div>
                            <div className="search-item">
                                <label className="search-label">订单状态</label>
                                <Select
                                    value={searchForm.orderStatus}
                                    onChange={(value) => setSearchForm(prev => ({ ...prev, orderStatus: value }))}
                                    className="search-select"
                                >
                                    <Option value="all">全部状态</Option>
                                    <Option value="0">待支付</Option>
                                    <Option value="1">已支付</Option>
                                    <Option value="2">已支付</Option>
                                    <Option value="3">已取消</Option>
                                </Select>
                            </div>
                            <div className="search-item">
                                <label className="search-label">下单时间</label>
                                <DatePicker.RangePicker
                                    value={[searchForm.startDate, searchForm.endDate]}
                                    onChange={(dates) => setSearchForm(prev => ({ 
                                        ...prev, 
                                        startDate: dates?.[0]?.toDate(), 
                                        endDate: dates?.[1]?.toDate() 
                                    }))}
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
                                key={order.orderId}
                                className="order-item"
                                onClick={() => handleOrderClick(order)}
                                role="button"
                                tabIndex={0}
                                onKeyPress={(e) => { if (e.key === 'Enter') handleOrderClick(order); }}
                            >
                                {/* 订单头部 - 发车时间 */}
                                <div className="order-header">
                                    <div className="departure-time">
                                        <span className="departure-label">发车时间</span>
                                        <span className="departure-datetime">{formatDateTime(order.departureDate + ' ' + order.departureTime)}</span>
                                    </div>
                                    <Tag color={ORDER_STATUS[order.orderStatus]?.color}>
                                        {ORDER_STATUS[order.orderStatus]?.text}
                                    </Tag>
                                </div>

                                {/* 订单主体内容 */}
                                <div className="order-content">
                                    {/* 左侧：车次和路线信息 */}
                                    <div className="order-left">
                                        <div className="order-info">
                                            <span className="order-number">订单号：{order.orderNumber}</span>
                                            <span className="order-time">下单时间：{formatDateTime(order.orderTime)}</span>
                                        </div>
                                        
                                        <div className="train-info">
                                            <div className="train-header">
                                                <Tag color={TRAIN_TYPES[order.trainNumber?.[0]]?.color}>
                                                    {TRAIN_TYPES[order.trainNumber?.[0]]?.text}
                                                </Tag>
                                                <span className="train-number">{order.trainNumber}</span>
                                            </div>
                                            
                                            <div className="route-info">
                                                <div className="departure">
                                                    <div className="station-name">{order.departureStationName}</div>
                                                    <div className="time">{formatTime(order.departureTime)}</div>
                                                </div>
                                                <div className="journey-line">
                                                    <div className="line"></div>
                                                    <div className="arrow">→</div>
                                                </div>
                                                <div className="arrival">
                                                    <div className="station-name">{order.arrivalStationName}</div>
                                                    <div className="time">{formatTime(order.arrivalTime)}</div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* 右侧：信息卡片 */}
                                    <div className="order-right">
                                        <div className="info-card">
                                            <div className="info-item">
                                                <span className="info-label">乘车人数</span>
                                                <span className="info-value">{order.ticketCount}人</span>
                                            </div>
                                            <div className="info-item">
                                                <span className="info-label">总价</span>
                                                <span className="info-value price">¥{order.totalAmount}</span>
                                            </div>
                                            <div className="info-item">
                                                <span className="info-label">发车日期</span>
                                                <span className="info-value">{formatDate(order.departureDate)}</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* 订单操作 */}
                                <div className="order-actions">
                                    {order.orderStatus === 0 && (
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
                                    {order.orderStatus === 1 && (
                                        <Button size="small">
                                            查看详情
                                        </Button>
                                    )}
                                    {order.orderStatus === 2 && (
                                        <Button size="small">
                                            查看详情
                                        </Button>
                                    )}
                                    {order.orderStatus === 3 && (
                                        <Button size="small">
                                            重新预订
                                        </Button>
                                    )}
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