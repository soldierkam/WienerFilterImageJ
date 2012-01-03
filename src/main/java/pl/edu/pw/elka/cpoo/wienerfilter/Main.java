package pl.edu.pw.elka.cpoo.wienerfilter;

import ij.ImagePlus;
import ij.Undo;
import ij.gui.GenericDialog;
import ij.measure.Measurements;
import ij.plugin.filter.PlugInFilter;
import ij.process.FHT;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Rectangle;
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
    private boolean padded;
    private int originalWidth;
    private int originalHeight;

    public Main() {
    }

    public void run(ImageProcessor ip) {
        int[] pixels = (int[]) ip.getPixels();
        int width = ip.getWidth();
        Rectangle r = ip.getRoi();

        ImageProcessor fft = doFFT(ip);
        //TODO: implement
        ImageProcessor out = doInvFFT(fft);
        ImagePlus outImage = new ImagePlus("Wiener out " + imagePlus.getTitle(), out);
        outImage.setCalibration(imagePlus.getCalibration());
        outImage.show();
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

    protected ImageProcessor doFFT(ImageProcessor imageProcessor) {
        FHT fft = new FHT(pad(imageProcessor));
        LOG.info("FFT: transform...");
        fft.transform();
        LOG.info("FFT: done");
        return fft;
    }

    protected ImageProcessor doInvFFT(ImageProcessor imageProcessor) {
        FHT fft = new FHT(imageProcessor);
        LOG.info("FFT: invert transformation...");
        fft.inverseTransform();
        LOG.info("FFT: done");
        return fft;
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

    ImageProcessor pad(ImageProcessor ip) {
        originalWidth = ip.getWidth();
        originalHeight = ip.getHeight();
        int maxN = Math.max(originalWidth, originalHeight);
        int i = 2;
        while (i < maxN) {
            i *= 2;
        }
        if (i == maxN && originalWidth == originalHeight) {
            padded = false;
            return ip;
        }
        maxN = i;
        LOG.info("Padding to " + maxN + "x" + maxN);
        ImageStatistics stats = ImageStatistics.getStatistics(ip, MEAN, null);
        ImageProcessor ip2 = ip.createProcessor(maxN, maxN);
        ip2.setValue(stats.mean);
        ip2.fill();
        ip2.insert(ip, 0, 0);
        padded = true;
        Undo.reset();
        //new ImagePlus("padded", ip2.duplicate()).show();
        return ip2;
    }
}
