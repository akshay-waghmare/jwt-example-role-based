drop table if exists role;
drop table if exists user;
drop table if exists user_roles;
create table role (id bigint not null auto_increment, description varchar(255), name varchar(255), primary key (id)) engine=MyISAM;
create table user (id bigint not null auto_increment, age integer, password varchar(255), salary bigint, username varchar(255), primary key (id)) engine=MyISAM;
create table user_roles (user_id bigint not null, role_id bigint not null, primary key (user_id, role_id)) engine=MyISAM;
create table bets(betId bigint not null auto_increment,user_id bigint not null,betType varchar(20),amount decimal(10,2),odd decimal(10,2),potentialWin decimal(10,2),status varchar(20),placedAT DATETIME,foreign key (user_Id) references user(id),primary key (betId))engine=MyISAM;
create table transaction(transaction_id bigint not null auto_increment,user_id bigint not null,transaction_Type varchar(20) not null,amount decimal(10,2) not null,transaction_Date DATETIME not null,status varchar(20)not null,remark varchar(20) null,fromto varchar(20) null,balance_After_transaction decimal(10,2) not null,foreign key (user_Id) references user(id),primary key (transaction_id))engine=MyISAM;
create table matches(match_id bigint not null,home_Team_name varchar(20) not null,away_Team_name varchar(20) not null,match_date datetime not null,sport_type varchar(20) not null,competition varchar(20) not null,match_status varchar(20) not null,result varchar(20) null,odd float null,location varchar(20) null, visibility boolean not null default true,match_link varchar(20) not null,primary key(match_id))engine=MyISAM;
alter table user_roles add constraint FKrhfovtciq1l558cw6udg0h0d3 foreign key (role_id) references role (id);
alter table user_roles add constraint FK55itppkw3i07do3h7qoclqd4k foreign key (user_id) references user (id);
INSERT INTO user (id, username, password, salary, age) VALUES (1, 'user1', '$2a$04$Ye7/lJoJin6.m9sOJZ9ujeTgHEVM4VXgI2Ingpsnf9gXyXEXf/IlW', 3456, 33);
INSERT INTO user (id, username, password, salary, age) VALUES (2, 'user2', '$2a$04$StghL1FYVyZLdi8/DIkAF./2rz61uiYPI3.MaAph5hUq03XKeflyW', 7823, 23);
INSERT INTO user (id, username, password, salary, age) VALUES (3, 'user3', '$2a$04$Lk4zqXHrHd82w5/tiMy8ru9RpAXhvFfmHOuqTmFPWQcUhBD8SSJ6W', 4234, 45);

INSERT INTO role (id, description, name) VALUES (4, 'Admin role', 'ADMIN');
INSERT INTO role (id, description, name) VALUES (5, 'User role', 'USER');

INSERT INTO user_roles (user_id, role_id) VALUES (1, 4);
INSERT INTO user_roles (user_id, role_id) VALUES (2, 5);