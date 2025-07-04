create table carriage_types
(
    type_id     int auto_increment
        primary key,
    type_name   varchar(20) not null comment '商务座, 一等座, 二等座, 硬座, 硬卧, 软卧等',
    seat_layout json        null comment '座位布局配置',
    seat_count  int         not null
);

create table passengers
(
    passenger_id   bigint auto_increment
        primary key,
    id_card_number varchar(20) not null,
    real_name      varchar(50) not null,
    passenger_type tinyint     not null comment '1-成人, 2-儿童, 3-学生, 4-残疾军人',
    phone_number   varchar(20) null,
    constraint id_card_number
        unique (id_card_number)
);

create index idx_id_card
    on passengers (id_card_number);

create table stations
(
    station_id   int auto_increment,
    station_name varchar(50) not null,
    city         varchar(50) not null,
    primary key (city, station_id),
    constraint uk_station_id
        unique (station_id),
    constraint uk_station_name
        unique (station_name)
);

create index idx_city_station_name
    on stations (city, station_name);

create table trains
(
    train_id         int auto_increment
        primary key,
    train_number     varchar(20) not null,
    train_type       varchar(10) not null comment 'G-高铁, D-动车, K-快速, T-特快, Z-直达, C-城际',
    start_station_id int         not null,
    end_station_id   int         not null,
    departure_time   time        not null,
    arrival_time     time        not null,
    duration_minutes int         not null,
    constraint train_number
        unique (train_number),
    constraint trains_ibfk_1
        foreign key (start_station_id) references stations (station_id),
    constraint trains_ibfk_2
        foreign key (end_station_id) references stations (station_id)
);

create table train_carriages
(
    carriage_id     bigint auto_increment,
    train_id        int         not null,
    carriage_number varchar(10) not null comment '车厢号',
    type_id         int         not null,
    primary key (train_id, carriage_id),
    constraint uk_carriage_id
        unique (carriage_id),
    constraint uk_train_carriage_number
        unique (train_id, carriage_number),
    constraint fk_carriage_train
        foreign key (train_id) references trains (train_id),
    constraint fk_carriage_type
        foreign key (type_id) references carriage_types (type_id)
)
    KEY_BLOCK_SIZE = 8
    row_format = COMPRESSED;

create table seats
(
    seat_id     bigint auto_increment,
    carriage_id bigint            not null,
    seat_number varchar(10)       not null comment '座位号如1A, 2B等',
    seat_type   varchar(10)       null comment '靠窗, 靠过道, 中间等',
    isAvailable tinyint default 1 null comment '座位是否可用',
    primary key (carriage_id, seat_id),
    constraint uk_carriage_seat_number
        unique (carriage_id, seat_number),
    constraint uk_seat_id
        unique (seat_id),
    constraint fk_seats_carriage
        foreign key (carriage_id) references train_carriages (carriage_id)
);

create index idx_available_seat
    on seats (isAvailable, carriage_id, seat_type);

create index idx_carriage_type
    on train_carriages (type_id);

create table train_stops
(
    stop_id             bigint auto_increment
        primary key,
    train_id            int  not null,
    station_id          int  not null,
    sequence_number     int  not null,
    arrival_time        time null,
    departure_time      time null,
    stop_minutes        int  null,
    distance_from_start int  null comment '距离始发站的距离(公里)',
    constraint train_id
        unique (train_id, station_id),
    constraint train_id_2
        unique (train_id, sequence_number),
    constraint train_stops_ibfk_1
        foreign key (train_id) references trains (train_id),
    constraint train_stops_ibfk_2
        foreign key (station_id) references stations (station_id)
);

create table ticket_inventory
(
    inventory_id      bigint auto_increment,
    train_id          int                                       not null,
    departure_stop_id bigint                                    not null comment '关联train_stops.stop_id',
    arrival_stop_id   bigint                                    not null comment '关联train_stops.stop_id',
    travel_date       date                                      not null,
    carriage_type_id  int                                       not null,
    total_seats       int                                       not null,
    available_seats   int                                       not null comment '可售余票（需配合cache_version使用）',
    cache_version     bigint       default 0                    not null comment '缓存数据版本号',
    db_version        int          default 0                    not null comment '数据库乐观锁版本',
    price             decimal(10, 2)                            not null,
    last_updated      timestamp(3) default CURRENT_TIMESTAMP(3) not null,
    primary key (train_id, departure_stop_id, arrival_stop_id, travel_date, carriage_type_id),
    constraint uk_inventory_id
        unique (inventory_id),
    constraint fk_inventory_arr_stop
        foreign key (arrival_stop_id) references train_stops (stop_id),
    constraint fk_inventory_carriage_type
        foreign key (carriage_type_id) references carriage_types (type_id),
    constraint fk_inventory_dep_stop
        foreign key (departure_stop_id) references train_stops (stop_id),
    constraint fk_inventory_train
        foreign key (train_id) references trains (train_id)
)
    comment '票务库存表（按车次-区间物理聚集）' KEY_BLOCK_SIZE = 8
                                              row_format = COMPRESSED;

