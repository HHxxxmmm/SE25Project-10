import React, { useCallback, useEffect, useState } from 'react';
import { Card, Tag, Pagination, Input, DatePicker, Button, Select, Divider, message } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { SearchOutlined, ClockCircleOutlined, EnvironmentOutlined } from '@ant-design/icons';
import { useSelector, useDispatch } from 'react-redux';
import { clearChangeTicket } from '../../store/actions/changeTicketActions';
import { trainAPI } from '../../services/api';
import dayjs from 'dayjs';
import "./style.css";

const { Option } = Select;

// localStorage 键名
const STORAGE_KEYS = {
    SEARCH_FORM: 'trains_search_form',
    SEARCH_RESULTS: 'trains_search_results',
    LAST_SEARCH_TIME: 'trains_last_search_time'
};

// 席别映射
const SEAT_TYPES = {
    1: { text: '商务座', color: 'purple' },
    2: { text: '一等座', color: 'gold' },
    3: { text: '二等座', color: 'blue' },
    4: { text: '硬座', color: 'orange' },
    5: { text: '硬卧', color: 'red' },
    6: { text: '无座', color: 'gray' },
};

// 车次类型映射
const TRAIN_TYPES = {
    'G': { text: '高铁', color: 'green' },
    'D': { text: '动车', color: 'blue' },
    'K': { text: '快速', color: 'orange' },
    'Z': { text: '直达', color: 'purple' },
    'T': { text: '特快', color: 'red' },
};

