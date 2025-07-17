import axios from 'axios';
import api, { authAPI } from '../../api/auth';
import { rest } from 'msw';
import { setupServer } from 'msw/node';

// Mock console methods to avoid cluttering test output
const originalConsoleLog = console.log;
const originalConsoleError = console.error;

// 模拟API响应的服务器
const server = setupServer(
  // 登录接口模拟
  rest.post('http://localhost:8080/api/auth/login', (req, res, ctx) => {
    const { phoneNumber, password } = req.body;
    
    if (phoneNumber === '13800138000' && password === 'password123') {
      return res(
        ctx.status(200),
        ctx.json({
          success: true,
          data: {
            id: 1,
            phoneNumber: '13800138000',
            name: '测试用户',
            token: 'fake-jwt-token'
          }
        })
      );
    }
    
    return res(
      ctx.status(401),
      ctx.json({
        success: false,
        message: '手机号或密码不正确'
      })
    );
  }),
  
  // 注册接口模拟
  rest.post('http://localhost:8080/api/auth/register', (req, res, ctx) => {
    const { phoneNumber } = req.body;
    
    if (phoneNumber === '13900139000') {
      return res(
        ctx.status(200),
        ctx.json({
          success: true,
          data: {
            id: 2,
            phoneNumber: '13900139000',
            name: req.body.name
          }
        })
      );
    }
    
    return res(
      ctx.status(400),
      ctx.json({
        success: false,
        message: '该手机号已被注册'
      })
    );
  }),
  
  // 登出接口模拟
  rest.post('http://localhost:8080/api/auth/logout', (_, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        success: true,
        message: '成功退出登录'
      })
    );
  }),
  
  // 获取当前用户信息接口模拟
  rest.get('http://localhost:8080/api/auth/currentUser', (req, res, ctx) => {
    const authHeader = req.headers.get('Authorization');
    
    if (authHeader && authHeader.includes('fake-jwt-token')) {
      return res(
        ctx.status(200),
        ctx.json({
          success: true,
          data: {
            id: 1,
            phoneNumber: '13800138000',
            name: '测试用户'
          }
        })
      );
    }
    
    return res(
      ctx.status(401),
      ctx.json({
        success: false,
        message: '未登录或会话已过期'
      })
    );
  }),

  // 处理网络错误的接口模拟
  rest.get('http://localhost:8080/api/network-error', (_, res) => {
    return res.networkError('模拟网络错误');
  }),

  // CORS错误模拟
  rest.get('http://localhost:8080/api/cors-error', (_, res) => {
    return res.networkError('CORS policy: No CORS header');
  }),

  // Network Error模拟
  rest.get('http://localhost:8080/api/network-cors-error', (_, res) => {
    return res.networkError('Network Error');
  }),

  // 特定CORS错误模拟
  rest.get('http://localhost:8080/api/cors-specific-error', (_, res) => {
    return res.networkError('CORS policy: No CORS header');
  }),

  // 测试端点
  rest.get('http://localhost:8080/api/test-endpoint', (_, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({ message: '测试成功' })
    );
  }),

  // 401未授权错误模拟
  rest.get('http://localhost:8080/api/unauthorized', (_, res, ctx) => {
    return res(
      ctx.status(401),
      ctx.json({
        success: false,
        message: '未授权访问'
      })
    );
  })
);

// 定义全局错误处理
global.fail = (message) => {
  throw new Error(message);
};

