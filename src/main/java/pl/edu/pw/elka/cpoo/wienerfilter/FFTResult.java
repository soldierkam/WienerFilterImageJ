/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.pw.elka.cpoo.wienerfilter;

import ij.process.ColorProcessor;
import ij.process.FHT;
import ij.process.ImageProcessor;

/**
 *
 * @author soldier
 */
public class FFTResult extends FHT{

    public FFTResult(ImageProcessor ip) {
        super(ip);
    }

    @Override
    public FHT divide(FHT fht) {
        ColorProcessor cp = this.rgb;
        FHT result = new FFTResult(super.divide(fht));
        result.rgb = cp;
        return result;
    }

    @Override
    public FHT multiply(FHT fht) {
        ColorProcessor cp = this.rgb;
        FHT result = new FFTResult(super.multiply(fht));
        result.rgb = cp;
        return result;
    }
    
    
}
