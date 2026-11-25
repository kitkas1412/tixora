"""Map JSON của ticketbox.vn API sang các dict khớp bảng tb_event / tb_showing / tb_ticket_type.

Chỉ dùng stdlib để test chạy được mà không cần cài thư viện.
"""
from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any

SITE = "https://ticketbox.vn"


def parse_ts(value: Any) -> datetime | None:
    """ISO 8601 (có thể kết thúc bằng Z) hoặc epoch giây/mili giây -> datetime UTC."""
    if value is None or value == "" or isinstance(value, bool):
        return None
    try:
        if isinstance(value, (int, float)) or (isinstance(value, str) and value.strip().lstrip("-").isdigit()):
            n = float(value)
            if abs(n) >= 1e11:  # mili giây
                n /= 1000
            return datetime.fromtimestamp(n, tz=timezone.utc)
        dt = datetime.fromisoformat(str(value).strip().replace("Z", "+00:00"))
    except (ValueError, OverflowError, OSError):
        return None
    return dt if dt.tzinfo else dt.replace(tzinfo=timezone.utc)


def clean(obj: Any) -> Any:
    """Bỏ ký tự \\x00 (Postgres từ chối) khỏi mọi chuỗi, kể cả trong dict/list lồng nhau."""
    if isinstance(obj, str):
        return obj.replace("\x00", "")
    if isinstance(obj, list):
        return [clean(v) for v in obj]
    if isinstance(obj, dict):
        return {clean(k): clean(v) for k, v in obj.items()}
    return obj


def category_name(c: Any) -> str | None:
    if isinstance(c, dict):
        for key in ("code", "key", "name"):
            if c.get(key):
                return str(c[key])
        return None
    return None if c is None else str(c)


def to_int(value: Any) -> int | None:
    if value is None or isinstance(value, bool):
        return None
    try:
        return int(value)
    except (ValueError, TypeError):
        return None


def to_text(value: Any) -> str | None:
    return None if value is None else str(value)


def event_url(slug: str | None, event_id: int) -> str:
    return f"{SITE}/{slug}-{event_id}" if slug else f"{SITE}/event-{event_id}"


def list_items(payload: dict) -> list[dict]:
    """Danh sách event của trang search: data.results."""
    return (payload.get("data") or {}).get("results") or []


def has_more(payload: dict) -> bool:
    return bool(((payload.get("data") or {}).get("pagination") or {}).get("hasMore"))


def city_map(payload: dict) -> dict[int, str]:
    """event id -> tên thành phố (tiếng Việt) từ discovery section."""
    out: dict[int, str] = {}
    for item in list_items(payload):
        eid = to_int(item.get("id"))
        city = item.get("city") or {}
        name = city.get("vi") or city.get("en")
        if eid is not None and name:
            out[eid] = name
    return out


def city_from_address(address: str | None) -> str | None:
    """Fallback (best effort): đoạn cuối của địa chỉ, tách bởi dấu phẩy."""
    if not address:
        return None
    last = address.split(",")[-1].strip()
    return last or None


def map_event(result: dict, list_item: dict | None = None, city: str | None = None) -> dict:
    """result = data.result của detail API. list_item dùng để bổ sung slug/ảnh nếu thiếu."""
    result = clean(result)
    list_item = clean(list_item or {})
    eid = to_int(result.get("id") if result.get("id") is not None else list_item.get("id"))
    slug = result.get("url") or list_item.get("url")
    cats = [n for n in map(category_name, result.get("categoriesV2") or []) if n]
    return {
        "id": eid,
        "name": result.get("title") or list_item.get("name") or "",
        "url": event_url(slug, eid),
        "image_url": result.get("bannerURL") or list_item.get("imageUrl"),
        "venue": result.get("venue"),
        "address": result.get("address"),
        "city": clean(city) or city_from_address(result.get("address")),
        "organizer": result.get("orgName"),
        "categories": cats,
        "start_time": parse_ts(result.get("startTime")),
        "end_time": parse_ts(result.get("endTime")),
        "status": to_text(result.get("status")),
        "min_price": to_int(result.get("minTicketPrice")),
        "is_free": result.get("isFree"),
        "raw": result,
    }


def map_showings(result: dict) -> list[tuple[dict, list[dict]]]:
    """Trả về [(showing_row, [ticket_type_row, ...]), ...]."""
    result = clean(result)
    out = []
    for s in result.get("showings") or []:
        showing = {
            "source_id": to_int(s.get("id")),
            "start_time": parse_ts(s.get("startTime")),
            "end_time": parse_ts(s.get("endTime")),
            "status": to_text(s.get("status")),
            "raw": s,
        }
        types = []
        for t in s.get("ticketTypes") or []:
            types.append({
                "source_id": to_int(t.get("id")),
                "name": t.get("name") or "",
                "price": to_int(t.get("price")),
                "original_price": to_int(t.get("originalPrice")),
                "sale_start": parse_ts(t.get("startTime")),
                "sale_end": parse_ts(t.get("endTime")),
                "status": to_text(t.get("status")),
                "is_free": t.get("isFree"),
                "raw": t,
            })
        out.append((showing, types))
    return out


def json_default(o: Any) -> str:
    if isinstance(o, datetime):
        return o.isoformat()
    raise TypeError(type(o))


def dumps(obj: Any) -> str:
    return json.dumps(obj, ensure_ascii=False, default=json_default)
