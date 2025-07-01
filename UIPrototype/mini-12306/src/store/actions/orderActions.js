import * as actionTypes from './actionTypes';

export const fetchOrders = () => async (dispatch) => {
    dispatch({ type: actionTypes.FETCH_ORDERS_REQUEST });

    try {
        // 这里替换为实际的 API 调用
        const orders = []; // 模拟数据
        dispatch({
            type: actionTypes.FETCH_ORDERS_SUCCESS,
            payload: orders
        });
    } catch (error) {
        dispatch({
            type: actionTypes.FETCH_ORDERS_FAILURE,
            error: error.message
        });
    }
};