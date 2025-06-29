import React, { useCallback, useEffect, useState } from 'react';
import { Card, Tag, Pagination, Input, DatePicker, Button, Select, Space, Divider } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { SearchOutlined, ClockCircleOutlined, EnvironmentOutlined } from '@ant-design/icons';
import generateTicketsforOneData from '../../mock/Train'; // 请根据实际路径调整
import "./style.css";

const { Search } = Input;
const { Option } = Select;

// 席别映射
const SEAT_TYPES = {
    1: { text: '头等座', color: 'gold' },
    2: { text: '商务座', color: 'purple' },
    3: { text: '二等座', color: 'blue' },
    4: { text: '无座', color: 'red' },
};

// 车次类型映射
const TRAIN_TYPES = {
    'G': { text: '高铁', color: 'green' },
    'K': { text: '快速', color: 'orange' },
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

// 限制日期选择：只能选择今天至15天后的日期
const disabledDate = (current) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const maxDate = new Date();
    maxDate.setDate(today.getDate() + 15);
    maxDate.setHours(23, 59, 59, 999);
    
    return current && (current.isBefore(today) || current.isAfter(maxDate));
};

export default function TrainsPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const [trains, setTrains] = useState([]);
    const [filteredTrains, setFilteredTrains] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [loading, setLoading] = useState(false);
    const pageSize = 10;

    // 搜索表单状态
    const [searchForm, setSearchForm] = useState({
        from: '',
        to: '',
        date: null,
        trainType: 'all'
    });

    // 从URL参数获取搜索条件
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const from = params.get('from') || '';
        const to = params.get('to') || '';
        const date = params.get('date') || '';
        
        setSearchForm(prev => ({
            ...prev,
            from,
            to,
            date: date ? new Date(date) : null
        }));
    }, [location]);

    // 生成车次数据
    useEffect(() => {
        setLoading(true);
        setTimeout(() => {
            const data = generateTicketsforOneData();
            if (data.news && data.news.length > 0) {
                setTrains(data.news);
                setFilteredTrains(data.news);
            }
            setLoading(false);
        }, 500);
    }, []);

    // 过滤车次
    const filterTrains = useCallback(() => {
        let filtered = [...trains];
        
        if (searchForm.from) {
            filtered = filtered.filter(train => 
                train.t_from.includes(searchForm.from) || 
                train.t_path.some(station => station.includes(searchForm.from))
            );
        }
        
        if (searchForm.to) {
            filtered = filtered.filter(train => 
                train.t_to.includes(searchForm.to) || 
                train.t_path.some(station => station.includes(searchForm.to))
            );
        }
        
        if (searchForm.trainType !== 'all') {
            filtered = filtered.filter(train => 
                train.train_id.startsWith(searchForm.trainType)
            );
        }
        
        setFilteredTrains(filtered);
        setCurrentPage(1);
    }, [trains, searchForm]);

    useEffect(() => {
        filterTrains();
    }, [filterTrains]);

    const handleSearch = () => {
        filterTrains();
    };

    const handleTrainClick = (train) => {
        navigate(`/submit-order?trainId=${train.train_id}&from=${train.t_from}&to=${train.t_to}&date=${formatDate(train.t_start_time)}`);
    };

    const handleDateChange = (date) => {
        setSearchForm(prev => ({ ...prev, date }));
    };

    const cardTitle = (
        <div className="card-title">
            <SearchOutlined style={{ marginRight: 8 }} />
            车票查询与购买
        </div>
    );

    // 分页数据
    const startIndex = (currentPage - 1) * pageSize;
    const currentTrains = filteredTrains.slice(startIndex, startIndex + pageSize);

    return (
        <div className="trains-page">
            <Card
                title={cardTitle}
                className="trains-card"
                headStyle={{ padding: 0, borderRadius: '4px 4px 0 0' }}
            >
                {/* 搜索表单 */}
                <div className="search-section">
                    <div className="search-form">
                        <div className="search-row">
                            <div className="search-item">
                                <label className="search-label">出发地</label>
                                <Input
                                    placeholder="请输入出发地"
                                    value={searchForm.from}
                                    onChange={(e) => setSearchForm(prev => ({ ...prev, from: e.target.value }))}
                                    className="search-input"
                                />
                            </div>
                            <div className="search-item">
                                <label className="search-label">目的地</label>
                                <Input
                                    placeholder="请输入目的地"
                                    value={searchForm.to}
                                    onChange={(e) => setSearchForm(prev => ({ ...prev, to: e.target.value }))}
                                    className="search-input"
                                />
                            </div>
                            <div className="search-item">
                                <label className="search-label">出发日期</label>
                                <DatePicker
                                    value={searchForm.date}
                                    onChange={handleDateChange}
                                    className="search-date"
                                    placeholder="选择日期"
                                    disabledDate={disabledDate}
                                />
                            </div>
                            <div className="search-item">
                                <label className="search-label">车次类型</label>
                                <Select
                                    value={searchForm.trainType}
                                    onChange={(value) => setSearchForm(prev => ({ ...prev, trainType: value }))}
                                    className="search-select"
                                >
                                    <Option value="all">全部</Option>
                                    <Option value="G">高铁</Option>
                                    <Option value="K">快速</Option>
                                </Select>
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

                {/* 车次列表 */}
                <div className="trains-list">
                    {currentTrains.length > 0 ? (
                        currentTrains.map(train => (
                            <div
                                key={train.train_id}
                                className="train-item"
                                onClick={() => handleTrainClick(train)}
                                role="button"
                                tabIndex={0}
                                onKeyPress={(e) => { if (e.key === 'Enter') handleTrainClick(train); }}
                            >
                                <div className="train-header">
                                    <div className="train-info">
                                        <Tag color={TRAIN_TYPES[train.train_id[0]]?.color}>
                                            {TRAIN_TYPES[train.train_id[0]]?.text}
                                        </Tag>
                                        <span className="train-number">{train.train_id}</span>
                                        <span className="train-duration">
                                            <ClockCircleOutlined style={{ marginRight: 4 }} />
                                            {calculateDuration(train.t_start_time, train.t_end_time)}
                                        </span>
                                    </div>
                                    <div className="train-route">
                                        <EnvironmentOutlined style={{ marginRight: 4 }} />
                                        {train.t_station_number}站
                                    </div>
                                </div>

                                <div className="train-details">
                                    <div className="time-info">
                                        <div className="departure">
                                            <div className="time">{formatTime(train.t_start_time)}</div>
                                            <div className="station">{train.t_from}</div>
                                        </div>
                                        <div className="journey-line">
                                            <div className="line"></div>
                                            <div className="arrow">→</div>
                                        </div>
                                        <div className="arrival">
                                            <div className="time">{formatTime(train.t_end_time)}</div>
                                            <div className="station">{train.t_to}</div>
                                        </div>
                                    </div>

                                    <div className="seats-info">
                                        {train.seat.map((seatType, index) => (
                                            <div key={seatType} className="seat-item">
                                                <Tag color={SEAT_TYPES[seatType]?.color}>
                                                    {SEAT_TYPES[seatType]?.text}
                                                </Tag>
                                                <span className="seat-count">
                                                    余{train.seat_number[index]}张
                                                </span>
                                                <span className="seat-price">
                                                    ¥{train.seat_price[index]}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                <div className="train-footer">
                                    <div className="route-stations">
                                        途经：{train.t_path.join(' → ')}
                                    </div>
                                    <Button type="primary" size="small">
                                        购买
                                    </Button>
                                </div>
                            </div>
                        ))
                    ) : (
                        <div className="no-trains">
                            <div className="no-trains-content">
                                <SearchOutlined style={{ fontSize: 48, color: '#ccc', marginBottom: 16 }} />
                                <p>暂无符合条件的车次</p>
                                <p>请尝试调整搜索条件</p>
                            </div>
                        </div>
                    )}
                </div>

                {/* 分页组件 */}
                {filteredTrains.length > pageSize && (
                    <Pagination
                        current={currentPage}
                        pageSize={pageSize}
                        total={filteredTrains.length}
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
                        <li>请提前30分钟到达车站办理乘车手续</li>
                        <li>请携带有效身份证件乘车</li>
                        <li>如需退票，请在发车前2小时办理</li>
                    </ul>
                </div>
            </Card>
        </div>
    );
}