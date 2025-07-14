const Mock = require("mockjs");
let Random = Mock.Random;

// 辅助函数：在给定时间字符串基础上加小时数，返回格式化字符串
function addHoursToDatetime(datetimeStr, hours) {
    const date = new Date(datetimeStr);
    date.setHours(date.getHours() + hours);
    return date.toISOString().slice(0, 19).replace('T', ' ');
}

module.exports = function generateOrdersData() {
    var data = { news: [] };

    for (var i = 0; i < 3; i++) {
        let train_info = [];
        train_info[0] = Random.pick(['G', 'K']);

        if (train_info[0] === 'G') {
            train_info[1] = 1; train_info[2] = 2; train_info[3] = 3; // 高铁有头等、商务、二等
        } else {
            train_info[1] = 1; train_info[2] = 3; train_info[3] = 4; // 快速有头等、二等、无座
        }

        const p_number = Random.integer(1, 3);
        const passengers = [];

        for (let j = 0; j < p_number; j++) {
            passengers.push({
                name: Random.cname(),
                id: Random.id(),
                phone: '1' + Random.string('number', 10),
                seat: train_info[Random.integer(1, 3)],
                price: Random.integer(100, 1000),
                ticket_type: Random.pick(['成人票', '学生票', '儿童票', '残军票']),
            });
        }

        const departTime = Random.datetime("yyyy-MM-dd HH:mm:ss");
        const arriveTime = addHoursToDatetime(departTime, Random.integer(1, 5));

        data.news.push({
            order_id: `20250627${String(i).padStart(4, '0')}`,
            train_id: `${train_info[0]}-${Random.integer(100, 999)}`,
            t_from_city: Random.city(),
            t_from_station: Random.pick(["东", "西", "南", "北", ""]),
            t_to_city: Random.city(),
            t_to_station: Random.pick(["东", "西", "南", "北", ""]),
            t_time: departTime,
            arrive_time: arriveTime,
            passengers: passengers,
            o_time: Random.datetime("yyyy-MM-dd HH:mm:ss"),
            o_status: Random.integer(1, 3),
        });
    }

    return data;
};