import sys
import unittest
from pathlib import Path

import httpx

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import parse  # noqa: E402
import scraper  # noqa: E402


class FakeClient:
    def __init__(self, pages):
        self.pages, self.calls = pages, 0

    def get(self, path, params=None):
        self.calls += 1
        return self.pages(params["page"])


def page(ids, more=True):
    return {"data": {"results": [{"id": i} for i in ids], "pagination": {"hasMore": more}}}


class PagingGuardTest(unittest.TestCase):
    def test_same_page_forever_stops(self):
        c = FakeClient(lambda n: page([1, 2]))
        items = list(scraper.iter_list_items(c, "upcoming", 2))
        self.assertEqual([i["id"] for i in items], [1, 2])
        self.assertEqual(c.calls, 2)

    def test_max_pages_cap(self):
        c = FakeClient(lambda n: page([n]))
        self.assertEqual(len(list(scraper.iter_list_items(c, "upcoming", 1, max_pages=5))), 5)

    def test_stops_on_empty_and_no_more(self):
        c = FakeClient(lambda n: page([n], more=n < 3))
        self.assertEqual(len(list(scraper.iter_list_items(c, "upcoming", 1))), 3)
        self.assertEqual(len(list(scraper.iter_list_items(FakeClient(lambda n: page([])), "past", 1))), 0)


class RetryTest(unittest.TestCase):
    def test_retry_after_capped(self):
        self.assertEqual(scraper.backoff_seconds(0, "9999"), scraper.MAX_RETRY_AFTER)
        self.assertEqual(scraper.backoff_seconds(0, "30"), 30)
        self.assertEqual(scraper.backoff_seconds(2, None), 8)

    def test_429_then_ok(self):
        n = {"i": 0}

        def handler(req):
            n["i"] += 1
            if n["i"] == 1:
                return httpx.Response(429, headers={"Retry-After": "9999"})
            return httpx.Response(200, json={"ok": 1})

        sleeps = []
        c = scraper.Client(0, transport=httpx.MockTransport(handler), sleep=sleeps.append)
        self.assertEqual(c.get("/x"), {"ok": 1})
        self.assertIn(scraper.MAX_RETRY_AFTER, sleeps)

    def test_non_json_raises_clear_error(self):
        c = scraper.Client(0, transport=httpx.MockTransport(lambda r: httpx.Response(200, text="<html>")),
                           sleep=lambda s: None)
        with self.assertRaises(scraper.FetchError):
            c.get("/x")

    def test_4xx_not_retried(self):
        calls = []
        c = scraper.Client(0, transport=httpx.MockTransport(
            lambda r: calls.append(1) or httpx.Response(404)), sleep=lambda s: None)
        with self.assertRaises(httpx.HTTPStatusError):
            c.get("/x")
        self.assertEqual(len(calls), 1)


class SanitizeTest(unittest.TestCase):
    def test_categories_dict(self):
        r = {"id": 1, "title": "t", "categoriesV2": ["a", {"code": "music", "name": "M"}, {"key": "k"},
                                                     {"name": "n"}, {"x": 1}]}
        self.assertEqual(parse.map_event(r)["categories"], ["a", "music", "k", "n"])

    def test_nul_stripped_everywhere(self):
        r = {"id": 1, "title": "a\x00b", "venue": "v\x00", "showings": [
            {"id": 2, "status": "s\x00", "ticketTypes": [{"id": 3, "name": "n\x00", "x\x00": ["y\x00"]}]}]}
        e = parse.map_event(r)
        self.assertEqual((e["name"], e["venue"]), ("ab", "v"))
        self.assertNotIn("\x00", parse.dumps(e["raw"]).replace("\\u0000", "\x00"))
        (sh, types), = parse.map_showings(r)
        self.assertEqual(sh["status"], "s")
        self.assertEqual(types[0]["name"], "n")
        self.assertEqual(types[0]["raw"], {"id": 3, "name": "n", "x": ["y"]})


if __name__ == "__main__":
    unittest.main()
