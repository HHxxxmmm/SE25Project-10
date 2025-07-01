import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import './index.css';
import App from './App';
import { AuthProvider } from './hooks/useAuth';
import reportWebVitals from './reportWebVitals';
import store from './store';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <React.StrictMode>
        <BrowserRouter>
            <Provider store={store}>
                <AuthProvider> {/* 现在 AuthProvider 只是形式上的包装 */}
                    <App />
                </AuthProvider>
            </Provider>
        </BrowserRouter>
    </React.StrictMode>
);

// 初始化检查认证状态
const checkAuth = () => {
    const savedUser = localStorage.getItem('mini12306_user');
    const loginTimestamp = localStorage.getItem('mini12306_login_time');

    if (savedUser && loginTimestamp) {
        try {
            const user = JSON.parse(savedUser);
            store.dispatch({
                type: 'LOGIN_SUCCESS',
                payload: user
            });
        } catch (err) {
            localStorage.removeItem('mini12306_user');
            localStorage.removeItem('mini12306_login_time');
        }
    }
};

checkAuth(); // 应用启动时检查认证状态

reportWebVitals();