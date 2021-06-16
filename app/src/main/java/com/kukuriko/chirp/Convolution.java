package com.kukuriko.chirp;

public class Convolution {


    public static double[] convolve(double[] data, double[] kernel, int nc) {
        final int halfsize = kernel.length / (2 * nc);
        final int dataSize = data.length / nc;
        final int kernelSize = kernel.length / nc;
        final double outbuffer[] = new double[data.length];

        for( int c = 0; c < nc; c++ ) {
            final double buffer[] = new double[dataSize + kernelSize];

            for (int i = 0; i < halfsize; i++)
                buffer[i] = data[0+c];
            for (int i = 0; i < dataSize; i++)
                buffer[halfsize + i] = data[i*nc+c];

            for (int i = 0; i < halfsize; i++)
                buffer[halfsize + dataSize + i] = data[(dataSize - 1)*nc + c];

            // convolveBuffer(buffer, kernel);
            for (int i = 0; i < dataSize; i++) {
                float sum = 0.0f;

                for (int j = 0, jj = kernelSize - 1; j < kernelSize; j++, jj--)
                    sum += buffer[i + j] * kernel[jj*nc+c];

                buffer[i] = sum;
            }
            // end convolveBuffer(buffer, kernel);

            for (int k = 0; k < dataSize; k++)
                outbuffer[k*nc+c] = buffer[k];

        }

        return outbuffer;
    }



    // compute the circular convolution of x and y
    public static double[] fftCircularConvolution(double[] x, double[] y, int nc, boolean conjugate) {

        FourierTransform ft = new FourierTransform(nc);
        int conjugatefactor = conjugate ? -1 : 1;
        // should probably pad x and y with 0s so that they have same length
        // and are powers of 2
        if (x.length != y.length) {
            throw new IllegalArgumentException("Dimensions don't agree");
        }

        int n = x.length;

        // compute FFT of each sequence
        float[][] fx = ft.process(x, true);
        float[][] fy = ft.process(y, true);

        // point-wise multiply
        float[][] fz = new float[nc][n];
        for(int c=0;c<nc;c++) {
            for (int i = 0; i < n-1; i+=2) {
                fz[c][i] = fx[c][i] * fy[c][i] - fx[c][i+1] * (conjugatefactor*fy[c][i+1]);
                fz[c][i+1] = fx[c][i] * (conjugatefactor*fy[c][i+1]) + fx[c][i+1] * fy[c][i];
            }
        }

        // compute inverse FFT
        return ft.inverseTransformToDouble(fz);
    }


    // compute the linear convolution of x and y
    public static double[] fftLinearConvolution(double[] x, double[] y, int nc) {
        int length=x.length+y.length-1;
        //if(x.length>=y.length)
        //    length=2*x.length;
        //else
        //    length=2*y.length;


        double[] a = new double[length];
        for (int i = 0;        i <   x.length; i++) a[i] = x[i];
        for (int i = x.length; i < length; i++) a[i] = 0.0;

        double[] b = new double[length];
        for (int i = 0;        i <   y.length; i++) b[i] = y[i];
        for (int i = y.length; i < length; i++) b[i] = 0.0;

        return fftCircularConvolution(a, b, nc, false);
    }



    // compute the linear convolution of x and y
    public static double[] fftLinearCorrelation(double[] x, double[] y, int nc) {
        int length=0;
        if(x.length>=y.length)
            length=x.length;
        else
            length=y.length;


        double[] a = new double[2*length];
        for (int i = 0;        i <   x.length; i++) a[i] = x[i];
        for (int i = x.length; i < 2*length; i++) a[i] = 0.0;

        double[] b = new double[2*length];
        for (int i = 0;        i <   y.length; i++) b[i] = y[i];
        for (int i = y.length; i < 2*length; i++) b[i] = 0.0;

        return fftCircularConvolution(a, b, nc, true);
    }


    private boolean normalizeCorrelation = true;

    public static double[] correlate(double[] data, double[] kernel, int nc) {
        final int halfsize = kernel.length / (2 * nc);
        final int dataSize = data.length / nc;
        final int kernelSize = kernel.length / nc;

        final double sdData = MyUtils.calculateSD(data);
        final double sdKernel = MyUtils.calculateSD(kernel);

        final double outbuffer[] = new double[data.length];

        for( int c = 0; c < nc; c++ ) {
            final double buffer[] = new double[dataSize + kernelSize];

            for (int i = 0; i < halfsize; i++)
                buffer[i] = data[0+c];
            for (int i = 0; i < dataSize; i++)
                buffer[halfsize + i] = data[i*nc+c];

            for (int i = 0; i < halfsize; i++)
                buffer[halfsize + dataSize + i] = data[(dataSize - 1)*nc + c];

            // convolveBuffer(buffer, kernel);
            for (int i = 0; i < dataSize; i++) {
                float sum = 0.0f;

                for (int j = 0; j < kernelSize; j++)
                    sum += buffer[i + j] * kernel[j*nc+c];

                buffer[i] = sum;
            }
            // end convolveBuffer(buffer, kernel);

            for (int k = 0; k < dataSize; k++)
                outbuffer[k*nc+c] = buffer[k]/(sdData*sdKernel);

        }

        return outbuffer;
    }

}
