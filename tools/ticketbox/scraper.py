"""Scraper ticketbox.vn (API công khai) -> PostgreSQL riêng.

Chạy: uv run --env-file <file env> scraper.py --dry-run --max-events 5
"""
from __future__ import annotations

import argparse
import json
import logging
import os
import sys
import time
from pathlib import Path

import httpx
import psycopg
from psycopg.types.json import Jsonb

import parse

API = "https://api-v2.ticketbox.vn"
USER_AGENT = "tixora-ticketbox-scraper/0.1 (personal learning project; low rate)"
CITY_SECTIONS = ("newly-on-sale", "coming-soon")
MAX_RETRIES = 5
MAX_RETRY_AFTER = 120  # giây
MAX_BAD_JSON = 3
MAX_PAGES = 1000
OUT_DIR = Path(__file__).parent / "out"

log = logging.getLogger("ticketbox")


class FetchError(RuntimeError):
    """Request thất bại sau khi retry, hoặc lỗi không thể retry."""


def backoff_seconds(attempt: int, retry_after: str | None) -> float:
    backoff = 2.0 ** (attempt + 1)
    if retry_after and retry_after.isdigit():
        backoff = max(backoff, min(float(retry_after), MAX_RETRY_AFTER))
    return backoff


class Client:
    """httpx client với delay giữa các request và retry/backoff."""

    def __init__(self, delay: float, transport: httpx.BaseTransport | None = None, sleep=time.sleep):
        self.delay = delay
        self.sleep = sleep
        self.http = httpx.Client(
            base_url=API, timeout=20.0, transport=transport,
            headers={"User-Agent": USER_AGENT, "Accept": "application/json"},
        )
        self._last = 0.0

    def close(self) -> None:
        self.http.close()

    def get(self, path: str, params: dict | None = None) -> dict:
        bad_json = 0
        for attempt in range(MAX_RETRIES + 1):
            wait = self.delay - (time.monotonic() - self._last)
            if wait > 0:
                self.sleep(wait)
            retry_after = None
            try:
                resp = self.http.get(path, params=params)
                self._last = time.monotonic()
                if resp.status_code == 429 or resp.status_code >= 500:
                    retry_after = resp.headers.get("Retry-After")
                    reason = f"HTTP {resp.status_code}"
                else:
                    resp.raise_for_status()  # 4xx khác: lỗi, không retry
                    try:
                        return resp.json()
                    except ValueError:
                        bad_json += 1
                        if bad_json >= MAX_BAD_JSON:
                            raise FetchError(f"GET {path}: response 200 không phải JSON ({bad_json} lần)")
                        reason = "non-JSON body"
            except (httpx.TimeoutException, httpx.TransportError) as e:
                self._last = time.monotonic()
                reason = type(e).__name__
            if attempt == MAX_RETRIES:
                raise FetchError(f"GET {path} failed after {MAX_RETRIES} retries ({reason})")
            backoff = backoff_seconds(attempt, retry_after)
            log.warning("GET %s: %s, retry %d/%d in %.0fs", path, reason, attempt + 1, MAX_RETRIES, backoff)
            self.sleep(backoff)
        raise AssertionError("unreachable")


def load_city_map(client: Client) -> dict[int, str]:
    cities: dict[int, str] = {}
    for section in CITY_SECTIONS:
        try:
            cities.update(parse.city_map(client.get(f"/gin/api/v2/discovery/onsale-sections/{section}")))
        except Exception as e:  # city chỉ là best effort
            log.warning("Không lấy được city từ section %s: %s", section, e)
    log.info("Có city cho %d event từ discovery", len(cities))
    return cities


