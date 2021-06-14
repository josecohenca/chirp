package com.kukuriko.chirp;

import android.graphics.Bitmap;

public class MyUtils {

    public static double calculateMean(double numArray[]) {
        double sum = 0.0;
        int length = numArray.length;

        for (double num : numArray) {
            sum += num;
        }

        double mean = sum / length;

        return mean;
    }

    public static double calculateSD(double numArray[])
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        double mean = calculateMean(numArray);

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        double sd = Math.sqrt(standardDeviation/length);
        return sd;
    }

    public static double[] convertShortDouble(short[] array) {
        double[] out = new double[array.length];

        for (int i=0; i<array.length; i++) {
            out[i] = (double)array[i];
        }

        return out;
    }

    public static short[] convertDoubleShort(double[] array) {
        short[] out = new short[array.length];

        for (int i=0; i<array.length; i++) {
            out[i] = (short)array[i];
        }

        return out;
    }

    public static float[] convertShortFloat(short[] array) {
        float[] out = new float[array.length];

        for (int i=0; i<array.length; i++) {
            out[i] = (float)array[i];
        }

        return out;
    }

    public static short[] convertFloatShort(float[] array) {
        short[] out = new short[array.length];

        for (int i=0; i<array.length; i++) {
            out[i] = (short)array[i];
        }

        return out;
    }

    public static double[] convertFloatDouble(float[] array) {
        double[] out = new double[array.length];

        for (int i=0; i<array.length; i++) {
            out[i] = (double)array[i];
        }

        return out;
    }


    public static float[] convertDoubleFloat(double[] array) {
        float[] out = new float[array.length];

        for (int i=0; i<array.length; i++) {
            out[i] = (float)array[i];
        }

        return out;
    }


    public static Bitmap bitmapFromArray(int[][] pixels2d){
        int width = pixels2d.length;
        int height = pixels2d[0].length;
        int[] pixels = new int[width * height];
        int pixelsIndex = 0;
        for (int i = 0; i < width; i++)
        {
            for (int j = 0; j < height; j++)
            {
                pixels[pixelsIndex] = pixels2d[i][j];
                pixelsIndex ++;
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }


}
