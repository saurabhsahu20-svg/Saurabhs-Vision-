import com.saurabhsvision.sbrowser.GriflyVersion;

public final class GriflyVersionTest {
    public static void main(String[] args){
        assert GriflyVersion.fromTag("grifly-v1.9.2-vision-site-study-dashboard-beta")!=null;
        assert GriflyVersion.fromTag("s-browser-v1.9.2-beta")==null;
        assert GriflyVersion.fromTag("grifly-v1.9.2-other")!=null;
        assert GriflyVersion.fromTag("grifly-v1.9.2.apk")==null;
        assert GriflyVersion.fromInstalled("1.9.2-beta")!=null;
        assert GriflyVersion.fromInstalled("unknown")==null;
        assert GriflyVersion.compare(GriflyVersion.fromTag("grifly-v1.10.0-beta"),
               GriflyVersion.fromInstalled("1.9.9-beta"))>0;
        assert GriflyVersion.compare(GriflyVersion.fromTag("grifly-v2.0.0-beta"),
               GriflyVersion.fromInstalled("1.99.99-beta"))>0;
        assert GriflyVersion.compare(GriflyVersion.fromTag("grifly-v1.9.2-beta"),
               GriflyVersion.fromInstalled("1.9.2-beta"))==0;
        assert GriflyVersion.compare(GriflyVersion.fromTag("grifly-v1.9.1-beta"),
               GriflyVersion.fromInstalled("1.9.2-beta"))<0;
        System.out.println("PASS: Grifly version tag filtering, comparisons, and invalid releases");
    }
}
