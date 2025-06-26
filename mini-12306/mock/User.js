const Mock = require("mockjs");//引入mockjs模块
let Random = Mock.Random;

module.exports = function() {
    var data = {//定义等下要返回的json数据
        news: []
    };

    for (var i = 0; i < 3; i++) {
        data.news.push({
            account_id: i,//固有id
            u_name: Random.cname(2,4),//姓名
            u_id: Random.id(),//身份证号
            u_phone: Random.string('number', 11),//电话
            u_account: Random.string(2,10),//账号
            u_password: Random.string(6,12),//密码
        })
    }
    return data; //返回json数据
}