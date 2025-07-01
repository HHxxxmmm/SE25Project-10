// src/mock/TicketsforOne.js
const Mock = require("mockjs");
let Random = Mock.Random;

module.exports = function generateTicketsforOneData() {
    var data = { news: [] };

    // 生成一个固定的乘客信息
    const passenger = {
        p_name: Random.cname(),
        p_id: Random.id(),
        p_phone: Random.string('number', 11),
    };

    const seatTypeMap = {
        1: '头等',
        2: '商务',
        3: '二等',
        4: '无座',
    };

    for (var i = 0; i < 100; i++) { // 生成100张车票
        let train_info = [];
        train_info[0] = Random.pick(['G','K']);

        if (train_info[0] === 'G') {
            train_info[1] = 1; train_info[2] = 2; train_info[3] = 3; // 高铁：头等、商务、二等
        } else {
            train_info[1] = 1; train_info[2] = 3; train_info[3] = 4; // 快速：头等、二等、无座
        }

        const ticketTypes = ['成人票', '学生票', '儿童票', '军人票'];
        const ticketType = Random.pick(ticketTypes);

        const carriageNumber = Random.integer(1, 20);
        const seatNumber = Random.integer(1, 50);
        const carriageSeat = `${carriageNumber}车${seatNumber}座`;

        const departureTime = Random.datetime("yyyy-MM-dd HH:mm:ss");
        const travelDuration = Random.integer(1, 5);
        const arrivalTime = new Date(departureTime);
        arrivalTime.setHours(arrivalTime.getHours() + travelDuration);

        data.news.push({
            t_id: i,
            train_id: `${train_info[0]}-${Random.integer(100, 999)}`,
            t_from_city: Random.city(),
            t_from_station: Random.pick("东","西","南","北",""),
            t_to_city: Random.city(),
            t_to_station: Random.pick("东","西","南","北",""),
            t_time: departureTime,
            t_arrival_time: arrivalTime.toISOString().slice(0, 19).replace('T', ' '),
            t_seat: seatTypeMap[train_info[Random.integer(1, 3)]],
            p_name: passenger.p_name,    // 使用固定乘客名
            p_id: passenger.p_id,        // 使用固定身份证
            p_phone: passenger.p_phone,  // 使用固定电话
            t_price: Random.integer(100, 1000),
            t_status: Random.integer(1, 2),
            ticket_type: ticketType,
            carriage_seat: carriageSeat,
        });
    }
    return data;
}