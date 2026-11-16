package com.saurabhsvision.sbrowser;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Date-constrained attributed news headline reader; no invented local news. */
public final class CityNewsFeed {
    public static final class Story {
        public final String title, source, url;
        public final long published;
        Story(String title,String source,String url,long published){
            this.title=title;this.source=source;this.url=url;this.published=published;
        }
    }
    public static final class Result {
        public final List<Story> stories;
        public final long checkedAt;
        public Result(List<Story> stories,long checkedAt){
            this.stories=Collections.unmodifiableList(stories);this.checkedAt=checkedAt;
        }
    }
    private CityNewsFeed(){}

    public static Result fetch(String city,boolean hindi,int days) throws Exception {
        String target=CityNewsSources.rssUrl(city,hindi,days);
        return fetchTrustedRss(target,days);
    }

    /** Reuse the same safe, dated RSS parser for Sports / Breaking headlines. */
    public static Result fetchTrustedRss(String target,int days) throws Exception {
        if(target==null)throw new IllegalArgumentException("Missing trusted news query");
        URL url=new URL(target);
        if(!"https".equals(url.getProtocol())||
            !"news.google.com".equalsIgnoreCase(url.getHost()))
            throw new IllegalArgumentException("Only trusted news feed host allowed");
        HttpURLConnection con=(HttpURLConnection)url.openConnection();
        con.setConnectTimeout(7500);con.setReadTimeout(7500);
        con.setRequestProperty("User-Agent","SBrowser-StudentNews/1.3");
        con.setRequestProperty("Accept","application/rss+xml, application/xml, text/xml");
        con.setInstanceFollowRedirects(true);
        try{
            if(con.getResponseCode()!=200)throw new Exception(
                "News service returned status "+con.getResponseCode());
            URL actual=con.getURL();
            if(!"https".equalsIgnoreCase(actual.getProtocol())||
                !"news.google.com".equalsIgnoreCase(actual.getHost()))
                throw new Exception("News feed moved to an unexpected server");
            byte[] buffer=new byte[4096];
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            try(InputStream in=con.getInputStream()){
                int n;
                while((n=in.read(buffer))!=-1){
                    if(output.size()+n>600000)
                        throw new Exception("Feed too large; try another city");
                    output.write(buffer,0,n);
                }
            }
            return parse(output.toByteArray(),days,System.currentTimeMillis());
        }finally{con.disconnect();}
    }

    public static Result parse(byte[] raw,int days,long now) throws Exception {
        XmlPullParser parser=Xml.newPullParser();
        // Refuse any document type declarations and entity-based documents.
        parser.setInput(new ByteArrayInputStream(raw),"UTF-8");
        ArrayList<Story> items=new ArrayList<>();
        LinkedHashSet<String> seen=new LinkedHashSet<>();
        boolean inItem=false,hasDoctype=false;
        String title="",source="",link="",date="";
        int event=parser.getEventType();
        while(event!=XmlPullParser.END_DOCUMENT){
            if(event==XmlPullParser.DOCDECL){
                hasDoctype=true;break;
            }
            if(event==XmlPullParser.START_TAG){
                String tag=parser.getName();
                if("item".equalsIgnoreCase(tag)){
                    inItem=true;title="";source="";link="";date="";
                }else if(inItem){
                    if("title".equalsIgnoreCase(tag))title=parser.nextText().trim();
                    else if("source".equalsIgnoreCase(tag))source=parser.nextText().trim();
                    else if("link".equalsIgnoreCase(tag))link=parser.nextText().trim();
                    else if("pubDate".equalsIgnoreCase(tag))date=parser.nextText().trim();
                }
            }else if(event==XmlPullParser.END_TAG&&
                "item".equalsIgnoreCase(parser.getName())){
                inItem=false;
                long published=publishedMillis(date);
                if(CityNewsSources.inWindow(published,now,days) &&
                    title.length()>8 && title.length()<500 &&
                    isWebUrl(link) && items.size()<80) {
                    String unique=title.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
                    if(seen.add(unique)){
                        items.add(new Story(title,source.isEmpty()?
                            "Publisher not specified":source,link,published));
                    }
                }
            }
            event=parser.next();
        }
        if(hasDoctype)throw new Exception("Unexpected XML document declaration");
        items.sort(Comparator.comparingLong((Story x)->x.published).reversed());
        if(items.size()>30)items=new ArrayList<>(items.subList(0,30));
        return new Result(items,now);
    }

    public static boolean isWebUrl(String value){
        if(value==null)return false;
        try{
            URL u=new URL(value);
            return ("https".equalsIgnoreCase(u.getProtocol()) ||
                    "http".equalsIgnoreCase(u.getProtocol())) &&
                u.getHost()!=null&&!u.getHost().isEmpty();
        }catch(Exception e){return false;}
    }
    public static long publishedMillis(String value){
        try{
            return ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant().toEpochMilli();
        }catch(Exception ignored){return -1L;}
    }
}
