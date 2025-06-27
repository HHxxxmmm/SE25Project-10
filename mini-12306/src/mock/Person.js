const Mock = require("mockjs");//引入mockjs模块
let Random = Mock.Random;

module.exports = function generatePersonData() {
    var data = {//定义等下要返回的json数据
        news: []
    };

    for (var i = 0; i < 3; i++) {
        let have_account = Random.boolean();//是否拥有账号

        let related_passenger_id = [];let related_passenger_name = [];

        //关联乘车人信息
        related_passenger_id[0] = Random.id();
        related_passenger_id[1] = Random.id();
        related_passenger_id[2] = Random.id();
        related_passenger_name[0] = Random.cname();
        related_passenger_name[1] = Random.cname();
        related_passenger_name[2] = Random.cname();

        if(have_account){
            data.news.push({  
            u_name: Random.cname(),//姓名
            u_id: Random.id(),//身份证号
            have_account: have_account,//有账号
            account_id: i,//账号数固有id
            u_phone: Random.string('number', 11),//电话
            u_account: Random.string(2,10),//账号
            u_password: Random.string(6,12),//密码
            related_passenger_id: related_passenger_id,//关联乘车人身份证号
            related_passenger_name: related_passenger_name,//关联乘车人姓名
            })
        }
        else{
            data.news.push({  
            u_name: Random.cname(),//姓名
            u_id: Random.id(),//身份证号
            have_account: have_account,//没有账号
            })
        }
        
    }
    return data; //返回json数据
}