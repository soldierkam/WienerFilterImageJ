package pl.edu.pw.elka.cpoo.wienerfilter;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Measurements;
import ij.plugin.filter.PlugInFilter;
import ij.process.FHT;
import ij.process.ImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author soldier
 */
public class Main implements PlugInFilter, Measurements {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);
    private ImagePlus imagePlus;
    private boolean debug = true;
    

    public Main() {
    }

    public void run(ImageProcessor ip) {
        FFTUtil fftUtil = new FFTUtilImpl(imagePlus.getBitDepth());
        
        FHT fft = fftUtil.doFFT(ip);
        //TODO: implement
        ImageProcessor out = fftUtil.doInvFFT(fft);
        ImagePlus outImage = new ImagePlus("Wiener out " + imagePlus.getTitle(), out);
        outImage.setCalibration(imagePlus.getCalibration());
        outImage.show();
        IJ.showProgress(1.0);
    }

    public int setup(String string, ImagePlus ip) {
        LOG.info("Setup plugin");
        this.imagePlus = ip;
        if ("about".equals(string)) {
            return DONE;
        }
        if ("options".equals(string)) {
            showDialog();
            return DONE;
        }
        if ("run".equals(string)) {
            run(ip.getProcessor());
            return DONE;
        }
        if (!"".equals(string)) {
            LOG.warn("Uknknown argument: " + string);
        }
        return DOES_RGB;
    }

    

    void showDialog() {
        LOG.info("Show dialog");
        GenericDialog gd = new GenericDialog("Wiena Filter Options");
        gd.setInsets(0, 20, 0);
        gd.addCheckbox("Debug", debug);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        debug = gd.getNextBoolean();
    }

}
