package com.kukuriko.chirp;

import com.kukuriko.chirp.MyUtils;

public class BandPassFilter
{
    /** The size of each required sample chunk */
    private int requiredSampleSetSize;

    /** The number of samples overlap required between each window */
    private int windowStep = 0;

    /** Whether or not the windows are overlapping */
    private boolean overlapping = false;

    /** A table of weights */
    protected double[] weightTable = null;

    /** A table of weights after window function*/
    protected double[] windowedWeightTable = null;

    /** Whether to apply the weights to the incoming signal */
    protected boolean useWindow = true;

    /** Hanning window audio processor */
    private HanningAudioProcessor hap = null;

    private int numChannels = 1;

    private boolean isNormalized = true;

    /** The lowest frequency at which audio will pass */
    private float highPass = 1;

    /** The highest frequency at which audio will pass */
    private float lowPass = 0;
    /**
     * 	Default constructor for non chainable processing.
     * 	@param sizeRequired Size of the window required
     */
    public BandPassFilter(final int sizeRequired, final float highPass, final float lowPass, final int numChannels, final boolean isNormalized )
    {
        this.requiredSampleSetSize = sizeRequired;
        this.numChannels = numChannels;
        this.highPass = highPass;
        this.lowPass = lowPass;
        this.isNormalized = isNormalized;
        this.hap = new HanningAudioProcessor( sizeRequired);
    }

    private final boolean normalized = true;
    private final double SHORTCUT = 6.0e-3;

    public double sinc(double x){
        final double scaledX = normalized ? Math.PI * x : x;
        if (Math.abs(scaledX) <= SHORTCUT) {
            // use Taylor series
            final double scaledX2 = scaledX * scaledX;
            return ((scaledX2 - 20) * scaledX2 + 120) / 120;
        } else {
            // use definition expression
            return Math.sin(scaledX) / scaledX;
        }
    }

    protected void generatSquareFilterTableCache( final int length, final int nc )
    {
        final int ns = length;
        double alpha = 0.5 * (ns - 1);
        double m;
        this.weightTable = new double[ ns*nc ];
        for( int n = 0; n < ns; n++ ) {
            for (int c = 0; c < nc; c++) {
                m = n - alpha;
                this.weightTable[n * nc + c] = highPass * sinc(highPass * m) - lowPass * sinc(lowPass * m);
            }
        }
    }



    final public double[] getWeightTable()
    {
        final int nc = this.numChannels;

        if( this.weightTable == null )
            this.generatSquareFilterTableCache( this.requiredSampleSetSize/nc, nc );

        return this.weightTable;
    }



    private void normalize(double[] s, int length, int nc){
        int ns = length;
        double sc;
        double acc=0;
        double scale_frequency = 0.5 * (this.lowPass + this.highPass);
        for (int c = 0; c < nc; c++) {
            acc=0;
            for( int n = 0; n < ns; n++ ) {
                acc += s[n * nc + c] * Math.cos(Math.PI * n * scale_frequency);
            }
            for( int n = 0; n < ns; n++ ) {
                s[n * nc + c] /= acc;
            }
        }
    }

    final public double[] getWindowedWeightTable()
    {
        final int nc = this.numChannels;

        if( this.windowedWeightTable == null )
            this.windowedWeightTable = this.hap.process(getWeightTable(), nc);

        if(isNormalized)
            normalize(this.windowedWeightTable, this.windowedWeightTable.length/nc, nc);

        return this.windowedWeightTable;

    }



    final public short[] process( final short[] b, final int numChannels )
    {
        final int nc = numChannels;


        double[] outb = Convolution.convolve(MyUtils.convertShortDouble(b), getWindowedWeightTable(), numChannels);

        return MyUtils.convertDoubleShort(outb);
    }

}