#!/usr/bin/env python3
"""Create a small, attributed, date-filtered school news digest for GitHub Pages.

No copied articles, snippets or fabricated news. Only headlines and links.
Third-party RSS has no service-level guarantee. Preserve previous successful
updates, retaining their original fetch timestamps on upstream errors.
"""
import argparse
import datetime as dt
import email.utils
import json
import pathlib
import sys
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

UTC=dt.timezone.utc
APPROVED={
    "newsonair.gov.in","pib.gov.in","ddnews.gov.in","thehindu.com",
    "indianexpress.com","hindustantimes.com","indiatoday.in","ndtv.com",
    "reuters.com","apnews.com","bbc.com","bbc.co.uk",
    "timesofindia.indiatimes.com","news18.com","business-standard.com",
    "livemint.com","economictimes.indiatimes.com","theprint.in",
    "espncricinfo.com","olympics.com","icc-cricket.com","bcci.tv",
    "sportstar.thehindu.com",
    "naidunia.com","haribhoomi.com","patrika.com","bhaskar.com",
    "jagran.com","etvbharat.com",
}
CATEGORIES=("breaking","sports")
REGIONS=("cg","india")
LANGUAGES=("hi","en")
NOW=lambda: dt.datetime.now(UTC)

def trusted(value):
    try:
        u=urllib.parse.urlsplit(value)
        host=(u.hostname or "").lower()
        return u.scheme=="https" and u.username is None and any(
            host==h or host.endswith("."+h) for h in APPROVED)
    except ValueError:
        return False

def google_rss(category,region,language):
    hindi=language=="hi"
    if category=="sports":
        phrase=("छत्तीसगढ़ खेल समाचार" if region=="cg" else "भारत खेल समाचार") if hindi else (
            "Chhattisgarh sports news" if region=="cg" else "India sports news")
    else:
        phrase=("छत्तीसगढ़ ताजा समाचार" if region=="cg" else "भारत प्रमुख समाचार") if hindi else (
            "Chhattisgarh latest news" if region=="cg" else "India latest news")
    return "https://news.google.com/rss/search?"+urllib.parse.urlencode({
        "q":phrase+" when:3d","hl":"hi" if hindi else "en-IN",
        "gl":"IN","ceid":"IN:"+("hi" if hindi else "en")
    })

def fetch_xml(url,opener=urllib.request.urlopen):
    req=urllib.request.Request(url,headers={
        "User-Agent":"Mozilla/5.0 (compatible; SBrowserNewsDigest/1.8)",
        "Accept":"application/rss+xml,application/xml,text/xml"})
    with opener(req,timeout=9) as response:
        data=response.read(650001)
        if len(data)>650000:
            raise ValueError("RSS exceeded size limit")
    if b"<!DOCTYPE" in data.upper() or b"<!ENTITY" in data.upper():
        raise ValueError("Unexpected DTD")
    return ET.fromstring(data)

def dated(value):
    if not value: return None
    try:
        result=email.utils.parsedate_to_datetime(value)
        if result.tzinfo is None:result=result.replace(tzinfo=UTC)
        return result.astimezone(UTC)
    except (TypeError,ValueError,OverflowError):
        return None

def collect(root,now,official=False):
    seen=set()
    out=[]
    for node in root.findall(".//item"):
        title=" ".join((node.findtext("title") or "").split()).strip()
        url=(node.findtext("link") or "").strip()
        when=dated(node.findtext("pubDate") or "")
        source=node.find("source")
        publisher=(source.text or "").strip() if source is not None else ""
        publisher_url=(source.attrib.get("url") or "") if source is not None else ""
        if official:
            publisher="Press Information Bureau"
            publisher_url="https://pib.gov.in/"
        if not (14<=len(title)<=240 and when and publisher):
            continue
        age=(now-when).total_seconds()
        if not (-300<=age<=3*86400):
            continue
        if not trusted(publisher_url):
            continue
        # Google News links can legitimately be a publisher-redirect gateway.
        parsed=urllib.parse.urlsplit(url)
        if parsed.scheme!="https" or not parsed.hostname:
            continue
        if official and not trusted(url):
            continue
        identity=(title.casefold(),publisher.casefold())
        if identity in seen:continue
        seen.add(identity)
        out.append({"title":title,"publisher":publisher,"publisher_url":publisher_url,
                    "url":url,"published_ms":int(when.timestamp()*1000)})
        if len(out)>=18:break
    return sorted(out,key=lambda x:x["published_ms"],reverse=True)

def get_feed(category,region,language,now,fetcher=fetch_xml):
    rows=[]
    try:
        rows=collect(fetcher(google_rss(category,region,language)),now)
    except Exception as exc:
        print("Google RSS unavailable:",str(exc)[:100],file=sys.stderr)
    # PIB is an independent official backup source for India general news.
    if category=="breaking" and region=="india":
        try:
            pib_url=("https://pib.gov.in/RssMain.aspx?ModId=6&Lang="+
                     ("2" if language=="hi" else "1")+"&Regid=3")
            rows+=collect(fetcher(pib_url),now,official=True)
        except Exception as exc:
            print("PIB RSS unavailable:",str(exc)[:100],file=sys.stderr)
    seen=set()
    unique=[]
    for row in sorted(rows,key=lambda x:x["published_ms"],reverse=True):
        signature=row["title"].casefold()
        if signature not in seen:
            unique.append(row);seen.add(signature)
        if len(unique)>=18:break
    return unique

def produce(previous,now,fetcher=fetch_xml):
    old=(previous or {}).get("feeds",{})
    result={"schema":1,"updated_ms":int(now.timestamp()*1000),"feeds":{}}
    for category in CATEGORIES:
        for region in REGIONS:
            for language in LANGUAGES:
                key=f"{category}_{region}_{language}"
                try:
                    items=get_feed(category,region,language,now,fetcher)
                    if not items:
                        raise ValueError("No dated, trusted RSS stories")
                    result["feeds"][key]={
                        "fetched_ms":int(now.timestamp()*1000),"stories":items}
                    print(key,len(items),"fresh attributed headlines")
                except Exception as exc:
                    original=old.get(key,{})
                    age=int(now.timestamp()*1000)-original.get("fetched_ms",0)
                    if original.get("stories") and 0<=age<=12*3600000:
                        result["feeds"][key]=original
                        print(key,"kept previous dated digest:",str(exc)[:90])
                    else:
                        result["feeds"][key]={"fetched_ms":0,"stories":[]}
                        print(key,"unavailable:",str(exc)[:100])
    return result

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--output",default="website/s-browser-news/latest.json")
    args=parser.parse_args()
    output=pathlib.Path(args.output)
    try:previous=json.loads(output.read_text(encoding="utf-8"))
    except (FileNotFoundError,json.JSONDecodeError):previous={}
    data=produce(previous,NOW())
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(data,ensure_ascii=False,
        indent=2,sort_keys=True)+"\n",encoding="utf-8")
    print("Published digest with",sum(bool(x["stories"]) for x in data["feeds"].values()),
          "populated category feeds")

if __name__=="__main__":main()
