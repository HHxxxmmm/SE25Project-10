import * as actionTypes from './actionTypes';
import { authAPI } from '../../api/auth';
import { message } from 'antd';

export const login = (credentials) => async (dispatch) => {
    dispatch({ type: actionTypes.LOGIN_REQUEST });

    try {
        const response = await authAPI.login(credentials.phoneNumber, credentials.password);
        const { user } = response.data;
        
        if (!response.data.success) {
            throw new Error(response.data.message || '登录失败');
        }

        // 注意：token和其他数据已经保存在cookie和session中，不需要使用localStorage
        
        dispatch({
            type: actionTypes.LOGIN_SUCCESS,
            payload: user
        });

        return user;
    } catch (error) {
        const errorMsg = error.response?.data?.message || error.message || '登录失败';
        dispatch({ type: actionTypes.LOGIN_FAILURE, error: errorMsg });
        // 移除全局消息提示，让组件处理错误显示
        throw error;
    }
};

export const logout = () => async (dispatch) => {
    console.log('====> [DEBUG] Redux logout action 开始执行', new Date().toLocaleTimeString());
    try {
        console.log('====> [DEBUG] 调用后端 logout API');
        await authAPI.logout();
        console.log('====> [DEBUG] 后端 logout API 调用成功');
        // 服务端会清理cookie和session
        console.log('====> [DEBUG] 即将分发 LOGOUT action');
        dispatch({ type: actionTypes.LOGOUT });
        console.log('====> [DEBUG] LOGOUT action 分发成功');
    } catch (error) {
        console.error('====> [DEBUG] 登出失败:', error);
        // 即使API调用失败，也清除前端状态
        console.log('====> [DEBUG] 尽管API调用失败，仍分发 LOGOUT action');
        dispatch({ type: actionTypes.LOGOUT });
        console.log('====> [DEBUG] LOGOUT action 分发成功（错误恢复）');
    }
    console.log('====> [DEBUG] Redux logout action 执行完毕', new Date().toLocaleTimeString());
};

export const register = (userData) => async (dispatch) => {
    dispatch({ type: actionTypes.REGISTER_REQUEST });

    try {
        const response = await authAPI.register(userData);
        
        if (!response.data.success) {
            throw new Error(response.data.message || '注册失败');
        }
        
        const { user } = response.data;
        
        dispatch({ 
            type: actionTypes.REGISTER_SUCCESS,
            payload: user
        });
        
        return { success: true, user };
    } catch (error) {
        const errorMsg = error.response?.data?.message || error.message || '注册失败';
        dispatch({ type: actionTypes.REGISTER_FAILURE, error: errorMsg });
        throw error;
    }
};

export const updateUser = (newData) => (dispatch, getState) => {
    const { user } = getState().auth;
    const updatedUser = { ...user, ...newData };
    
    // 在真实场景中，这里应该调用更新用户信息的API
    // 目前先简单更新Redux状态
    dispatch({
        type: actionTypes.UPDATE_USER,
        payload: updatedUser
    });
};

// 检查当前会话用户
export const checkCurrentUser = () => async (dispatch) => {
    dispatch({ type: actionTypes.LOGIN_REQUEST });
    
    try {
        const response = await authAPI.getCurrentUser();
        
        if (response.data.success && response.data.user) {
            dispatch({
                type: actionTypes.LOGIN_SUCCESS,
                payload: response.data.user
            });
            return response.data.user;
        } else {
            dispatch({ type: actionTypes.LOGOUT });
            return null;
        }
    } catch (error) {
        console.error('获取当前用户失败:', error);
        dispatch({ type: actionTypes.LOGOUT });
        return null;
    }
};