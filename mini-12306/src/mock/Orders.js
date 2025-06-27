const Mock = require("mockjs");//引入mockjs模块
let Random = Mock.Random;

module.exports = function generateOrdersData() {
    var data = {//定义等下要返回的json数据
        news: []
    };

    for (var i = 0; i < 3; i++) {
        
        let train_info = [];
        train_info[0] = Random.pick(['G','K']);

        if(train_info[0] === 'G') {
            train_info[1] = 1; train_info[2] = 2; train_info[3] = 3;//高铁有头等、商务、二等
        } else {
            train_info[1] = 1; train_info[2] = 3; train_info[3] = 4;//快速有头等、二等、无座
        }
        //1为头等，2为商务，3为二等，4为无座

        // 随机生成乘车人数
        const p_number = Random.integer(1, 3);
        
        // 创建包含 p_number 个元素的数组
        const passengerNames = [];
        const passengerIds = [];
        
        for (let j = 0; j < p_number; j++) {
            passengerNames.push(Random.cname()); // 生成中文姓名
            passengerIds.push(Random.id());      // 生成身份证号
        }

        data.news.push({
            order_id: i,//固有id
            train_id: `${train_info[0]}-${Random.integer(100,999)}`,//车次 
            t_from_city: Random.city(),//起始城市
            t_from_station: Random.pick("东","西","南","北",""),//起始车站
            t_to_city: Random.city(),//终点城市
            t_to_station: Random.pick("东","西","南","北",""),//终点车站
            t_time: Random.datetime("yyyy-MM-dd HH:mm:ss"),//发车时间
            t_seat: train_info[Random.integer(1,3)],//席别
            p_number: p_number,//乘车人数
            p_name: passengerNames,//乘车人姓名
            p_id: passengerIds,//乘车人身份证号码
            o_price: Random.integer(100,3000),//订单价格
            o_time:Random.datetime("yyyy-MM-dd HH:mm:ss"),//支付时间
            o_status:Random.integer(1,3),//1为待支付，2为已支付，3为已完成
        })
    }
    return data; //返回json数据
}