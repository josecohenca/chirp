package com.kukuriko.chirp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;


public class MainService extends Service {

    private boolean isRecording=false;
    private static final String TAG = "MainService";
    private final int duration = 500;
    private final int maxRange = 10;
    private final int speedOfSound = 343;
    private final int maxDelayRTT = 2*(1000*maxRange)/speedOfSound;
    private final int SAMPLING_RATE_IN_HZ = 192000;
    private int filterSize = 1024;
    private final double nyqRate = SAMPLING_RATE_IN_HZ/2.0;
    private final int numSample = duration * SAMPLING_RATE_IN_HZ / 1000;
    private final int numChannels = 2;

    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO; // same as (CHANNEL_IN_LEFT | CHANNEL_IN_RIGHT)
    // other MIC combinations: CHANNEL_IN_FRONT_BACK , CHANNEL_IN_X_AXIS , CHANNEL_IN_Y_AXIS , CHANNEL_IN_Z_AXIS
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BPP = 16; //bits per sample
    private final float floatScale = 1 << (RECORDER_BPP - 1);
    private final int recordingDuration = duration + maxDelayRTT;
    private final int BUFFER_SIZE = recordingDuration * numChannels * SAMPLING_RATE_IN_HZ / 1000;

    double freq1 = 18000;
    double freq2 = 23000;
    double testFreq = freq1;//8000;
    double guardFreq = 100;
    double testFreqNorm = 8000/nyqRate;
    double freq1Norm = 18000/nyqRate;
    double freq2Norm = 23000/nyqRate;


    public static final float FREQ_LP_BEAT = 150.0f;
    public static final float T_FILTER = (float) (1.0f / (2.0f * Math.PI * FREQ_LP_BEAT));
    public static final float BEAT_RTIME = 0.02f;


    private File audioFile;
    private static String audioFilePath;
    private File imageFile;
    private static String imageFilePath;

    public static String getAudioFilePath(){
        return audioFilePath;
    }

    private FourierTransform ft;
    private BandPassFilter bp;

    public enum WaveType {
        SINE,
        CHIRP
    }

    private WaveType waveType = WaveType.SINE;

    protected static boolean serviceFinished=false;

    private AudioRecord recorder = null;
    private Thread recordingThread = null;

    private boolean outputFile = false;

