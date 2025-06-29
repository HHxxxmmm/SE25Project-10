import React, { useState, useEffect } from 'react';
import { Card, Radio, Button, message, Row, Col, Divider, Typography } from 'antd';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { 
    AlipayCircleOutlined, 
    WechatOutlined, 
    CreditCardOutlined, 
    ArrowLeftOutlined
} from '@ant-design/icons';
import './style.css';
import generateOrdersData from '../../mock/Orders';

const { Title, Text } = Typography;

const PaymentPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [searchParams] = useSearchParams();
    const orderId = searchParams.get('orderId');
    
    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);
    const [paymentMethod, setPaymentMethod] = useState('alipay');
    const [timeLeft, setTimeLeft] = useState(15 * 60); // 15分钟倒计时
    
    // 加载订单数据
    useEffect(() => {
        const fetchOrder = async () => {
            try {
                setLoading(true);
                console.log('获取订单参数:', orderId);
                console.log('从本地存储获取:', localStorage.getItem('current_order_id'));
                
                // 使用URL参数或本地存储的ID
                const orderIdToUse = orderId || localStorage.getItem('current_order_id');
                
                // 模拟API请求获取订单数据
                const data = generateOrdersData();
                let foundOrder = null;
                
                if (orderIdToUse) {
                    console.log('查找订单:', orderIdToUse);
                    foundOrder = data.news.find(o => o.order_id === orderIdToUse);
                    
                    // 如果没找到订单，创建一个新的模拟订单
                    if (!foundOrder) {
                        console.log('未找到有效订单，创建模拟订单');
                        
                        // 使用已有数据作为模板
                        const templateOrder = data.news[0];
                        
                        // 创建新的订单对象
                        foundOrder = {
                            ...templateOrder,
                            order_id: orderIdToUse,
                            o_status: 1, // 待支付状态
                            o_time: new Date().toISOString().replace('T', ' ').substring(0, 19),
                        };
                    }
                } else if (data.news.length > 0) {
                    console.log('未找到订单ID，使用第一个待支付订单');
                    foundOrder = data.news.find(o => o.o_status === 1);
                }
                
                if (!foundOrder) {
                    console.error('未找到有效订单');
                    message.error('未找到有效的待支付订单');
                    navigate('/orders');
                    return;
                }
                
                console.log('使用订单:', foundOrder);
                setOrder(foundOrder);
                
            } catch (error) {
                console.error('获取订单数据失败:', error);
                message.error('获取订单数据失败，请重试');
            } finally {
                setLoading(false);
            }
        };
        
        fetchOrder();
    }, [orderId, navigate]);
    
    // 倒计时效果
    useEffect(() => {
        if (!order) return;
        
        const timer = setInterval(() => {
            setTimeLeft(prev => {
                if (prev <= 1) {
                    clearInterval(timer);
                    message.warning('支付超时，订单已取消');
                    navigate('/orders');
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
        
        return () => clearInterval(timer);
    }, [order, navigate]);
    
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
        try {
            setLoading(true);
            
            // 模拟支付处理
            await new Promise(resolve => setTimeout(resolve, 1500));
            
            message.success('支付成功');
            navigate('/order-detail', { state: { orderId: order.order_id, paid: true } });
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
    
    // 计算总价格
    const calculateTotalPrice = (order) => {
        if (!order || !order.passengers) return 0;
        return order.passengers.reduce((sum, passenger) => sum + passenger.price, 0);
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
                            <Title level={4}>订单信息</Title>
                        </Col>
                        
                        <Col span={24}>
                            <div className="train-info-card">
                                <div className="train-date">发车时间：{order.t_time.slice(0, 10)}</div>
                                <div className="train-route">
                                    <div className="station-block">
                                        <div className="station-time">{order.t_time.slice(11, 16)}</div>
                                        <div className="station-name">{order.t_from_city}{order.t_from_station}</div>
                                    </div>
                                    
                                    <div className="arrow-block">
                                        <div className="train-number">{order.train_id}</div>
                                        <div className="arrow">→</div>
                                    </div>
                                    
                                    <div className="station-block">
                                        <div className="station-time">{order.arrive_time.slice(11, 16)}</div>
                                        <div className="station-name">{order.t_to_city}{order.t_to_station}</div>
                                    </div>
                                </div>
                            </div>
                        </Col>
                        
                        <Col span={24}>
                            <div className="passengers-detail">
                                <div className="detail-label">乘车人信息：</div>
                                {order.passengers.map((passenger, index) => (
                                    <div key={index} className="passenger-detail-item">
                                        <div className="passenger-info">
                                            <span className="passenger-name">{passenger.name}</span>
                                            <span className="passenger-id">（{passenger.id}）</span>
                                            <span className="passenger-seat">
                                                {passenger.seat === 1 ? '头等座' : 
                                                 passenger.seat === 2 ? '商务座' : 
                                                 passenger.seat === 3 ? '二等座' : '无座'}
                                            </span>
                                            <span className="passenger-price">
                                                ¥{passenger.price}
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
                                    ¥{calculateTotalPrice(order)}
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
                        确认支付 ¥{calculateTotalPrice(order)}
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
