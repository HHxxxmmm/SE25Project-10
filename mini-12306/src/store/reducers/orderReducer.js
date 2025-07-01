import * as actionTypes from '../actions/actionTypes';

const initialState = {
    orders: [],
    loading: false,
    error: null
};

export default function orderReducer(state = initialState, action) {
    switch (action.type) {
        case actionTypes.FETCH_ORDERS_REQUEST:
            return { ...state, loading: true, error: null };

        case actionTypes.FETCH_ORDERS_SUCCESS:
            return { ...state, orders: action.payload, loading: false };

        case actionTypes.FETCH_ORDERS_FAILURE:
            return { ...state, loading: false, error: action.error };

        default:
            return state;
    }
}