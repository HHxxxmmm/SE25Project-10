import { combineReducers } from 'redux';
import authReducer from './authReducer';
import orderReducer from './orderReducer';
import ticketReducer from './ticketReducer';

const rootReducer = combineReducers({
    auth: authReducer,
    orders: orderReducer,
    tickets: ticketReducer
});

export default rootReducer;