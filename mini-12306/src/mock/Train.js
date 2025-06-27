const Mock = require("mockjs");//引入mockjs模块
let Random = Mock.Random;

module.exports = function generateTrainData() {
    var data = {//定义等下要返回的json数据
        news: []
    };

    for (var i = 0; i < 3; i++) {

        let train_info = [];let seat_number = [];let seat_price = [];
        let train_style = Random.pick(['G','K']);

        if(train_style === 'G') {
            train_info[0] = 1; train_info[1] = 2; train_info[2] = 3;//高铁有头等、商务、二等
        } else {
            train_info[0] = 1; train_info[1] = 3; train_info[2] = 4;//快速有头等、二等、无座
        }
        //1为头等，2为商务，3为二等，4为无座

        seat_number[0] = Random.integer(100,300);
        seat_number[1] = Random.integer(100,300);
        seat_number[2] = Random.integer(100,300);
        //席别库存

        seat_price[0] = Random.integer(100,300);
        seat_price[1] = Random.integer(100,300);
        seat_price[2] = Random.integer(100,300);
        //席别价格

        // 随机生成站点数
        const station_number = Random.integer(3, 5);
        
        // 创建包含 station_number 个元素的数组
        const path = [];

        for (let j = 0; j < station_number; j++) {
            path.push(Random.city() + Random.pick("东","西","南","北","")); // 生成站点名
        }

        data.news.push({
            train_id: `${train_style}-${Random.integer(100,999)}`,//车次 
            t_station_number: station_number,//站点数
            t_path: path,//路线
            t_from: path[0],//起点站
            t_to: path[station_number-1],//终点站
            t_start_time: Random.datetime("yyyy-MM-dd HH:mm:ss"),//发车时间
            t_end_time: Random.datetime("yyyy-MM-dd HH:mm:ss"),//到达时间
            seat: train_info,//席别
            seat_number: seat_number,//不同席别的库存
            seat_price: seat_price,//不同席别的价格
        })
    }
    return data;//返回json数据
}