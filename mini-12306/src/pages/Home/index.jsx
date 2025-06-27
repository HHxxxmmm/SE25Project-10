import React from 'react';
import { Carousel, Card, DatePicker, Input } from 'antd';
import './style.css';

const { Search } = Input;

export default function HomePage() {
    const ads = [
        '/images/ads/ad01.jpg',
        '/images/ads/ad02.jpg',
        '/images/ads/ad03.jpg',
        '/images/ads/ad04.jpg',
        '/images/ads/ad05.jpg',
        '/images/ads/ad06.jpg',
    ];

    // 公告数据
    const announcements = [
        { id: 1, title: '公告1', content: '这里是公告1的内容摘要...' },
        { id: 2, title: '公告2', content: '这里是公告2的内容摘要...' },
        { id: 3, title: '公告3', content: '这里是公告3的内容摘要...' },
        { id: 4, title: '公告4', content: '这里是公告4的内容摘要...' },
        { id: 5, title: '公告5', content: '这里是公告5的内容摘要...' },
    ];

    // 功能按钮
    const actionButtons = [
        { id: 1, name: '用户须知', link: '/user-guide' },
        { id: 2, name: '购票指南', link: '/ticket-guide' },
        { id: 3, name: '联系反馈', link: '/contact' },
    ];

    return (
        <div className="home-page">
            {/* 轮播图部分 */}
            <div className="ad-carousel-container">
                {/* 查询窗口 */}
                <div className="search-overlay">
                    <div className="search-panel">
                        <h2 className="search-title">车票查询</h2>
                        <div className="search-form">
                            <div className="search-row">
                                <span className="search-label">出发地点</span>
                                <Input
                                    placeholder="请输入车站或市(县)名"
                                    className="search-input"
                                />
                            </div>
                            <div className="search-row">
                                <span className="search-label">到达地点</span>
                                <Input
                                    placeholder="请输入车站或市(县)名"
                                    className="search-input"
                                />
                            </div>
                            <div className="search-row">
                                <span className="search-label">出发日期</span>
                                <DatePicker
                                    className="search-date"
                                    style={{ width: '100%' }}
                                />
                            </div>
                            <button className="search-button">
                                查询车票
                            </button>
                        </div>
                    </div>
                </div>

                <Carousel
                    autoplay
                    effect="fade"
                    dots={{ className: "custom-dots" }}
                    autoplaySpeed={3000}
                >
                    {ads.map((ad, index) => (
                        <div key={index} className="slide-container">
                            <img
                                src={ad}
                                alt={`广告 ${index + 1}`}
                                className="ad-image"
                                onError={(e) => {
                                    e.target.src = '/images/placeholder.jpg';
                                    console.error(`图片加载失败: ${ad}`);
                                }}
                            />
                        </div>
                    ))}
                </Carousel>
            </div>

            {/* 公告栏部分 */}
            <div className="announcement-container">
                <Card className="announcement-card">
                    <div className="announcement-content">
                        {/* 左侧公告列表 */}
                        <div className="announcement-list">
                            <h2 className="announcement-title">最新公告</h2>
                            <ul>
                                {announcements.map(announcement => (
                                    <li key={announcement.id} className="announcement-item">
                                        <h3>{announcement.title}</h3>
                                        <p>{announcement.content}</p>
                                    </li>
                                ))}
                            </ul>
                        </div>

                        {/* 右侧功能按钮 */}
                        <div className="action-buttons">
                            {actionButtons.map(button => (
                                <button
                                    key={button.id}
                                    className="action-button"
                                    onClick={() => console.log(`跳转到: ${button.link}`)}
                                >
                                    {button.name}
                                </button>
                            ))}
                        </div>
                    </div>
                </Card>
            </div>
        </div>
    );
}