create index idx_route_train_date_type
    on ticket_inventory (departure_stop_id, arrival_stop_id, train_id, travel_date, carriage_type_id);

create index idx_travel_date
    on ticket_inventory (travel_date);

create index idx_station
    on train_stops (station_id);

create index end_station_id
    on trains (end_station_id);

create index idx_train_number
    on trains (train_number);

create index start_station_id
    on trains (start_station_id);

create table users
(
    user_id           bigint auto_increment
        primary key,
    real_name         varchar(50)                        not null,
    password_hash     varchar(255)                       not null,
    email             varchar(100)                       null,
    phone_number      varchar(20)                        not null,
    id_card_number    varchar(18)                        not null,
    registration_time datetime default CURRENT_TIMESTAMP not null,
    last_login_time   datetime                           null,
    account_status    tinyint  default 1                 not null comment '1-正常, 0-冻结',
    constraint email
        unique (email),
    constraint id_card_number
        unique (id_card_number),
    constraint phone_number
        unique (phone_number)
);

create table orders
(
    order_id       bigint auto_increment
        primary key,
    order_number   varchar(32)                        not null,
    user_id        bigint                             not null,
    order_time     datetime default CURRENT_TIMESTAMP not null,
    total_amount   decimal(10, 2)                     not null,
    payment_time   datetime                           null,
    payment_method varchar(20)                        null,
    order_status   tinyint  default 0                 not null comment '0-待支付, 1-已支付, 2-已完成, 3-已取消',
    constraint order_number
        unique (order_number),
    constraint orders_ibfk_1
        foreign key (user_id) references users (user_id)
);

create index idx_order_time
    on orders (order_time);

create index idx_user
    on orders (user_id);

create table tickets
(
    ticket_id         bigint auto_increment,
    ticket_number     varchar(32)                        not null,
    order_id          bigint                             not null,
    passenger_id      bigint                             not null,
    train_id          int                                not null,
    departure_stop_id bigint                             not null,
    arrival_stop_id   bigint                             not null,
    travel_date       date                               not null,
    running_days      int      default 1                 not null comment '列车运行天数',
    carriage_type_id  int                                not null,
    carriage_number   varchar(10)                        null,
    seat_number       varchar(10)                        null,
    price             decimal(10, 2)                     not null,
    ticket_status     tinyint  default 0                 not null comment '0-未使用, 1-已使用, 2-已退票, 3-已改签',
    digital_signature varchar(255)                       null comment '电子票证签名',
    created_time      datetime default CURRENT_TIMESTAMP not null,
    ticket_type       tinyint  default 1                 not null comment '1-成人, 2-儿童, 3-学生, 4-残疾, 5-军人',
    primary key (passenger_id, order_id, ticket_id),
    constraint ticket_id
        unique (ticket_id),
    constraint ticket_number
        unique (ticket_number),
    constraint fk_tickets_arrival_stop
        foreign key (arrival_stop_id) references train_stops (stop_id),
    constraint fk_tickets_departure_stop
        foreign key (departure_stop_id) references train_stops (stop_id),
    constraint tickets_ibfk_1
        foreign key (order_id) references orders (order_id),
    constraint tickets_ibfk_2
        foreign key (passenger_id) references passengers (passenger_id),
    constraint tickets_ibfk_3
        foreign key (train_id) references trains (train_id),
    constraint tickets_ibfk_6
        foreign key (carriage_type_id) references carriage_types (type_id)
)
    KEY_BLOCK_SIZE = 8
    row_format = COMPRESSED;

create index carriage_type_id
    on tickets (carriage_type_id);

create index idx_arrival_stop
    on tickets (arrival_stop_id);

create index idx_departure_stop
    on tickets (departure_stop_id);

create index idx_order_passenger
    on tickets (order_id, passenger_id);

create index idx_order_status
    on tickets (order_id, ticket_status);

create index idx_passenger_order
    on tickets (passenger_id, order_id);

create index idx_status_created
    on tickets (ticket_status, created_time);

create index idx_train_date
    on tickets (train_id, travel_date);

create table user_passenger_relations
(
    relation_id   bigint auto_increment
        primary key,
    user_id       bigint                             not null,
    passenger_id  bigint                             not null,
    relation_type tinyint                            not null comment '1-本人, 2-亲属, 3-其他',
    alias         varchar(50)                        null comment '用户为乘客设置的别名',
    added_time    datetime default CURRENT_TIMESTAMP not null,
    constraint user_id
        unique (user_id, passenger_id),
    constraint user_passenger_relations_ibfk_1
        foreign key (user_id) references users (user_id),
    constraint user_passenger_relations_ibfk_2
        foreign key (passenger_id) references passengers (passenger_id)
);

create index passenger_id
    on user_passenger_relations (passenger_id);

create index idx_id_card
    on users (id_card_number);

create index idx_phone
    on users (phone_number);

create index username
    on users (real_name);


