import axios from 'axios';

// 确保使用正确的后端API地址
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// 配置axios实例
const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // 支持跨域请求携带cookie
  timeout: 10000, // 10秒超时
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 在发送请求之前可以做一些处理
    console.log('发送请求:', config.url);
    return config;
  },
  (error) => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    console.log('接收响应:', response.data);
    return response;
  },
  (error) => {
    // 详细记录错误信息，便于调试
    console.error('响应错误:', {
      message: error.message,
      status: error.response?.status,
      data: error.response?.data,
      config: error.config
    });
    
    // 处理401未授权错误
    if (error.response?.status === 401) {
      console.log('未授权，需要登录');
      // 这里可以加入重定向到登录页的逻辑
    }
    
    // 处理CORS错误
    if (error.message.includes('Network Error') || error.message.includes('CORS')) {
      console.error('可能是CORS问题，请检查后端CORS配置');
    }
    
    return Promise.reject(error);
  }
);

// 用户认证相关API
export const authAPI = {
  // 用户登录
  login: (phoneNumber, password) => {
    return api.post('/auth/login', { phoneNumber, password });
  },
  
  // 用户注册
  register: (userData) => {
    return api.post('/auth/register', userData);
  },
  
  // 用户登出
  logout: async () => {
    console.log('====> [DEBUG] API logout 函数被调用', new Date().toLocaleTimeString());
    try {
      const response = await api.post('/auth/logout');
      console.log('====> [DEBUG] API logout 调用成功，响应:', response.data);
      return response;
    } catch (error) {
      console.error('====> [DEBUG] API logout 调用失败:', error);
      throw error;
    }
  },
  
  // 获取当前用户信息
  getCurrentUser: () => {
    return api.get('/auth/currentUser');
  }
};

export default api;
