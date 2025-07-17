/**
 * authActions 测试
 */
import * as actionTypes from '../../store/actions/actionTypes';
import {
  login,
  logout,
  register,
  updateUser,
  checkCurrentUser
} from '../../store/actions/authActions';
import { authAPI } from '../../api/auth';

// 模拟 authAPI
jest.mock('../../api/auth', () => ({
  authAPI: {
    login: jest.fn(),
    logout: jest.fn(),
    register: jest.fn(),
    getCurrentUser: jest.fn()
  }
}));

// 模拟 antd message
jest.mock('antd', () => ({
  message: {
    success: jest.fn(),
    error: jest.fn(),
    loading: jest.fn(),
    info: jest.fn()
  }
}));

describe('Auth Actions', () => {
  // 每次测试前重置模拟
  beforeEach(() => {
    jest.clearAllMocks();
  });
  
  // 创建一个模拟的 dispatch 函数
  const mockDispatch = jest.fn();
  
  describe('login action', () => {
    it('应该成功调度 LOGIN_SUCCESS', async () => {
      // 准备
      const mockUser = { id: '1', phoneNumber: '13800138000', realName: '测试用户' };
      const mockCredentials = { phoneNumber: '13800138000', password: 'password123' };
      const mockResponse = {
        data: {
          success: true,
          user: mockUser
        }
      };
      
      // 设置 authAPI.login 的模拟返回值
      authAPI.login.mockResolvedValue(mockResponse);
      
      // 执行
      await login(mockCredentials)(mockDispatch);
      
      // 验证
      expect(authAPI.login).toHaveBeenCalledWith(mockCredentials.phoneNumber, mockCredentials.password);
      expect(mockDispatch).toHaveBeenCalledTimes(2);
      expect(mockDispatch).toHaveBeenNthCalledWith(1, {
        type: actionTypes.LOGIN_REQUEST
      });
      expect(mockDispatch).toHaveBeenNthCalledWith(2, {
        type: actionTypes.LOGIN_SUCCESS,
        payload: mockUser
      });
    });
    
    it('应该在登录失败时调度 LOGIN_FAILURE', async () => {
      // 准备
      const mockCredentials = { phoneNumber: '13800138000', password: 'wrongpassword' };
      const mockError = new Error('密码错误');
      mockError.response = { data: { message: '密码错误' } };
      
      // 设置 authAPI.login 的模拟抛出错误
      authAPI.login.mockRejectedValue(mockError);
      
      // 执行 & 验证
      try {
        await login(mockCredentials)(mockDispatch);
      } catch (error) {
        expect(error).toBe(mockError);
      }
      
      // 验证 dispatch 调用
      expect(mockDispatch).toHaveBeenCalledTimes(2);
      expect(mockDispatch).toHaveBeenNthCalledWith(1, {
        type: actionTypes.LOGIN_REQUEST
      });
      expect(mockDispatch).toHaveBeenNthCalledWith(2, {
        type: actionTypes.LOGIN_FAILURE,
        error: '密码错误'
      });
    });
  });
  
  describe('logout action', () => {
    it('应该成功调度 LOGOUT', async () => {
      // 设置 authAPI.logout 的模拟返回值
      authAPI.logout.mockResolvedValue({ data: { success: true } });
      
      // 执行
      await logout()(mockDispatch);
      
      // 验证
      expect(authAPI.logout).toHaveBeenCalled();
      expect(mockDispatch).toHaveBeenCalledWith({
        type: actionTypes.LOGOUT
      });
    });
    
    it('即使 API 失败也应该调度 LOGOUT', async () => {
      // 设置 authAPI.logout 的模拟抛出错误
      authAPI.logout.mockRejectedValue(new Error('API失败'));
      
      // 执行
      await logout()(mockDispatch);
      
      // 验证
      expect(authAPI.logout).toHaveBeenCalled();
      expect(mockDispatch).toHaveBeenCalledWith({
        type: actionTypes.LOGOUT
      });
    });
  });
  
  describe('register action', () => {
    it('应该成功调度 REGISTER_SUCCESS', async () => {
      // 准备
      const mockUserData = {
        phoneNumber: '13900139000',
        password: 'password123',
        realName: '新用户',
        idCardNumber: '110101199001010022',
        email: 'new@example.com'
      };
      const mockUser = { ...mockUserData, id: '2' };
      const mockResponse = {
        data: {
          success: true,
          user: mockUser
        }
      };
      
      // 设置 authAPI.register 的模拟返回值
      authAPI.register.mockResolvedValue(mockResponse);
      
      // 执行
      await register(mockUserData)(mockDispatch);
      
      // 验证
      expect(authAPI.register).toHaveBeenCalledWith(mockUserData);
      expect(mockDispatch).toHaveBeenCalledTimes(2);
      expect(mockDispatch).toHaveBeenNthCalledWith(1, {
        type: actionTypes.REGISTER_REQUEST
      });
      expect(mockDispatch).toHaveBeenNthCalledWith(2, {
        type: actionTypes.REGISTER_SUCCESS,
        payload: mockUser
      });
    });
    
    it('应该在注册失败时调度 REGISTER_FAILURE', async () => {
      // 准备
      const mockUserData = {
        phoneNumber: '13800138000', // 已存在的手机号
        password: 'password123',
        realName: '测试用户',
        idCardNumber: '110101199001010011',
        email: 'test@example.com'
      };
      const mockError = new Error('手机号已被注册');
      mockError.response = { data: { message: '手机号已被注册' } };
      
      // 设置 authAPI.register 的模拟抛出错误
      authAPI.register.mockRejectedValue(mockError);
      
      // 执行 & 验证
      try {
        await register(mockUserData)(mockDispatch);
      } catch (error) {
        expect(error).toBe(mockError);
      }
      
      // 验证 dispatch 调用
      expect(mockDispatch).toHaveBeenCalledTimes(2);
      expect(mockDispatch).toHaveBeenNthCalledWith(1, {
        type: actionTypes.REGISTER_REQUEST
      });
      expect(mockDispatch).toHaveBeenNthCalledWith(2, {
        type: actionTypes.REGISTER_FAILURE,
        error: '手机号已被注册'
      });
    });
  });
  
  describe('updateUser action', () => {
    it('应该正确更新用户信息', () => {
      // 准备
      const currentUser = { id: '1', phoneNumber: '13800138000', realName: '测试用户' };
      const newData = { realName: '更新的姓名' };
      const expectedUser = { ...currentUser, ...newData };
      const getState = () => ({ auth: { user: currentUser } });
      
      // 执行
      updateUser(newData)(mockDispatch, getState);
      
      // 验证
      expect(mockDispatch).toHaveBeenCalledWith({
        type: actionTypes.UPDATE_USER,
        payload: expectedUser
      });
    });
  });
  
  describe('checkCurrentUser action', () => {
    it('应该成功获取并调度当前用户信息', async () => {
      // 准备
      const mockUser = { id: '1', phoneNumber: '13800138000', realName: '测试用户' };
      const mockResponse = {
        data: {
          success: true,
          user: mockUser
        }
      };
      
      // 设置 authAPI.getCurrentUser 的模拟返回值
      authAPI.getCurrentUser.mockResolvedValue(mockResponse);
      
      // 执行
      const result = await checkCurrentUser()(mockDispatch);
      
      // 验证
      expect(authAPI.getCurrentUser).toHaveBeenCalled();
      expect(mockDispatch).toHaveBeenCalledTimes(2);
      expect(mockDispatch).toHaveBeenNthCalledWith(1, {
        type: actionTypes.LOGIN_REQUEST
      });
      expect(mockDispatch).toHaveBeenNthCalledWith(2, {
        type: actionTypes.LOGIN_SUCCESS,
        payload: mockUser
      });
      expect(result).toEqual(mockUser);
    });
    
    it('应该在获取用户信息失败时调度 LOGOUT', async () => {
      // 设置 authAPI.getCurrentUser 的模拟抛出错误
      authAPI.getCurrentUser.mockRejectedValue(new Error('API失败'));
      
      // 执行
      const result = await checkCurrentUser()(mockDispatch);
      
      // 验证
      expect(authAPI.getCurrentUser).toHaveBeenCalled();
      expect(mockDispatch).toHaveBeenCalledTimes(2);
      expect(mockDispatch).toHaveBeenNthCalledWith(1, {
        type: actionTypes.LOGIN_REQUEST
      });
      expect(mockDispatch).toHaveBeenNthCalledWith(2, {
        type: actionTypes.LOGOUT
      });
      expect(result).toBeNull();
    });
    
    it('应该在返回无效用户数据时调度 LOGOUT', async () => {
      // 设置 authAPI.getCurrentUser 的模拟返回无效响应
      authAPI.getCurrentUser.mockResolvedValue({
        data: {
          success: false
        }
      });
      
      // 执行
      const result = await checkCurrentUser()(mockDispatch);
      
      // 验证
      expect(authAPI.getCurrentUser).toHaveBeenCalled();
      expect(mockDispatch).toHaveBeenCalledTimes(2);
      expect(mockDispatch).toHaveBeenNthCalledWith(1, {
        type: actionTypes.LOGIN_REQUEST
      });
      expect(mockDispatch).toHaveBeenNthCalledWith(2, {
        type: actionTypes.LOGOUT
      });
      expect(result).toBeNull();
    });
  });
});
