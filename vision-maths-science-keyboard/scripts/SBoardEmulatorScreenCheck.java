import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Ensures the actual S Board light keyboard occupies the bottom of a landscape screenshot. */
public final class SBoardEmulatorScreenCheck {
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("Expected screenshot PNG");
        BufferedImage im=ImageIO.read(new File(args[0]));
        if(im==null) throw new AssertionError("Screenshot could not be decoded");
        int width=im.getWidth(), height=im.getHeight();
        if(width<=height) throw new AssertionError("Not landscape: "+width+"x"+height);
        int sampleCount=0,lightKeyboard=0,slate=0;
        for(int y=(int)(height*.57);y<(int)(height*.91);y+=5){
            for(int x=(int)(width*.09);x<(int)(width*.85);x+=5){
                int c=im.getRGB(x,y);
                int red=(c>>>16)&255,green=(c>>>8)&255,blue=c&255;
                sampleCount++;
                if(red>=215 && red<249 && green>=210 && green<250 &&
                    blue>=218 && blue<=252 && Math.abs(red-green)<12)lightKeyboard++;
                if(red>=88 && red<=155 && green>=88 && green<=160 &&
                    blue>=102 && blue<=180 && blue>=red)slate++;
            }
        }
        double lightRatio=(double)lightKeyboard/sampleCount;
        System.out.printf("Landscape %dx%d, panel samples %.1f%%, slate-key samples %d%n",
                          width,height,lightRatio*100.0,slate);
        if(lightRatio<.07 && slate<35)
            throw new AssertionError("S Board appears hidden or not rendered in landscape");
        System.out.println("PASS: S Board key panel visibly rendered in landscape.");
    }
}
