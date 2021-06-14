package com.kukuriko.chirp;


import org.jtransforms.fft.FloatFFT_1D;

public class FourierTransform
{
    /** The last generated FFT */
    private float[][] lastFFT = null;

    /** The scaling factor to apply prior to the FFT */
    private float scalingFactor = 1;

    /** Whether to pad the input to the next power of 2 */
    private boolean padToNextPowerOf2 = true;

    /** Whether to divide the real return parts by the size of the input */
    private final boolean normalise = true;

    private int numChannels;


    /**
     * 	Constructor for chaining.
     */
    public FourierTransform( int numChannels )
    {
        this.numChannels = numChannels;
    }

    /**
     * 	Process the given sample buffer
     *	@param sb The sample buffer
     *	@return The sample buffer
     */
    public short[] process( final short[] sb )
    {
        // The number of channels we need to process
        final int nChannels = this.numChannels;

        // Number of samples we'll need to process for each channel
        final int nSamplesPerChannel = sb.length / nChannels;

        // The size of the FFT to generate
        final int sizeOfFFT = this.padToNextPowerOf2 ?
                this.nextPowerOf2( nSamplesPerChannel ) : nSamplesPerChannel;

        // The Fourier transformer we're going to use
        final FloatFFT_1D fft = new FloatFFT_1D( nSamplesPerChannel );

        // Creates an FFT for each of the channels in turn
        this.lastFFT = new float[nChannels][];
        for( int c = 0; c < nChannels; c++ )
        {
            // Twice the length to account for imaginary parts
            this.lastFFT[c] = new float[ sizeOfFFT*2 ];

            // Fill the array
            for( int x = 0; x < nSamplesPerChannel; x++ )
                this.lastFFT[c][x*2] = sb[( x*nChannels+c )] * this.scalingFactor;

//			System.out.println( "FFT Input (channel "+c+"), length "+this.lastFFT[c].length+": " );
//			System.out.println( Arrays.toString( this.lastFFT[c] ));

            // Perform the FFT (using jTransforms)
            fft.complexForward( this.lastFFT[c] );


//			System.out.println( "FFT Output (channel "+c+"): " );
//			System.out.println( Arrays.toString( this.lastFFT[c] ));
        }

        if( this.normalise )
            this.normaliseReals( sizeOfFFT );

        return sb;
    }

    /**
     * 	Process the given sample buffer
     *	@param sb The sample buffer
     *	@return The sample buffer
     */
    public double[] process( final double[] sb )
    {
        // The number of channels we need to process
        final int nChannels = this.numChannels;

        // Number of samples we'll need to process for each channel
        final int nSamplesPerChannel = sb.length / nChannels;

        // The size of the FFT to generate
        final int sizeOfFFT = this.padToNextPowerOf2 ?
                this.nextPowerOf2( nSamplesPerChannel ) : nSamplesPerChannel;

        // The Fourier transformer we're going to use
        final FloatFFT_1D fft = new FloatFFT_1D( nSamplesPerChannel );

        // Creates an FFT for each of the channels in turn
        this.lastFFT = new float[nChannels][];
        for( int c = 0; c < nChannels; c++ )
        {
            // Twice the length to account for imaginary parts
            this.lastFFT[c] = new float[ sizeOfFFT*2 ];

            // Fill the array
            for( int x = 0; x < nSamplesPerChannel; x++ )
                this.lastFFT[c][x*2] = (float)sb[( x*nChannels+c )] * this.scalingFactor;

//			System.out.println( "FFT Input (channel "+c+"), length "+this.lastFFT[c].length+": " );
//			System.out.println( Arrays.toString( this.lastFFT[c] ));

            // Perform the FFT (using jTransforms)
            fft.complexForward( this.lastFFT[c] );

            if( this.normalise )
                this.normaliseReals( sizeOfFFT );

//			System.out.println( "FFT Output (channel "+c+"): " );
//			System.out.println( Arrays.toString( this.lastFFT[c] ));
        }

        return sb;
    }

