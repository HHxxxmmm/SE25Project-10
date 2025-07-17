/**
 * 测试环境配置文件
 * 这个文件会在Jest启动时自动加载
 */
// jest-dom adds custom jest matchers for asserting on DOM nodes.
import '@testing-library/jest-dom'; // 提供额外的断言
import { configure } from '@testing-library/react';
import { server } from './__tests__/mocks/server';

// 模拟 window.matchMedia - 解决 Ant Design 相关测试问题
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(), // 废弃的 API
    removeListener: jest.fn(), // 废弃的 API
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// 设置React测试库
configure({
  testIdAttribute: 'data-testid',
});

// 在所有测试之前建立API模拟服务器
beforeAll(() => {
  server.listen({
    onUnhandledRequest: 'warn', // 警告未被捕获的请求
  });
});

// 在每个测试之间重置服务器处理程序
afterEach(() => {
  server.resetHandlers();
});

// 在所有测试之后关闭服务器
afterAll(() => {
  server.close();
});

// 静默控制台错误和警告
jest.spyOn(console, 'error').mockImplementation(() => {});
jest.spyOn(console, 'warn').mockImplementation(() => {});
jest.spyOn(console, 'log').mockImplementation(() => {});
