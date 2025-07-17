/* eslint-env node */
/**
 * @jest-environment node
 * API请求处理器
 * 这不是一个测试文件
 */
import { rest } from 'msw';

// API基础URL
const API_BASE_URL = 'http://localhost:8080/api';

// 模拟用户数据
const mockUser = {
  id: "1",
  phoneNumber: "13800138000",
  realName: "测试用户",
  email: "test@example.com",
  idCardNumber: "110101199001010011",
  accountStatus: "ACTIVE",
  lastLoginTime: "2025-07-16T10:00:00"
};

// 请求处理器
export const handlers = [
  // 登录请求处理
  rest.post(`${API_BASE_URL}/auth/login`, (req, res, ctx) => {
    const { phoneNumber, password } = req.body;
    
    // 验证手机号和密码
    if (phoneNumber === '13800138000' && password === 'password123') {
      return res(
        ctx.status(200),
        ctx.json({
          success: true,
          user: mockUser,
          message: "登录成功"
        })
      );
    } else if (phoneNumber !== '13800138000') {
      return res(
        ctx.status(400),
        ctx.json({
          success: false,
          message: "用户不存在"
        })
      );
    } else {
      return res(
        ctx.status(400),
        ctx.json({
          success: false,
          message: "密码错误"
        })
      );
    }
  }),
  
  // 注册请求处理
  rest.post(`${API_BASE_URL}/auth/register`, (req, res, ctx) => {
    const userData = req.body;
    
    // 验证手机号是否已注册
    if (userData.phoneNumber === '13800138000') {
      return res(
        ctx.status(400),
        ctx.json({
          success: false,
          message: "手机号已被注册"
        })
      );
    }
    
    // 验证邮箱是否已注册
    if (userData.email === 'test@example.com') {
      return res(
        ctx.status(400),
        ctx.json({
          success: false,
          message: "邮箱已被注册"
        })
      );
    }
    
    // 验证身份证号码是否合法
    const idCardPattern = /^[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;
    if (!idCardPattern.test(userData.idCardNumber)) {
      return res(
        ctx.status(400),
        ctx.json({
          success: false,
          message: "身份证号码不合法"
        })
      );
    }
    
    // 注册成功
    return res(
      ctx.status(200),
      ctx.json({
        success: true,
        user: {
          ...userData,
          id: "2",
          accountStatus: "ACTIVE",
          lastLoginTime: new Date().toISOString()
        },
        message: "注册成功"
      })
    );
  }),
  
  // 登出请求处理
  rest.post(`${API_BASE_URL}/auth/logout`, (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        success: true,
        message: "登出成功"
      })
    );
  }),
  
  // 获取当前用户信息
  rest.get(`${API_BASE_URL}/auth/currentUser`, (req, res, ctx) => {
    // 检查认证状态 (在实际应用中这应该检查cookie/token)
    const isAuthenticated = req.headers.get('Authorization') === 'Bearer test-token';
    
    if (isAuthenticated) {
      return res(
        ctx.status(200),
        ctx.json({
          success: true,
          user: mockUser
        })
      );
    } else {
      return res(
        ctx.status(401),
        ctx.json({
          success: false,
          message: "未认证"
        })
      );
    }
  })
];
