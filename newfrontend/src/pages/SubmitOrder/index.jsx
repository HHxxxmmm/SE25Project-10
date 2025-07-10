import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { message, Spin, Select, Input } from 'antd';
import AddPassenger from '../AddPassenger';
import { orderAPI, passengerAPI, ticketAPI } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import './style.css';

// 自定义消息提示组件
const CustomMessage = ({ type, content, visible, onClose }) => {
    if (!visible) return null;
    
    const getMessageStyle = () => {
        const baseStyle = {
            position: 'fixed',
            top: '60px',
            left: '50%',
            transform: 'translateX(-50%)',
            padding: '8px 16px',
            borderRadius: '4px',
            color: 'white',
            zIndex: 9999,
            fontSize: '14px',
            fontWeight: '500',
            boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
            cursor: 'pointer',
        };
        
        switch (type) {
            case 'success':
                return { ...baseStyle, backgroundColor: '#52c41a' };
            case 'error':
                return { ...baseStyle, backgroundColor: '#ff4d4f' };
            case 'warning':
                return { ...baseStyle, backgroundColor: '#faad14', color: '#000' };
            default:
                return { ...baseStyle, backgroundColor: '#1890ff' };
        }
    };
    
    return (
        <div style={getMessageStyle()} onClick={onClose}>
            {content}
        </div>
    );
};

// 乘客类型映射
const PASSENGER_TYPE = {
    1: '成人',
    2: '儿童',
    3: '学生',
    4: '残疾',
    5: '军人',
};

// 票种映射
const TICKET_TYPE = {
    1: '成人票',
    2: '儿童票',
    3: '学生票',
    4: '残疾票',
    5: '军人票',
};

// 根据乘客类型获取可选择的票种
const getAvailableTicketTypes = (passengerType) => {
    switch (passengerType) {
        case 1: // 成人
            return [1]; // 只能买成人票
        case 2: // 儿童
            return [2]; // 只能买儿童票
        case 3: // 学生
            return [1, 3]; // 可以买成人票和学生票
        case 4: // 残疾
            return [1, 4]; // 可以买成人票和残疾票
        case 5: // 军人
            return [1, 5]; // 可以买成人票和军人票
        default:
            return [1]; // 默认只能买成人票
    }
};

const idTypes = [
    "居民身份证"
];

const seatTypeMap = {
    1: "头等",
    2: "商务座",
    3: "二等座",
    4: "无座"
};

