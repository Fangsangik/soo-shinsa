
insert into grade (point_rate, requirement, created_at, updated_at, name)
values (0.3,0,now(),now(),'ROOKIE'),
       (0.5,100,now(),now(),'BRONZE'),
       (0.7,200,now(),now(),'SILVER'),
       (1,300,now(),now(),'GOLD');

insert into users (email, name, password, phone_num, role, status) values ('eee','eee','password1!','eee','VENDOR','ACTIVE');
insert into Users (email, name, password, phone_num, role, status) values ('lfridlington0@webmd.com', 'Liv Fridlington', 'password1!', '2593133637', 'CUSTOMER', 'ACTIVE');
insert into Users (email, name, password, phone_num, role, status) values ('wglayzer1@barnesandnoble.com', 'Welbie Glayzer', 'fE2?q4~`mM=', '2542570100', 'CUSTOMER', 'ACTIVE');
insert into Users (email, name, password, phone_num, role, status) values ('maudas2@opensource.org', 'Mata Audas', 'cJ0\|?0%A}),a', '2926395206', 'CUSTOMER', 'ACTIVE');
insert into Users (email, name, password, phone_num, role, status) values ('acoomber3@e-recht24.de', 'Ammamaria Coomber', 'tM7,TqL_9(6%H},', '2248255918', 'CUSTOMER', 'ACTIVE');
insert into Users (email, name, password, phone_num, role, status) values ('gwhifen4@latimes.com', 'Grace Whifen', 'lI1$dC$61PSLw%t', '3481764576', 'CUSTOMER', 'ACTIVE');
insert into Users (email, name, password, phone_num, role, status) values ('nmccomiskey5@weebly.com', 'Novelia McComiskey', 'dT9`e.z)', '7488313419', 'CUSTOMER', 'ACTIVE');
insert into Users (email, name, password, phone_num, role, status) values ('gpaddison6@tripadvisor.com', 'Germaine Paddison', 'mY4\d`kt`/1}b=zl', '4218439549', 'CUSTOMER', 'ACTIVE');
insert into Users (email, name, password, phone_num, role, status) values ('jskpsey7@epa.gov', 'Jackqueline Skpsey', 'dJ1~Ia<u?b#', '4694637620', 'CUSTOMER', 'ACTIVE');
insert into Users (email, name, password, phone_num, role, status) values ('rkrelle8@diigo.com', 'Rafe Krelle', 'hZ7~ilvQt$Yc5FX', '3742992616', 'CUSTOMER', 'ACTIVE');


insert into brand (created_at,user_id, status, registration_num, context, name) values (now(),1, 'OPEN', 123, 123, 'Mrs');
insert into brand (created_at,user_id, status, registration_num, context, name) values (now(),1, 'OPEN', 123, 123, 'Rev');
insert into brand (created_at,user_id, status, registration_num, context, name) values (now(),1, 'OPEN', 123, 123, 'Ms');

insert into category (brand_id, created_at, parent_id, updated_at, name)
values (1,now(),null,now(),'swimming');


insert into product (category_id,created_at,price, brand_id, product_status, name)values (1,now(), '41300', 1, 'AVAILABLE', 'Creme De Banane - Marie');
insert into product (category_id,created_at,price, brand_id, product_status, name) values (1,now(),'34600', 1, 'AVAILABLE', 'The Pop Shoppe - Grape');
insert into product (category_id,created_at,price, brand_id, product_status, name) values (1,now(),'73800', 3, 'AVAILABLE', 'Bacardi Limon');
insert into product (category_id,created_at,price, brand_id, product_status, name) values (1,now(),'49200', 2, 'AVAILABLE', 'Fenngreek Seed');
insert into product (category_id,created_at,price, brand_id, product_status, name) values (1,now(),'58600', 3, 'AVAILABLE', 'Tart - Pecan Butter Squares');
insert into product (category_id,created_at,price, brand_id, product_status, name) values (1,now(),'07600', 2, 'AVAILABLE', 'Chicken Giblets');
insert into product (category_id,created_at,price, brand_id, product_status, name) values (1,now(),'30800', 2, 'AVAILABLE', 'Ocean Spray - Ruby Red');
insert into product (category_id,created_at,price, brand_id, product_status, name) values (1,now(),'12700', 2, 'AVAILABLE', 'Stock - Veal, White');
insert into product (category_id,created_at,price, brand_id, product_status, name) values (1,now(),'36600', 2, 'AVAILABLE', 'Energy Drink');
insert into product (category_id,created_at,price, brand_id, product_status, name) values (1,now(),'20600', 2, 'AVAILABLE', 'Lettuce - Sea / Sea Asparagus');

