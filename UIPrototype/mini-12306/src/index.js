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

// 初始化检查认证状态 - 优化版本
const checkAuth = async () => {
    try {
        // 只在需要时检查认证状态，减少不必要的API调用
        const { checkCurrentUser } = require('./store/actions/authActions');
        
        // 检查当前路径，如果是公开页面则跳过认证检查
        const currentPath = window.location.pathname;
        const publicPaths = ['/trains', '/home'];
        const isPublicPath = publicPaths.some(path => currentPath.startsWith(path));
        
        if (!isPublicPath) {
        store.dispatch(checkCurrentUser());
        }
    } catch (err) {
        console.error('检查认证状态失败:', err);
    }
};

// 应用启动时检查认证状态 - 延迟执行，减少对页面加载的影响
setTimeout(checkAuth, 500); // 增加延迟，确保应用已完全加载

reportWebVitals();