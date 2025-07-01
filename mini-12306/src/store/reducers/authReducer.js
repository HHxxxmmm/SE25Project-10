import * as actionTypes from '../actions/actionTypes';

const initialState = {
    user: null,
    isAuthenticated: false,
    loading: false,
    error: null
};

export default function authReducer(state = initialState, action) {
    switch (action.type) {
        case actionTypes.LOGIN_REQUEST:
        case actionTypes.REGISTER_REQUEST:
            return { ...state, loading: true, error: null };

        case actionTypes.LOGIN_SUCCESS:
            return {
                ...state,
                user: action.payload,
                isAuthenticated: true,
                loading: false
            };

        case actionTypes.LOGIN_FAILURE:
        case actionTypes.REGISTER_FAILURE:
            return {
                ...state,
                loading: false,
                error: action.error
            };

        case actionTypes.REGISTER_SUCCESS:
            return {
                ...state,
                loading: false
            };

        case actionTypes.LOGOUT:
            return initialState;

        case actionTypes.UPDATE_USER:
            return {
                ...state,
                user: action.payload
            };

        default:
            return state;
    }
}