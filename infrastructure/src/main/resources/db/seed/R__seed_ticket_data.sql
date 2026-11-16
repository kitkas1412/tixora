-- R: seed data cho ticket / ticket_item (CHỈ profile local; bật qua spring.flyway.locations trong application-local.yml).
-- Port từ section4 environment/mysql/init/ticket_init.sql (MySQL `desc` -> description).
-- Repeatable migration: Flyway chạy lại khi checksum đổi, nên phải idempotent.
-- Idempotent theo name: chỉ insert khi chưa có dòng cùng name. Đổi name sẽ để lại dòng cũ.
-- Chỉ so khớp theo name: đổi giá trị khác (price, stock, time) làm script chạy lại nhưng KHÔNG update dòng đã có; muốn làm mới thì chạy `docker compose -f environment/compose.yaml down -v` (xóa toàn bộ dữ liệu local).
-- ticket_item.activity_id lấy theo id thật của ticket cha (tra theo name), không hardcode id.

INSERT INTO ticket (name, description, start_time, end_time, status, updated_at, created_at)
SELECT v.name, v.description, v.start_time, v.end_time, v.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('Đợt Mở Bán Vé Ngày 12/12', 'Sự kiện mở bán vé đặc biệt cho ngày 12/12',
     TIMESTAMP '2024-12-12 00:00:00', TIMESTAMP '2024-12-12 23:59:59', 1),
    ('Đợt Mở Bán Vé Ngày 01/01', 'Sự kiện mở bán vé cho ngày đầu năm mới 01/01',
     TIMESTAMP '2025-01-01 00:00:00', TIMESTAMP '2025-01-01 23:59:59', 1)
) AS v (name, description, start_time, end_time, status)
WHERE NOT EXISTS (SELECT 1 FROM ticket t WHERE t.name = v.name);

INSERT INTO ticket_item (name, description, stock_initial, stock_available, is_stock_prepared,
                         price_original, price_flash, sale_start_time, sale_end_time, status,
                         activity_id, updated_at, created_at)
SELECT v.name, v.description, v.stock_initial, v.stock_available, v.is_stock_prepared,
       v.price_original, v.price_flash, v.sale_start_time, v.sale_end_time, v.status,
       p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    -- Ticket items cho sự kiện 12/12
    ('Vé Sự Kiện 12/12 - Hạng Phổ Thông', 'Vé phổ thông cho sự kiện ngày 12/12', 1000, 1000, FALSE,
     100000::bigint, 10000::bigint, TIMESTAMP '2024-12-12 00:00:00', TIMESTAMP '2024-12-12 23:59:59', 1,
     'Đợt Mở Bán Vé Ngày 12/12'),
    ('Vé Sự Kiện 12/12 - Hạng VIP', 'Vé VIP cho sự kiện ngày 12/12', 500, 500, FALSE,
     200000::bigint, 15000::bigint, TIMESTAMP '2024-12-12 00:00:00', TIMESTAMP '2024-12-12 23:59:59', 1,
     'Đợt Mở Bán Vé Ngày 12/12'),
    -- Ticket items cho sự kiện 01/01
    ('Vé Sự Kiện 01/01 - Hạng Phổ Thông', 'Vé phổ thông cho sự kiện ngày 01/01', 2000, 2000, FALSE,
     100000::bigint, 10000::bigint, TIMESTAMP '2025-01-01 00:00:00', TIMESTAMP '2025-01-01 23:59:59', 1,
     'Đợt Mở Bán Vé Ngày 01/01'),
    ('Vé Sự Kiện 01/01 - Hạng VIP', 'Vé VIP cho sự kiện ngày 01/01', 1000, 1000, FALSE,
     200000::bigint, 15000::bigint, TIMESTAMP '2025-01-01 00:00:00', TIMESTAMP '2025-01-01 23:59:59', 1,
     'Đợt Mở Bán Vé Ngày 01/01')
) AS v (name, description, stock_initial, stock_available, is_stock_prepared,
        price_original, price_flash, sale_start_time, sale_end_time, status, ticket_name)
JOIN (SELECT name, MIN(id) AS id FROM ticket GROUP BY name) p ON p.name = v.ticket_name
WHERE NOT EXISTS (SELECT 1 FROM ticket_item ti WHERE ti.name = v.name);
