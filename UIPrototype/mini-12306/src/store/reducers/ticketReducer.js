// src/store/reducers/ticketReducer.js
import * as actionTypes from '../actions/actionTypes';

const initialState = {
    tickets: [],
    loading: false,
    error: null
};

export default function ticketReducer(state = initialState, action) {
    switch (action.type) {
        case actionTypes.FETCH_TICKETS_REQUEST:
            return { ...state, loading: true, error: null };

        case actionTypes.FETCH_TICKETS_SUCCESS:
            return { ...state, tickets: action.payload, loading: false };

        case actionTypes.FETCH_TICKETS_FAILURE:
            return { ...state, loading: false, error: action.error };

        default:
            return state;
    }
}