function formatDate(datetime) {
    if (!datetime) return '';
    // 返回完整的日期时间，格式：YYYY-MM-DD HH:mm
    const dateTime = datetime.split(' ');
    if (dateTime.length >= 2) {
        const date = dateTime[0];
        const time = dateTime[1].slice(0, 5); // 只取 HH:mm 部分
        return `${date} ${time}`;
    }
    return datetime;
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

// 移除日期选择限制，允许选择任意日期
const disabledDate = (current) => {
    return false; // 不限制任何日期
};

// 保存搜索状态到 localStorage
const saveSearchState = (searchForm, searchResults) => {
    try {
        const stateToSave = {
            searchForm: {
                ...searchForm,
                date: searchForm.date ? searchForm.date.format('YYYY-MM-DD') : null
            },
            searchResults,
            timestamp: Date.now()
        };
        localStorage.setItem(STORAGE_KEYS.SEARCH_FORM, JSON.stringify(stateToSave));
    } catch (error) {
        console.error('保存搜索状态失败:', error);
    }
};

// 从 localStorage 恢复搜索状态
const loadSearchState = () => {
    try {
        const saved = localStorage.getItem(STORAGE_KEYS.SEARCH_FORM);
        if (saved) {
            const state = JSON.parse(saved);
            // 检查数据是否在24小时内
            const isExpired = Date.now() - state.timestamp > 24 * 60 * 60 * 1000;
            if (!isExpired) {
                return {
                    searchForm: {
                        ...state.searchForm,
                        date: state.searchForm.date ? dayjs(state.searchForm.date) : null
                    },
                    searchResults: state.searchResults
                };
            }
        }
    } catch (error) {
        console.error('恢复搜索状态失败:', error);
    }
    return null;
};

// 清除搜索状态
const clearSearchState = () => {
    try {
        localStorage.removeItem(STORAGE_KEYS.SEARCH_FORM);
    } catch (error) {
        console.error('清除搜索状态失败:', error);
    }
};

export default function TrainsPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const dispatch = useDispatch();
    const changeTicketState = useSelector(state => state.changeTicket);
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

    // 组件初始化时恢复搜索状态
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const from = params.get('from') || '';
        const to = params.get('to') || '';
        const date = params.get('date') || '';
        
        // 如果URL中有搜索参数，优先使用URL参数
        if (from || to || date) {
            const newSearchForm = {
            from,
            to,
                date: date ? dayjs(date) : null,
                trainType: 'all'
            };
            setSearchForm(newSearchForm);
            
            // 如果有完整的搜索条件，执行搜索
            if (from && to && date) {
                setLoading(true);
                trainAPI.searchTrains(from, to, date)
                    .then(data => {
                        setTrains(data);
                        setFilteredTrains(data);
                        // 保存搜索状态
                        saveSearchState(newSearchForm, data);
                    })
                    .catch((error) => {
                        console.error('搜索车次失败:', error);
                        message.error('搜索失败，请稍后重试');
                        setTrains([]);
                        setFilteredTrains([]);
                    })
                    .finally(() => setLoading(false));
            }
        } else {
            // 如果URL中没有搜索参数，尝试从localStorage恢复
            const savedState = loadSearchState();
            if (savedState) {
                setSearchForm(savedState.searchForm);
                setTrains(savedState.searchResults);
                setFilteredTrains(savedState.searchResults);
            } else {
                // 如果localStorage也没有，加载全部车次数据
        setLoading(true);
                trainAPI.getTrainList()
                    .then(data => {
                        setTrains(data);
                        setFilteredTrains(data);
            })
                    .catch((error) => {
                        console.error('获取车次列表失败:', error);
                setTrains([]);
                setFilteredTrains([]);
            })
            .finally(() => setLoading(false));
            }
        }
    }, [location.search]); // 当URL参数变化时执行

    // 监听页面离开，清除改签状态
    useEffect(() => {
        const handleBeforeUnload = () => {
            if (changeTicketState.isChanging) {
                dispatch(clearChangeTicket());
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [changeTicketState.isChanging, dispatch]);

    // 过滤车次（仅用于本地过滤，主要搜索通过API进行）
    const filterTrains = useCallback(() => {
        let filtered = [...trains];
        
        // 只进行车次类型过滤，站点搜索通过API进行
        if (searchForm.trainType !== 'all') {
            filtered = filtered.filter(train => 
                train.train_id && train.train_id.startsWith(searchForm.trainType)
            );
        }
        
        setFilteredTrains(filtered);
        setCurrentPage(1);
    }, [trains, searchForm.trainType]);

    // 当trains数据或搜索条件变化时，重新过滤
    useEffect(() => {
        if (trains.length > 0) {
        filterTrains();
        }
    }, [filterTrains, trains, searchForm.trainType]);

    const handleSearch = () => {
        // 改签模式下不允许修改出发地和到达地
        if (changeTicketState.isChanging) {
            message.warning('改签模式下不能修改出发地和到达地');
            return;
        }

        // 如果没有填写搜索条件，重新加载全部车次数据
        if (!searchForm.from && !searchForm.to && !searchForm.date) {
            setLoading(true);
            trainAPI.getTrainList()
                .then(data => {
                    setTrains(data);
                    setFilteredTrains(data);
                    // 清除保存的搜索状态
                    clearSearchState();
                })
                .catch((error) => {
                    console.error('获取车次列表失败:', error);
                    message.error('获取车次列表失败，请稍后重试');
                    setTrains([]);
                    setFilteredTrains([]);
                })
                .finally(() => setLoading(false));
            
            // 清除URL参数
            navigate('/trains', { replace: true });
            return;
        }
        
        // 如果有搜索条件，执行搜索
        if (!searchForm.from || !searchForm.to || !searchForm.date) {
            message.warning('请填写完整的搜索条件');
            return;
        }
        
        setLoading(true);
        // 使用 dayjs 格式化日期
        const travelDate = searchForm.date ? searchForm.date.format('YYYY-MM-DD') : '';
        
        // 更新URL参数以保持搜索状态
        const searchParams = new URLSearchParams();
        searchParams.set('from', searchForm.from);
        searchParams.set('to', searchForm.to);
        searchParams.set('date', travelDate);
        navigate(`/trains?${searchParams.toString()}`, { replace: true });
        
        trainAPI.searchTrains(searchForm.from, searchForm.to, travelDate)
            .then(data => {
                setTrains(data);
                setFilteredTrains(data);
                // 保存搜索状态
                saveSearchState(searchForm, data);
            })
            .catch((error) => {
                console.error('搜索车次失败:', error);
                message.error('搜索失败，请稍后重试');
                setTrains([]);
                setFilteredTrains([]);
            })
            .finally(() => setLoading(false));
    };

    const handleTrainClick = (train) => {
        // 从车次数据中提取库存ID列表
        const inventoryIds = train.inventory_ids || [];
        
        // 构建URL参数
        const params = new URLSearchParams();
        params.set('trainId', train.trainId || train.train_id);
        params.set('from', train.fromStation || train.t_from);
        params.set('to', train.toStation || train.t_to);
        params.set('date', train.travelDate || searchForm.date?.format('YYYY-MM-DD'));
        
        // 添加库存ID列表
        if (inventoryIds.length > 0) {
            params.set('inventoryIds', inventoryIds.join(','));
        }
        
        navigate(`/submit-order?${params.toString()}`);
    };

    const handleDateChange = (date) => {
        setSearchForm(prev => ({ ...prev, date }));
    };

    const cardTitle = (
        <div className="card-title">
            <SearchOutlined style={{ marginRight: 8 }} />
            {changeTicketState.isChanging ? '改签车票查询' : '车票查询与购买'}
            {changeTicketState.isChanging && (
                <Tag color="orange" style={{ marginLeft: 8 }}>
                    改签模式
                </Tag>
            )}
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
                {/* 改签模式提示 */}
                {changeTicketState.isChanging && (
                    <div style={{ 
                        backgroundColor: '#fff7e6', 
                        border: '1px solid #ffd591', 
                        borderRadius: '4px', 
                        padding: '12px', 
                        marginBottom: '16px' 
                    }}>
                        <p style={{ margin: 0, color: '#d46b08' }}>
                            <strong>改签模式：</strong>
                            您正在为订单 {changeTicketState.originalOrderNumber || '未知订单'} 进行改签，
                            出发地和到达地已锁定为 {changeTicketState.departureStation} → {changeTicketState.arrivalStation}
                        </p>
                    </div>
                )}

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
                                    disabled={changeTicketState.isChanging}
                                />
                            </div>
                            <div className="search-item">
                                <label className="search-label">目的地</label>
                                <Input
                                    placeholder="请输入目的地"
                                    value={searchForm.to}
                                    onChange={(e) => setSearchForm(prev => ({ ...prev, to: e.target.value }))}
                                    className="search-input"
                                    disabled={changeTicketState.isChanging}
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
                                    <Option value="D">动车</Option>
                                    <Option value="K">快速</Option>
                                    <Option value="Z">直达</Option>
                                    <Option value="T">特快</Option>
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
                                    {searchForm.from && searchForm.to && searchForm.date ? '搜索' : '显示全部'}
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>

                <Divider />

                {/* 车次列表 */}
                <div className="trains-list">
                    {loading ? (
                        <div className="loading-container">
                            <div className="loading-content">
                                <div className="loading-spinner"></div>
                                <p>正在加载车次信息...</p>
                            </div>
                        </div>
                    ) : filteredTrains && filteredTrains.length > 0 ? (
                        currentTrains.map(train => (
                            <div
                                key={train.trainId || train.train_id || Math.random()}
                                className="train-item"
                                onClick={() => handleTrainClick(train)}
                                role="button"
                                tabIndex={0}
                                onKeyPress={(e) => { if (e.key === 'Enter') handleTrainClick(train); }}
                            >
                                <div className="train-header">
                                    <div className="train-info">
                                        <Tag color={TRAIN_TYPES[train.train_id?.[0]]?.color}>
                                            {TRAIN_TYPES[train.train_id?.[0]]?.text || '未知'}
                                        </Tag>
                                        <span className="train-number">{train.train_id || '未知'}</span>
                                        <span className="train-duration">
                                            <ClockCircleOutlined style={{ marginRight: 4 }} />
                                            {formatDate(train.t_start_time)}
                                        </span>
                                    </div>
                                    <div className="train-route">
                                        <EnvironmentOutlined style={{ marginRight: 4 }} />
                                        {train.t_station_number || 0}站
                                    </div>
                                </div>

                                <div className="train-details">
                                    <div className="time-info">
                                        <div className="departure">
                                            <div className="time">{formatTime(train.t_start_time)}</div>
                                            <div className="station">{train.t_from || '未知'}</div>
                                        </div>
                                        <div className="journey-line">
                                            <div className="line"></div>
                                            <div className="arrow">→</div>
                                        </div>
                                        <div className="arrival">
                                            <div className="time">{formatTime(train.t_end_time)}</div>
                                            <div className="station">{train.t_to || '未知'}</div>
                                        </div>
                                    </div>

                                    <div className="seats-info">
                                        {train.seat && train.seat.length > 0 ? (
                                            train.seat.map((seatType, index) => (
                                                <div key={index} className="seat-item">
                                                <Tag color={SEAT_TYPES[seatType]?.color}>
                                                        {SEAT_TYPES[seatType]?.text || '未知'}
                                                </Tag>
                                                <span className="seat-count">
                                                        余{train.seat_number?.[index] || 0}张
                                                </span>
                                                <span className="seat-price">
                                                        ¥{train.seat_price?.[index] || 0}
                                                </span>
                                            </div>
                                            ))
                                        ) : (
                                            <div className="no-seats">暂无座位信息</div>
                                        )}
                                    </div>
                                </div>

                                <div className="train-footer">
                                    <div className="route-stations">
                                        途经：{train.t_path?.join(' → ') || '未知'}
                                    </div>
                                    <Button type="primary" size="small">
                                        {changeTicketState.isChanging ? '选择改签' : '购买'}
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
                        {changeTicketState.isChanging && (
                            <li>改签模式下，出发地和到达地不可修改</li>
                        )}
                    </ul>
                </div>
            </Card>
        </div>
    );
}