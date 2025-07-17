/* eslint-env node */
/**
 * @jest-environment node
 * Mock Service Worker 服务器设置
 * 这不是一个测试文件
 */
import { setupServer } from 'msw/node';
import { handlers } from './handlers';

// 创建请求拦截服务器
export const server = setupServer(...handlers);
