导出数据时忘了关redis同步
因此在导入完成后需要运行
UPDATE mini12306.ticket_inventory
SET available_seats = total_seats;



测试修改了数据库后想要数据库恢复的话
SET FOREIGN_KEY_CHECKS =0;
DELETE FROM tickets;
SET FOREIGN_KEY_CHECKS =1;
ALTER TABLE tickets AUTO_INCREMENT=1;

SET FOREIGN_KEY_CHECKS =0;
DELETE FROM orders;
SET FOREIGN_KEY_CHECKS =1;
ALTER TABLE orders AUTO_INCREMENT=1;

UPDATE mini12306.seats SET date_1 = 0;
UPDATE mini12306.seats SET date_2 =0;
UPDATE mini12306.seats SET date_3 = 0;
UPDATE mini12306.seats SET date_4 = 0;
UPDATE mini12306.seats SET date_5 = 0;
UPDATE mini12306.seats SET date_6 = 0;
UPDATE mini12306.seats SET date_7 = 0;
UPDATE mini12306.seats SET date_8 = 0;
UPDATE mini12306.seats SET date_9 = 0;
UPDATE mini12306.seats SET date_10 = 0;




UPDATE mini12306.ticket_inventory SET cache_version = 1;
UPDATE mini12306.ticket_inventory SET db_version = 1;


UPDATE mini12306.ticket_inventory
SET available_seats = total_seats;


运行这些即可



注意数据导入命令为

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.4/Uploads/passengers.csv'
INTO TABLE passengers
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'

注意路径替换
注意记事本无法显示的下划线（表名和csv文件名内的空）

如果一开始就报错，尝试SET NAMES  utf8mb64;若之后有需求改回客户端默认的gbk以防止powershell自带的gbk编码造成的冲突，再改回SET NAMES gbk；即可

注意注意注意！

数据导入的顺序为
passengers    ->     users    ->   user_passenger_relations
carriage_types
stations    ->   trains    ->   train_stops    ->   train_carriages  ->    seats   ->     ticket_inventory
