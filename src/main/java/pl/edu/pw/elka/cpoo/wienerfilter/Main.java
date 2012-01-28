package pl.edu.pw.elka.cpoo.wienerfilter;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Measurements;
import ij.plugin.filter.PlugInFilter;
import ij.process.FHT;
import ij.process.FloatProcessor;
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
    private boolean blur = false;
    private static int blurMaskSize = 5;
    private static double K = 0;
    private static double std = 2.0;

    public Main() {
        LOG.debug("Create filter");
    }

    @Override
    public void run(ImageProcessor ip) {
        final GenericDialog gd = new GenericDialog("Wiena Filter Options");
        gd.addNumericField("K: ", K, 1);
        gd.addNumericField("Std: ", std, 1);
        gd.addNumericField("Blur mask size: ", blurMaskSize, 2);
        gd.addCheckbox("Blur", blur);
        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        K = gd.getNextNumber();
        std = gd.getNextNumber();
        blurMaskSize = (int) gd.getNextNumber();
        blur = gd.getNextBoolean();

        final FFTUtil fftUtil = new FFTUtilImpl(imagePlus.getBitDepth());
        final FloatProcessor blurMask = calcBlurMask(ip.getWidth(), ip.getHeight(), std);
        FHT fft = fftUtil.doFFT(ip);

        final String windowTitle;
        if (blur) {

            ImagePlus outImage1 = new ImagePlus("Gaussian Blur Mask "
                    + imagePlus.getTitle() + "std:" + std + "size" + blurMaskSize, blurMask);
            outImage1.setCalibration(imagePlus.getCalibration());
            outImage1.show();

            FHT fftOfBlurMask = calcFFT(blurMask);
            fft = fft.multiply(fftOfBlurMask);
            windowTitle = "Blurred out " + imagePlus.getTitle() + "std:" + std + "size" + blurMaskSize;

        } else {
        	FHT fft2 = fftUtil.doFFT(ip);
            FHT fftOfBlurMask = calcFFT(blurMask);
            fftOfBlurMask = fftOfBlurMask.multiply(fftOfBlurMask);
            FHT fftOfBlurMask2 = calcFFT(blurMask);
            fftOfBlurMask2 = fftOfBlurMask2.multiply(fftOfBlurMask2);
            fft = fft2.divide(fftOfBlurMask);
            if (K > 0) {
                fftOfBlurMask2.add(K);
                fftOfBlurMask = fftOfBlurMask.divide(fftOfBlurMask2);
                fft = fft.multiply(fftOfBlurMask);
            }

            windowTitle = "Wiener out " + imagePlus.getTitle() + "K:" + K;
        }

        final ImageProcessor out = fftUtil.doInvFFT(fft);

        ImagePlus outImage = new ImagePlus(windowTitle, out);
        outImage.setCalibration(imagePlus.getCalibration());
        outImage.show();
        IJ.showProgress(1.0);
    }

    private FHT calcFFT(ImageProcessor imgProc) {
        return new FFTUtilImpl(imagePlus.getBitDepth()).doFFT(imgProc);
    }

    private FloatProcessor calcBlurMask(int width, int height, double std) {
        final int x = blurMaskSize;
        final int y = blurMaskSize;
        float[] table = new float[x * y];

        int nr = blurMaskSize;
        int nc = blurMaskSize;
        double acum = 0;
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                final double dist;

                dist = ((i - nc / 2) * (i - nc / 2) + (j - nr / 2) * (j - nr / 2));

                table[i + j * y] = (float) Math.exp(-dist / (2 * std * std));
                acum += table[i + j * y];
            }
        }

        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                table[i + j * y] /= acum;
            }
        }
        
        FloatProcessor h = new FloatProcessor(width, height);
        FloatProcessor temp = new FloatProcessor(nr, nc);
        temp.setPixels(table);
        h.insert(temp, 0, 0);
        return h;
    }

    @Override
    public int setup(String string, ImagePlus ip) {
        LOG.info("Setup plugin");
        this.imagePlus = ip;
        if ("about".equals(string)) {
            return DONE;
        }
        if ("run".equals(string)) {
            if (ip == null) {
                return DONE;
            }
            run(ip.getProcessor());
            return DONE;
        }
        if (!"".equals(string)) {
            LOG.warn("Uknknown argument: " + string);
        }
        return DOES_RGB;
    }

    static protected double[][] getFloatPixels(FloatProcessor ip) {
        float[] pix = (float[]) ip.getPixels();
        double[][] pix2d = new double[ip.getWidth()][ip.getHeight()];
        for (int j = 0; j < ip.getHeight(); j++) {
            int offs = j * ip.getWidth();
            for (int i = 0; i < ip.getWidth(); i++) {
                pix2d[i][j] = (double) pix[offs + i];
            }
        }
        return pix2d;
    }

    /*
     * set block of float pixels
     */
    static protected void setFloatPixels(FloatProcessor ip, double[][] dp) {
        float[] pix = new float[ip.getWidth() * ip.getHeight()];
        for (int j = 0; j < dp.length; j++) {
            int offs = j * dp.length;
            for (int i = 0; i < dp.length; i++) {
                pix[offs + i] = (float) (dp[i][j]);
            }
        }
        ip.setPixels(pix);
    }
}
