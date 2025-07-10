// src/store/actions/ticketActions.js
import * as actionTypes from './actionTypes';

export const fetchTickets = () => async (dispatch) => {
    dispatch({ type: actionTypes.FETCH_TICKETS_REQUEST });

    try {
        // 这里替换为实际的 API 调用
        const tickets = []; // 模拟数据
        dispatch({
            type: actionTypes.FETCH_TICKETS_SUCCESS,
            payload: tickets
        });
    } catch (error) {
        dispatch({
            type: actionTypes.FETCH_TICKETS_FAILURE,
            error: error.message
        });
    }
};