    @Override
    public void onCreate() {
        audioFilePath = this.getFilesDir()+"/voice8K16bitstereo.wav";
        imageFilePath = this.getFilesDir()+"/voice8K16bitstereo.png";
        MainActivity.allData = new ArrayBlockingQueue<>(MainActivity.maxLoops);
        MainActivity.sample = new double[numSample];
        MainActivity.generatedSnd = new byte[numChannels * numSample];

        Log.i(TAG, "Service onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Log.i(TAG, "Service onStartCommand");

        isRecording=true;
        waveType=WaveType.values()[SettingsActivity.getDropDownValue()];
        Thread startRecording = new Thread(new Runnable(){
            public void run() {
                startRecordLoop();
            }
        });
        startRecording.start();

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i( TAG,  "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        isRecording=false;
        Log.i( TAG,  "Service onDestroy");
    }



    private void startRecordLoop(){
        genTone();

        int loop = 0;
        while(isRecording) {

            Thread threadPlay = new Thread(new Runnable() {
                public void run() {
                    playSound();
                }
            });
            threadPlay.start();

            Thread threadRecord = new Thread(new Runnable() {
                public void run() {
                    startRecording();
                }
            });
            threadRecord.start();

            try {
                threadPlay.join();
                threadRecord.join();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(loop%20==0)
                calculateCurrentLoopVal();
            loop++;
        }

        calculateCurrentLoopVal();

        serviceFinished=true;
    }


    protected void playSound () {
        AudioTrack audioTrack = null;
        try {
            //audioTrack = new AudioTrack(AudioManager.STREAM_ALARM, SAMPLING_RATE_IN_HZ, AudioFormat.CHANNEL_OUT_FRONT_CENTER, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length, AudioTrack.MODE_STATIC);
            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLING_RATE_IN_HZ)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(MainActivity.generatedSnd.length)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build();

            audioTrack.write(MainActivity.generatedSnd, 0, MainActivity.generatedSnd.length);
            audioTrack.play();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void calculateCurrentLoopVal(){
        MainActivity.sData = MainActivity.allData.peek();

        MainActivity.oData = new float[numChannels][];
        for( int c = 0; c < numChannels; c++ ) {
            MainActivity.oData[c] = new float[MainActivity.sData.length/numChannels];
            for (int i = 0; i < MainActivity.sData.length/numChannels; i += numChannels) {
                final float y = (MainActivity.sData[i * numChannels + c]) / floatScale;
                MainActivity.oData[c][i]=y;
            }
        }

        ft=new FourierTransform(numChannels);
        bp = new BandPassFilter( filterSize, (float)((freq2+guardFreq)/nyqRate), (float)((freq1-guardFreq)/nyqRate), numChannels, true);

        ft.process( MainActivity.sData );
        MainActivity.specData = this.ft.getPowerMagnitudes();

        int accDetection = 0;
        short[][] aData = (short[][])MainActivity.allData.toArray();
        if (SettingsActivity.getApplyFilterCheck()) applyFilter(MainActivity.sData);
        if (SettingsActivity.getApplyConvCheck()) applyCorrelation(MainActivity.sData);
        if (SettingsActivity.getApplyEnvCheck()){
            if(aData.length>1) {
                for (int i = 0; i < aData.length; i++) {
                    if (applyEnvelope(aData[i]))
                        accDetection++;
                }
            }
        }

        if(aData.length>= 9 && accDetection >= 5){
            Toast.makeText(this, "Obstacle!",
                    Toast.LENGTH_SHORT).show();
        }
        updateGraphsBroadcast();

    }



    private void applyFilter(short[] myData){
        MainActivity.sData = bp.process( myData, numChannels );
        MainActivity.oData = new float[numChannels][];
        for( int c = 0; c < numChannels; c++ ) {
            MainActivity.oData[c] = new float[MainActivity.sData.length/numChannels];
            for (int i = 0; i < MainActivity.sData.length/numChannels; i += numChannels) {
                MainActivity.oData[c][i] = MainActivity.sData[i * numChannels + c];
            }
        }

        ft.process( MainActivity.sData );
        MainActivity.specData = this.ft.getPowerMagnitudes();
    }

    private void applyCorrelation(short[] myData){
        MainActivity.sData = MyUtils.convertDoubleShort(Convolution.correlate(MyUtils.convertShortDouble(myData),
                MyUtils.convertShortDouble(byte2short(MainActivity.generatedSnd)), numChannels));
        MainActivity.oData = new float[numChannels][];
        for( int c = 0; c < numChannels; c++ ) {
            MainActivity.oData[c] = new float[MainActivity.sData.length/numChannels];
            for (int i = 0; i < MainActivity.sData.length/numChannels; i += numChannels) {
                MainActivity.oData[c][i] = (float)MainActivity.sData[i * numChannels + c];
            }
        }

        ft.process( MainActivity.sData );
        MainActivity.specData = this.ft.getPowerMagnitudes();
    }


    private boolean applyEnvelope(short[] myData){
        boolean ret = false;

        float input;
        float EnvIn;
        float filter1Out;
        float filter2Out;
        float peakEnv;
        boolean beatTrigger;
        boolean prevBeatPulse;
        boolean beatPulse;
        float kBeatFilter = (float) (1.0 / (SAMPLING_RATE_IN_HZ * T_FILTER));
        float beatRelease = (float) Math.exp( -1.0f / (SAMPLING_RATE_IN_HZ * BEAT_RTIME) );
        float distFactor = speedOfSound/SAMPLING_RATE_IN_HZ;
        int peakNum;
        double mean = MyUtils.calculateMean(MyUtils.convertShortDouble(myData));

        if(MainActivity.oldMean==0) MainActivity.oldMean=mean;
        else MainActivity.oldMean = mean*MainActivity.lambda+(1-MainActivity.lambda)*MainActivity.oldMean;

        MainActivity.distanceCorrection = new float[numChannels];
        MainActivity.peakIndex = new int[numChannels][5];
        MainActivity.peakWidth = new float[numChannels][5];
        MainActivity.midPeak = new int[numChannels][5];
        MainActivity.movingPeakIndex = new int[numChannels][5];
        MainActivity.movingPeakDistance = new float[numChannels][5];
        MainActivity.movingPeakAngle = new float[5];
        MainActivity.movingObstacleAngle = new float[5];
        MainActivity.movingObstacleCollision = new boolean[5];

        MainActivity.oData = new float[numChannels][];
        MainActivity.specData = new float[numChannels][];

        for( int c = 0; c < numChannels; c++ ) {
            filter1Out = 0;
            filter2Out = 0;
            peakEnv = 0.0f;
            peakNum = 0;
            beatPulse = false;
            beatTrigger = false;
            prevBeatPulse = false;
            MainActivity.oData[c] = new float[myData.length / numChannels];
            MainActivity.specData[c] = new float[myData.length / numChannels];
            for (int i = 0; i < myData.length / numChannels; i += numChannels) {
                input = (float)myData[i * numChannels + c] / Short.MAX_VALUE;

                // Step 1 : 2nd order low pass filter (made of two 1st order RC filter)
                filter1Out = filter1Out + (kBeatFilter * (input - filter1Out));
                filter2Out = filter2Out + (kBeatFilter * (filter1Out - filter2Out));

                // Step 2 : peak detector
                EnvIn = Math.abs( filter2Out );
                if( EnvIn > peakEnv )
                    peakEnv = EnvIn; // Attack time = 0
                else
                {
                    peakEnv *= beatRelease;
                    peakEnv += (1.0f - beatRelease) * EnvIn;
                }

                MainActivity.oData[c][i] = peakEnv; // or EnvIn ?

                // Step 3 : Schmitt trigger
                if( !beatTrigger )
                {
                    if( peakEnv > MainActivity.oldMean ){
                        beatTrigger = true;
                        if(peakNum<5) MainActivity.peakIndex[c][peakNum] = i;
                        peakNum++;
                    }
                }
                else
                {
                    if( peakEnv < MainActivity.oldMean*0.5 ){
                        beatTrigger = false;
                        MainActivity.peakWidth[c][peakNum-1] = (i-MainActivity.peakIndex[c][peakNum-1]) * distFactor;
                        MainActivity.midPeak[c][peakNum-1] = (i+MainActivity.peakIndex[c][peakNum-1])/2;
                    }
                }

                //// Step 4 : rising edge detector
                //beatPulse = false;
                //if( (beatTrigger) && (!prevBeatPulse) ) {
                //    beatPulse = true;
                //}
                //prevBeatPulse = beatTrigger;

                MainActivity.specData[c][i] = (beatTrigger ? peakNum : 0);
            }

            MainActivity.distanceCorrection[c] = (MainActivity.peakIndex[c][1] - MainActivity.peakIndex[c][0]) * distFactor;

            if(MainActivity.prevPeakIndex != null) {
                for (int i = 0; i < MainActivity.peakIndex[c].length; i++) {
                    if ((MainActivity.prevPeakIndex[c][i] - MainActivity.peakIndex[c][i]) * distFactor > MainActivity.obstacleSpeed) {
                        MainActivity.movingPeakIndex[c][i] = MainActivity.peakIndex[c][i];
                        MainActivity.movingPeakDistance[c][i] = (MainActivity.peakIndex[c][i] - MainActivity.peakIndex[c][0]) * distFactor;
                    } else {
                        MainActivity.movingPeakIndex[c][i] = -1;
                        MainActivity.movingPeakDistance[c][i] = -1;
                    }
                }
            }
        }
        MainActivity.prevPeakIndex=MainActivity.peakIndex;

        boolean applyDistanceCorrection = false;
        if(numChannels>1) {
            for (int i = 0; i < MainActivity.movingPeakDistance[0].length; i++) {
                if (MainActivity.movingPeakDistance[0][i] > -1 && MainActivity.movingPeakDistance[1][i] > -1) {
                    double d1 = MainActivity.movingPeakDistance[0][i];
                    double d2 = MainActivity.movingPeakDistance[1][i];
                    double d3 = MainActivity.distanceCorrection[1];
                    double lmic = MainActivity.deviceHeight;
                    if (applyDistanceCorrection) d2 -= d3;

                    double peakAngleNum = Math.pow(d1, 2) + Math.pow(lmic, 2) - Math.pow(d2, 2);
                    double peakAngleDenom = 2 * d1 * lmic;
                    MainActivity.movingPeakAngle[i] = (float) Math.toDegrees(Math.PI - Math.acos(peakAngleNum / peakAngleDenom));  //theta

                    double df = d1 + (double) MainActivity.peakWidth[0][i] / (2 * d1);

                    MainActivity.movingObstacleAngle[i] = (float) Math.toDegrees((Math.acos(df / d1)));  //delta
                    MainActivity.movingObstacleCollision[i] = MainActivity.movingPeakAngle[i] <= MainActivity.movingObstacleAngle[i];
                    if(MainActivity.movingObstacleCollision[i]) ret = true;
                }
            }
        }

        return ret;
    }


    private void updateGraphsBroadcast() {
        Intent intent = new Intent(MainActivity.mainActivity_NotificationStr);
        //intent.putExtra(RESULT, result);
        sendBroadcast(intent);
    }

    private byte[] short2byte(short[] myData) {
        int shortArrsize = myData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        //writeToFile("", false);
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (myData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (myData[i] >> 8);

            //if (i>10000 && i <10100) {
            //    Log.i("Received value:", String.format("Amplitude is:  %d and bytes %d,%d at loop %d", sData[i],bytes[i * 2],bytes[(i * 2) + 1], i));
            //}
            //writeToFile(String.format("%s\n",Long.toString(sData[i] & 0xFFFF)), true);
            //sData[i] = 0;
        }
        return bytes;
    }


    private short[] byte2short(byte[] myData) {
        int byteArrsize = myData.length;
        short[] shorts = new short[byteArrsize / 2];

        //writeToFile("", false);
        for (int i = 0; i < byteArrsize; i++) {
            shorts[i / 2] += (i % 2 == 0 ? myData[i] : (short)(myData[i] << 8));
        }
        return shorts;
    }



    public static short minValue(final short[] arr) {
        if (arr.length < 0)
            return 0;

        short min = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
        }

        return min;
    }

    public static short maxValue(final short[] arr) {
        if (arr.length < 0)
            return 0;

        short max = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }

        return max;
    }


    void genTone() {

        double instfreq = 0, numerator;
        //append=false;
        //writeToFile("", false);
        for (int i = 0; i < numSample; i++) {
            numerator = (double) (i) / (double) numSample;
            instfreq = freq1 + (numerator * (freq2 - freq1));
            //if ((i % 1000) == 0) {
            //    Log.i("Current Freq:", String.format("Freq is:  %f at loop %d of %d", instfreq, i, numSample));
            //}
            Double s = 0.0;

            if(waveType== WaveType.CHIRP) {
                s = Math.pow(Math.sin(Math.PI * i / (numSample - 1)), 2) * Math.sin(2 * Math.PI * i / (SAMPLING_RATE_IN_HZ / instfreq));
            }
            else if (waveType== WaveType.SINE)
                s = Math.sin(2*Math.PI*testFreq*i/SAMPLING_RATE_IN_HZ);

            MainActivity.sample[i] = s;
            //writeToFile(s.toString()+"\n", true);
        }
        int idx = 0;
        for (final double dVal :  MainActivity.sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767)); // max positive sample for signed 16 bit integers is 32767
            // in 16 bit wave PCM, first byte is the low order byte (pcm: pulse control modulation)
            MainActivity.generatedSnd[idx++] = (byte) (val & 0x00ff);
            MainActivity.generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }


    private int getMaxSupportedSampleRate() {
        /*
         * Valid Audio Sample rates
         *
         * @see <a
         * href="http://en.wikipedia.org/wiki/Sampling_%28signal_processing%29"
         * >Wikipedia</a>
         */
        final int validSampleRates[] = new int[] { 8000, 11025, 16000, 22050,
                32000, 37800, 44056, 44100, 47250, 48000, 50000, 50400, 88200,
                96000, 176400, 192000, 352800, 2822400, 5644800 };
        /*
         * Selecting default audio input source for recording since
         * AudioFormat.CHANNEL_CONFIGURATION_DEFAULT is deprecated and selecting
         * default encoding format.
         */
        for (int i = validSampleRates.length-1; i >= 0; i--) {
            int result = AudioRecord.getMinBufferSize(validSampleRates[i],
                    AudioFormat.CHANNEL_IN_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (result != AudioRecord.ERROR
                    && result != AudioRecord.ERROR_BAD_VALUE && result > 0) {
                // return the mininum supported audio sample rate
                return validSampleRates[i];
            }
        }
        // If none of the sample rates are supported return -1 handle it in
        // calling method
        return -1;
    }

    private void startRecording() {
        AudioManager aManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        String prop = aManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED);
        //int maxSampleRate = getMaxSupportedSampleRate();

        //int bufferSizeFactor = 2;
        //int bufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT) * bufferSizeFactor;

        recorder = new AudioRecord(MediaRecorder.AudioSource.UNPROCESSED, SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();
        isRecording = true;

        try {
            Thread.sleep(recordingDuration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        writeAudioDataToArray();
    }

    private void stopRecording() {
        if (null == recorder) {
            return;
        }

        isRecording = false;

        recorder.stop();

        recorder.release();

        recorder = null;

        recordingThread = null;
    }

    private int HEADER_SIZE = 36;

    private void writeWaveFile(File file, int channels, byte[] content) {
        long fileSize = content.length; //file.length();
        long totalSize = fileSize+HEADER_SIZE; //fileSize+36;
        long byteRate = SAMPLING_RATE_IN_HZ * channels * 2; //2 byte per 1 sample for 1 channel.
        byte[] header = generateHeader(fileSize, totalSize, SAMPLING_RATE_IN_HZ, channels, byteRate);
        try {
            final RandomAccessFile wavFile = randomAccessFile(file);
            wavFile.seek(0); // to the beginning
            wavFile.write(header);
            wavFile.write(content);
            wavFile.close();
        } catch (FileNotFoundException e) {
            Log.e("Exception", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private RandomAccessFile randomAccessFile(File file) {
        RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return randomAccessFile;
    }

    private byte[] generateHeader(
            long totalAudioLen, long totalDataLen, long longSampleRate, int channels,
            long byteRate) {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return header;
    }


    private void writeToFile(String data, boolean append, File f) {
        try {
            FileOutputStream stream = new FileOutputStream(f, append);
            try {
                stream.write(data.getBytes());
            } finally {
                stream.close();
            }
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    private void writeAudioDataToArray() {
        // Write the output audio in byte

        MainActivity.sData = new short[BUFFER_SIZE];

        recorder.read(MainActivity.sData, 0, BUFFER_SIZE);

        MainActivity.bData = short2byte(MainActivity.sData);

        if (outputFile) {
            audioFile = new File(audioFilePath);
            //writeWaveFile(audioFile, AUDIO_FORMAT, bData);
            writeToFile(Arrays.toString(MainActivity.sData).replace(",", "\n"), false, audioFile);
        }

        stopRecording();

        if (MainActivity.allData.remainingCapacity() == 0)
            try {
                MainActivity.allData.take();
            } catch (Exception e) {
                Log.e(TAG, "Failed take : " + e.toString());
            }

        MainActivity.allData.offer(MainActivity.sData.clone());
    }


}
