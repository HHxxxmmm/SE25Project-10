import React, { useState, useEffect } from 'react';
import { Card, Radio, Button, message, Row, Col, Divider, Typography } from 'antd';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { ArrowLeftOutlined, AlipayCircleOutlined, WechatOutlined, CreditCardOutlined } from '@ant-design/icons';
import './style.css';
import { orderAPI, waitlistAPI } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';

const { Title, Text } = Typography;

const PaymentPage = () => {
    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);
    const [timeLeft, setTimeLeft] = useState(15 * 60); // 15分钟倒计时
    const [paymentMethod, setPaymentMethod] = useState('alipay');
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();
    const orderId = searchParams.get('orderId');
    const waitlistId = searchParams.get('waitlistId');
    const isWaitlist = searchParams.get('isWaitlist') === 'true';
    
    // 加载订单数据
    useEffect(() => {
        const fetchOrder = async () => {
            // 检查用户是否已登录
            if (!user) {
                console.log('用户未登录，跳转到登录页');
                message.error('请先登录');
                navigate('/login');
                return;
            }

            // 检查用户ID是否存在 - 后端返回的是userId字段
            const userId = user.userId;
            if (!userId) {
                console.log('用户ID不存在:', user);
                message.error('用户信息不完整，请重新登录');
                navigate('/login');
                return;
            }

            try {
                setLoading(true);
                console.log('获取订单参数:', orderId);
                
                // 使用URL参数或本地存储的ID
                const orderIdToUse = orderId || localStorage.getItem('current_order_id');
                const waitlistIdToUse = waitlistId || localStorage.getItem('current_waitlist_id');
                
                if (!orderIdToUse && !waitlistIdToUse) {
                    console.error('未找到订单ID或候补订单ID');
                    message.error('未找到有效的订单ID');
                    navigate('/orders');
                    return;
                }
                
                // 调用后端API获取订单详情
                console.log('获取订单详情，用户ID:', userId, '是否候补订单:', isWaitlist);
                
                let orderDetail;
                if (isWaitlist && waitlistIdToUse) {
                    // 获取候补订单详情
                    const result = await waitlistAPI.getWaitlistOrderDetail(userId, waitlistIdToUse);
                    if (result.status === 'SUCCESS') {
                        orderDetail = result.waitlistOrder;
                    } else {
                        throw new Error(result.message || '获取候补订单详情失败');
                    }
                } else {
                    // 获取普通订单详情
                    orderDetail = await orderAPI.getOrderDetail(orderIdToUse, userId);
                }
                console.log('订单详情:', orderDetail);
                
                if (orderDetail) {
                    setOrder(orderDetail);
                } else {
                    message.error('获取订单详情失败');
                    navigate('/orders');
                    return;
                }
                
            } catch (error) {
                console.error('获取订单数据失败:', error);
                message.error('获取订单数据失败，请重试');
                navigate('/orders');
            } finally {
                setLoading(false);
            }
        };
        
        // 只有在用户存在且已认证时才获取订单
        if (user && user.userId) {
        fetchOrder();
        }
    }, [orderId, user]); // 移除navigate依赖，避免不必要的重新渲染
    
    // 倒计时效果 - 基于订单创建时间计算
    useEffect(() => {
        if (!order || !order.orderTime) return;
        
        // 计算订单创建时间到现在的秒数
        const calculateTimeLeft = () => {
            const orderTime = new Date(order.orderTime);
            const now = new Date();
            const elapsedSeconds = Math.floor((now - orderTime) / 1000);
            const timeoutSeconds = 15 * 60; // 15分钟超时
            const remainingSeconds = Math.max(0, timeoutSeconds - elapsedSeconds);
            return remainingSeconds;
        };
        
        // 初始化倒计时
        setTimeLeft(calculateTimeLeft());
        
        const timer = setInterval(() => {
            const remaining = calculateTimeLeft();
            setTimeLeft(remaining);
            
            if (remaining <= 0) {
                    clearInterval(timer);
                    message.warning('支付超时，订单已取消');
                    navigate('/orders');
                }
        }, 1000);
        
        return () => clearInterval(timer);
    }, [order, navigate]);
    
    // 定期检查订单是否已超时（每10秒检查一次）
    useEffect(() => {
        if (!order || !user) return;
        
        const checkTimeout = async () => {
            try {
                const response = await orderAPI.checkOrderTimeout(order.orderId, user.userId);
                if (response.status === 'FAILURE' && response.message.includes('超时')) {
                    message.warning('订单已超时，正在跳转...');
                    navigate('/orders');
                }
            } catch (error) {
                console.error('检查订单超时失败:', error);
            }
        };
        
        // 立即检查一次
        checkTimeout();
        
        // 每10秒检查一次
        const timeoutCheckTimer = setInterval(checkTimeout, 10000);
        
        return () => clearInterval(timeoutCheckTimer);
    }, [order, user, navigate]);
    
    // 格式化倒计时显示
    const formatTimeLeft = () => {
        const minutes = Math.floor(timeLeft / 60);
        const seconds = timeLeft % 60;
        return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    };
    
    // 处理支付方式变更
    const handlePaymentMethodChange = (e) => {
        setPaymentMethod(e.target.value);
    };
    
    // 处理提交支付
    const handleSubmitPayment = async () => {
        // 检查用户是否已登录
        if (!user) {
            message.error('请先登录');
            navigate('/login');
            return;
        }

        // 检查用户ID是否存在 - 后端返回的是userId字段
        const userId = user.userId;
        if (!userId) {
            message.error('用户信息不完整，请重新登录');
            navigate('/login');
            return;
        }
        
        // 确保有有效的orderId或waitlistId
        const orderIdToUse = orderId || localStorage.getItem('current_order_id');
        const waitlistIdToUse = waitlistId || localStorage.getItem('current_waitlist_id');
        
        if (!orderIdToUse && !waitlistIdToUse) {
            message.error('订单ID不能为空');
            return;
        }
        
        try {
            setLoading(true);
            
            let paymentResponse;
            if (isWaitlist && waitlistIdToUse) {
                // 支付候补订单
                console.log('提交候补订单支付，用户ID:', userId, '候补订单ID:', waitlistIdToUse);
                paymentResponse = await waitlistAPI.payWaitlistOrder(waitlistIdToUse, userId);
            } else {
                // 支付普通订单
            console.log('提交支付，用户ID:', userId, '订单ID:', orderIdToUse);
                paymentResponse = await orderAPI.payOrder(orderIdToUse, userId);
            }
            console.log('支付响应:', paymentResponse);
            
            if (paymentResponse.status === 'SUCCESS') {
                message.success('支付成功');
                if (isWaitlist && waitlistIdToUse) {
                    navigate(`/order-detail?waitlistId=${waitlistIdToUse}`);
                } else {
                navigate(`/order-detail?orderId=${orderIdToUse}`);
                }
            } else {
                message.error('支付失败: ' + paymentResponse.message);
            }
        } catch (error) {
            console.error('支付处理失败:', error);
            message.error('支付失败，请重试');
        } finally {
            setLoading(false);
        }
    };
    
    // 处理取消支付
    const handleCancelPayment = () => {
        navigate('/orders');
    };
    
    if (loading || !order) {
        return (
            <div className="payment-loading">
                <div className="loading-spinner"></div>
                <p>正在加载订单信息...</p>
            </div>
        );
    }
    
    return (
        <Card className="payment-card" bordered={false} bodyStyle={{ padding: 0 }}>
            <div className="card-title">
                <ArrowLeftOutlined className="back-icon" onClick={() => navigate('/orders')} />
                未完成
                <div className="timer-badge">
                    <span className="payment-timer-label">剩余：</span>
                    <span className="payment-timer">{formatTimeLeft()}</span>
                </div>
            </div>
            
            <Divider />
            
            <div className="payment-content">
                <div className="order-summary">
                    <Row gutter={[16, 16]}>
                        <Col span={24}>
                            <Title level={4}>{isWaitlist ? '候补订单信息' : '订单信息'}</Title>
                        </Col>
                        
                        <Col span={24}>
                            <div className="train-info-card">
                                <div className="train-date">发车时间：{order.travelDate}</div>
                                <div className="train-route">
                                    <div className="station-block">
                                        <div className="station-name">{order.departureStation}</div>
                                        <div className="station-time">{order.departureTime}</div>
                                    </div>
                                    
                                    <div className="arrow-block">
                                        <div className="train-number">{order.trainNumber}</div>
                                        <div className="arrow">→</div>
                                    </div>
                                    
                                    <div className="station-block">
                                        <div className="station-name">{order.arrivalStation}</div>
                                        <div className="station-time">{order.arrivalTime}</div>
                                    </div>
                                </div>
                            </div>
                        </Col>
                        
                        <Col span={24}>
                            <div className="passengers-detail">
                                <div className="detail-label">乘车人信息：</div>
                                {(isWaitlist ? order.items : order.tickets).map((item, index) => (
                                    <div key={index} className="passenger-detail-item">
                                        <div className="passenger-info">
                                            <span className="passenger-name">{isWaitlist ? item.passengerName : item.passengerName}</span>
                                            <span className="passenger-id">（{isWaitlist ? item.idCardNumber : item.idCardNumber}）</span>
                                            <span className="passenger-seat">
                                                {isWaitlist ? item.carriageTypeName : item.carriageType}
                                            </span>
                                            <span className="passenger-price">
                                                ¥{item.price || '待定'}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </Col>
                        
                        <Col span={24}>
                            <div className="order-price">
                                <Text type="secondary">总金额：</Text>
                                <Text type="danger" strong className="price-value">
                                    ¥{order.totalAmount}
                                </Text>
                            </div>
                        </Col>
                    </Row>
                </div>
                
                <Divider />
                
                <div className="payment-method-section">
                    <Title level={4}>支付方式</Title>
                    <Radio.Group 
                        onChange={handlePaymentMethodChange} 
                        value={paymentMethod}
                        className="payment-methods"
                    >
                        <Radio.Button value="alipay" className="payment-method-option">
                            <AlipayCircleOutlined className="payment-icon alipay" />
                            <span>支付宝</span>
                        </Radio.Button>
                        <Radio.Button value="wechat" className="payment-method-option">
                            <WechatOutlined className="payment-icon wechat" />
                            <span>微信支付</span>
                        </Radio.Button>
                        <Radio.Button value="bankcard" className="payment-method-option">
                            <CreditCardOutlined className="payment-icon bankcard" />
                            <span>银行卡</span>
                        </Radio.Button>
                    </Radio.Group>
                </div>
                
                <div className="payment-action">
                    <Button 
                        type="primary" 
                        size="large" 
                        onClick={handleSubmitPayment} 
                        loading={loading}
                        className="pay-button"
                    >
                        立即支付
                    </Button>
                    
                    <Button 
                        size="large"
                        onClick={handleCancelPayment}
                        className="cancel-button"
                    >
                        取消支付
                    </Button>
                </div>
            </div>
            
            <div className="payment-notice">
                <h3>温馨提示：</h3>
                <ul>
                    <li>请在 {Math.floor(timeLeft / 60)} 分钟内完成支付，超时订单将自动取消</li>
                    <li>支付成功后，系统将自动为您出票</li>
                    <li>如遇支付问题，请联系客服 400-123-4567</li>
                </ul>
            </div>
        </Card>
    );
};

// 修复导出问题 - 确保添加默认导出
export default PaymentPage;
