登录命令行客户端mysql -u [用户名] -p     然后输入密码登录，
用USE [数据库名]切换到目标数据库（需要已建成，未建成的用CREATE DATABASE [数据库名]即可建成）

对于建库脚本(schema.sql)，在命令行客户端中输入命令：
SOURCE [建库脚本绝对地址，不要带引号，改为正斜杠/]

对于数据导入的脚本，在命令行客户端输入命令（注意下划线，记事本将下划线显示为空，但表名和文件名中没有空，若有则为下划线）
LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.4/Uploads/ticket_inventory_data_utf8mb4.csv'
INTO TABLE ticket_inventory
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n';

形如这样的命令，输入时替换掉文件名和表名即可

如果一开始就报错，尝试SET NAMES  utf8mb64;若之后有需求改回客户端默认的gbk以防止powershell自带的gbk编码造成的冲突，再改回SET NAMES gbk；即可

注意注意注意！

数据导入的顺序为
passengers    ->     users    ->   user_passenger_relations
carriage_types
stations    ->   trains    ->   train_stops    ->   train_carriages  ->    seats   ->     ticket_inventory