    /**
     * 	Divides the real parts of the last FFT by the given size
     *	@param size the divisor
     */
    private void normaliseReals( final int size )
    {
        for( int c = 0; c < this.lastFFT.length; c++ )
            for( int i = 0; i < this.lastFFT[c].length; i +=2 )
                this.lastFFT[c][i] /= size;
    }

    /**
     * 	Returns the next power of 2 superior to n.
     *	@param n The value to find the next power of 2 above
     *	@return The next power of 2
     */
    private int nextPowerOf2( final int n )
    {
        return (int)Math.pow( 2, 32 - Integer.numberOfLeadingZeros(n - 1) );
    }

    /**
     * 	Given some transformed audio data, will convert it back into
     * 	a sample chunk. The number of channels given audio format
     * 	must match the data that is provided in the transformedData array.
     *
     *	@param transformedData The frequency domain data
     *	@return A
     */
    public short[] inverseTransform( final float[][] transformedData )
    {
        // Check the data has something in it.
        if( transformedData == null || transformedData.length == 0 )
            throw new IllegalArgumentException( "No data in data chunk" );

        // Check that the transformed data has the same number of channels
        // as the data we've been given.
        if( transformedData.length != this.numChannels )
            throw new IllegalArgumentException( "Number of channels in audio " +
                    "format does not match given data." );

        // The number of channels
        final int nChannels = transformedData.length;

        // The Fourier transformer we're going to use
        final FloatFFT_1D fft = new FloatFFT_1D( transformedData[0].length/2 );

        // Create a sample buffer to put the time domain data into
        final short[] sb = new short[transformedData[0].length/2 *	nChannels];

        // Perform the inverse on each channel
        for( int channel = 0; channel < transformedData.length; channel++ )
        {
            // Convert frequency domain back to time domain
            fft.complexInverse( transformedData[channel], true );

            // Set the data in the buffer
            for( int x = 0; x < transformedData[channel].length/2; x++ )
                sb[( x*nChannels+channel)]= (short)transformedData[channel][x] ;
        }

        // Return a new sample chunk
        return sb;
    }



    public double[] inverseTransformToDouble( final float[][] transformedData )
    {
        // Check the data has something in it.
        if( transformedData == null || transformedData.length == 0 )
            throw new IllegalArgumentException( "No data in data chunk" );

        // Check that the transformed data has the same number of channels
        // as the data we've been given.
        if( transformedData.length != this.numChannels )
            throw new IllegalArgumentException( "Number of channels in audio " +
                    "format does not match given data." );

        // The number of channels
        final int nChannels = transformedData.length;

        // The Fourier transformer we're going to use
        final FloatFFT_1D fft = new FloatFFT_1D( transformedData[0].length/2 );

        // Create a sample buffer to put the time domain data into
        final double[] sb = new double[transformedData[0].length/2 *	nChannels];

        // Perform the inverse on each channel
        for( int channel = 0; channel < transformedData.length; channel++ )
        {
            // Convert frequency domain back to time domain
            fft.complexInverse( transformedData[channel], true );

            // Set the data in the buffer
            for( int x = 0; x < transformedData[channel].length/2; x++ )
                sb[( x*nChannels+channel)]= (short)transformedData[channel][x] ;
        }

        // Return a new sample chunk
        return sb;
    }

    /**
     * 	Get the last processed FFT frequency data.
     * 	@return The fft of the last processed window
     */
    public float[][] getLastFFT()
    {
        return this.lastFFT;
    }

    /**
     * 	Returns the magnitudes of the last FFT data. The length of the
     * 	returned array of magnitudes will be half the length of the FFT data
     * 	(up to the Nyquist frequency).
     *
     *	@return The magnitudes of the last FFT data.
     */
    public float[][] getMagnitudes()
    {
        final float[][] mags = new float[this.lastFFT.length][];
        for( int c = 0; c < this.lastFFT.length; c++ )
        {
            mags[c] = new float[ this.lastFFT[c].length/4 ];
            for( int i = 0; i < this.lastFFT[c].length/4; i++ )
            {
                final float re = this.lastFFT[c][i*2];
                final float im = this.lastFFT[c][i*2+1];
                mags[c][i] = (float)Math.sqrt( re*re + im*im );
            }
        }

        return mags;
    }