const SubmitOrder = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { user } = useAuth();
    const [searchText, setSearchText] = useState('');
    const [selectedPassengers, setSelectedPassengers] = useState([]);
    const [passengerDetails, setPassengerDetails] = useState({});
    const [showAddPassengerModal, setShowAddPassengerModal] = useState(false);
    const [trainInfo, setTrainInfo] = useState(null);
    const [passengersData, setPassengersData] = useState([]);
    const [passengerIdMap, setPassengerIdMap] = useState({});
    const [passengerTypeMap, setPassengerTypeMap] = useState({});
    const [passengerPhoneMap, setPassengerPhoneMap] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [loading, setLoading] = useState(true);
    const [response, setResponse] = useState(null);

    useEffect(() => {
        const fetchPrepareOrderData = async () => {
            try {
                // 使用当前登录用户的ID
                const currentUserId = user?.userId;
                
                if (!currentUserId) {
                    message.error('请先登录');
                    navigate('/login');
                    return;
                }
                
                // 从URL参数获取库存ID
                const params = new URLSearchParams(location.search);
                const inventoryIdsParam = params.get('inventoryIds');
                let inventoryIds = [];
                
                if (inventoryIdsParam) {
                    // 解析库存ID列表
                    inventoryIds = inventoryIdsParam.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
                }
                
                // 如果没有库存ID，使用默认值（向后兼容）
                if (inventoryIds.length === 0) {
                    inventoryIds = [1, 11, 21];
                }
                
                console.log('获取准备订单信息:', { currentUserId, inventoryIds });
                const response = await orderAPI.prepareOrder(currentUserId, inventoryIds);
                console.log('准备订单信息响应:', response);
                
                if (response && response.trainInfo) {
                    setTrainInfo(response.trainInfo);
                    setPassengersData(response.passengers.map(p => p.realName));
                    setResponse(response);
                    
                    // 构建乘客ID映射
                    const idMap = {};
                    const typeMap = {};
                    const phoneMap = {};
                    response.passengers.forEach(passenger => {
                        idMap[passenger.realName] = passenger.idCardNumber;
                        typeMap[passenger.realName] = passenger.passengerType;
                        phoneMap[passenger.realName] = passenger.phoneNumber;
                    });
                    setPassengerIdMap(idMap);
                    setPassengerTypeMap(typeMap);
                    setPassengerPhoneMap(phoneMap);
                } else {
                    message.error('获取订单准备信息失败');
                    navigate('/trains');
                }
            } catch (error) {
                console.error('获取订单准备信息失败:', error);
                message.error('获取订单准备信息失败，请稍后重试');
                navigate('/trains');
            } finally {
                setLoading(false);
            }
        };

        fetchPrepareOrderData();
    }, [navigate, location.search]);

    const filteredPassengers = passengersData.filter(name =>
        name.includes(searchText)
    );

    const togglePassenger = (name) => {
        setSelectedPassengers(prev => {
            if (prev.includes(name)) {
                const newArr = prev.filter(n => n !== name);
                setPassengerDetails(details => {
                    const newDetails = { ...details };
                    delete newDetails[name];
                    return newDetails;
                });
                return newArr;
            } else {
                const passengerType = passengerTypeMap[name] || 1;
                const availableTicketTypes = getAvailableTicketTypes(passengerType);
                const defaultTicketType = availableTicketTypes[0];
                
                setPassengerDetails(details => ({
                    ...details,
                    [name]: {
                        ticketType: TICKET_TYPE[defaultTicketType],
                        ticketTypeValue: defaultTicketType,
                        seatType: "二等座",
                        idType: idTypes[0],
                        idNumber: passengerIdMap[name] || ''
                    }
                }));
                return [...prev, name];
            }
        });
    };

    const removePassenger = (name) => {
        setSelectedPassengers(prev => prev.filter(n => n !== name));
        setPassengerDetails(details => {
            const newDetails = { ...details };
            delete newDetails[name];
            return newDetails;
        });
    };

    const updateDetail = (name, field, value) => {
        setPassengerDetails(details => ({
            ...details,
            [name]: {
                ...details[name],
                [field]: value
            }
        }));
    };

    // 检查是否可以添加乘车人
    const checkCanAddPassenger = async () => {
        try {
            const currentUserId = user?.userId; // 使用当前登录用户的ID
            const response = await passengerAPI.checkCanAddPassenger(currentUserId);
            
            if (response.allowed) {
                setShowAddPassengerModal(true);
            } else {
                // 使用原生alert确保用户能看到提示
                alert(response.message || '无法添加乘车人');
                message.warning(response.message || '无法添加乘车人');
            }
        } catch (error) {
            console.error('检查添加乘车人权限失败:', error);
            // 使用原生alert确保用户能看到提示
            alert('检查权限失败，请稍后重试');
            message.error('检查权限失败，请稍后重试');
        }
    };

    // 处理提交订单
    const handleSubmitOrder = async () => {
        console.log('开始提交订单...');
                    console.log('选中的乘客:', selectedPassengers);
            console.log('乘客详情:', passengerDetails);
            console.log('响应数据:', response);
            console.log('车厢信息:', response?.carriages);
            
            // 检查乘客详情结构
            selectedPassengers.forEach(name => {
                console.log(`乘客 ${name} 的详情:`, passengerDetails[name]);
            });
        
        if (selectedPassengers.length === 0) {
            message.warning('请至少选择一位乘车人');
            return;
        }

        if (!response || !response.trainInfo) {
            message.error('车次信息不完整，请刷新页面重试');
            return;
        }

        setSubmitting(true);

        try {
            console.log('开始构建购票请求...');
            // 构建购票请求
            const currentUserId = user?.userId; // 使用当前登录用户的ID
            
            // 从response中获取车次信息
            const trainId = response.trainInfo.trainId;
            const departureStopId = response.trainInfo.departureStopId;
            const arrivalStopId = response.trainInfo.arrivalStopId;
            const travelDate = response.trainInfo.travelDate;
            
            console.log('车次信息:', { trainId, departureStopId, arrivalStopId, travelDate });
            
            // 获取每个乘客选择的席别
            console.log('开始获取乘客席别信息...');
            const passengerSeatTypes = selectedPassengers.map(name => {
                const seatType = passengerDetails[name]?.seatType;
                console.log(`乘客 ${name} 的席别:`, seatType);
                if (!seatType) {
                    throw new Error(`乘客 ${name} 未选择席别`);
                }
                // 处理席别名称，去掉价格信息
                const cleanSeatType = seatType.replace(/（¥\d+元）/, '');
                console.log(`乘客 ${name} 的清理后席别:`, cleanSeatType);
                return cleanSeatType;
            });
            console.log('所有乘客席别:', passengerSeatTypes);

            // 根据席别名称找到对应的车厢类型ID
            console.log('开始构建席别映射...');
            console.log('后端返回的车厢信息:', response.carriages);
            const seatTypeToCarriageId = {};
            response.carriages.forEach(carriage => {
                seatTypeToCarriageId[carriage.carriageTypeName] = carriage.carriageTypeId;
                console.log(`席别映射: ${carriage.carriageTypeName} -> ${carriage.carriageTypeId}`);
            });
            console.log('席别映射表:', seatTypeToCarriageId);

            // 验证所有席别都有效
            const invalidSeatTypes = passengerSeatTypes.filter(seatType => !seatTypeToCarriageId[seatType]);
            if (invalidSeatTypes.length > 0) {
                message.error(`无效的席别选择: ${invalidSeatTypes.join(', ')}`);
                setSubmitting(false);
                return;
            }

            // 使用第一个乘客选择的席别作为所有乘客的席别
            const firstPassengerSeatType = passengerSeatTypes[0];
            const carriageTypeId = seatTypeToCarriageId[firstPassengerSeatType];
            
            // 检查所有乘客是否选择了相同的席别
            const differentSeatTypes = passengerSeatTypes.filter(seatType => seatType !== firstPassengerSeatType);
            if (differentSeatTypes.length > 0) {
                message.warning(`检测到不同席别选择，将统一使用 ${firstPassengerSeatType}`);
            }

            // 构建乘客信息列表
            const passengers = selectedPassengers.map(name => {
                const passengerId = response.passengers.find(p => p.realName === name)?.passengerId;
                const ticketTypeValue = passengerDetails[name]?.ticketTypeValue || 1;
                const seatType = passengerDetails[name]?.seatType;
                const carriageTypeId = seatTypeToCarriageId[seatType];
                
                return {
                    passengerId: passengerId,
                    ticketType: ticketTypeValue,
                    carriageTypeId: carriageTypeId
                };
            });

            // 验证所有乘客ID都存在
            const missingPassengers = passengers.filter(p => !p.passengerId);
            if (missingPassengers.length > 0) {
                message.error('部分乘客信息不完整，请刷新页面重试');
                setSubmitting(false);
                return;
            }

            const bookingRequest = {
                userId: currentUserId,
                trainId: trainId,
                departureStopId: departureStopId,
                arrivalStopId: arrivalStopId,
                travelDate: travelDate,
                carriageTypeId: carriageTypeId,
                passengers: passengers
            };

            console.log('发送购票请求:', bookingRequest);
            
            // 保存调试信息到localStorage
            localStorage.setItem('debug_booking_request', JSON.stringify(bookingRequest));
            localStorage.setItem('debug_passenger_details', JSON.stringify(passengerDetails));
            localStorage.setItem('debug_response_data', JSON.stringify(response));

            // 调用后端购票API
            const bookingResponse = await ticketAPI.bookTickets(bookingRequest);
            console.log('购票响应:', bookingResponse);
            
            // 保存响应信息
            localStorage.setItem('debug_booking_response', JSON.stringify(bookingResponse));

            if (bookingResponse.status === 'SUCCESS') {
                // 购票成功，获取订单号
                const orderNumber = bookingResponse.orderNumber;
                console.log('购票成功，订单号:', orderNumber);
                
                message.success('购票成功，正在处理订单...');
                
                // 轮询获取订单ID
                let retryCount = 0;
                const maxRetries = 15; // 增加重试次数
                const pollInterval = 200; // 200毫秒，提高响应速度
                
                const pollOrderId = async () => {
                    try {
                        const orderResponse = await orderAPI.getOrderIdByOrderNumber(orderNumber);
                        console.log('查询订单ID响应:', orderResponse);
                        
                        if (orderResponse.status === 'SUCCESS') {
                            const orderId = orderResponse.orderId;
                            console.log('获取到订单ID:', orderId);
                            
                            // 设置本地存储
                            try {
                                localStorage.setItem('current_order_id', orderId);
                                localStorage.setItem('current_order_number', orderNumber);
                            } catch (e) {
                                console.error('无法保存订单信息:', e);
                            }
                            
                            // 跳转到支付页面
                            window.location.href = `/payment?orderId=${orderId}`;
                        } else if (orderResponse.status === 'NOT_FOUND') {
                            retryCount++;
                            if (retryCount < maxRetries) {
                                console.log(`订单尚未创建，${pollInterval}毫秒后重试 (${retryCount}/${maxRetries})`);
                                setTimeout(pollOrderId, pollInterval);
                            } else {
                                message.error('订单处理超时，请稍后查看订单状态');
                                setSubmitting(false);
                            }
                        } else {
                            message.error('查询订单失败: ' + orderResponse.message);
                            setSubmitting(false);
                        }
                    } catch (error) {
                        console.error('查询订单ID失败:', error);
                        retryCount++;
                        if (retryCount < maxRetries) {
                            console.log(`查询失败，${pollInterval}毫秒后重试 (${retryCount}/${maxRetries})`);
                            setTimeout(pollOrderId, pollInterval);
                        } else {
                            message.error('查询订单失败，请稍后重试');
                            setSubmitting(false);
                        }
                    }
                };
                
                // 开始轮询
                setTimeout(pollOrderId, pollInterval);
            } else {
                message.error(bookingResponse.message || '购票失败');
                setSubmitting(false);
            }
        } catch (error) {
            console.error('购票失败:', error);
            console.error('错误详情:', error.message);
            console.error('错误堆栈:', error.stack);
            message.error('购票失败，请稍后重试');
            setSubmitting(false);
        }
    };

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" />
                <div style={{ marginTop: '16px' }}>加载中...</div>
            </div>
        );
    }

    return (
        <>
            <div className="train-info-wrapper">
                <div className="train-info-header">
                    列车信息（以下余票信息仅供参考）
                </div>
                <div className="train-info-content">
                    {trainInfo ? (
                        <>
                            <div className="train-details">
                                <span className="bold-text">{formatDate(trainInfo.travelDate)}</span> &nbsp;
                                <span>{trainInfo.trainNumber}</span>
                                <span className="small-text">次</span> &nbsp;
                                {trainInfo.departureStation} &nbsp;
                                <span className="small-text">（</span>
                                <span className="bold-text">{formatTime(trainInfo.departureTime)}</span>
                                <span className="small-text">开）</span> &nbsp;
                                — {trainInfo.arrivalStation} &nbsp;
                                <span className="small-text">（</span>
                                <span className="bold-text">{formatTime(trainInfo.arrivalTime)}</span>
                                <span className="small-text">到）</span>
                            </div>
                            <hr />
                            <div className="ticket-info-seat">
                                {response && response.carriages && response.carriages.map((carriage, index) => (
                                    <span key={index} className="ticket-item ticket-available" style={{ marginRight: '20px' }}>
                                        <span className="seat-type">{carriage.carriageTypeName}</span>
                                        （<span className="price price-available">¥{carriage.price}元</span>） 有票
                                    </span>
                                ))}
                            </div>
                            <div className="price-note">
                                *显示的价格均为实际活动折扣后票价，供您参考，查看公布票价 。具体票价以您确认支付时实际购买的铺别票价为准。
                            </div>
                        </>
                    ) : (
                        <div>列车信息加载中...</div>
                    )}
                </div>
            </div>

            <div className="passenger-info-wrapper">
                <div className="passenger-info-header">
                    <span>乘客信息（填写说明）</span>
                    <input
                        type="search"
                        placeholder="搜索乘车人"
                        value={searchText}
                        onChange={e => setSearchText(e.target.value)}
                        className="passenger-search"
                        aria-label="搜索乘车人"
                    />
                </div>
                <div className="passenger-info-content">
                    <div className="passenger-label">
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            width="20"
                            height="20"
                            fill="currentColor"
                            viewBox="0 0 16 16"
                            aria-hidden="true"
                        >
                            <path d="M3 14s-1 0-1-1 1-4 6-4 6 3 6 4-1 1-1 1H3zm5-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" />
                        </svg>
                        <span>乘车人</span>
                    </div>
                    <div className="passenger-list">
                        {filteredPassengers.map(name => (
                            <label key={name} className="passenger-item">
                                <input
                                    type="checkbox"
                                    checked={selectedPassengers.includes(name)}
                                    onChange={() => togglePassenger(name)}
                                />
                                <span>{name}</span>
                            </label>
                        ))}
                        <button
                            className="add-passenger-button"
                            type="button"
                            aria-label="添加乘车人"
                            onClick={checkCanAddPassenger}
                        >
                            <span className="plus-sign">+</span> 添加乘车人
                        </button>
                    </div>

                    <hr />

                    {selectedPassengers.length > 0 && (
                        <div className="passenger-details-table-wrapper">
                            <table className="passenger-details-table">
                                <thead>
                                <tr>
                                    <th>序号</th>
                                    <th>票种</th>
                                    <th style={{ textAlign: 'left' }}>席别</th>
                                    <th>姓名</th>
                                    <th>手机号</th>
                                    <th>证件类型</th>
                                    <th>证件号码</th>
                                    <th>操作</th>
                                </tr>
                                </thead>
                                <tbody>
                                {selectedPassengers.map((name, index) => {
                                    const passengerType = passengerTypeMap[name] || 1;
                                    const availableTicketTypes = getAvailableTicketTypes(passengerType);
                                    
                                    return (
                                        <tr key={name}>
                                            <td>{index + 1}</td>
                                            <td>
                                                <Select
                                                    value={passengerDetails[name]?.ticketTypeValue}
                                                    onChange={(value) => {
                                                        updateDetail(name, 'ticketTypeValue', value);
                                                    }}
                                                    style={{ width: 120 }}
                                                >
                                                    {getAvailableTicketTypes(passengerType).map(type => (
                                                        <Select.Option key={type} value={type}>
                                                            {TICKET_TYPE[type]}
                                                        </Select.Option>
                                                    ))}
                                                </Select>
                                            </td>
                                            <td style={{ textAlign: 'left' }}>
                                                <Select
                                                    value={passengerDetails[name]?.seatType || (response?.carriages?.[0]?.carriageTypeName || '商务座')}
                                                    onChange={(value) => updateDetail(name, 'seatType', value)}
                                                    style={{ width: 120 }}
                                                >
                                                    {response && response.carriages && response.carriages.map((carriage, idx) => (
                                                        <Select.Option key={idx} value={carriage.carriageTypeName}>
                                                            {carriage.carriageTypeName}
                                                        </Select.Option>
                                                    ))}
                                                </Select>
                                            </td>
                                            <td>{name}</td>
                                            <td>{passengerPhoneMap[name]}</td>
                                            <td>
                                                <Select
                                                    value={passengerDetails[name]?.idType}
                                                    onChange={(value) => updateDetail(name, 'idType', value)}
                                                    style={{ width: 120 }}
                                                >
                                                    {idTypes.map(type => (
                                                        <Select.Option key={type} value={type}>
                                                            {type}
                                                        </Select.Option>
                                                    ))}
                                                </Select>
                                            </td>
                                            <td>
                                                <Input
                                                    value={passengerDetails[name]?.idNumber || ''}
                                                    placeholder="请输入证件号码"
                                                    style={{ width: 200 }}
                                                    readOnly
                                                />
                                            </td>
                                            <td>
                                                <button
                                                    type="button"
                                                    className="delete-row-button"
                                                    aria-label={`删除乘车人 ${name}`}
                                                    onClick={() => removePassenger(name)}
                                                >
                                                    ×
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })}
                                </tbody>
                            </table>
                        </div>
                    )}

                    <div className="button-row">
                        <button
                            className="btn btn-white"
                            type="button"
                            onClick={() => navigate(-1)}
                        >
                            上一步
                        </button>
                        <button
                            className="btn btn-blue"
                            type="button"
                            onClick={() => {
                                console.log('提交订单按钮被点击');
                                console.log('submitting状态:', submitting);
                                console.log('selectedPassengers长度:', selectedPassengers.length);
                                handleSubmitOrder();
                            }}
                            disabled={submitting || selectedPassengers.length === 0}
                        >
                            {submitting ? '提交中...' : '提交订单'}
                        </button>
                    </div>

                    <div className="tips-section">
                        <div className="tips-title">温馨提示</div>
                        <div className="tips-content">
                            <p>• 请确保乘车人信息准确无误，购票后不可修改</p>
                            <p>• 请提前30分钟到达车站，凭有效证件取票</p>
                            <p>• 如需退票，请在开车前2小时办理</p>
                            <p>• 儿童票适用于1.2米以下儿童，需成人陪同</p>
                            <p>• 学生票需提供有效学生证</p>
                        </div>
                    </div>
                </div>
            </div>

            {showAddPassengerModal && (
                <div className="add-passenger-page">
                    <div className="modal-wrapper" role="dialog" aria-modal="true" aria-labelledby="modal-title" style={{ position: 'relative' }}>
                        <AddPassenger onClose={() => setShowAddPassengerModal(false)} />
                    </div>
                </div>
            )}
        </>
    );
};

export default SubmitOrder;

function formatDate(date) {
    if (!date) return '';
    return date.toString();
}

function formatTime(time) {
    if (!time) return '';
    return time.toString().slice(0, 5);
}