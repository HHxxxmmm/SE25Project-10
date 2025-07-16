import React, { useState, useEffect } from 'react';
import { Card, Typography, Divider, message, Spin, Button, Modal, Alert } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ticketAPI } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import QRCodeCanvas from '../../components/QRCodeCanvas';
import './style.css'; // 样式文件

const { Text } = Typography;

// 车票状态映射
const TICKET_STATUS = {
    0: { text: '待支付', colorClass: 'status-pending' },
    1: { text: '未使用', colorClass: 'status-unused' },
    2: { text: '已使用', colorClass: 'status-used' },
    3: { text: '已退票', colorClass: 'status-refunded' },
    4: { text: '已改签', colorClass: 'status-changed' },
};

// 票种映射
const TICKET_TYPE = {
    1: '成人票',
    2: '儿童票',
    3: '学生票',
    4: '残疾票',
    5: '军人票',
};

// 乘客类型映射
const PASSENGER_TYPE = {
    1: '成人',
    2: '儿童',
    3: '学生',
    4: '残疾军人',
};

const TicketDetailPage = () => {
    const [ticket, setTicket] = useState(null);
    const [loading, setLoading] = useState(true);
    const [digitalTicket, setDigitalTicket] = useState(null);
    const [digitalLoading, setDigitalLoading] = useState(false);
    const [qrVisible, setQrVisible] = useState(false);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();

    // 获取车票详情数据
    useEffect(() => {
        const fetchTicketDetail = async () => {
            const ticketId = searchParams.get('ticketId');
            const currentUserId = user?.userId; // 使用当前登录用户的ID

            if (!ticketId) {
                message.error('车票ID不存在');
                navigate('/my-tickets');
                return;
            }

            if (!currentUserId) {
                message.error('请先登录');
                navigate('/login');
                return;
            }

            setLoading(true);
            try {
                const response = await ticketAPI.getTicketDetail(parseInt(ticketId), currentUserId);
                
                if (response.status === 'SUCCESS' && response.ticket) {
                    setTicket(response.ticket);
                } else {
                    message.error(response.message || '获取车票详情失败');
                    navigate('/my-tickets');
                }
            } catch (error) {
                console.error('获取车票详情失败:', error);
                message.error('获取车票详情失败，请稍后重试');
                navigate('/my-tickets');
            } finally {
                setLoading(false);
            }
        };

        fetchTicketDetail();
    }, [searchParams, navigate]);
    
    // 获取数字票证数据
    const fetchDigitalTicket = async () => {
        const ticketId = searchParams.get('ticketId');
        const currentUserId = user?.userId;
        
        console.log('准备获取数字票证数据', { ticketId, currentUserId });
        
        if (!ticketId || !currentUserId) {
            console.error('缺少必要参数', { ticketId, currentUserId });
            return;
        }
        
        setDigitalLoading(true);
        try {
            console.log('调用API获取数字票证数据...');
            const response = await ticketAPI.getDigitalTicket(parseInt(ticketId), currentUserId);
            console.log('获取数字票证响应:', response);
            
            if (response.status === 'SUCCESS' && response.ticketData) {
                // 检查QR码数据格式
                const qrData = response.ticketData.qrCodeData;
                console.log('成功获取票证数据:', { 
                    ticketId: response.ticketData.ticketId,
                    ticketNumber: response.ticketData.ticketNumber,
                    qrDataLength: qrData?.length,
                    publicKeyLength: response.ticketData.publicKey?.length,
                    qrDataCharCodes: qrData ? Array.from(qrData.substring(0, 20)).map(c => c.charCodeAt(0)) : []
                });
                
                // 处理二维码数据，确保可以正确渲染
                if (qrData && qrData.length > 0) {
                    try {
                        // 验证QR数据格式是否正确
                        const parts = qrData.split('|');
                        console.log('QR数据分段:', {
                            partsCount: parts.length,
                            partsLengths: parts.map(p => p.length)
                        });
                        
                        if (parts.length !== 8) {
                            console.warn('QR数据格式不正确，应有8个部分但实际有', parts.length);
                        }
                    } catch (e) {
                        console.error('QR数据格式检查失败:', e);
                    }
                } else {
                    console.error('获取到的QR码数据为空');
                }
                
                setDigitalTicket(response.ticketData);
            } else {
                console.error('获取数字票证失败:', response.message);
                message.error(response.message || '获取数字票证失败');
            }
        } catch (error) {
            console.error('获取数字票证异常:', error);
            message.error('获取数字票证失败，请稍后重试');
        } finally {
            setDigitalLoading(false);
        }
    };
    
    // 显示二维码
    const showQRCode = () => {
        console.log('显示二维码', { digitalTicket: !!digitalTicket });
        if (!digitalTicket) {
            console.log('未找到票证数据，正在获取...');
            fetchDigitalTicket();
        }
        setQrVisible(true);
    };
    
    // 这里删除了不再需要的票证验证相关函数

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" />
                <div style={{ marginTop: '16px' }}>加载中...</div>
            </div>
        );
    }

    if (!ticket) {
        return <div>车票信息不存在</div>;
    }

    const formatDate = (dateString) => {
        if (!dateString) return '';
        return dateString.split('T')[0] || dateString.split(' ')[0] || '';
    };

    const formatTime = (timeString) => {
        if (!timeString) return '';
        const timePart = timeString.includes('T') ? 
            timeString.split('T')[1] : 
            timeString.split(' ')[1] || timeString;
        return timePart.slice(0, 5);
    };

    const tips = [
        '请提前15分钟到达车站，避免误车。',
        '车票一经售出，退票可能会产生一定费用，请慎重考虑。',
        '乘车时请携带有效身份证件，配合工作人员查验。',
        '请妥善保管车票，遗失不补。',
        '如需改签或退票，请提前办理相关手续。',
    ];

    return (
        <Card className="ticket-detail-card" bordered={false} bodyStyle={{ padding: 0 }}>
            <div className="card-title">
                <span className="back-link" onClick={() => navigate(-1)}>返回</span>
                车票详情
                <span className={`ticket-status ${TICKET_STATUS[ticket.ticketStatus]?.colorClass || ''}`}>
                    {TICKET_STATUS[ticket.ticketStatus]?.text || '未知状态'}
                </span>
            </div>

            <Divider />

            <div className="train-info-card">
                <div className="train-date">{formatDate(ticket.travelDate)} {formatTime(ticket.departureTime)}</div>
                <div className="train-route">
                    <div className="station-block">
                        <div className="station-name">{ticket.departureStationName}</div>
                        <div className="station-time">{formatTime(ticket.departureTime)}开</div>
                    </div>

                    <div className="arrow-block">
                        <div className="train-number">{ticket.trainNumber}</div>
                        <div className="arrow">→</div>
                    </div>

                    <div className="station-block">
                        <div className="station-name">{ticket.arrivalStationName}</div>
                        <div className="station-time">{formatTime(ticket.arrivalTime)}到</div>
                    </div>
                </div>
            </div>

            <Divider />

            <div className="ticket-info">
                <div><strong>乘车人：</strong>{ticket.passengerName}</div>
                <div><strong>身份证号：</strong>{ticket.passengerIdCard}</div>
                <div><strong>手机号：</strong>{ticket.passengerPhone}</div>
                <div><strong>乘客类型：</strong>{PASSENGER_TYPE[ticket.passengerType] || '未知'}</div>
                <div><strong>席别：</strong>{ticket.carriageTypeName || '未知'}</div>
                <div><strong>票种：</strong>{TICKET_TYPE[ticket.ticketType] || '未知'}</div>
                <div><strong>车厢座位：</strong>{ticket.carriageNumber}车{ticket.seatNumber}座</div>
                <div><strong>票价：</strong>¥{ticket.price}</div>
                <div><strong>车票号：</strong>{ticket.ticketNumber}</div>
                <div><strong>订单号：</strong>{ticket.orderNumber}</div>
                <div><strong>订单状态：</strong>{ticket.orderStatusText}</div>
                <div><strong>创建时间：</strong>{ticket.createdTime}</div>
                {ticket.paymentTime && (
                    <div><strong>支付时间：</strong>{ticket.paymentTime}</div>
                )}
            </div>

            <Divider />

            <div className="warm-tips">
                <h3>温馨提示</h3>
                <ul>
                    {tips.map((tip, index) => (
                        <li key={index}>{tip}</li>
                    ))}
                </ul>
            </div>
            
            {/* 电子票证按钮，只对未使用的车票显示 */}
            {ticket.ticketStatus === 1 && (
                <div style={{ textAlign: 'center', margin: '20px 0' }}>
                    <Button type="primary" onClick={showQRCode} loading={digitalLoading}>
                        查看电子票证
                    </Button>
                </div>
            )}
            
            {/* 电子票证弹窗 */}
            <Modal
                title="电子票证"
                open={qrVisible}
                onCancel={() => setQrVisible(false)}
                footer={null}
                width={500}
            >
                <div style={{ textAlign: 'center', padding: '20px' }}>
                    {digitalLoading ? (
                        <Spin tip="正在加载票证数据..."/>
                    ) : digitalTicket ? (
                        <div>
                            <div className="qrcode-container" style={{ marginBottom: '20px' }}>
                                {digitalTicket.qrCodeData ? (
                                    <div className="qrcode-wrapper" style={{ 
                                        border: '1px solid #ddd', 
                                        padding: '20px', 
                                        display: 'inline-block',
                                        backgroundColor: '#fff' 
                                    }}>
                                        <QRCodeCanvas
                                            value={digitalTicket.qrCodeData}
                                            size={280}
                                            level="M"
                                        />
                                    </div>
                                ) : (
                                    <Alert
                                        message="无法生成二维码"
                                        description="未收到有效的二维码数据，请稍后再试"
                                        type="error"
                                        showIcon
                                    />
                                )}
                            </div>
                            <div className="ticket-info" style={{ marginTop: '20px', textAlign: 'left' }}>
                                <div><strong>车次：</strong>{digitalTicket.trainNumber}</div>
                                <div><strong>乘车日期：</strong>{digitalTicket.travelDate}</div>
                                <div><strong>乘车人：</strong>{digitalTicket.passengerName}</div>
                                <div><strong>票证编号：</strong>{digitalTicket.ticketNumber}</div>
                            </div>
                        </div>
                    ) : (
                        <div>
                            <Alert 
                                message="无法加载数字票证数据" 
                                type="error" 
                                style={{marginTop: '20px'}} 
                            />
                            <Button 
                                onClick={fetchDigitalTicket} 
                                style={{marginTop: '10px'}}
                            >
                                重试
                            </Button>
                        </div>
                    )}
                </div>
            </Modal>
        </Card>
    );
};

export default TicketDetailPage;