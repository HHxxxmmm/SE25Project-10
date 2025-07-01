import * as actionTypes from './actionTypes';

// 模拟用户数据
const mockUser = {
    id: 1,
    username: 'demo_user',
    realName: '测试用户',
    phone: '13800138000',
    idCard: '110101199001011234'
};

export const login = (credentials) => async (dispatch) => {
    dispatch({ type: actionTypes.LOGIN_REQUEST });

    try {
        // 模拟API调用
        const user = {
            ...mockUser,
            username: credentials.username || mockUser.username
        };

        localStorage.setItem('mini12306_user', JSON.stringify(user));
        localStorage.setItem('mini12306_login_time', Date.now().toString());

        dispatch({
            type: actionTypes.LOGIN_SUCCESS,
            payload: user
        });

        return user;
    } catch (error) {
        dispatch({ type: actionTypes.LOGIN_FAILURE, error: error.message });
        throw error;
    }
};

export const logout = () => (dispatch) => {
    localStorage.removeItem('mini12306_user');
    localStorage.removeItem('mini12306_login_time');
    dispatch({ type: actionTypes.LOGOUT });
};

export const register = (userData) => async (dispatch) => {
    dispatch({ type: actionTypes.REGISTER_REQUEST });

    try {
        // 模拟注册流程
        dispatch({ type: actionTypes.REGISTER_SUCCESS });
        return { success: true };
    } catch (error) {
        dispatch({ type: actionTypes.REGISTER_FAILURE, error: error.message });
        throw error;
    }
};

export const updateUser = (newData) => (dispatch, getState) => {
    const { user } = getState().auth;
    const updatedUser = { ...user, ...newData };

    localStorage.setItem('mini12306_user', JSON.stringify(updatedUser));
    dispatch({
        type: actionTypes.UPDATE_USER,
        payload: updatedUser
    });
};