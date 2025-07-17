import { configureStore } from '@reduxjs/toolkit';
import rootReducer from './reducers';

// Redux 持久化中间件
const localStorageMiddleware = ({ getState }) => {
    return (next) => (action) => {
        const result = next(action);
        const { auth } = getState();

        if (action.type === 'LOGIN_SUCCESS' || action.type === 'UPDATE_USER') {
            localStorage.setItem('mini12306_user', JSON.stringify(auth.user));
            localStorage.setItem('mini12306_login_time', Date.now().toString());
        }

        if (action.type === 'LOGOUT') {
            localStorage.removeItem('mini12306_user');
            localStorage.removeItem('mini12306_login_time');
        }

        return result;
    };
};

// 从 localStorage 重新 hydrate state
const reHydrateStore = () => {
    try {
        const user = localStorage.getItem('mini12306_user');
        if (user) {
            return {
                auth: {
                    user: JSON.parse(user),
                    isAuthenticated: true,
                    loading: false,
                    error: null
                }
            };
        }
    } catch (e) {
        console.error('Failed to parse stored user data', e);
    }

    return undefined;
};

const store = configureStore({
    reducer: rootReducer,
    preloadedState: reHydrateStore(),
    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware().concat(localStorageMiddleware),
    devTools: process.env.NODE_ENV !== 'production'
});

export default store;