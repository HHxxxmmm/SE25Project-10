// API服务文件
const API_BASE_URL = 'http://localhost:8080/api';

// 通用请求方法
const request = async (url, options = {}) => {
    const config = {
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
        request(`/passenger/check-add/${userId}`),
    
    // 添加乘车人
    addPassenger: (userId, realName, idCardNumber, phoneNumber) => 
        request('/passenger/add', {
            method: 'POST',
            body: JSON.stringify({ 
                userId, 
                realName, 
                idCardNumber, 
                phoneNumber 
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
};

// 用户相关API
export const userAPI = {
    // 获取用户信息
    getUserInfo: (userId) => 
        request(`/user/info?userId=${userId}`),
};

export default {
    ticket: ticketAPI,
    order: orderAPI,
    user: userAPI,
    passenger: passengerAPI,
}; 