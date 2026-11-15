import com.saurabhsvision.sbrowser.GovtSources;
import java.util.List;

public class GovtSourcesTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static boolean includes(String id,List<GovtSources.Source> list){
        for(GovtSources.Source s:list)if(s.id.equals(id))return true;
        return false;
    }
    public static void main(String[] args){
        check(GovtSources.DISTRICTS.length==33,"33 Chhattisgarh districts");
        check(GovtSources.OFFICIAL.size()>=20,"20 official sources");
        check("Durg".equals(GovtSources.normalizedArea("Bhilai")),
            "Bhilai maps to Durg");
        check("Mahasamund".equals(GovtSources.normalizedArea("Saraipali")),
            "Saraipali maps to Mahasamund");
        check(includes("durg",GovtSources.filter("Bhilai","District jobs","teacher")),
            "Teacher query must preserve Durg official portal");
        check(includes("mahasamund",GovtSources.filter("Saraipali","District jobs","driver")),
            "Saraipali maps to Mahasamund official notice portal");
        check(includes("rrb",GovtSources.filter("All Chhattisgarh","Railway","ALP")),
            "Railway search includes RRB");
        check(!includes("rrb",GovtSources.filter("All Chhattisgarh","School admissions","")),
            "Railway sources absent from school admissions filter");
        check(includes("jee_main",GovtSources.filter("Raipur","Engineering exams","JEE")),
            "National JEE portal visible even for a chosen city");
        check(includes("navodaya",GovtSources.filter("Bhilai","School admissions","")),
            "Navodaya sources available across Chhattisgarh");
        check(includes("ntse",GovtSources.filter("All Chhattisgarh","School admissions","")),
            "NTSE links to NCERT only; no active status claims");
        for(GovtSources.Source s:GovtSources.OFFICIAL){
            check(s.url.startsWith("https://"),"HTTPS official URL: "+s.id);
            check(!s.host().isEmpty(),"URL host: "+s.id);
        }
        String url=GovtSources.officialSearch(GovtSources.OFFICIAL.get(0),"teacher vacancy");
        check(url.startsWith("https://www.google.com/search?q="),"HTTPS search");
        check(url.contains("site%3Araipur.gov.in"),"Official host restriction");
        String district=GovtSources.districtSearch("Surguja","recruitment");
        check(district.contains("site%3Agov.in"),"Government domain restriction");
        System.out.println("PASS official source directory, 33 districts, city mappings and searches");
    }
}
