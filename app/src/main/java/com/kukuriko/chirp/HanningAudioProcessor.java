package com.kukuriko.chirp;

public class HanningAudioProcessor
{
    /** The size of each required sample chunk */
    private int requiredSampleSetSize = 512;

    /** The number of samples overlap required between each window */
    private int windowStep = 0;

    /** Whether or not the windows are overlapping */
    private boolean overlapping = false;

    /** A table of weights */
    protected double[] weightTable = null;

    /** Whether to apply the weights to the incoming signal */
    protected boolean useWeights = true;

    /**
     * 	Default constructor for non chainable processing.
     * 	@param sizeRequired Size of the window required
     */
    public HanningAudioProcessor( final int sizeRequired)
    {
        this.requiredSampleSetSize = sizeRequired;
    }


    protected void generateWeightTableCache( final int length, final int nc )
    {
        final int ns = length;
        this.weightTable = new double[ length*nc ];
        for( int n = 0; n < ns; n++ )
            for( int c = 0; c < nc; c++ )
                this.weightTable[n*nc+c] = 0.5*(1-Math.cos((2*Math.PI*n)/ns));
    }


    final public double[] process( final double[] b, final int numChannels )
    {
        final int nc = numChannels;
        double[] outb = new double[b.length];
        if( this.weightTable == null )
            this.generateWeightTableCache( b.length/nc, nc );

        for( int c = 0; c < nc; c++ )
        {
            for( int n = 0; n < b.length/nc; n++ )
            {
                final double x = b[(n*nc+c)];
                double v = (float)(x * this.weightTable[n*nc+c]);
                if( !this.useWeights ) v = x;
                outb[( n*nc+c)] = v;
            }
        }

        return outb;
    }

}