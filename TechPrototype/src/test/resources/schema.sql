-- 创建测试数据库表结构
CREATE TABLE IF NOT EXISTS trains (
    train_id INT PRIMARY KEY,
    train_number VARCHAR(20) NOT NULL,
    train_type VARCHAR(20),
    departure_time TIME,
    arrival_time TIME,
    duration_minutes INT
);

CREATE TABLE IF NOT EXISTS stations (
    station_id BIGINT PRIMARY KEY,
    station_name VARCHAR(100) NOT NULL,
    city VARCHAR(50),
    province VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS train_stops (
    stop_id BIGINT PRIMARY KEY,
    train_id INT,
    station_id INT,
    sequence_number INT,
    arrival_time TIME,
    departure_time TIME,
    FOREIGN KEY (train_id) REFERENCES trains(train_id),
    FOREIGN KEY (station_id) REFERENCES stations(station_id)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    order_time TIMESTAMP,
    order_status TINYINT,
    payment_method VARCHAR(20),
    payment_time TIMESTAMP,
    ticket_count INT,
    total_amount DECIMAL(10,2)
);

CREATE TABLE IF NOT EXISTS tickets (
    ticket_id BIGINT PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL,
    order_id BIGINT,
    passenger_id BIGINT,
    train_id INT,
    departure_stop_id BIGINT,
    arrival_stop_id BIGINT,
    travel_date DATE,
    carriage_type_id INT,
    carriage_number VARCHAR(10),
    seat_number VARCHAR(10),
    price DECIMAL(10,2),
    ticket_status TINYINT,
    ticket_type TINYINT,
    created_time TIMESTAMP,
    digital_signature VARCHAR(255),
    running_days INT,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE IF NOT EXISTS ticket_inventory (
    inventory_id BIGINT PRIMARY KEY,
    train_id INT,
    departure_stop_id BIGINT,
    arrival_stop_id BIGINT,
    travel_date DATE,
    carriage_type_id INT,
    total_seats INT,
    available_seats INT,
    price DECIMAL(10,2),
    cache_version BIGINT,
    db_version INT,
    last_updated TIMESTAMP
);

CREATE TABLE IF NOT EXISTS carriage_types (
    type_id INT PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone_number VARCHAR(20),
    password_hash VARCHAR(255),
    account_status TINYINT,
    registration_time TIMESTAMP,
    last_login_time TIMESTAMP,
    passenger_id BIGINT,
    related_passenger VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS passengers (
    passenger_id BIGINT PRIMARY KEY,
    real_name VARCHAR(50),
    id_card_number VARCHAR(20),
    phone_number VARCHAR(20),
    passenger_type TINYINT
);

CREATE TABLE IF NOT EXISTS user_passenger_relation (
    relation_id BIGINT PRIMARY KEY,
    user_id BIGINT,
    passenger_id BIGINT,
    relation_type VARCHAR(20),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (passenger_id) REFERENCES passengers(passenger_id)
);

-- 插入测试数据
INSERT INTO trains (train_id, train_number, train_type, departure_time, arrival_time, duration_minutes) VALUES
(1, 'G11', '高铁', '8:00:00', '10:00:00', 120),
(2, 'G12', '高铁', '9:00:00', '11:00:00', 120),
(3, 'G13', '高铁', '10:00:00', '12:00:00', 120),
(100, 'G1', '高铁', '8:00:00', '14:00:00', 360);

INSERT INTO stations (station_id, station_name, city, province) VALUES
(1, '北京站', '北京', '北京市'),
(2, '上海站', '上海', '上海市'),
(497, '北京站', '北京', '北京市'),
(500, '上海站', '上海', '上海市');

INSERT INTO train_stops (stop_id, train_id, station_id, sequence_number, arrival_time, departure_time) VALUES
(10, 1, 1, 1, '8:00:00', NULL),
(20, 1, 2, 2, '9:00:00', '9:05:00'),
(40, 2, 1, 1, '9:00:00', NULL),
(50, 2, 2, 2, '10:00:00', '10:05:00'),
(60, 3, 1, 1, '10:00:00', NULL),
(497, 100, 497, 1, '8:00:00', '8:00:00'),
(500, 100, 500, 2, '14:00:00', '14:00:00');

INSERT INTO orders (order_id, order_number, user_id, order_time, order_status, ticket_count, total_amount) VALUES
(1, 'ORD123456', 1, '2025-01-01 10:00:00', 0, 1, 1000),
(2, 'ORD123457', 1, '2025-01-01 11:00:00', 1, 2, 2000);

INSERT INTO tickets (ticket_id, ticket_number, order_id, passenger_id, train_id, departure_stop_id, arrival_stop_id, travel_date, carriage_type_id, carriage_number, seat_number, price, ticket_status, ticket_type, created_time) VALUES
(1, 'T123456', 1, 10, 1, 10, 20, '2025-01-01', 1, '01', '01A', 1000, 0, 1, '2025-01-01 10:00:00'),
(2, 'T123457', 2, 11, 1, 10, 20, '2025-01-01', 1, '02', '02B', 100.0, 1, 1, '2025-01-01 11:00:00');

INSERT INTO ticket_inventory (inventory_id, train_id, departure_stop_id, arrival_stop_id, travel_date, carriage_type_id, total_seats, available_seats, price, cache_version, db_version) VALUES
(1, 1, 10, 20, '2025-01-01', 1, 100, 50, 1000, 1, 1),
(2, 100, 497, 500, '2025-07-20', 5, 100, 50, 100.0, 1, 1);

INSERT INTO carriage_types (type_id, type_name, description) VALUES
(1, '一等座', '一等座车厢'),
(2, '二等座', '二等座车厢'),
(5, '二等座', '二等座车厢');

INSERT INTO users (user_id, real_name, email, phone_number, passenger_id) VALUES
(1, '张三', 'zhangsan@example.com', '13800138000', 1);

INSERT INTO passengers (passenger_id, real_name, id_card_number, phone_number, passenger_type) VALUES
(1, '张三', '110101199001011234', '13800138000', 1),
(10, '李四', '110101199001011235', '13800138001', 1),
(11, '王五', '110101199001011236', '13800138002', 1);

INSERT INTO user_passenger_relation (relation_id, user_id, passenger_id, relation_type) VALUES
(1, 1, 1, '本人'),
(2, 1, 10, '亲属'),
(3, 1, 11, '亲属'); 

