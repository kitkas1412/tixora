# Ticketbox scraper

Tool độc lập (không thuộc Maven) lấy sự kiện công khai, suất diễn (showing) và loại vé (ticket type) từ API công khai của ticketbox.vn rồi upsert vào một PostgreSQL riêng (cổng 5434, tách khỏi DB của Tixora).

## Cảnh báo

Đọc https://ticketbox.vn/customer-terms-of-use trước khi chạy. Điều khoản về scraping chưa được kiểm chứng. Chỉ dùng cho mục đích học tập cá nhân, giữ tốc độ thấp (`--delay` mặc định 1 giây giữa các request) và không phát tán dữ liệu.

## Các bước

1. Tạo file env từ mẫu rồi đặt mật khẩu `TICKETBOX_DB_PASSWORD`:

   ```
   cp tools/ticketbox/.env.example tools/ticketbox/.env
   ```

2. Khởi động PostgreSQL (cần Docker; schema được tạo từ `db/init/001_schema.sql` lần đầu):

   ```
   docker compose -f tools/ticketbox/compose.yaml up -d
   ```

3. Chạy thử, không ghi DB (in JSON lines):

   ```
   cd tools/ticketbox && uv run --env-file .env scraper.py --dry-run --max-events 5
   ```

4. Chạy thật:

   ```
   cd tools/ticketbox && uv run --env-file .env scraper.py
   cd tools/ticketbox && uv run --env-file .env scraper.py --filter all --page-size 50 --delay 1.5
   ```

Tuỳ chọn: `--filter upcoming|past|all`, `--max-events N`, `--page-size N`, `--delay SECONDS`, `--dry-run`, `--save-raw` (ghi JSON gốc vào `tools/ticketbox/out/`).

Biến môi trường: `TICKETBOX_DB_HOST` (127.0.0.1), `TICKETBOX_DB_PORT` (5434), `TICKETBOX_DB_NAME` (ticketbox), `TICKETBOX_DB_USER` (ticketbox), `TICKETBOX_DB_PASSWORD` (bắt buộc trừ khi `--dry-run`).

Mỗi event được ghi trong một transaction: upsert `tb_event`, xoá các `tb_showing` cũ (cascade sang `tb_ticket_type`) rồi chèn lại. Chạy lại an toàn. Event lỗi chi tiết sẽ bị bỏ qua và ghi log; cuối cùng có bản tổng kết.

## Test (offline)

```
cd tools/ticketbox && python3 -m unittest discover -s tests
```

## Reset DB

`down -v` xoá volume và toàn bộ dữ liệu đã scrape:

```
docker compose -f tools/ticketbox/compose.yaml down -v
```

## Ví dụ SQL

```sql
-- Số lượng dữ liệu
SELECT (SELECT count(*) FROM tb_event) AS events,
       (SELECT count(*) FROM tb_showing) AS showings,
       (SELECT count(*) FROM tb_ticket_type) AS ticket_types;

-- Event sắp diễn ra theo thành phố
SELECT city, count(*) FROM tb_event WHERE start_time > now() GROUP BY city ORDER BY 2 DESC;

-- Loại vé rẻ nhất mỗi event
SELECT e.name, min(t.price) AS min_price
FROM tb_event e
JOIN tb_showing s ON s.event_id = e.id
JOIN tb_ticket_type t ON t.showing_id = s.id
GROUP BY e.id, e.name ORDER BY min_price NULLS LAST LIMIT 20;

-- Truy vấn trong JSON gốc
SELECT id, raw->>'orgName' FROM tb_event WHERE 'music' = ANY (categories);
```