insert into product_option (created_at,product_id, size, product_status, color) values (now(),2, '2XL', 'AVAILABLE', 'Violet');
insert into product_option (created_at,product_id, size, product_status, color) values (now(),9, '2XL', 'AVAILABLE', 'Red');
insert into product_option (created_at,product_id, size, product_status, color) values (now(),8, '2XL', 'AVAILABLE', 'Indigo');
insert into product_option (created_at,product_id, size, product_status, color) values (now(),5, 'XS', 'AVAILABLE', 'Crimson');
insert into product_option (created_at,product_id, size, product_status, color) values (now(),3, 'M', 'AVAILABLE', 'Turquoise');
insert into product_option (created_at,product_id, size, product_status, color) values (now(),2, 'S', 'AVAILABLE', 'Aquamarine');
insert into product_option (created_at,product_id, size, product_status, color) values (now(),1, 'S', 'AVAILABLE', 'Blue');
insert into product_option (created_at,product_id, size, product_status, color) values (now(),8, 'XS', 'AVAILABLE', 'Pink');
insert into product_option (created_at,product_id, size, product_status, color) values (now(),1, 'S', 'AVAILABLE', 'Mauv');
insert into product_option (created_at,product_id, size, product_status, color) values (now(),5, '2XL', 'AVAILABLE', 'Red');

insert into orders (created_at, total_price, user_id, order_id, status) values ('2023-08-18 02:02:53', 390.58, 3, 8066, 'PAYMENTCOMPLETED');
insert into orders (created_at, total_price, user_id, order_id, status) values ('2023-07-03 14:03:23', 36.5, 3, 2928, 'PAYMENTCOMPLETED');
insert into orders (created_at, total_price, user_id, order_id, status) values ('2023-06-07 15:16:24', 567.08, 5, 6531, 'RETURN');
insert into orders (created_at, total_price, user_id, order_id, status) values ('2024-11-18 20:05:09', 842.55, 7, 9298, 'PAYMENTCOMPLETED');
insert into orders (created_at, total_price, user_id, order_id, status) values ('2023-02-28 04:59:24', 836.97, 9, 4971, 'PAYMENTCOMPLETED');
insert into orders (created_at, total_price, user_id, order_id, status) values ('2024-12-30 21:40:16', 144.99, 10, 8390, 'RETURN');
insert into orders (created_at, total_price, user_id, order_id, status) values ('2024-03-14 06:56:31', 748.87, 9, 1360, 'PAYMENTCOMPLETED');
insert into orders (created_at, total_price, user_id, order_id, status) values ('2022-08-10 20:51:10', 368.53, 5, 2077, 'PAYMENTCOMPLETED');
insert into orders (created_at, total_price, user_id, order_id, status) values ('2024-06-19 16:17:29', 301.16, 3, 9064, 'RETURN');
insert into orders (created_at, total_price, user_id, order_id, status) values ('2022-12-22 12:41:57', 267.72, 8, 8855, 'RETURN');


insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-04-23 08:10:56', 10, 3, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-10-06 13:40:30', 2, 8, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-04-22 02:09:41', 10, 2, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-01-28 21:40:49', 5, 1, 4);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-08-18 12:51:54', 2, 6, 5);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-03-06 20:07:50', 5, 10, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-12-15 15:10:27', 8, 8, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-04-25 15:56:56', 1, 7, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-04-12 11:39:50', 9, 4, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-09-11 04:12:19', 2, 7, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-04-13 19:23:31', 3, 1, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-07-07 20:43:11', 10, 5, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-08-12 22:20:28', 8, 3, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-04-12 09:34:07', 8, 9, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-01-19 21:37:25', 7, 7, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-04-12 23:05:37', 3, 9, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-12-26 08:51:09', 3, 4, 3);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-08-05 11:12:13', 5, 6, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-05-09 00:15:01', 4, 10, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-02-12 07:18:43', 7, 8, 5);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-05-04 09:34:55', 6, 2, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-05-24 01:20:42', 9, 4, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-09-14 00:01:12', 4, 5, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-03-16 16:11:37', 5, 1, 3);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-08-13 16:24:43', 7, 7, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-04-12 20:52:49', 7, 5, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-07-27 02:32:21', 6, 10, 3);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-11-17 17:34:11', 7, 8, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-06-25 00:33:37', 10, 2, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-08-09 23:33:35', 2, 1, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-09-07 11:02:40', 9, 2, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-06-29 06:57:39', 6, 4, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-04-21 19:14:58', 10, 2, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-04-10 17:34:08', 8, 9, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-09-21 23:21:13', 6, 9, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-01-31 22:03:34', 3, 1, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-06-11 12:36:39', 6, 1, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-09-02 19:06:00', 2, 8, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-08-03 09:32:31', 10, 10, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-10-31 16:17:20', 6, 7, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-06-22 20:15:50', 6, 6, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-08-13 06:10:10', 1, 1, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-07-31 18:36:02', 6, 2, 3);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-03-15 01:31:35', 2, 8, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-03-20 12:41:48', 3, 3, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-10-13 22:18:20', 10, 2, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-01-16 08:10:58', 9, 3, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-04-25 01:18:10', 5, 2, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-01-17 12:38:39', 2, 1, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-02-24 03:38:59', 1, 1, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-01-12 03:24:53', 7, 5, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-11-12 04:08:13', 10, 8, 5);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-06-29 02:25:06', 1, 4, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-09-10 15:20:45', 9, 5, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-05-08 05:41:36', 7, 8, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-10-09 22:02:37', 3, 2, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-07-12 11:41:07', 2, 6, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-07-09 02:42:26', 3, 8, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-12-12 11:37:17', 10, 4, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-02-03 11:46:59', 6, 4, 4);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-10-03 07:50:56', 10, 8, 3);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-02-26 10:29:50', 6, 7, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-03-08 17:52:31', 9, 10, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-01-01 05:40:32', 4, 9, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-11-04 21:27:40', 3, 1, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-08-22 18:41:58', 9, 3, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-12-02 07:09:25', 7, 1, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-12-25 22:46:25', 2, 3, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-10-20 10:20:40', 1, 1, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-10-15 15:08:09', 8, 3, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-08-07 05:57:09', 6, 5, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-06-21 05:38:22', 8, 10, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-03-02 14:56:11', 7, 9, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-10-16 22:50:15', 8, 3, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-09-30 00:10:27', 3, 7, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-07-24 13:23:01', 1, 6, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-12-16 20:01:42', 5, 1, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-06-28 14:42:25', 8, 6, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-01-09 05:06:41', 2, 8, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-12-02 22:53:24', 8, 8, 5);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-04-15 13:37:38', 9, 2, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-12-13 13:31:04', 2, 10, 6);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-05-11 06:12:10', 10, 5, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-10-13 03:16:49', 2, 3, 10);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-04-14 23:32:28', 1, 4, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-09-12 10:39:18', 1, 4, 4);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-03-16 08:19:30', 8, 3, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-06-22 16:09:07', 8, 10, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-07-30 22:51:51', 10, 4, 3);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-05-30 08:39:32', 1, 10, 1);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-05-27 07:55:47', 4, 2, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-07-21 06:48:26', 10, 7, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-09-19 06:47:14', 9, 9, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-10-30 18:27:27', 3, 2, 9);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-01-01 08:27:36', 10, 9, 5);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-06-13 06:58:55', 3, 9, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-09-02 08:34:04', 9, 3, 7);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2023-02-05 16:50:19', 4, 4, 8);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2022-01-28 02:11:16', 10, 3, 2);
insert into order_items (created_at, quantity, orders_id, product_id) values ('2024-07-26 15:43:24', 9, 5, 8);

