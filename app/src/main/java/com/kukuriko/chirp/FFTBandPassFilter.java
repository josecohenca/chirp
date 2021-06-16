package com.kukuriko.chirp;


public class FFTBandPassFilter
{
    /** The Fourier transformer */
    private FourierTransform ft = null;

    /** Hanning window audio processor */
    private HanningAudioProcessor hap = null;

    /** Hanning window audio processor */
    private int filterSize = 2048;

    /** The lowest frequency at which audio will pass */
    private int highPassHz = 23500;

    /** The highest frequency at which audio will pass */
    private int lowPassHz = 17500;

    /** The Nyquist Rate */
    private float nyqRate = 5000;


    /**
     * 	Default constructor
     *
     * 	@param highPassHz The frequency of the high pass filter
     * 	@param lowPassHz  The frequency of the low pass filter.
     */
    public FFTBandPassFilter( final int highPassHz, final int lowPassHz, final float nyqRate, final int numChannels )
    {
        this.ft = new FourierTransform(numChannels);
        this.hap = new HanningAudioProcessor( filterSize );
        this.highPassHz = highPassHz;
        this.lowPassHz = lowPassHz;
        this.nyqRate = nyqRate;
    }



    final public short[] process(final short[] sample, final double sampleRateKhz, final int numChannels) throws Exception{
        int sampleLength = sample.length;
        short[] outSample = new short[(short)Math.ceil((sampleLength/filterSize))*filterSize];
        short[] s = new short[filterSize];
        int index = 0;
        int copyLength = 0;
        while(index>=sampleLength){
            if(index+filterSize>sampleLength){
                copyLength = sampleLength-index ;
            }
            else
                copyLength = filterSize;
            System.arraycopy(sample, index, s, 0, copyLength);
            processSample(s, sampleRateKhz, numChannels);
            System.arraycopy(s, 0, outSample, index, filterSize);
            index+=filterSize;
        }
        return outSample;
    }



    final public short[] processSample( final short[] sample, final double sampleRateHz, final int numChannels )
    {
        // Perform an FFT on the filter.
        final float[][] transformedData = this.ft.process(this.hap.process(MyUtils.convertShortDouble(sample), numChannels), true);

        // Number of channels to process
        final int nc = transformedData.length;

        // If the FFT failed we'll not try to process anything
        if( nc > 0 )
        {
            // The size of each bin in Hz (using the first channel as examplar)
            final double binSize = (sampleRateHz) / (transformedData[0].length/2);

            // Work out which bins we will wipe out
            final int highPassBin = (int)Math.floor( this.highPassHz / binSize );
            final int lowPassBin  = (int)Math.ceil(  this.lowPassHz  / binSize );

            // Loop through the channels.
            for( int c = 0; c < nc; c++ )
            {
                // Process the samples
                for( int i = 0; i < transformedData[c].length; i++ )
                    if( i < lowPassBin || i > highPassBin )
                        transformedData[c][i] = 0;
            }

            // Do the inverse transform from frequency to time.
            final short[] s = this.ft.inverseTransform(transformedData);

            return s;
        }
        else return sample;
    }

}

