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
import java.lang.Math;
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

        FloatProcessor fft = (FloatProcessor) doFFT(ip);
        
        double sigma = 1;
		double gamma = 1;
		double alpha = 1;
		int [][] table = new int [10][10];
	
		for (int i = 0; i < 10; ++i) {
			for (int j = 0; j < 10; ++j) {
				table[i][j] = 1;
			}
		}	
		fft = (FloatProcessor) wierner(fft,table, sigma, gamma, alpha);
        ImageProcessor out = doInvFFT(fft);
        ImagePlus outImage = new ImagePlus("Wiener out " + imagePlus.getTitle(), out);
        outImage.setCalibration(imagePlus.getCalibration());
        outImage.show();
    }

    
	public ImageProcessor wierner(FloatProcessor fft,int[][] ht, double sigma,
			double gamma, double alpha) {

		int x = fft.getWidth();
		int y = fft.getHeight();
		
		FloatProcessor h = new FloatProcessor(x, y);

		double[][] table = new double[10][10];
		for (int i = 0; i < 10; ++i) {
			for (int j = 0; j < 10; ++j) {
				table[i][j] = ht[i][j] / 16;
			}
		}
		this.setFloatPixels(h, table);

		double[][] Yf = this.getFloatPixels(fft);
		double[][] Hf = this.getFloatPixels((FloatProcessor) doFFT(h));
		
		double[][] sHf = new double[x][y];
		double[][] iHf = new double[x][y];
		double[][] Pyf = new double[x][y];
		double[][] eXf = new double[x][y];
		double[][] Gf = new double[x][y];

	
		for (int i = 0; i < x; ++i) {
			for (int j = 0; j < y; ++j) {
				if (Hf[i][j] == 0 || Hf[i][j] < 0) {
					sHf[i][j] = 1 / gamma;
				} else {
					sHf[i][j] = Hf[i][j];
				}
				iHf[i][j] = 1 / sHf[i][j];

				if (Math.abs(Hf[i][j]) * gamma > 1) {
					iHf[i][j] = iHf[i][j] * Hf[i][j];
				} else {
					iHf[i][j] = iHf[i][j] * gamma * Math.abs(sHf[i][j]);

					Pyf[i][j] = Math.pow(Math.abs(Yf[i][j]), 2) / x * x;
					;

					if (Pyf[i][j] > Math.pow(sigma, 2)) {
					} else {
						Pyf[i][j] = Pyf[i][j] * sigma * sigma;
					}
					Gf[i][j] = (iHf[i][j] * (Pyf[i][j] - (sigma * sigma)))
							/ (Pyf[i][j] - ((1 - alpha) * sigma * sigma));

					eXf[i][j] = Gf[i][j] * Yf[i][j];
				}

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
		
		this.setFloatPixels(fft, eXf);
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

    
    protected double[][] getFloatPixels(FloatProcessor ip) {
	      float[] pix = (float[]) ip.getPixels();
	      double[][] pix2d = new double[ip.getWidth()][ip.getHeight()];
	      for (int j=0;j<ip.getHeight();j++) {
	        int offs = j*ip.getWidth();
	        for (int i=0;i<ip.getWidth();i++) {
	          pix2d[i][j] = (double)pix[offs+i];
	        }
	      }
	      return pix2d;
	    }

	    /* set block of float pixels */
	    protected void setFloatPixels(FloatProcessor ip, double[][] dp) {
	      FloatProcessor fp = new FloatProcessor(dp.length,dp[0].length);
	      float[] pix = new float[dp.length*dp[0].length];
	      for (int j=0;j<dp.length;j++) {
	        int offs = j*dp.length;
	        for (int i=0;i<dp.length;i++) {
	          pix[offs+i] = (float) (dp[i][j]);
	        }
	      }
	      fp.setPixels(pix);
	      ip.insert(fp,0,0);
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