describe('API模块测试', () => {
  // 在所有测试之前启动模拟服务器
  beforeAll(() => {
    server.listen({onUnhandledRequest: 'error'});
    // 静默控制台输出以保持测试输出干净
    console.log = jest.fn();
    console.error = jest.fn();
    
    // 模拟XMLHttpRequest以避免JSDOM网络错误
    jest.spyOn(global, 'XMLHttpRequest').mockImplementation(() => ({
      open: jest.fn(),
      send: jest.fn(),
      setRequestHeader: jest.fn(),
      onreadystatechange: jest.fn(),
      readyState: 4,
      status: 200,
      response: JSON.stringify({success: true})
    }));
  });

  // 每个测试之后重置请求处理程序
  afterEach(() => {
    server.resetHandlers();
    jest.clearAllMocks();
  });

  // 在所有测试之后关闭服务器
  afterAll(() => {
    server.close();
    // 恢复控制台功能
    console.log = originalConsoleLog;
    console.error = originalConsoleError;
    jest.restoreAllMocks();
  });

  describe('Axios实例配置测试', () => {
    let originalEnv;
    
    beforeEach(() => {
      // 保存原始环境变量
      originalEnv = process.env.REACT_APP_API_URL;
    });
    
    afterEach(() => {
      // 恢复原始环境变量
      process.env.REACT_APP_API_URL = originalEnv;
      // 重新加载模块以重置配置
      jest.resetModules();
    });
    
    test('应该使用环境变量中的API_BASE_URL', () => {
      // 设置环境变量
      process.env.REACT_APP_API_URL = 'http://test-api.example.com';
      
      // 重新导入模块以应用新的环境变量
      jest.resetModules();
      const { default: freshApi } = require('../../api/auth');
      
      // 检查是否使用了环境变量中的URL
      expect(freshApi.defaults.baseURL).toBe('http://test-api.example.com');
    });
    
    test('当环境变量不存在时应该使用默认URL', () => {
      // 清除环境变量
      delete process.env.REACT_APP_API_URL;
      
      // 重新导入模块以应用新的环境变量
      jest.resetModules();
      const { default: freshApi } = require('../../api/auth');
      
      // 检查是否使用了默认URL
      expect(freshApi.defaults.baseURL).toBe('http://localhost:8080/api');
    });
    
    test('应该正确配置baseURL', () => {
      expect(api.defaults.baseURL).toBe('http://localhost:8080/api');
    });

    test('应该启用withCredentials', () => {
      expect(api.defaults.withCredentials).toBe(true);
    });

    test('应该设置超时时间', () => {
      expect(api.defaults.timeout).toBe(10000);
    });

    test('应该设置正确的Content-Type', () => {
      expect(api.defaults.headers['Content-Type']).toBe('application/json');
    });
  });

  describe('拦截器测试', () => {
    test('请求拦截器应记录请求URL', async () => {
      // 直接模拟请求拦截器的行为，而不是实际发送请求
      const mockConfig = { url: '/test-endpoint' };
      const requestInterceptor = api.interceptors.request.handlers[0].fulfilled;
      requestInterceptor(mockConfig);
      
      expect(console.log).toHaveBeenCalledWith('发送请求:', '/test-endpoint');
    });

    test('请求拦截器应处理错误', async () => {
      // 直接测试请求拦截器的错误处理
      const mockError = new Error('请求拦截器错误测试');
      const requestErrorHandler = api.interceptors.request.handlers[0].rejected;
      
      try {
        await requestErrorHandler(mockError);
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.message).toBe('请求拦截器错误测试');
        expect(console.error).toHaveBeenCalledWith('请求错误:', mockError);
      }
    });

    test('响应拦截器应记录响应数据', async () => {
      // 直接测试响应拦截器
      const mockResponse = { 
        data: { message: '测试成功' } 
      };
      const responseInterceptor = api.interceptors.response.handlers[0].fulfilled;
      responseInterceptor(mockResponse);
      
      expect(console.log).toHaveBeenCalledWith('接收响应:', mockResponse.data);
    });

    test('响应拦截器应处理401错误', async () => {
      // 清除所有之前的模拟调用
      console.log.mockClear();
      console.error.mockClear();
      
      // 创建一个401错误对象
      const mockError = {
        message: '未授权访问',
        response: {
          status: 401,
          data: {
            success: false,
            message: '未认证'
          }
        },
        config: {}
      };
      
      // 直接测试响应拦截器的错误处理
      const responseErrorHandler = api.interceptors.response.handlers[0].rejected;
      
      try {
        await responseErrorHandler(mockError);
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.response.status).toBe(401);
      }
      
      // 验证相应的控制台消息被记录
      expect(console.error).toHaveBeenCalled();
      expect(console.log).toHaveBeenCalledWith('未授权，需要登录');
    });

    test('响应拦截器应处理网络错误', async () => {
      // 清除所有之前的模拟调用
      console.log.mockClear();
      console.error.mockClear();
      
      // 创建一个网络错误对象
      const mockError = {
        message: 'Network Error',
        config: {}
      };
      
      // 直接测试响应拦截器的错误处理
      const responseErrorHandler = api.interceptors.response.handlers[0].rejected;
      
      try {
        await responseErrorHandler(mockError);
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.message).toBe('Network Error');
      }
      
      // 验证相应的控制台消息被记录
      expect(console.error).toHaveBeenCalled();
      expect(console.error).toHaveBeenCalledWith('可能是CORS问题，请检查后端CORS配置');
    });

    test('响应拦截器应处理CORS错误', async () => {
      // 清除所有之前的模拟调用
      console.log.mockClear();
      console.error.mockClear();
      
      // 创建一个CORS错误对象
      const mockError = {
        message: 'CORS policy: No CORS header',
        config: {}
      };
      
      // 直接测试响应拦截器的错误处理
      const responseErrorHandler = api.interceptors.response.handlers[0].rejected;
      
      try {
        await responseErrorHandler(mockError);
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.message).toBe('CORS policy: No CORS header');
      }
      
      // 验证相应的控制台消息被记录
      expect(console.error).toHaveBeenCalled();
      expect(console.error).toHaveBeenCalledWith('可能是CORS问题，请检查后端CORS配置');
    });
  });

  describe('认证API测试', () => {
    beforeEach(() => {
      console.log.mockClear();
      console.error.mockClear();
    });
    
    test('login函数应在凭据正确时返回用户数据', async () => {
      // 模拟axios.post方法
      jest.spyOn(api, 'post').mockImplementationOnce(() => {
        return Promise.resolve({
          status: 200,
          data: {
            success: true,
            message: '登录成功',
            user: {
              id: '1',
              phoneNumber: '13800138000',
              realName: '测试用户',
              email: 'test@example.com',
              idCardNumber: '110101199001010011',
              accountStatus: 'ACTIVE',
              lastLoginTime: '2025-07-16T10:00:00'
            }
          }
        });
      });

      const response = await authAPI.login('13800138000', 'password123');
      
      expect(response.status).toBe(200);
      expect(response.data).toEqual({
        success: true,
        message: '登录成功',
        user: {
          id: '1',
          phoneNumber: '13800138000',
          realName: '测试用户',
          email: 'test@example.com',
          idCardNumber: '110101199001010011',
          accountStatus: 'ACTIVE',
          lastLoginTime: '2025-07-16T10:00:00'
        }
      });
    });

    test('login函数应在凭据错误时返回错误', async () => {
      // 模拟axios.post方法抛出错误
      jest.spyOn(api, 'post').mockImplementationOnce(() => {
        return Promise.reject({
          response: {
            status: 400,
            data: {
              success: false,
              message: '手机号或密码不正确'
            }
          }
        });
      });
      
      try {
        await authAPI.login('13800138000', 'wrong-password');
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.response.status).toBe(400);
        expect(error.response.data).toEqual({
          success: false,
          message: '手机号或密码不正确'
        });
      }
    });

    test('login函数应处理服务器内部错误', async () => {
      // 模拟axios.post方法抛出服务器错误
      jest.spyOn(api, 'post').mockImplementationOnce(() => {
        const error = new Error('服务器内部错误');
        error.response = {
          status: 500,
          data: {
            success: false,
            message: '服务器内部错误'
          }
        };
        return Promise.reject(error);
      });
      
      try {
        await authAPI.login('13800138000', 'password123');
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.response.status).toBe(500);
      }
    });

    test('register函数应在注册成功时返回用户数据', async () => {
      // 模拟axios.post方法
      jest.spyOn(api, 'post').mockImplementationOnce(() => {
        return Promise.resolve({
          status: 200,
          data: {
            success: true,
            data: {
              id: 2,
              phoneNumber: '13900139000',
              name: '新用户'
            }
          }
        });
      });
      
      const userData = {
        phoneNumber: '13900139000',
        password: 'password123',
        name: '新用户'
      };
      
      const response = await authAPI.register(userData);
      
      expect(response.status).toBe(200);
      expect(response.data).toEqual({
        success: true,
        data: {
          id: 2,
          phoneNumber: '13900139000',
          name: '新用户'
        }
      });
    });

    test('register函数应在手机号已存在时返回错误', async () => {
      // 模拟axios.post方法抛出错误
      jest.spyOn(api, 'post').mockImplementationOnce(() => {
        return Promise.reject({
          response: {
            status: 400,
            data: {
              success: false,
              message: '手机号已被注册'
            }
          }
        });
      });
      
      const userData = {
        phoneNumber: '13800138000', // 已被注册的号码
        password: 'password123',
        name: '新用户'
      };
      
      try {
        await authAPI.register(userData);
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.response.status).toBe(400);
        expect(error.response.data).toEqual({
          success: false,
          message: '手机号已被注册'
        });
      }
    });

    test('register函数应处理网络错误', async () => {
      // 模拟axios.post方法抛出网络错误
      jest.spyOn(api, 'post').mockImplementationOnce(() => {
        const error = new Error('Network Error');
        // 网络错误没有response对象
        return Promise.reject(error);
      });

      try {
        await authAPI.register({
          phoneNumber: '13900139000',
          password: 'password123',
          name: '新用户'
        });
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.message).toBe('Network Error');
        // 对于网络错误，没有response对象
        expect(error.response).toBeUndefined();
      }
    });

    test('logout函数应成功登出', async () => {
      // 清除之前的模拟调用
      console.log.mockClear();
      console.error.mockClear();
      
      // 模拟axios.post方法
      jest.spyOn(api, 'post').mockImplementationOnce(() => {
        return Promise.resolve({
          status: 200,
          data: {
            success: true,
            message: '登出成功'
          }
        });
      });
      
      const response = await authAPI.logout();
      
      expect(response.status).toBe(200);
      expect(response.data).toEqual({
        success: true,
        message: '登出成功'
      });
      
      // 验证控制台日志调用
      expect(console.log).toHaveBeenCalledWith(expect.stringContaining('====> [DEBUG] API logout 函数被调用'), expect.any(String));
      expect(console.log).toHaveBeenCalledWith(expect.stringContaining('====> [DEBUG] API logout 调用成功'), expect.any(Object));
    });

    test('logout函数应处理错误情况', async () => {
      // 清除之前的模拟调用
      console.log.mockClear();
      console.error.mockClear();
      
      // 模拟axios.post方法抛出错误
      jest.spyOn(api, 'post').mockImplementationOnce(() => {
        const error = new Error('服务器错误');
        error.response = {
          status: 500,
          data: {
            success: false,
            message: '服务器内部错误'
          }
        };
        return Promise.reject(error);
      });
      
      try {
        await authAPI.logout();
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.response.status).toBe(500);
        expect(console.error).toHaveBeenCalledWith(expect.stringContaining('====> [DEBUG] API logout 调用失败'), expect.any(Object));
      }
    });

    test('getCurrentUser函数应在已登录时返回用户信息', async () => {
      // 模拟axios.get方法
      jest.spyOn(api, 'get').mockImplementationOnce(() => {
        return Promise.resolve({
          status: 200,
          data: {
            success: true,
            data: {
              id: 1,
              phoneNumber: '13800138000',
              name: '测试用户'
            }
          }
        });
      });

      const response = await authAPI.getCurrentUser();
      
      expect(response.status).toBe(200);
      expect(response.data).toEqual({
        success: true,
        data: {
          id: 1,
          phoneNumber: '13800138000',
          name: '测试用户'
        }
      });
    });

    test('getCurrentUser函数应在未登录时返回错误', async () => {
      // 模拟axios.get方法抛出错误
      jest.spyOn(api, 'get').mockImplementationOnce(() => {
        return Promise.reject({
          response: {
            status: 401,
            data: {
              success: false,
              message: '未认证'
            }
          }
        });
      });
      
      try {
        await authAPI.getCurrentUser();
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.response.status).toBe(401);
        expect(error.response.data).toEqual({
          success: false,
          message: '未认证'
        });
      }
    });
  });
  
  describe('异常情况测试', () => {
    // 在每个测试前清除拦截器状态
    beforeEach(() => {
      // 清除之前的模拟调用
      console.log.mockClear();
      console.error.mockClear();
    });
    
    test('应处理网络超时情况', async () => {
      // 模拟axios.get方法抛出超时错误
      jest.spyOn(api, 'get').mockImplementationOnce(() => {
        const error = new Error('Network Error');
        return Promise.reject(error);
      });
      
      try {
        await api.get('/timeout');
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.message).toBe('Network Error');
        
        // 验证响应拦截器的错误处理
        const responseErrorHandler = api.interceptors.response.handlers[0].rejected;
        try {
          await responseErrorHandler(error);
        } catch (e) {
          // 拦截器会重新抛出错误
        }
        
        // 验证控制台错误调用
        expect(console.error).toHaveBeenCalled();
      }
    });
    
    test('应处理服务器内部错误', async () => {
      // 模拟axios.get方法抛出服务器错误
      jest.spyOn(api, 'get').mockImplementationOnce(() => {
        const error = new Error('服务器内部错误');
        error.response = {
          status: 500,
          data: {
            success: false,
            message: '服务器内部错误'
          }
        };
        return Promise.reject(error);
      });
      
      try {
        await api.get('/server-error');
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.response.status).toBe(500);
        expect(error.response.data).toEqual({
          success: false,
          message: '服务器内部错误'
        });
        
        // 验证响应拦截器的错误处理
        const responseErrorHandler = api.interceptors.response.handlers[0].rejected;
        try {
          await responseErrorHandler(error);
        } catch (e) {
          // 拦截器会重新抛出错误
        }
        
        // 验证控制台错误调用
        expect(console.error).toHaveBeenCalled();
      }
    });

    test('logout函数应处理网络错误', async () => {
      // 模拟axios.post方法抛出网络错误
      jest.spyOn(api, 'post').mockImplementationOnce(() => {
        const error = new Error('fail is not defined');
        return Promise.reject(error);
      });
      
      try {
        await authAPI.logout();
        fail('应该抛出错误但没有');
      } catch (error) {
        expect(error.message).toBe('fail is not defined');
        expect(console.error).toHaveBeenCalledWith(expect.stringContaining('====> [DEBUG] API logout 调用失败'), expect.any(Object));
      }
    });
  });
});
