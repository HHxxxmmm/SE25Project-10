import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import './index.css';
import App from './App';
import { AuthProvider } from './hooks/useAuth';
import reportWebVitals from './reportWebVitals';
import store from './store';

// 定义会话超时配置，便于调试
const SESSION_TIMEOUT = 4 * 60 * 1000; // 4分钟
console.log('====> [DEBUG] 应用启动 - 配置会话超时:', SESSION_TIMEOUT / 1000, '秒', new Date().toLocaleTimeString());

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <React.StrictMode>
        <BrowserRouter>
            <Provider store={store}>
                <AuthProvider config={{ sessionTimeout: SESSION_TIMEOUT }}> {/* 设置会话超时 */}
                    <App />
                </AuthProvider>
            </Provider>
        </BrowserRouter>
    </React.StrictMode>
);

// 初始化检查认证状态
const checkAuth = async () => {
    try {
        // 导入authActions中的checkCurrentUser函数
        const { checkCurrentUser } = require('./store/actions/authActions');
        // 调用API检查当前用户的会话状态
        store.dispatch(checkCurrentUser());
    } catch (err) {
        console.error('检查认证状态失败:', err);
    }
};

// 应用启动时检查认证状态
setTimeout(checkAuth, 100); // 稍微延迟，确保应用已完全加载

reportWebVitals();