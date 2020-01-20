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