    /**
     * 	Returns the power magnitudes of the last FFT data. The length of the
     * 	returned array of magnitudes will be half the length of the FFT data
     * 	(up to the Nyquist frequency). The power is calculated using:
     * 	<p><code>10log10( real^2 + imaginary^2 )</code></p>
     *
     *	@return The magnitudes of the last FFT data.
     */
    public float[][] getPowerMagnitudes()
    {
        final float[][] mags = new float[this.lastFFT.length][];
        for( int c = 0; c < this.lastFFT.length; c++ )
        {
            mags[c] = new float[ this.lastFFT[c].length/4 ];
            for( int i = 0; i < this.lastFFT[c].length/4; i++ )
            {
                final float re = this.lastFFT[c][i*2];
                final float im = this.lastFFT[c][i*2+1];
                mags[c][i] = 10f * (float)Math.log10( re*re + im*im );
            }
        }

        return mags;
    }

    /**
     * 	Scales the real and imaginary parts by the scalar prior to
     * 	calculating the (square) magnitude for normalising the outputs.
     * 	Returns only those values up to the Nyquist frequency.
     *
     *	@param scalar The scalar
     *	@return Normalised magnitudes.
     */
    public float[][] getNormalisedMagnitudes( final float scalar )
    {
        final float[][] mags = new float[this.lastFFT.length][];
        for( int c = 0; c < this.lastFFT.length; c++ )
        {
            mags[c] = new float[ this.lastFFT[c].length/4 ];
            for( int i = 0; i < this.lastFFT[c].length/4; i++ )
            {
                final float re = this.lastFFT[c][i*2] * scalar;
                final float im = this.lastFFT[c][i*2+1] * scalar;
                mags[c][i] = re*re + im*im;
            }
        }

        return mags;
    }

    /**
     * 	Returns just the real numbers from the last FFT. The result will include
     * 	the symmetrical part.
     *	@return The real numbers
     */
    public float[][] getReals()
    {
        final float[][] reals = new float[this.lastFFT.length][];
        for( int c = 0; c < this.lastFFT.length; c++ )
        {
            reals[c] = new float[ this.lastFFT[c].length/2 ];
            for( int i = 0; i < this.lastFFT[c].length/2; i++ )
                reals[c][i] = this.lastFFT[c][i*2];
        }

        return reals;
    }

    /**
     * 	Get the scaling factor in use.
     *	@return The scaling factor.
     */
    public float getScalingFactor()
    {
        return this.scalingFactor;
    }

    /**
     * 	Set the scaling factor to use. This factor will be applied to signal
     * 	data prior to performing the FFT. The default is, of course, 1.
     *	@param scalingFactor The scaling factor to use.
     */
    public void setScalingFactor( final float scalingFactor )
    {
        this.scalingFactor = scalingFactor;
    }

    /**
     *	Returns whether the input will be padded to be the length
     *	of the next higher power of 2.
     *	@return TRUE if the input will be padded, FALSE otherwise.
     */
    public boolean isPadToNextPowerOf2()
    {
        return this.padToNextPowerOf2;
    }

    /**
     * 	Set whether to pad the input to the next power of 2.
     *	@param padToNextPowerOf2 TRUE to pad the input, FALSE otherwise
     */
    public void setPadToNextPowerOf2( final boolean padToNextPowerOf2 )
    {
        this.padToNextPowerOf2 = padToNextPowerOf2;
    }

    public double binToHz(final int binIndex, final float sampleRate, final int fftSize) {
        return binIndex * sampleRate / (double) fftSize;
    }
}

