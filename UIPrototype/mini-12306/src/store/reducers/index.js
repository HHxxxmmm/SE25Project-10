import { combineReducers } from 'redux';
import authReducer from './authReducer';
import orderReducer from './orderReducer';
import ticketReducer from './ticketReducer';
import changeTicketReducer from './changeTicketReducer';

const rootReducer = combineReducers({
    auth: authReducer,
    orders: orderReducer,
    tickets: ticketReducer,
    changeTicket: changeTicketReducer
});

export default rootReducer;