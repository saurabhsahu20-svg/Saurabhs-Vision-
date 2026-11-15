package com.saurabhsvision.sbrowser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Official portal directory, not an unverified list of "open" vacancies. */
public final class GovtSources {
    private GovtSources(){}

    public static final class Source {
        public final String id, title, authority, category, area, url, note;
        Source(String id,String title,String authority,String category,String area,
               String url,String note){
            this.id=id;this.title=title;this.authority=authority;
            this.category=category;this.area=area;this.url=url;this.note=note;
        }
        public String host(){
            try{return new java.net.URI(url).getHost().toLowerCase(Locale.ROOT);}
            catch(Exception e){return "";}
        }
    }

    public static final String[] CITIES={"All Chhattisgarh",
        "Raipur","Bhilai","Durg","Rajnandgaon","Bilaspur","Dhamtari",
        "Mahasamund","Saraipali","Raigarh","Other CG district"};
    public static final String[] DISTRICTS={"Balod","Baloda Bazar-Bhatapara",
        "Balrampur-Ramanujganj","Bastar","Bemetara","Bijapur","Bilaspur",
        "Dantewada","Dhamtari","Durg","Gariaband","Gaurela-Pendra-Marwahi",
        "Janjgir-Champa","Jashpur","Kabirdham","Kanker","Khairagarh-Chhuikhadan-Gandai",
        "Kondagaon","Korba","Koriya","Mahasamund","Manendragarh-Chirmiri-Bharatpur",
        "Mohla-Manpur-Ambagarh Chowki","Mungeli","Narayanpur","Raigarh","Raipur",
        "Rajnandgaon","Sakti","Sarangarh-Bilaigarh","Sukma","Surajpur","Surguja"};

    public static final String[] TYPES={"All","District jobs","CG Govt",
        "Railway","Central Govt","Engineering exams","School admissions"};

    public static final List<Source> OFFICIAL;
    static {
        ArrayList<Source> a=new ArrayList<>();
        // URLs below are direct public recruitment / authority portals.
        a.add(new Source("raipur","Raipur recruitment","District Raipur","District jobs","Raipur",
            "https://raipur.gov.in/en/notice_category/recruitment/","District recruitment notices and results"));
        a.add(new Source("durg","Bhilai & Durg recruitment","District Durg","District jobs","Durg",
            "https://durg.gov.in/en/notice_category/recruitment/","Bhilai is within Durg district"));
        a.add(new Source("rajnandgaon","Rajnandgaon recruitment","District Rajnandgaon",
            "District jobs","Rajnandgaon","https://rajnandgaon.gov.in/en/notice_category/recruitment/",
            "District-level notices"));
        a.add(new Source("bilaspur","Bilaspur recruitment","District Bilaspur",
            "District jobs","Bilaspur","https://bilaspur.gov.in/en/notice_category/recruitment/",
            "District-level notices"));
        a.add(new Source("dhamtari","Dhamtari recruitment","District Dhamtari",
            "District jobs","Dhamtari","https://dhamtari.gov.in/en/notice_category/recruitment/",
            "District-level notices"));
        a.add(new Source("mahasamund","Mahasamund & Saraipali recruitment","District Mahasamund",
            "District jobs","Mahasamund","https://mahasamund.gov.in/en/notice_category/recruitment/",
            "Saraipali recruitment can appear on the Mahasamund district portal"));
        a.add(new Source("raigarh","Raigarh recruitment","District Raigarh",
            "District jobs","Raigarh","https://raigarh.gov.in/en/notice_category/recruitment/",
            "District-level notices"));
        a.add(new Source("cgvyapam","CG Vyapam / Selection Board","CG Vyapam","CG Govt",
            "Chhattisgarh","https://vyapamcg.cgstate.gov.in/","Recruitment exams, notices, results"));
        a.add(new Source("cgpsc","CG Public Service Commission","CGPSC","CG Govt",
            "Chhattisgarh","https://psc.cg.gov.in/","Adverts, recruitment notices, results"));
        a.add(new Source("cghealth","CG Medical Education recruitment","CG Medical Education",
            "CG Govt","Chhattisgarh","https://cgcme.cgstate.gov.in/noticeboard/recruitment",
            "Health and medical recruitment notices"));
        a.add(new Source("rrb","Railway Recruitment Board Bilaspur","RRB Bilaspur",
            "Railway","Chhattisgarh","https://rrbbilaspur.gov.in/recruitment.html",
            "Railway central employment notices (CENs)"));
        a.add(new Source("upsc","Union Public Service Commission","UPSC",
            "Central Govt","India","https://upsc.gov.in/","Central government examinations"));
        a.add(new Source("ssc","Staff Selection Commission","SSC",
            "Central Govt","India","https://ssc.gov.in/","Government recruitment and exam notices"));
        a.add(new Source("employment","National Career Service","Ministry of Labour",
            "Central Govt","India","https://www.ncs.gov.in/","Government career portal: review each employer independently"));
        a.add(new Source("jee_main","JEE Main","National Testing Agency",
            "Engineering exams","India","https://jeemain.nta.nic.in/","JEE Main official notices and applications"));
        a.add(new Source("jee_advanced","JEE Advanced","IIT JEE Advanced",
            "Engineering exams","India","https://jeeadv.ac.in/","JEE Advanced eligibility and important notices"));
        a.add(new Source("navodaya","Navodaya Vidyalaya (JNVST)","Navodaya Vidyalaya Samiti",
            "School admissions","India","https://navodaya.gov.in/","School admission and selection-test updates"));
        a.add(new Source("navodaya_form","JNVST admission notices","NVS / NIC",
            "School admissions","India","https://cbseitms.rcil.gov.in/nvs","Admissions, instructions and admit cards"));
        a.add(new Source("sainik","Sainik School entrance (AISSEE)","NTA / Sainik Schools",
            "School admissions","India","https://exams.nta.nic.in/sainik-school-society/",
            "Entrance applications and scorecards"));
        a.add(new Source("ntse","NTSE official NCERT page","NCERT",
            "School admissions","India","https://ncert.nic.in/","NTSE schedule/status must be checked; no active round implied"));
        OFFICIAL=Collections.unmodifiableList(a);
    }

