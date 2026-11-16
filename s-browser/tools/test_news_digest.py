#!/usr/bin/env python3
"""No-network tests for official/publisher whitelist and accurate news dates."""
import datetime as dt
import email.utils
import importlib.util
import pathlib

SCRIPT=pathlib.Path(__file__).with_name("refresh_assembly_news.py")
spec=importlib.util.spec_from_file_location("s_browser_digest",SCRIPT)
news=importlib.util.module_from_spec(spec)
spec.loader.exec_module(news)
now=dt.datetime(2026,11,16,11,0,tzinfo=dt.timezone.utc)
published=email.utils.format_datetime(now-dt.timedelta(hours=1))
old=email.utils.format_datetime(now-dt.timedelta(days=8))

def rss(title="India students take part in science event",date=published,
        source="The Hindu",domain="https://www.thehindu.com"):
    return f"""<rss version="2.0"><channel><title>Recent news</title><item>
      <title>{title}</title><link>https://news.google.com/rss/articles/abc</link>
      <pubDate>{date}</pubDate><source url="{domain}">{source}</source>
      </item></channel></rss>"""

def parse(s):
    import xml.etree.ElementTree as ET
    return ET.fromstring(s)

assert news.trusted("https://www.thehindu.com/politics")
assert news.trusted("https://www.pib.gov.in/")
for spoof in (
    "https://thehindu.com.evil.example/",
    "https://thehindu.com@evil.example/",
    "http://www.thehindu.com/",
    "https://attacker.invalid/"):
    assert not news.trusted(spoof), spoof
assert len(news.collect(parse(rss()),now))==1
assert len(news.collect(parse(rss(date=old)),now))==0
assert len(news.collect(parse(rss(date="unknown")),now))==0
assert len(news.collect(parse(rss(domain="https://evil.example")),now))==0
assert len(news.collect(parse(rss(title="Hi")),now))==0

# API errors must not create fictional content; preserve genuine, recent prior data.
fake_story=news.collect(parse(rss()),now)
existing={"feeds":{"sports_cg_hi":{"fetched_ms":int((now-dt.timedelta(hours=2)).timestamp()*1000),
                                 "stories":fake_story}}}
def fail(_url):raise OSError("network unavailable")
output=news.produce(existing,now,fetcher=fail)
assert len(output["feeds"])==8
assert len(output["feeds"]["sports_cg_hi"]["stories"])==1
assert output["feeds"]["sports_cg_hi"]["fetched_ms"]==existing["feeds"]["sports_cg_hi"]["fetched_ms"]
assert not output["feeds"]["breaking_india_en"]["stories"]
assert all(x["fetched_ms"]<=int(now.timestamp()*1000) for x in output["feeds"].values())
assert news.google_rss("sports","cg","hi").startswith("https://news.google.com/rss/")
print("PASS timestamp filtering, no fake data, publisher URL rejection and previous-data honesty")
