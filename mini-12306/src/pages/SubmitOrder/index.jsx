import React, { useState, useEffect } from 'react';
import AddPassenger from '../AddPassenger';
import './style.css';

import generatePersonData from '../../mock/Person';
import generateTrainData from '../../mock/Train';

const ticketTypes = [
    "成人票",
    "学生票",
    "儿童票",
    "残军票"
];

const idTypes = [
    "居民身份证"
];

const seatTypeMap = {
    1: "头等",
    2: "商务座",
    3: "二等座",
    4: "无座"
};

const SubmitOrder = () => {
    const [searchText, setSearchText] = useState('');
    const [selectedPassengers, setSelectedPassengers] = useState([]);
    const [passengerDetails, setPassengerDetails] = useState({});
    const [showAddPassengerModal, setShowAddPassengerModal] = useState(false);
    const [trainInfo, setTrainInfo] = useState(null);
    const [passengersData, setPassengersData] = useState([]);
    const [passengerIdMap, setPassengerIdMap] = useState({});

    useEffect(() => {
        const personData = generatePersonData();
        const trainData = generateTrainData();

        if (personData.news && personData.news.length > 0) {
            const user = personData.news[0];
            const mainUserName = user.u_name;
            const relatedNames = user.related_passenger_name || [];
            const relatedIds = user.related_passenger_id || [];

            const allPassengers = [mainUserName, ...relatedNames];
            setPassengersData(allPassengers);

            // 姓名到身份证号映射
            const idMap = {};
            idMap[mainUserName] = user.u_id;
            relatedNames.forEach((name, idx) => {
                idMap[name] = relatedIds[idx] || '';
            });
            setPassengerIdMap(idMap);
        }

        if (trainData.news && trainData.news.length > 0) {
            setTrainInfo(trainData.news[0]);
        }
    }, []);

    const filteredPassengers = passengersData.filter(name =>
        name.includes(searchText)
    );

    // 只保留有票的席别，格式 "席别（¥价格元）"
    const seatOptions = trainInfo?.seat?.map((code, idx) => {
        if (trainInfo.seat_number[idx] > 0) {
            const seatName = seatTypeMap[code] || '未知席别';
            const price = trainInfo.seat_price[idx];
            return `${seatName}（¥${price}元）`;
        }
        return null;
    }).filter(Boolean) || [];

    const togglePassenger = (name) => {
        setSelectedPassengers(prev => {
            if (prev.includes(name)) {
                const newArr = prev.filter(n => n !== name);
                setPassengerDetails(details => {
                    const newDetails = { ...details };
                    delete newDetails[name];
                    return newDetails;
                });
                return newArr;
            } else {
                setPassengerDetails(details => ({
                    ...details,
                    [name]: {
                        ticketType: ticketTypes[0],
                        seatType: seatOptions[0] || '',
                        idType: idTypes[0],
                        idNumber: passengerIdMap[name] || ''
                    }
                }));
                return [...prev, name];
            }
        });
    };

    const removePassenger = (name) => {
        setSelectedPassengers(prev => prev.filter(n => n !== name));
        setPassengerDetails(details => {
            const newDetails = { ...details };
            delete newDetails[name];
            return newDetails;
        });
    };

    const updateDetail = (name, field, value) => {
        setPassengerDetails(details => ({
            ...details,
            [name]: {
                ...details[name],
                [field]: value
            }
        }));
    };

    return (
        <>
            <div className="train-info-wrapper">
                <div className="train-info-header">
                    列车信息（以下余票信息仅供参考）
                </div>
                <div className="train-info-content">
                    {trainInfo && trainInfo.t_start_time && trainInfo.t_end_time ? (
                        <>
                            <div className="train-details">
                                {formatDate(trainInfo.t_start_time)} {trainInfo.train_id}次 {trainInfo.t_from}（{formatTime(trainInfo.t_start_time)}开）—{trainInfo.t_to}（{formatTime(trainInfo.t_end_time)}到）
                            </div>
                            <hr />
                            <div className="ticket-info">
                                {trainInfo.seat.map((seatTypeCode, idx) => {
                                    const hasTicket = trainInfo.seat_number[idx] > 0;
                                    return (
                                        <span
                                            key={seatTypeCode}
                                            className={`ticket-item ${hasTicket ? 'ticket-available' : 'ticket-unavailable'}`}
                                            style={{ marginRight: '20px' }}
                                            aria-label={`${seatTypeMap[seatTypeCode]} ${hasTicket ? '有票' : '无票'}`}
                                        >
                                            <span className="seat-type">
                                                {seatTypeMap[seatTypeCode]}
                                            </span>
                                            （
                                            <span className={`price ${hasTicket ? 'price-available' : 'price-unavailable'}`}>
                                                ¥{trainInfo.seat_price[idx]}元
                                            </span>
                                            ） {hasTicket ? '有票' : '无票'}
                                        </span>
                                    );
                                })}
                            </div>
                            <div className="price-note">
                                *显示的价格均为实际活动折扣后票价，供您参考，查看公布票价 。具体票价以您确认支付时实际购买的铺别票价为准。
                            </div>
                        </>
                    ) : (
                        <div>列车信息加载中...</div>
                    )}
                </div>
            </div>

            <div className="passenger-info-wrapper">
                <div className="passenger-info-header">
                    <span>乘客信息（填写说明）</span>
                    <input
                        type="search"
                        placeholder="搜索乘车人"
                        value={searchText}
                        onChange={e => setSearchText(e.target.value)}
                        className="passenger-search"
                        aria-label="搜索乘车人"
                    />
                </div>
                <div className="passenger-info-content">
                    <div className="passenger-label">
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            width="20"
                            height="20"
                            fill="currentColor"
                            viewBox="0 0 16 16"
                            aria-hidden="true"
                        >
                            <path d="M3 14s-1 0-1-1 1-4 6-4 6 3 6 4-1 1-1 1H3zm5-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" />
                        </svg>
                        <span>乘车人</span>
                    </div>
                    <div className="passenger-list">
                        {filteredPassengers.map(name => (
                            <label key={name} className="passenger-item">
                                <input
                                    type="checkbox"
                                    checked={selectedPassengers.includes(name)}
                                    onChange={() => togglePassenger(name)}
                                />
                                <span>{name}</span>
                            </label>
                        ))}
                        <button
                            className="add-passenger-button"
                            type="button"
                            aria-label="添加乘车人"
                            onClick={() => setShowAddPassengerModal(true)}
                        >
                            <span className="plus-sign">+</span> 添加乘车人
                        </button>
                    </div>

                    <hr />

                    {selectedPassengers.length > 0 && (
                        <div className="passenger-details-table-wrapper">
                            <table className="passenger-details-table">
                                <thead>
                                <tr>
                                    <th>序号</th>
                                    <th>票种</th>
                                    <th>席别</th>
                                    <th>姓名</th>
                                    <th>证件类型</th>
                                    <th>证件号码</th>
                                    <th>操作</th>
                                </tr>
                                </thead>
                                <tbody>
                                {selectedPassengers.map((name, index) => (
                                    <tr key={name}>
                                        <td>{index + 1}</td>
                                        <td>
                                            <select
                                                value={passengerDetails[name]?.ticketType || ticketTypes[0]}
                                                onChange={e => updateDetail(name, 'ticketType', e.target.value)}
                                            >
                                                {ticketTypes.map(type => (
                                                    <option key={type} value={type}>{type}</option>
                                                ))}
                                            </select>
                                        </td>
                                        <td>
                                            <select
                                                value={passengerDetails[name]?.seatType || seatOptions[0] || ''}
                                                onChange={e => updateDetail(name, 'seatType', e.target.value)}
                                            >
                                                {seatOptions.map(seat => (
                                                    <option key={seat} value={seat}>{seat}</option>
                                                ))}
                                            </select>
                                        </td>
                                        <td>{name}</td>
                                        <td>
                                            <select
                                                value={passengerDetails[name]?.idType || idTypes[0]}
                                                onChange={e => updateDetail(name, 'idType', e.target.value)}
                                            >
                                                {idTypes.map(id => (
                                                    <option key={id} value={id}>{id}</option>
                                                ))}
                                            </select>
                                        </td>
                                        <td>
                                            <input
                                                type="text"
                                                value={passengerDetails[name]?.idNumber || ''}
                                                onChange={e => updateDetail(name, 'idNumber', e.target.value)}
                                                placeholder="请输入证件号码"
                                            />
                                        </td>
                                        <td>
                                            <button
                                                type="button"
                                                className="delete-row-button"
                                                aria-label={`删除乘车人 ${name}`}
                                                onClick={() => removePassenger(name)}
                                            >
                                                ×
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    <div className="button-row">
                        <button className="btn btn-white" type="button">上一步</button>
                        <button className="btn btn-blue" type="button">提交订单</button>
                    </div>

                    <div className="tip-wrapper">
                        <div className="tip-content">
                            <p>温馨提示：</p>
                            <p>请仔细核对乘客信息，确保准确无误，以免影响出行。</p>
                        </div>
                    </div>
                </div>
            </div>

            {showAddPassengerModal && (
                <div className="add-passenger-page">
                    <div className="modal-wrapper" role="dialog" aria-modal="true" aria-labelledby="modal-title" style={{ position: 'relative' }}>
                        <AddPassenger onClose={() => setShowAddPassengerModal(false)} />
                    </div>
                </div>
            )}
        </>
    );
};

export default SubmitOrder;

function formatDate(datetime) {
    return datetime?.split(' ')[0] || '';
}

function formatTime(datetime) {
    return datetime?.split(' ')[1]?.slice(0, 5) || '';
}