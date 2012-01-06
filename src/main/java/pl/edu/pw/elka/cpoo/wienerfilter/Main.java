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
    private boolean debug = true;

    public Main() {
    }

    public void run(ImageProcessor ip) {

        FFTUtil fftUtil = new FFTUtilImpl(imagePlus.getBitDepth());
        FHT fft = fftUtil.doFFT(ip);

        double sigma = 1;
        double gamma = 1;
        double alpha = 1;
        int[][] table = new int[10][10];

        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                table[i][j] = 1;
            }
        }
        fft = wierner(fft, table, sigma, gamma, alpha);
        ImageProcessor out = fftUtil.doInvFFT(fft);
        ImagePlus outImage = new ImagePlus("Wiener out " + imagePlus.getTitle(), out);
        outImage.setCalibration(imagePlus.getCalibration());
        outImage.show();
        IJ.showProgress(1.0);
    }

    public FHT wierner(FHT fft, int[][] ht, double sigma,
            double gamma, double alpha) {

        int x = fft.getWidth();
        int y = fft.getHeight();
        double sigmaPower = Math.pow(sigma, 2);
        FloatProcessor h = new FloatProcessor(x, y);

        double[][] table = new double[10][10];
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                table[i][j] = ht[i][j] / 16;
            }
        }
        setFloatPixels(h, table);

        double[][] Yf = getFloatPixels(fft);
        double[][] Hf = getFloatPixels(new FFTUtilImpl(0).doFFT(h));

        double[][] sHf = new double[x][y];
        double[][] iHf = new double[x][y];
        double[][] Pyf = new double[x][y];
        double[][] eXf = new double[x][y];
        double[][] Gf = new double[x][y];


        for (int i = 0; i < x; ++i) {
            for (int j = 0; j < y; ++j) {
                
                //sHf = Hf.*(abs(Hf)>0)+1/gamma*(abs(Hf)==0);
                if (Hf[i][j] == 0 || Hf[i][j] < 0) {
                    sHf[i][j] = 1 / gamma;
                } else {
                    sHf[i][j] = Hf[i][j];
                }
                iHf[i][j] = 1 / sHf[i][j];

                //iHf = iHf.*(abs(Hf)*gamma>1)+gamma*abs(sHf).*iHf.*(abs(sHf)*gamma<=1); 
                if (Math.abs(Hf[i][j]) * gamma > 1) {
                    //iHf[i][j] = iHf[i][j] * Hf[i][j];
                } else {
                    iHf[i][j] = gamma * Math.abs(sHf[i][j]) * iHf[i][j];
                }

                //Pyf = abs(Yf).^2/SIZE^2;
                Pyf[i][j] = Math.pow(Math.abs(Yf[i][j]), 2) / (x * x);

                //Pyf = Pyf.*(Pyf>sigma^2)+sigma^2*(Pyf<=sigma^2);
                if (Pyf[i][j] > sigmaPower) {
                } else {
                    Pyf[i][j] = Pyf[i][j] * sigmaPower;
                }
                //Gf = iHf.*(Pyf-sigma^2)./(Pyf-(1-alpha)*sigma^2);
                Gf[i][j] = (iHf[i][j] * (Pyf[i][j] - sigmaPower))
                        / (Pyf[i][j] - ((1 - alpha) * sigmaPower));

                eXf[i][j] = Gf[i][j] * Yf[i][j];

            }
        }

        /*
         * 
         * 
         * Gf = iHf.*(Pyf-sigma^2)./(Pyf-(1-alpha)*sigma^2);
         * 
         * % max(max(abs(Gf).^2)) % should be equal to gamma^2 % Restorated
         * image without denoising eXf = Gf.*Yf; ex = real(ifft2(eXf));
         * 
         * 
         * 
         * ewx = wienerFilter(real,table,sigma,gamma,alpha);
         * 
         * sHf = Hf.*(abs(Hf)>0)+1/gamma*(abs(Hf)==0); wartosci z 0 +1
         */

        setFloatPixels(fft, eXf);
        return fft;

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

    /* set block of float pixels */
    static protected void setFloatPixels(FloatProcessor ip, double[][] dp) {
        FloatProcessor fp = new FloatProcessor(dp.length, dp[0].length);
        float[] pix = new float[dp.length * dp[0].length];
        for (int j = 0; j < dp.length; j++) {
            int offs = j * dp.length;
            for (int i = 0; i < dp.length; i++) {
                pix[offs + i] = (float) (dp[i][j]);
            }
        }
        fp.setPixels(pix);
        ip.insert(fp, 0, 0);
    }
}
