const Mock = require("mockjs");//引入mockjs模块
let Random = Mock.Random;

module.exports = function generatePersonData() {
    var data = {
        news: []
    };

    for (var i = 0; i < 3; i++) {
        let have_account = Random.boolean();

        let related_passenger_id = [];
        let related_passenger_name = [];

        if (have_account) {
            // 关联乘车人数量随机 0~3
            let relatedCount = Random.integer(0, 3);
            for (let j = 0; j < relatedCount; j++) {
                related_passenger_id[j] = Random.id();
                related_passenger_name[j] = Random.cname();
            }
        }

        if (have_account) {
            data.news.push({
                u_name: Random.cname(),
                u_id: Random.id(),
                have_account: have_account,
                account_id: i,
                u_phone: Random.string('number', 11),
                u_account: Random.string(2, 10),
                u_password: Random.string(6, 12),
                related_passenger_id: related_passenger_id,
                related_passenger_name: related_passenger_name,
            });
        } else {
            data.news.push({
                u_name: Random.cname(),
                u_id: Random.id(),
                have_account: have_account,
            });
        }
    }
    return data;
}