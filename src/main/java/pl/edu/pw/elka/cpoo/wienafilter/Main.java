package pl.edu.pw.elka.cpoo.wienafilter;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import java.awt.Rectangle;

/**
 *
 * @author soldier
 */
public class Main implements PlugInFilter {

    
    private ImagePlus imagePlus;

    public void run(ImageProcessor ip) {
        byte[] pixels = (byte[]) ip.getPixels();
        int width = ip.getWidth();
        Rectangle r = ip.getRoi();
        
    }

    public int setup(String string, ImagePlus ip) {
        this.imagePlus = ip;
        if("about".equals(string)){
            return DONE;
        }
        return DOES_RGB;
    }
}
