import com.saurabhsvision.sbrowser.ModelPaperCatalog;
import java.util.HashSet;

public final class ModelPaperCatalogTest {
    public static void main(String[] args){
        assert ModelPaperCatalog.SUBJECTS.length==6;
        assert ModelPaperCatalog.SERIES.length==3;
        assert ModelPaperCatalog.allPdfPaths().size()==66;
        HashSet<String> chosen=new HashSet<>();
        for(int subject=0;subject<6;subject++){
            for(int type=0;type<3;type++){
                for(int set=1;set<=5;set++){
                    String path=ModelPaperCatalog.path(subject,type,set);
                    assert ModelPaperCatalog.expectedPdf(path);
                    assert path.startsWith("Class_10/10_");
                    assert path.endsWith(".pdf");
                    chosen.add(path);
                }
            }
        }
        assert chosen.size()==66;
        assert !ModelPaperCatalog.expectedPdf("../model.pdf");
        assert !ModelPaperCatalog.expectedPdf("Class_10/10_Science/Set_06.pdf");
        assert !ModelPaperCatalog.expectedPdf("Class_10/10_Physics/Full75_Set_01.pdf");
        assert !ModelPaperCatalog.expectedPdf("Class_12/12_Mathematics/Full75_Set_01.pdf");
        assert ModelPaperCatalog.path(0,1,1).equals(ModelPaperCatalog.path(0,1,5));
        assert ModelPaperCatalog.friendlyName(2,0,3).endsWith("75_Marks_Set_03.pdf");
        System.out.println("PASS: 66 expected CGBSE Class 10 paths, strict naming and three series.");
    }
}
