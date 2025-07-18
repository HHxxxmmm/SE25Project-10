import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { message, Spin, Select, Input, Button } from 'antd';
import { useSelector, useDispatch } from 'react-redux';
import { clearChangeTicket } from '../../store/actions/changeTicketActions';
import AddPassenger from '../AddPassenger';
import { orderAPI, passengerAPI, ticketAPI, waitlistAPI } from '../../services/api';
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
    const dispatch = useDispatch();
    const { user } = useAuth();
    const changeTicketState = useSelector(state => state.changeTicket);
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
    const [showWaitlistModal, setShowWaitlistModal] = useState(false);
    const [waitlistSubmitting, setWaitlistSubmitting] = useState(false);

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
                setResponse(response);
                
                // 构建乘客ID映射
                const idMap = {};
                const typeMap = {};
                const phoneMap = {};
                
                // 统一使用后端返回的乘客信息
                response.passengers.forEach(passenger => {
                    idMap[passenger.realName] = passenger.idCardNumber;
                    typeMap[passenger.realName] = passenger.passengerType;
                    phoneMap[passenger.realName] = passenger.phoneNumber;
                });
                
                setPassengerIdMap(idMap);
                setPassengerTypeMap(typeMap);
                setPassengerPhoneMap(phoneMap);

                // 如果是改签模式，从原始订单获取乘客信息
                if (changeTicketState.isChanging) {
                    try {
                        // 获取原始订单详情
                        const orderDetailResponse = await orderAPI.getOrderDetail(changeTicketState.originalOrderId, currentUserId);
                        console.log('原始订单详情:', orderDetailResponse);
                        
                        if (orderDetailResponse && orderDetailResponse.tickets) {
                            // 从原始订单的车票中获取乘客信息，只获取用户选择改签的车票
                            const originalPassengers = orderDetailResponse.tickets
                                .filter(ticket => 
                                    ticket.ticketStatus === 1 && // 只获取未使用的车票
                                    changeTicketState.ticketIds.includes(ticket.ticketId) // 只获取用户选择改签的车票
                                )
                                .map(ticket => ({
                                    realName: ticket.passengerName,
                                    passengerId: ticket.passengerId,
                                    idCardNumber: ticket.idCardNumber,
                                    passengerType: ticket.passengerType || 1,
                                    phoneNumber: ticket.phoneNumber || '' // 直接从车票信息中获取手机号
                                }));
                            
                            console.log('改签乘客信息:', originalPassengers);
                            console.log('改签车票ID:', changeTicketState.ticketIds);
                            
                            // 设置乘客数据
                            const passengerNames = originalPassengers.map(p => p.realName);
                            setPassengersData(passengerNames);
                            setSelectedPassengers(passengerNames);
                            
                            // 更新乘客映射
                            originalPassengers.forEach(passenger => {
                                idMap[passenger.realName] = passenger.idCardNumber;
                                typeMap[passenger.realName] = passenger.passengerType;
                                phoneMap[passenger.realName] = passenger.phoneNumber; // 直接使用从车票信息中获取的手机号
                            });
                            
                            setPassengerIdMap(idMap);
                            setPassengerTypeMap(typeMap);
                            setPassengerPhoneMap(phoneMap);
                            
                            // 为改签乘客设置默认详情
                            passengerNames.forEach(name => {
                                const passenger = originalPassengers.find(p => p.realName === name);
                                if (passenger) {
                                    const passengerType = passenger.passengerType || 1;
                                    const availableTicketTypes = getAvailableTicketTypes(passengerType);
                                    const defaultTicketType = availableTicketTypes[0];
                                    
                                    setPassengerDetails(details => ({
                                        ...details,
                                        [name]: {
                                            ticketType: TICKET_TYPE[defaultTicketType],
                                            ticketTypeValue: defaultTicketType,
                                            seatType: response.carriages?.[0]?.carriageTypeName || "二等座",
                                            idType: idTypes[0],
                                            idNumber: passenger.idCardNumber || ''
                                        }
                                    }));
                                }
                            });
                        } else {
                            message.error('获取原始订单信息失败');
                        }
                    } catch (error) {
                        console.error('获取原始订单详情失败:', error);
                        message.error('获取原始订单信息失败');
                    }
                } else {
                    // 非改签模式，使用后端返回的乘客信息
                    setPassengersData(response.passengers.map(p => p.realName));
                }
            } else {
                message.error('获取订单信息失败');
            }
        } catch (error) {
            console.error('获取准备订单信息失败:', error);
            message.error('获取订单信息失败，请稍后重试');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPrepareOrderData();
    }, [user, location.search, changeTicketState.isChanging]);

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

    const filteredPassengers = passengersData.filter(name =>
        name.includes(searchText)
    );

    const togglePassenger = (name) => {
        // 改签模式下不允许取消选择乘客
        if (changeTicketState.isChanging) {
            message.warning('改签模式下不能取消选择乘客');
            return;
        }

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
        // 改签模式下不允许移除乘客
        if (changeTicketState.isChanging) {
            message.warning('改签模式下不能移除乘客');
            return;
        }

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
        // 改签模式下不允许添加乘车人
        if (changeTicketState.isChanging) {
            message.warning('改签模式下不能添加乘车人');
            return;
        }

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

            // 如果是改签模式，使用改签API
            if (changeTicketState.isChanging) {
                console.log('改签状态检查:', changeTicketState);
                
                // 构建改签请求
                const changeTicketRequest = {
                    userId: currentUserId,
                    originalOrderId: changeTicketState.originalOrderId,
                    ticketIds: changeTicketState.ticketIds,
                    newTrainId: trainId,
                    newDepartureStopId: departureStopId,
                    newArrivalStopId: arrivalStopId,
                    newTravelDate: travelDate,
                    newCarriageTypeId: seatTypeToCarriageId[passengerSeatTypes[0]], // 使用第一个乘客的席别
                    passengers: selectedPassengers.map(name => {
                        const passengerId = response.passengers.find(p => p.realName === name)?.passengerId;
                        const ticketTypeValue = passengerDetails[name]?.ticketTypeValue || 1;
                        const seatType = passengerDetails[name]?.seatType;
                        const carriageTypeId = seatTypeToCarriageId[seatType];
                        
                        return {
                            passengerId: passengerId,
                            ticketType: ticketTypeValue,
                            carriageTypeId: carriageTypeId
                        };
                    })
                };

                console.log('发送改签请求:', changeTicketRequest);
                
                // 调用后端改签API
                const changeResponse = await ticketAPI.changeTickets(changeTicketRequest);
                console.log('改签响应:', changeResponse);
                
                if (changeResponse.status === 'SUCCESS') {
                    // 改签成功，获取订单号
                    const orderNumber = changeResponse.orderNumber;
                    console.log('改签成功，订单号:', orderNumber);
                    
                    message.success('改签成功，正在处理订单...');
                    
                    // 轮询获取订单ID
                    let retryCount = 0;
                    const maxRetries = 15;
                    const pollInterval = 200;
                    
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
                                
                                // 清除改签状态
                                dispatch(clearChangeTicket());
                                
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
                    message.error(changeResponse.message || '改签失败');
                    setSubmitting(false);
                }
            } else {
                // 正常购票流程
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
                } else if (bookingResponse.status === 'INSUFFICIENT_STOCK') {
                    // 库存不足，显示候补订单弹窗
                    setShowWaitlistModal(true);
                    setSubmitting(false);
                } else {
                    message.error(bookingResponse.message || '购票失败');
                    setSubmitting(false);
                }
            }
        } catch (error) {
            console.error('购票失败:', error);
            console.error('错误详情:', error.message);
            console.error('错误堆栈:', error.stack);
            message.error('购票失败，请稍后重试');
            setSubmitting(false);
        }
    };

    // 候补订单处理函数
    const handleCreateWaitlistOrder = async () => {
        setWaitlistSubmitting(true);
        
        try {
            const currentUserId = user?.userId;
            
            // 构建候补订单请求（与正常购票请求相同）
            const bookingRequest = {
                userId: currentUserId,
                trainId: response.trainInfo.trainId,
                departureStopId: response.trainInfo.departureStopId,
                arrivalStopId: response.trainInfo.arrivalStopId,
                travelDate: response.trainInfo.travelDate,
                carriageTypeId: response.carriages[0].carriageTypeId, // 使用第一个车厢类型
                passengers: selectedPassengers.map(name => {
                    const passengerId = response.passengers.find(p => p.realName === name)?.passengerId;
                    const ticketTypeValue = passengerDetails[name]?.ticketTypeValue || 1;
                    const seatType = passengerDetails[name]?.seatType;
                    const seatTypeToCarriageId = {};
                    response.carriages.forEach(carriage => {
                        seatTypeToCarriageId[carriage.carriageTypeName] = carriage.carriageTypeId;
                    });
                    const carriageTypeId = seatTypeToCarriageId[seatType];
                    
                    return {
                        passengerId: passengerId,
                        ticketType: ticketTypeValue,
                        carriageTypeId: carriageTypeId
                    };
                })
            };

            console.log('发送候补订单请求:', bookingRequest);
            
            // 调用候补订单API
            const result = await waitlistAPI.createWaitlistOrder(bookingRequest);
            console.log('候补订单响应:', result);
            
            if (result.status === 'SUCCESS') {
                message.success('候补订单创建成功！');
                setShowWaitlistModal(false);
                
                // 跳转到支付页面
                window.location.href = `/payment?waitlistId=${result.orderId}&isWaitlist=true`;
            } else {
                message.error(result.message || '候补订单创建失败');
            }
        } catch (error) {
            console.error('创建候补订单失败:', error);
            message.error('创建候补订单失败，请稍后重试');
        } finally {
            setWaitlistSubmitting(false);
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
            {/* 改签模式提示 */}
            {changeTicketState.isChanging && (
                <div style={{ 
                    backgroundColor: '#fff7e6', 
                    border: '1px solid #ffd591', 
                    borderRadius: '4px', 
                    padding: '12px', 
                    marginBottom: '16px',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                }}>
                    <div>
                        <p style={{ margin: 0, color: '#d46b08' }}>
                            <strong>改签模式：</strong>
                            您正在为订单 {changeTicketState.originalOrderNumber} 进行改签，
                            只能选择改签乘客，且不能添加或移除乘客
                        </p>
                    </div>
                    <Button 
                        type="default" 
                        size="small"
                        onClick={() => {
                            dispatch(clearChangeTicket());
                            message.success('已退出改签模式');
                            navigate('/orders'); // 退出后返回订单列表
                        }}
                        style={{ 
                            borderColor: '#ffd591', 
                            color: '#d46b08',
                            marginLeft: '12px'
                        }}
                    >
                        退出改签模式
                    </Button>
                </div>
            )}

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
                                    <span key={index} className={`ticket-item ${carriage.hasStock ? 'ticket-available' : 'ticket-unavailable'}`} style={{ marginRight: '20px' }}>
                                        <span className="seat-type">{carriage.carriageTypeName}</span>
                                        （<span className={`price ${carriage.hasStock ? 'price-available' : 'price-unavailable'}`}>¥{carriage.price}元</span>） 
                                        {carriage.hasStock ? `有票(${carriage.availableStock}张)` : '无票'}
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
                        disabled={changeTicketState.isChanging}
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
                                    disabled={changeTicketState.isChanging}
                                />
                                <span>{name}</span>
                            </label>
                        ))}
                        {!changeTicketState.isChanging && (
                            <button
                                className="add-passenger-button"
                                type="button"
                                aria-label="添加乘车人"
                                onClick={checkCanAddPassenger}
                            >
                                <span className="plus-sign">+</span> 添加乘车人
                            </button>
                        )}
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
                                    {!changeTicketState.isChanging && <th>操作</th>}
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
                                            <td>{passengerPhoneMap[name] || ''}</td>
                                            <td>
                                                <Select
                                                    value={passengerDetails[name]?.idType || idTypes[0]}
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
                                                    onChange={(e) => updateDetail(name, 'idNumber', e.target.value)}
                                                    placeholder="请输入证件号码"
                                                    style={{ width: 200 }}
                                                />
                                            </td>
                                            {!changeTicketState.isChanging && (
                                                <td>
                                                    <button
                                                        type="button"
                                                        onClick={() => removePassenger(name)}
                                                        style={{
                                                            background: 'none',
                                                            border: 'none',
                                                            color: '#ff4d4f',
                                                            cursor: 'pointer',
                                                            textDecoration: 'underline'
                                                        }}
                                                    >
                                                        移除
                                                    </button>
                                                </td>
                                            )}
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
                            {submitting ? '提交中...' : (changeTicketState.isChanging ? '确认改签' : '提交订单')}
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

            {/* 添加乘车人模态框 */}
            {showAddPassengerModal && (
                        <AddPassenger 
                    visible={showAddPassengerModal}
                            onClose={() => setShowAddPassengerModal(false)}
                            onSuccess={() => {
                                setShowAddPassengerModal(false);
                                // 重新获取准备订单数据以更新乘客列表
                                fetchPrepareOrderData();
                            }}
                        />
            )}

            <CustomMessage
                type="success"
                content="订单提交成功！"
                visible={false}
                onClose={() => {}}
            />

            {/* 候补订单弹窗 */}
            {showWaitlistModal && (
                <div className="waitlist-modal-overlay">
                    <div className="waitlist-modal">
                        <div className="waitlist-modal-header">
                            <h3>候补订单</h3>
                        </div>
                        <div className="waitlist-modal-content">
                            <p>当前订单所需余票不足，是否提交候补订单？</p>
                            <p className="waitlist-note">
                                候补订单将在有票时自动为您购票，候补期限为发车前2小时。
                            </p>
                        </div>
                        <div className="waitlist-modal-footer">
                            <button
                                className="btn btn-white"
                                onClick={() => setShowWaitlistModal(false)}
                                disabled={waitlistSubmitting}
                            >
                                取消
                            </button>
                            <button
                                className="btn btn-blue"
                                onClick={handleCreateWaitlistOrder}
                                disabled={waitlistSubmitting}
                            >
                                {waitlistSubmitting ? '提交中...' : '提交候补订单'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};



function formatDate(date) {
    if (!date) return '';
    const dateObj = new Date(date);
    return dateObj.toLocaleDateString('zh-CN');
}

function formatTime(time) {
    if (!time) return '';
    // 如果time是完整的日期时间字符串，提取时间部分
    if (time.includes('T') || time.includes(' ')) {
        const timeObj = new Date(time);
        return timeObj.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    }
    // 如果time只是时间字符串（如 "08:00"），直接返回
    if (time.includes(':')) {
        return time;
    }
    // 其他情况，尝试解析
    try {
        const timeObj = new Date(time);
        return timeObj.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
        return time;
    }
}

export default SubmitOrder;
