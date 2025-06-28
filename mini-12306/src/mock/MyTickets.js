const Mock = require("mockjs");//引入mockjs模块
let Random = Mock.Random;

module.exports = function generateMyTicketsData() {
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

        data.news.push({
            t_id: i,//固有id
            train_id: `${train_info[0]}-${Random.integer(100,999)}`,//车次
            t_from_city: Random.city(),//起始城市
            t_from_station: Random.pick("东","西","南","北",""),//起始车站
            t_to_city: Random.city(),//终点城市
            t_to_station: Random.pick("东","西","南","北",""),//终点车站
            t_time: Random.datetime("yyyy-MM-dd HH:mm:ss"),//发车时间
            t_seat: train_info[Random.integer(1,3)],//席别
            p_name: Random.cname(),//乘车人姓名
            p_id: Random.id(),//乘车人身份证号
            p_phone: Random.string('number', 11),//乘车人电话
            t_price: Random.integer(100,1000),//票价
            t_status: Random.integer(1,2),//1表示未完成，2表示已完成
        })
    }
    return data; //返回json数据
}