def iter_list_items(client, flt: str, page_size: int, max_pages: int = MAX_PAGES):
    seen: set = set()
    for page in range(1, max_pages + 1):
        payload = client.get("/search/v3/events", {"q": "", "filter": flt, "page": page, "limit": page_size})
        items = parse.list_items(payload)
        new = [i for i in items if i.get("id") not in seen]
        if not new:
            if items:
                log.warning("Trang %d (%s) không có event mới, dừng phân trang", page, flt)
            return
        seen.update(i.get("id") for i in new)
        yield from new
        if not parse.has_more(payload):
            return
    log.warning("Đạt giới hạn %d trang (%s), dừng phân trang", max_pages, flt)


def save_event(conn, ev: dict, showings: list) -> int:
    """Một transaction cho mỗi event. Trả về số ticket type đã ghi."""
    n_types = 0
    with conn.transaction(), conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO tb_event (id, name, url, image_url, venue, address, city, organizer, categories,
                                  start_time, end_time, status, min_price, is_free, raw, scraped_at)
            VALUES (%(id)s, %(name)s, %(url)s, %(image_url)s, %(venue)s, %(address)s, %(city)s, %(organizer)s,
                    %(categories)s, %(start_time)s, %(end_time)s, %(status)s, %(min_price)s, %(is_free)s,
                    %(raw)s, now())
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name, url = EXCLUDED.url, image_url = EXCLUDED.image_url,
                venue = EXCLUDED.venue, address = EXCLUDED.address, city = EXCLUDED.city,
                organizer = EXCLUDED.organizer, categories = EXCLUDED.categories,
                start_time = EXCLUDED.start_time, end_time = EXCLUDED.end_time, status = EXCLUDED.status,
                min_price = EXCLUDED.min_price, is_free = EXCLUDED.is_free, raw = EXCLUDED.raw,
                scraped_at = now()
            """,
            {**ev, "raw": Jsonb(ev["raw"])},
        )
        cur.execute("DELETE FROM tb_showing WHERE event_id = %s", (ev["id"],))  # cascade ticket types
        for showing, types in showings:
            cur.execute(
                """
                INSERT INTO tb_showing (event_id, source_id, start_time, end_time, status, raw)
                VALUES (%s, %s, %s, %s, %s, %s) RETURNING id
                """,
                (ev["id"], showing["source_id"], showing["start_time"], showing["end_time"],
                 showing["status"], Jsonb(showing["raw"])),
            )
            showing_id = cur.fetchone()[0]
            for t in types:
                cur.execute(
                    """
                    INSERT INTO tb_ticket_type (showing_id, source_id, name, price, original_price,
                                                sale_start, sale_end, status, is_free, raw)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    (showing_id, t["source_id"], t["name"], t["price"], t["original_price"],
                     t["sale_start"], t["sale_end"], t["status"], t["is_free"], Jsonb(t["raw"])),
                )
                n_types += 1
    return n_types


def connect():
    password = os.environ.get("TICKETBOX_DB_PASSWORD")
    if not password:
        sys.exit("Thiếu TICKETBOX_DB_PASSWORD (nạp qua uv run --env-file, hoặc dùng --dry-run)")
    try:
        port = int(os.environ.get("TICKETBOX_DB_PORT", "5434"))
    except ValueError:
        sys.exit("TICKETBOX_DB_PORT phải là số")
    host = os.environ.get("TICKETBOX_DB_HOST", "127.0.0.1")
    try:
        return psycopg.connect(
            host=host, port=port,
            dbname=os.environ.get("TICKETBOX_DB_NAME", "ticketbox"),
            user=os.environ.get("TICKETBOX_DB_USER", "ticketbox"),
            password=password, connect_timeout=10,
        )
    except psycopg.Error as e:
        sys.exit(f"Không kết nối được DB tại {host}:{port} ({type(e).__name__}); kiểm tra compose và thông tin đăng nhập")


def int_range(lo: int, hi: int | None = None):
    def check(v: str) -> int:
        n = int(v)
        if n < lo or (hi is not None and n > hi):
            raise argparse.ArgumentTypeError(f"phải trong khoảng {lo}..{hi if hi is not None else '∞'}")
        return n
    return check


