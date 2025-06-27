const Mock = require("mockjs");//引入mockjs模块
let Random = Mock.Random;

module.exports = function() {
    var data = {//定义等下要返回的json数据
        news: []
    };

    //定义一个有账号的人
    data.news.push({  
            u_name: Random.cname(2,4),//姓名
            u_id: Random.id(),//身份证号
            have_account: true,//拥有账号
            account_id: 0,//账号数固有id
            u_phone: Random.string('number', 11),//电话
            u_account: Random.string(2,10),//账号
            u_password: Random.string(6,12),//密码
        })

    for (var i = 1; i < 3; i++) {
        let have_account = Random.boolean();//是否拥有账号

        if(have_account){
            data.news.push({  
            u_name: Random.cname(2,4),//姓名
            u_id: Random.id(),//身份证号
            have_account: have_account,//有账号
            account_id: i,//账号数固有id
            u_phone: Random.string('number', 11),//电话
            u_account: Random.string(2,10),//账号
            u_password: Random.string(6,12),//密码
            })
        }
        else{
            data.news.push({  
            u_name: Random.cname(2,4),//姓名
            u_id: Random.id(),//身份证号
            have_account: have_account,//没有账号
            })
        }
        
    }
    return data; //返回json数据
}