// 开始改签
export const startChangeTicket = (changeTicketData) => ({
    type: 'START_CHANGE_TICKET',
    payload: changeTicketData
});

// 清除改签状态
export const clearChangeTicket = () => ({
    type: 'CLEAR_CHANGE_TICKET'
});

// 更新改签信息
export const updateChangeTicketInfo = (info) => ({
    type: 'UPDATE_CHANGE_TICKET_INFO',
    payload: info
}); 