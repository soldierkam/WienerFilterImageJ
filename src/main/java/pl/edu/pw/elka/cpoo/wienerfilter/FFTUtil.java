package pl.edu.pw.elka.cpoo.wienerfilter;

import ij.process.FHT;
import ij.process.ImageProcessor;

/**
 *
 * @author soldier
 */
public interface FFTUtil {

    FHT doFFT(ImageProcessor imageProcessor);

    ImageProcessor doInvFFT(FHT imageProcessor);
    
}
