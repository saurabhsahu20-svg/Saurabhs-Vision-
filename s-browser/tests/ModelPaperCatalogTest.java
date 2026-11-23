import com.saurabhsvision.sbrowser.ModelPaperCatalog;
import java.util.HashSet;
import java.util.Set;

public final class ModelPaperCatalogTest {
    public static void main(String[] args){
        assert ModelPaperCatalog.SUBJECTS.length==6;
        assert ModelPaperCatalog.CLASS12_SUBJECTS.length==12;
        assert ModelPaperCatalog.class10Paths().size()==66;
        assert ModelPaperCatalog.class12Paths().size()==72;
        assert ModelPaperCatalog.visionKaSaarPaths().size()==2;
        assert ModelPaperCatalog.allPdfPaths().size()==140;
        Set<String> selected=new HashSet<>();
        for(int i=0;i<6;i++){
            for(int kind=0;kind<3;kind++){
                for(int set=1;set<=5;set++){
                    String path=ModelPaperCatalog.path(10,i,kind,set);
                    assert ModelPaperCatalog.expectedPdf(path);
                    assert path.startsWith("Class_10/10_");
                    selected.add(path);
                }
            }
        }
        assert selected.size()==66;
        for(int i=0;i<12;i++)for(int kind=0;kind<2;kind++)
            for(int set=1;set<=5;set++){
                String path=ModelPaperCatalog.path(12,i,kind,set);
                assert ModelPaperCatalog.expectedPdf(path);
                selected.add(path);
            }
        assert selected.size()==138;
        String m=ModelPaperCatalog.path(10,2,3,1);
        String s=ModelPaperCatalog.path(10,4,3,5);
        assert m.equals("Class_10/10_Mathematics/Vision_Ka_Saar.pdf");
        assert s.equals("Class_10/10_Science/Vision_Ka_Saar.pdf");
        selected.add(m);selected.add(s);
        assert selected.equals(ModelPaperCatalog.allPdfPaths());
        assert ModelPaperCatalog.validImport(ModelPaperCatalog.class10Paths());
        assert ModelPaperCatalog.validImport(ModelPaperCatalog.class12Paths());
        assert ModelPaperCatalog.validImport(ModelPaperCatalog.visionKaSaarPaths());
        assert ModelPaperCatalog.validImport(ModelPaperCatalog.allPdfPaths());
        assert !ModelPaperCatalog.validImport(Set.of("Class_10/10_Science/Vision_Ka_Saar.pdf"));
        assert !ModelPaperCatalog.expectedPdf("../escape.pdf");
        assert !ModelPaperCatalog.expectedPdf("Class_12/12_Physics/Practice_Set_06.pdf");
        assert !ModelPaperCatalog.expectedPdf("Class_10/10_Physics/Full75_Set_01.pdf");
        assert ModelPaperCatalog.path(10,0,1,1).equals(ModelPaperCatalog.path(10,0,1,5));
        assert ModelPaperCatalog.path(12,0,1,1).equals(ModelPaperCatalog.path(12,0,1,5));
        assert ModelPaperCatalog.friendlyName(12,0,0,1).contains("Class_12");
        boolean invalidRejected=false;
        try{ModelPaperCatalog.path(10,0,3,1);}catch(IllegalArgumentException ok){invalidRejected=true;}
        assert invalidRejected;
        invalidRejected=false;
        try{ModelPaperCatalog.path(12,3,2,1);}catch(IllegalArgumentException ok){invalidRejected=true;}
        assert invalidRejected;
        invalidRejected=false;
        try{ModelPaperCatalog.path(9,0,0,1);}catch(IllegalArgumentException ok){invalidRejected=true;}
        assert invalidRejected;
        System.out.println("PASS: 140 strict PDF paths, 66/72/2 ZIP manifests, subject and grade safety.");
    }
}
