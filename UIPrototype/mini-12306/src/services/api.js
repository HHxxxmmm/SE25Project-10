// API服务文件
const API_BASE_URL = 'http://localhost:8080/api';

// 通用请求方法
const request = async (url, options = {}) => {
    const config = {
        credentials: 'include', // 确保cookie被发送
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
        ...options,
    };

    console.log('发送API请求:', `${API_BASE_URL}${url}`, config);

    try {
        const response = await fetch(`${API_BASE_URL}${url}`, config);
        const data = await response.json();
        
        console.log('API响应状态:', response.status);
        console.log('API响应数据:', data);
        
        if (!response.ok) {
            throw new Error(data.message || '请求失败');
        }
        
        return data;
    } catch (error) {
        console.error('API请求错误:', error);
        throw error;
    }
};

// 乘客管理相关API
export const passengerAPI = {
    // 检查是否可以添加乘车人
    checkCanAddPassenger: (userId) => 
        request(`/passenger/check-add?userId=${userId}`),

    // 添加乘车人
    addPassenger: (passengerData) => 
        request('/passenger/add', {
            method: 'POST',
            body: JSON.stringify(passengerData),
        }),

    // 删除乘车人
    deletePassenger: (userId, passengerId) => 
        request('/passenger/delete', {
            method: 'DELETE',
            body: JSON.stringify({
                userId: userId,
                passengerId: passengerId
            }),
        }),
};

// 车票相关API
export const ticketAPI = {
    // 购票
    bookTickets: (bookingRequest) => 
        request('/ticket/book', {
            method: 'POST',
            body: JSON.stringify(bookingRequest),
        }),
    
    // 改签
    changeTickets: (changeTicketRequest) => 
        request('/ticket/change', {
            method: 'POST',
            body: JSON.stringify(changeTicketRequest),
        }),
    
    // 获取本人车票
    getMyTickets: (userId) => 
        request(`/ticket/my-tickets?userId=${userId}`),
    
    // 根据状态获取本人车票
    getMyTicketsByStatus: (userId, ticketStatus) => 
        request(`/ticket/my-tickets/status?userId=${userId}&ticketStatus=${ticketStatus}`),
    
    // 根据日期范围获取本人车票
    getMyTicketsByDateRange: (userId, startDate, endDate) => 
        request(`/ticket/my-tickets/date-range?userId=${userId}&startDate=${startDate}&endDate=${endDate}`),
    
    // 根据状态和日期范围获取本人车票
    getMyTicketsByStatusAndDateRange: (userId, ticketStatus, startDate, endDate) => 
        request(`/ticket/my-tickets/status-date-range?userId=${userId}&ticketStatus=${ticketStatus}&startDate=${startDate}&endDate=${endDate}`),
    
    // 获取车票详情
    getTicketDetail: (ticketId, userId) => 
        request(`/ticket/detail?ticketId=${ticketId}&userId=${userId}`),
    
    // 退票
    refundTickets: (userId, orderId, ticketIds) => 
        request('/ticket/refund', {
            method: 'POST',
            body: JSON.stringify({ 
                userId, 
                orderId, 
                ticketIds,
                refundReason: "行程变更"
            }),
        }),
};

// 订单相关API
export const orderAPI = {
    // 获取我的订单
    getMyOrders: (userId) => 
        request(`/orders/my?userId=${userId}`),
    
    // 根据条件获取我的订单
    getMyOrdersByConditions: (userId, orderNumber, startDate, endDate, orderStatus, trainNumber) => {
        const params = new URLSearchParams();
        params.append('userId', userId);
        if (orderNumber) params.append('orderNumber', orderNumber);
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        if (orderStatus !== undefined && orderStatus !== null) params.append('orderStatus', orderStatus);
        if (trainNumber) params.append('trainNumber', trainNumber);
        
        return request(`/orders/my/filter?${params.toString()}`);
    },
    
    // 获取订单详情
    getOrderDetail: (orderId, userId) => 
        request(`/orders/detail?orderId=${orderId}&userId=${userId}`),
    
    // 取消订单
    cancelOrder: (orderId, userId) => 
        request('/orders/cancel', {
            method: 'POST',
            body: JSON.stringify({ orderId, userId }),
        }),
    
    // 退票准备阶段 - 获取退票信息
    getRefundPreparation: (userId, orderId, ticketIds) => 
        request('/orders/refund/preparation', {
            method: 'POST',
            body: JSON.stringify({ userId, orderId, ticketIds }),
        }),
    
    // 准备提交订单
    prepareOrder: (userId, inventoryIds) => 
        request('/prepare-order/prepare', {
            method: 'POST',
            body: JSON.stringify({ userId, inventoryIds }),
        }),
    
    // 根据订单号获取订单ID
    getOrderIdByOrderNumber: (orderNumber) => 
        request(`/orders/order-id?orderNumber=${orderNumber}`),
    
    // 支付订单
    payOrder: (orderId, userId) => 
        request('/payment/pay', {
            method: 'POST',
            body: JSON.stringify({ orderId, userId }),
        }),
    
    // 检查订单是否已超时
    checkOrderTimeout: (orderId, userId) => 
        request(`/orders/check-timeout?orderId=${orderId}&userId=${userId}`),
};

// 用户相关API
export const userAPI = {
    // 获取用户信息
    getUserInfo: (userId) => 
        request(`/user/info?userId=${userId}`),
};

// 车次相关API
export const trainAPI = {
    // 获取所有车次列表
    getTrainList: () => 
        request('/trains/list'),
    
    // 根据站点名称和日期搜索车次
    searchTrains: (fromStation, toStation, travelDate) => 
        request(`/trains/search?fromStation=${encodeURIComponent(fromStation)}&toStation=${encodeURIComponent(toStation)}&travelDate=${travelDate}`),
    
    // 获取直达车次
    getDirectTrains: (startStationId, endStationId) => 
        request(`/trains/direct?startStationId=${startStationId}&endStationId=${endStationId}`),
    
    // 根据时间区间获取车次
    getTrainsByTime: (startTime, endTime) => 
        request(`/trains/byTime?start=${startTime}&end=${endTime}`),
    
    // 获取中转车次
    getTransferTrains: (startStationId, endStationId) => 
        request(`/trains/transfer?startStationId=${startStationId}&endStationId=${endStationId}`),
};

// 个人中心相关API
export const profileAPI = {
    // 获取用户个人资料
    getUserProfile: (userId) => 
        request(`/profile/${userId}`),
    
    // 更新用户个人资料
    updateUserProfile: (userId, profileData) => 
        request(`/profile/${userId}`, {
            method: 'PUT',
            body: JSON.stringify(profileData),
        }),
    
    // 修改密码
    changePassword: (changePasswordData) => 
        request('/profile/change-password', {
            method: 'POST',
            body: JSON.stringify(changePasswordData),
        }),
    
    // 更新最后登录时间
    updateLastLoginTime: (userId, loginTime) => 
        request('/profile/update-login-time', {
            method: 'POST',
            body: JSON.stringify({ userId, loginTime }),
        }),
    
    // 更新账户状态（管理员功能）
    updateAccountStatus: (userId, accountStatus) => 
        request('/profile/update-account-status', {
            method: 'POST',
            body: JSON.stringify({ userId, accountStatus }),
        }),
};

export default {
    ticket: ticketAPI,
    order: orderAPI,
    user: userAPI,
    passenger: passengerAPI,
    train: trainAPI,
    profile: profileAPI,
}; 