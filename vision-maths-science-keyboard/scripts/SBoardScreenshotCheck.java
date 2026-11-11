import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Pixel-only Android IME screenshot test; no OCR or accessibility assumptions. */
public final class SBoardScreenshotCheck {
    private static void fail(String message){throw new AssertionError("FAIL: "+message);}
    private static boolean dark(int pixel){
        return ((pixel>>16)&255)<22 && ((pixel>>8)&255)<22 && (pixel&255)<22;
    }
    private static boolean neonCyan(int pixel){
        int r=(pixel>>16)&255, g=(pixel>>8)&255, b=pixel&255;
        return g>150 && b>185 && b>r+55 && g>r+60;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1) fail("Pass captured screen PNG path");
        BufferedImage screenshot=ImageIO.read(new File(args[0]));
        if(screenshot==null) fail("ImageIO could not open screenshot");
        int width=screenshot.getWidth(),height=screenshot.getHeight();
        if(width<=height)fail("Expected a real landscape screen but got "+width+"x"+height);
        System.out.println("SCREEN: "+width+" x "+height);
        int navigationTop=-1;
        for(int y=(int)(height*.66);y<height-10;y++){
            int darkSamples=0;
            for(int j=0;j<16;j++){
                int x=(int)(width*(.07+.86*j/15.0));
                if(dark(screenshot.getRGB(x,y)))darkSamples++;
            }
            if(darkSamples>=14){navigationTop=y;break;}
        }
        if(navigationTop<0)fail("Could not identify system navigation region");
        List<Integer> wideNeonRows=new ArrayList<>();
        int left=(int)(width*.32),right=(int)(width*.72);
        for(int y=(int)(height*.56);y<navigationTop;y++){
            int total=0,cyan=0;
            for(int x=left;x<right;x+=4){
                total++;
                if(neonCyan(screenshot.getRGB(x,y)))cyan++;
            }
            if(cyan>=total*.65)wideNeonRows.add(y);
        }
        List<int[]> bands=new ArrayList<>();
        for(int y:wideNeonRows){
            if(bands.isEmpty()||y>bands.get(bands.size()-1)[1]+1)
                bands.add(new int[]{y,y});
            else bands.get(bands.size()-1)[1]=y;
        }
        for(int[] band:bands)System.out.println("NEON BAND: "+band[0]+".."+band[1]);
        if(bands.size()<2)fail("Wide Spacebar outline is missing");
        int[] bottom=bands.get(bands.size()-1);
        int[] top=null;
        for(int i=bands.size()-2;i>=0;i--){
            int[] previous=bands.get(i);
            if(previous[1]-previous[0]>=1 && bottom[0]-previous[1]>height*.09){
                top=previous;break;
            }
        }
        if(top==null)fail("Could not see Spacebar top and bottom cyan borders");
        int gap=navigationTop-bottom[1];
        System.out.println("SPACEBAR TOP: "+top[0]);
        System.out.println("SPACEBAR BOTTOM: "+bottom[1]);
        System.out.println("NAVIGATION START: "+navigationTop);
        System.out.println("CLEARANCE: "+gap+"px");
        if(gap<Math.max(8,(int)(height*.015)))
            fail("Spacebar bottom cyan border is hidden by Android navigation bar");
        if(bottom[1]-top[0]<height*.09)
            fail("Spacebar row is vertically collapsed or cut");
        System.out.println("PASS: Full Spacebar row bottom border visible above Android navigation bar");
    }
}
