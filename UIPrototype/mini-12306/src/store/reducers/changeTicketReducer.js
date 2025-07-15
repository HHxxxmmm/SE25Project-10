const initialState = {
    isChanging: false,
    originalOrderId: null,
    originalOrderNumber: null,
    ticketIds: [],
    passengerIds: [],
    departureStation: '',
    arrivalStation: '',
    travelDate: '',
    originalTrainInfo: null
};

const changeTicketReducer = (state = initialState, action) => {
    switch (action.type) {
        case 'START_CHANGE_TICKET':
            return {
                ...state,
                isChanging: true,
                originalOrderId: action.payload.originalOrderId,
                originalOrderNumber: action.payload.originalOrderNumber,
                ticketIds: action.payload.ticketIds,
                passengerIds: action.payload.passengerIds,
                departureStation: action.payload.departureStation,
                arrivalStation: action.payload.arrivalStation,
                travelDate: action.payload.travelDate,
                originalTrainInfo: action.payload.originalTrainInfo
            };
        
        case 'CLEAR_CHANGE_TICKET':
            return {
                ...initialState
            };
        
        case 'UPDATE_CHANGE_TICKET_INFO':
            return {
                ...state,
                ...action.payload
            };
        
        default:
            return state;
    }
};

export default changeTicketReducer; 