    public static String normalizedArea(String selected){
        if(selected==null)return "";
        if(selected.equalsIgnoreCase("Bhilai"))return "Durg";
        if(selected.equalsIgnoreCase("Saraipali"))return "Mahasamund";
        return selected;
    }
    public static List<Source> filter(String area,String category,String query){
        String place=normalizedArea(area);
        String type=category==null?"All":category;
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        ArrayList<Source> items=new ArrayList<>();
        for(Source source:OFFICIAL){
            boolean fitsType=type.equals("All")||source.category.equals(type);
            boolean isGlobal=source.area.equals("Chhattisgarh")||source.area.equals("India");
            boolean fitsCity=place==null||place.isEmpty()||place.equals("All Chhattisgarh")||
                isGlobal||source.area.equalsIgnoreCase(place);
            boolean fitsQuery=q.isEmpty()||
                (source.title+" "+source.authority+" "+source.note+" "+
                source.area+" "+source.category).toLowerCase(Locale.ROOT).contains(q);
            if(fitsType&&fitsCity&&fitsQuery)items.add(source);
        }
        return items;
    }

    /** Restricts web queries to official hosts, not unverified job-ad sites. */
    public static String officialSearch(Source source,String query){
        String q=query==null?"":query.trim();
        String search="site:"+source.host()+" (recruitment OR vacancy OR notice OR admission)";
        if(!q.isEmpty())search+=" "+q;
        return "https://www.google.com/search?q="+
            URLEncoder.encode(search,StandardCharsets.UTF_8);
    }
    public static String districtSearch(String district,String keywords){
        String safeDistrict=district==null?"Chhattisgarh":district.replaceAll("[^A-Za-z0-9 -]"," ").trim();
        String search="site:gov.in recruitment "+safeDistrict+" Chhattisgarh";
        if(keywords!=null&&!keywords.trim().isEmpty())search+=" "+keywords.trim();
        return "https://www.google.com/search?q="+
            URLEncoder.encode(search,StandardCharsets.UTF_8);
    }
}
