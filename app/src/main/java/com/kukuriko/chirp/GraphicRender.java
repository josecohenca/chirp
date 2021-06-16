package com.kukuriko.chirp;


import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


import com.musicg.wave.Wave;
import com.musicg.wave.extension.Spectrogram;

import static com.kukuriko.chirp.BuildConfig.DEBUG;

public class GraphicRender{

    public static final float WAVEFORM_DEFAULT_TIMESTEP = 0.1F;
    private int xMarker=-1;
    private int yMarker=-1;

    public GraphicRender(){
    }

    /**
     * Render a waveform of a wave file
     *
     * @param wave	Wave object
     * @param filename	output file
     */
    public void renderWaveform(Wave wave, String filename) {
        renderWaveform(wave,WAVEFORM_DEFAULT_TIMESTEP);
    }

    /**
     * Render a waveform of a wave file
     *
     * @param wave	Wave object
     * @param timeStep	time interval in second, as known as 1/fps
     */
    public Bitmap renderWaveform(Wave wave, float timeStep) {

        // for signed signals, the middle is 0 (-1 ~ 1)
        double middleLine=0;

        // usually 8bit is unsigned
        if (wave.getWaveHeader().getBitsPerSample()==8){
            // for unsigned signals, the middle is 0.5 (0~1)
            middleLine=0.5;
        }

        double[] nAmplitudes = wave.getNormalizedAmplitudes();
        int width = (int) (nAmplitudes.length / wave.getWaveHeader().getSampleRate() / timeStep);
        int height = 500;
        int middle = height / 2;
        int magnifier = 1000;

        int numSamples = nAmplitudes.length;

        if (width>0){
            int numSamplePerTimeFrame = numSamples / width;

            int[] scaledPosAmplitudes = new int[width];
            int[] scaledNegAmplitudes = new int[width];

            // width scaling
            for (int i = 0; i < width; i++) {
                double sumPosAmplitude = 0;
                double sumNegAmplitude = 0;
                int startSample=i * numSamplePerTimeFrame;
                for (int j = 0; j < numSamplePerTimeFrame; j++) {
                    double a = nAmplitudes[startSample + j];
                    if (a > middleLine) {
                        sumPosAmplitude += (a-middleLine);
                    } else {
                        sumNegAmplitude += (a-middleLine);
                    }
                }

                int scaledPosAmplitude = (int) (sumPosAmplitude
                        / numSamplePerTimeFrame * magnifier + middle);
                int scaledNegAmplitude = (int) (sumNegAmplitude
                        / numSamplePerTimeFrame * magnifier + middle);

                scaledPosAmplitudes[i] = scaledPosAmplitude;
                scaledNegAmplitudes[i] = scaledNegAmplitude;
            }

            // render wave form image


            Bitmap bufferedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            //BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // set default white background
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int y = height - j;    // j from -ve to +ve, i.e. draw from top to bottom
                    if(j >= scaledNegAmplitudes[i] && j < scaledPosAmplitudes[i]) {
                        if (y < 0) {
                            y = 0;
                        } else if (y >= height) {
                            y = height - 1;
                        }
                        bufferedImage.setPixel(i, y, 0xff << 24 | 0);
                    }
                    else{
                        bufferedImage.setPixel(i, y, 0xff << 24 | 0xff<<16 | 0xff<<8 | 0xff);
                    }
                }
            }
            // end render wave form image

            if(DEBUG) {
                File path = MainActivity.myContext.getFilesDir();
                File file = new File(path, "debug_wave.png"); // the File to save
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bufferedImage.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return bufferedImage;
            // end export image
        }
        else{
            System.err.println("renderWaveform error: Empty Wave");
            return  null;
        }
    }

    /**
     * Render a spectrogram of a wave file
     *
     * @param spectrogram	spectrogram object
     */
    public Bitmap renderSpectrogram(Spectrogram spectrogram){
        return renderSpectrogramData(spectrogram.getNormalizedSpectrogramData());
    }

    /**
     *
     * Render a spectrogram data array
     *
     * @param spectrogramData	spectrogramData[time][frequency]=intensity, which time is the x-axis, frequency is the y-axis, intensity is the color darkness
     */
    public Bitmap renderSpectrogramData(double[][] spectrogramData) {

        if (spectrogramData!=null){
            int width=spectrogramData.length;
            int height=spectrogramData[0].length;

            Bitmap bufferedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int i=0; i<width; i++){
                if (i==xMarker){
                    for (int j=0; j<height; j++){
                        bufferedImage.setPixel(i, j, 0xFF00);	// green
                    }
                }
                else{
                    for (int j=0; j<height; j++){
                        int value;
                        if (j==yMarker){
                            value=0xFF0000;	// red
                        }
                        else{
                            value=255-(int)(spectrogramData[i][j]*255);
                        }
                        bufferedImage.setPixel(i, height-1-j, 0xff << 24 | value<<16 | value<<8 | value);
                    }
                }
            }

            if(DEBUG) {
                File path = MainActivity.myContext.getFilesDir();
                File file = new File(path, "debug_mel.png"); // the File to save
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bufferedImage.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return bufferedImage;
        }
        else{
            System.err.println("renderSpectrogramData error: Empty Wave");
            return  null;
        }
    }

    /**
     * Set the vertical marker
     *
     * @param x	x-offset pixel of the marker
     */
    public void setVerticalMarker(int x){
        this.xMarker=x;
    }

    /**
     * Set the horizontal marker
     *
     * @param y	y-offset pixel of the marker
     */
    public void setHorizontalMarker(int y){
        this.yMarker=y;
    }

    /**
     * Reset the markers
     */
    public void resetMarkers(){
        xMarker=-1;
        yMarker=-1;
    }
}