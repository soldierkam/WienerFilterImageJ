package pl.edu.pw.elka.cpoo.wienerfilter;

import ij.Undo;
import ij.measure.Measurements;
import ij.process.ColorProcessor;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author soldier
 */
public class FFTUtilImpl implements Measurements, FFTUtil{

    private final static Logger LOG = LoggerFactory.getLogger(FFTUtilImpl.class);
    private boolean padded;
    private int originalWidth;
    private int originalHeight;
    private int imgBitDepth;

    public FFTUtilImpl(int bitDepth) {
        this.imgBitDepth = bitDepth;
    }

    @Override
    public FHT doFFT(ImageProcessor imageProcessor) {
        FHT fft = newFHT(imageProcessor);
        LOG.info("FFT: transform...");
        fft.transform();
        LOG.info("FFT: done");
        return fft;
    }

    @Override
    public ImageProcessor doInvFFT(FHT imageProcessor) {
        FHT fft = imageProcessor;
        LOG.info("FFT: invert transformation...");
        fft.inverseTransform();
        ImageProcessor imgOut = fft;
        if (originalWidth > 0) {
            fft.setRoi(0, 0, originalWidth, originalHeight);
            LOG.info("Crop image " + originalWidth + "x" + originalHeight);
            imgOut = fft.crop();
        }
        int bitDepth = fft.originalBitDepth > 0 ? fft.originalBitDepth : imgBitDepth;
        switch (bitDepth) {
            case 8:
                imgOut = imgOut.convertToByte(false);
                break;
            case 16:
                imgOut = imgOut.convertToShort(false);
                break;
            case 24:
                if (fft.rgb == null) {
                    throw new IllegalStateException("RGB is null");
                }
                ColorProcessor rgb = (ColorProcessor) fft.rgb.duplicate();
                rgb.setBrightness((FloatProcessor) imgOut);
                imgOut = rgb;
                fft.rgb = null;
                break;
            case 32:
                break;
        }
        if (bitDepth != 24 && fft.originalColorModel != null) {
            imgOut.setColorModel(fft.originalColorModel);
        }
        LOG.info("FFT: done");
        return imgOut;
    }

    private FHT newFHT(ImageProcessor ip) {
        FHT fht;
        if (ip instanceof ColorProcessor) {
            LOG.info("Extract color");
            ImageProcessor ip2 = ((ColorProcessor) ip).getBrightness();
            fht = new FHT(pad(ip2));
            fht.rgb = (ColorProcessor) ip.duplicate(); // save so we can later update the brightness
        } else {
            fht = new FHT(pad(ip));
        }
        if (padded) {
            fht.originalWidth = originalWidth;
            fht.originalHeight = originalHeight;
        }
        fht.originalBitDepth = imgBitDepth;
        fht.originalColorModel = ip.getColorModel();
        return fht;
    }

    private ImageProcessor pad(ImageProcessor ip) {
        originalWidth = ip.getWidth();
        originalHeight = ip.getHeight();
        LOG.info("Need padding from " + originalWidth + "x" + originalHeight + "?");

        int maxN = Math.max(originalWidth, originalHeight);
        int i = 2;
        while (i < maxN) {
            i *= 2;
        }
        ImageProcessor result;
        if (i == maxN && originalWidth == originalHeight) {
            padded = false;
            result = ip;
        } else {
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
            result = ip2;
        }

        return result;
    }
}