def min_float(lo: float):
    def check(v: str) -> float:
        n = float(v)
        if n < lo:
            raise argparse.ArgumentTypeError(f"phải >= {lo}")
        return n
    return check


def run(args, client: Client, conn) -> tuple[dict, bool]:
    """Trả về (stats, aborted)."""
    cities = load_city_map(client)
    if args.save_raw:
        OUT_DIR.mkdir(exist_ok=True)
    filters = ["upcoming", "past"] if args.filter == "all" else [args.filter]
    stats = {"events": 0, "showings": 0, "ticket_types": 0, "failures": 0}
    seen: set[int] = set()
    try:
        for flt in filters:
            for item in iter_list_items(client, flt, args.page_size):
                eid = parse.to_int(item.get("id"))
                if eid is None or eid in seen:
                    continue
                seen.add(eid)
                try:
                    payload = client.get(f"/gin/api/v2/events/{eid}")
                    result = (payload.get("data") or {}).get("result")
                    if not result:
                        raise ValueError("detail rỗng")
                    ev = parse.map_event(result, item, cities.get(eid))
                    showings = parse.map_showings(result)
                    n_types = sum(len(t) for _, t in showings)
                    if args.save_raw:
                        (OUT_DIR / f"{eid}.json").write_text(
                            json.dumps(payload, ensure_ascii=False, indent=1), encoding="utf-8")
                    if conn is None:
                        print(parse.dumps({"event": ev,
                                           "showings": [{**s, "ticket_types": t} for s, t in showings]}))
                    else:
                        n_types = save_event(conn, ev, showings)
                except Exception as e:
                    stats["failures"] += 1
                    log.error("Bỏ qua event %s: %s", eid, e)
                    if conn is not None and (isinstance(e, psycopg.OperationalError) or conn.closed):
                        log.error("Mất kết nối DB, dừng chạy")
                        return stats, True
                    continue
                stats["events"] += 1
                stats["showings"] += len(showings)
                stats["ticket_types"] += n_types
                log.info("event %s (%s): %d showings, %d ticket types", eid, ev["name"], len(showings), n_types)
                if args.max_events and stats["events"] >= args.max_events:
                    return stats, False
    except (httpx.HTTPError, FetchError, ValueError) as e:  # lỗi khi phân trang danh sách
        log.error("Lỗi khi lấy danh sách event, dừng: %s", e)
        return stats, True
    except KeyboardInterrupt:
        log.warning("Dừng bởi người dùng")
    return stats, False


def main() -> int:
    ap = argparse.ArgumentParser(description="Scrape sự kiện công khai từ ticketbox.vn vào PostgreSQL")
    ap.add_argument("--filter", choices=["upcoming", "past", "all"], default="upcoming")
    ap.add_argument("--max-events", type=int_range(1), default=None, help="giới hạn số event (>=1; mặc định: không giới hạn)")
    ap.add_argument("--page-size", type=int_range(1, 100), default=50, help="1..100 (mặc định 50)")
    ap.add_argument("--delay", type=min_float(0.5), default=1.0, help="giây giữa các request (>=0.5, mặc định 1.0)")
    ap.add_argument("--dry-run", action="store_true", help="in JSON lines, không ghi DB")
    ap.add_argument("--save-raw", action="store_true", help="ghi JSON gốc vào tools/ticketbox/out/")
    args = ap.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    conn = None if args.dry_run else connect()
    try:
        client = Client(args.delay)
        try:
            stats, aborted = run(args, client, conn)
        finally:
            client.close()
    finally:
        if conn is not None:
            conn.close()
    log.info("Tổng kết: %(events)d events, %(showings)d showings, %(ticket_types)d ticket types, "
             "%(failures)d lỗi", stats)
    return 1 if aborted or (stats["failures"] and not stats["events"]) else 0


if __name__ == "__main__":
    sys.exit(main())
