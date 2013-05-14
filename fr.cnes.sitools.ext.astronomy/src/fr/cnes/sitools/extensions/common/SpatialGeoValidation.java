/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.extensions.common;

import java.util.Map;

/**
 *
 * @author malapert
 */
public class SpatialGeoValidation extends NumberArrayValidation {
    
    private int indexLong;
    private int indexLat;
    private double[] raRange;
    private double[] decRange;
    private static final int MIN = 0;
    private static final int MAX = 1;
    
    public SpatialGeoValidation(final Validation validation, final String keyword, final int indexLong, final int indexLat, final double[] raRange, final double[] decRange) {
        super(validation, keyword, ",", 2);
        setIndexLat(indexLat);
        setIndexLong(indexLong);
        setRaRange(raRange);
        setDecRange(decRange);
    }

    @Override
    protected Map<String, String> localValidation() {
        final Map<String, String> error = super.localValidation();        
        final String value = getMap().get(this.getKeywordToTest());
        final String[] array = value.split(getSplitChar());
        if (error.isEmpty()) {
            final double valRa = Double.valueOf(array[getIndexLong()]);
            final double valDec = Double.valueOf(array[getIndexLat()]);
            if (valRa < getRaRange()[MIN] || valRa > getRaRange()[MAX]) {
                error.put(getKeywordToTest(), "RA (=" + valRa + ") must be in [" + getRaRange()[MIN] + "," + getRaRange()[MAX] + "]");
            }
            if (valDec < getDecRange()[MIN] || valRa > getDecRange()[MAX]) {
                error.put(getKeywordToTest(), "Dec (=" + valDec + ") must be in [" + getDecRange()[MIN] + "," + getDecRange()[MAX] + "]");
            }            
        }
        return error;
    }

    /**
     * @return the indexLong
     */
    protected int getIndexLong() {
        return indexLong;
    }

    /**
     * @param indexLong the indexLong to set
     */
    protected void setIndexLong(int indexLong) {
        this.indexLong = indexLong;
    }

    /**
     * @return the indexLat
     */
    protected int getIndexLat() {
        return indexLat;
    }

    /**
     * @param indexLat the indexLat to set
     */
    protected void setIndexLat(int indexLat) {
        this.indexLat = indexLat;
    }

    /**
     * @return the raRange
     */
    protected double[] getRaRange() {
        return raRange;
    }

    /**
     * @param raRange the raRange to set
     */
    protected void setRaRange(double[] raRange) {
        this.raRange = raRange;
    }

    /**
     * @return the decRange
     */
    protected double[] getDecRange() {
        return decRange;
    }

    /**
     * @param decRange the decRange to set
     */
    protected void setDecRange(double[] decRange) {
        this.decRange = decRange;
    }
    
    
    
}
