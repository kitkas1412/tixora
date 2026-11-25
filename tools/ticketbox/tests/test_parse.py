import json
import sys
import unittest
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import parse  # noqa: E402

FIX = Path(__file__).parent / "fixtures"


def load(name):
    return json.loads((FIX / name).read_text(encoding="utf-8"))


class ParseTsTest(unittest.TestCase):
    def test_iso_z(self):
        self.assertEqual(parse.parse_ts("2026-01-31T17:00:00Z"), datetime(2026, 1, 31, 17, tzinfo=timezone.utc))

    def test_epoch_seconds_and_millis(self):
        expected = datetime(2026, 1, 31, 17, tzinfo=timezone.utc)
        self.assertEqual(parse.parse_ts(1769878800), expected)
        self.assertEqual(parse.parse_ts(1769878800000), expected)
        self.assertEqual(parse.parse_ts("1769878800"), expected)

    def test_naive_is_utc_and_garbage_is_none(self):
        self.assertEqual(parse.parse_ts("2026-01-31T17:00:00").tzinfo, timezone.utc)
        self.assertIsNone(parse.parse_ts("not a date"))
        self.assertIsNone(parse.parse_ts(None))
        self.assertIsNone(parse.parse_ts(""))


class ListTest(unittest.TestCase):
    def test_list_and_pagination(self):
        p = load("list.json")
        items = parse.list_items(p)
        self.assertEqual(len(items), 2)
        self.assertTrue(parse.has_more(p))
        self.assertFalse(parse.has_more({"data": {"results": []}}))
        self.assertEqual(parse.list_items({}), [])


class EventTest(unittest.TestCase):
    def setUp(self):
        self.result = load("detail.json")["data"]["result"]

    def test_map_event(self):
        e = parse.map_event(self.result, city="Yên Bái")
        self.assertEqual(e["id"], 25635)
        self.assertEqual(e["url"], "https://ticketbox.vn/trai-nghiem-bay-du-luon-mu-cang-chai-25635")
        self.assertEqual(e["city"], "Yên Bái")
        self.assertEqual(e["organizer"], "Mebayluon Paragliding")
        self.assertEqual(e["categories"], ["attractionsexperiences"])
        self.assertEqual(e["min_price"], 2190000)
        self.assertIs(e["is_free"], False)
        self.assertEqual(e["start_time"], datetime(2026, 1, 31, 17, tzinfo=timezone.utc))
        self.assertEqual(e["raw"], self.result)

    def test_city_fallback_from_address(self):
        e = parse.map_event(self.result)
        self.assertEqual(e["city"], "Yen Bai Province")

    def test_showings_and_ticket_types(self):
        sh = parse.map_showings(self.result)
        self.assertEqual(len(sh), 2)
        showing, types = sh[0]
        self.assertEqual(showing["source_id"], 27154173936843)
        self.assertEqual(showing["status"], "book_now")
        self.assertEqual(len(types), 2)
        t = types[0]
        self.assertEqual((t["source_id"], t["price"], t["original_price"]), (1057526, 2190000, 2190000))
        self.assertEqual(t["sale_end"], datetime(2027, 2, 27, 17, tzinfo=timezone.utc))

    def test_missing_fields(self):
        e = parse.map_event({"id": 1, "title": "x"})
        self.assertEqual(e["url"], "https://ticketbox.vn/event-1")
        self.assertEqual(e["categories"], [])
        self.assertEqual(parse.map_showings({"id": 1}), [])


class CityMapTest(unittest.TestCase):
    def test_city_map(self):
        m = parse.city_map(load("discovery.json"))
        self.assertEqual(m[26859], "TP.HCM")
        self.assertEqual(m[26863], "Hà Nội")


if __name__ == "__main__":
    unittest.main()
