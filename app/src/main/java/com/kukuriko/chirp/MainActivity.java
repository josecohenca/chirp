package com.kukuriko.chirp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.semantive.waveformandroid.waveform.Segment;
import com.semantive.waveformandroid.waveform.WaveformFragment;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.video.xuggle.XuggleAudio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_AUDIO_PERMISSION_RESULT = 12345;
    private final int duration = 2000;
    private final int maxRange = 10;
    private final int speedOfSound = 343;
    private final int maxDelay = (1000*maxRange)/speedOfSound;
    private final int sampleRate = 192000;
    private final int numSample = (duration * sampleRate)/1000;
    double sample[] = new double[numSample];
    double testFreq = 8000;
    double freq1 = 18000;
    double freq2 = 23000;
    byte[] generatedSnd = new byte[2 * numSample];
    byte[] bData;
    Handler handler = new Handler();
    private CustomWaveformFragment customFragment;
    private File audiofile;
    private final int SAMPLING_RATE_IN_HZ = sampleRate;

    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;

    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BPP = 16; //bits per sample

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private final int BUFFER_SIZE_FACTOR = 2;
    private final int BUFFER_SIZE = ((duration+maxDelay) * BUFFER_SIZE_FACTOR * sampleRate)/1000;


    private static String logpath;
    private File file;
    private static String audioFilePath;

    public static String getAudioFilePath(){
        return audioFilePath;
    }

    private void writeToFile(String data, boolean append) {
        try {
            FileOutputStream stream = new FileOutputStream(file, append);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_AUDIO_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not have audio on record", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static Bitmap createBitmap(MBFImage img, Bitmap bmap){
        if (bmap == null || bmap.getWidth() != img.getWidth() || bmap.getHeight() != img.getHeight() || bmap.getConfig() != Bitmap.Config.ARGB_8888){
            bmap = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);
        }
        bmap.setPixels(img.toPackedARGBPixels(), 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());
        return bmap;
    }

    public static MBFImage createMBFImage(Bitmap image, boolean alpha){
        final int[] data = new int[image.getHeight()*image.getWidth()];
        image.getPixels(data, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        return new MBFImage(data, image.getWidth(), image.getHeight(), alpha);
    }

    public static void viewImage(MBFImage img, ImageView view){
        Bitmap image = createBitmap(img, null);
        view.setImageBitmap(image);
    }

    private void drawImage(String filepath){
        final XuggleAudio a = new XuggleAudio( filepath );

        // This is how wide we're going to draw the display
        final int w = 1920;

        // This is how high we'll draw the display
        final int h = 200;

        final MBFImage img = org.openimaj.vis.audio.AudioOverviewVisualisation.
                getAudioWaveformImage( a, w, h, new Float[]{0f,0f,0f,1f},
                        new Float[]{1f,1f,1f,1f} );
        ImageView myImage = (ImageView) findViewById(R.id.imageView1);
        viewImage(img, myImage);
        //// Display the image
        //DisplayUtilities.display( img );
//
        // File f = new File("audioWaveform.png");
        //// Write the image to a file.
        //try
        //{
        //    ImageUtilities.write( img, "png", f );
        //}
        //catch( final IOException e )
        //{
        //    e.printStackTrace();
        //}


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button bt = findViewById(R.id.button);

        logpath = this.getFilesDir()+"/log.txt";;
        file = new File(logpath);
        audioFilePath = this.getFilesDir()+"/voice8K16bitstereo.wav";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                // put your code for Version>=Marshmallow
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(this, "App required access to audio", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO
                }, REQUEST_AUDIO_PERMISSION_RESULT);
            }

        }



        try {
            genTone();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread threadPlay = new Thread(new Runnable() {
                    public void run() {
                        handler.post(new Runnable() {
                            public void run() {
                                playSound();
                            }
                        });

                    }
                });
                //threadPlay.start();

                Thread threadRecord = new Thread(new Runnable() {
                    public void run() {
                        handler.post(new Runnable() {
                            public void run() {
                                startRecording();
                            }
                        });

                    }
                });

                threadRecord.start();
            }
        });

    }


    protected void onResume() {
        super.onResume();
    }

    void genTone() throws IOException {

        double instfreq = 0, numerator;
        //append=false;
        //writeToFile("", false);
        for (int i = 0; i < numSample; i++) {
            numerator = (double) (i) / (double) numSample;
            instfreq = freq1 + (numerator * (freq2 - freq1));
            if ((i % 1000) == 0) {
                Log.i("Current Freq:", String.format("Freq is:  %f at loop %d of %d", instfreq, i, numSample));
            }
            //Double s = Math.pow(Math.sin(Math.PI*i/(numSample-1)),2)*Math.sin(2 * Math.PI * i / (sampleRate / instfreq));
            Double s = Math.sin(Math.PI*testFreq*i/(sampleRate));
            sample[i] = s;
            //writeToFile(s.toString()+"\n", true);
        }
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767)); // max positive sample for signed 16 bit integers is 32767
            // in 16 bit wave PCM, first byte is the low order byte (pcm: pulse control modulation)
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playSound () {
        AudioTrack audioTrack = null;
        try {
            audioTrack = new AudioTrack(AudioManager.STREAM_ALARM, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length, AudioTrack.MODE_STATIC);
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
            audioTrack.play();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        recorder.startRecording();
        isRecording = true;

        try {
            Thread.sleep((1000*BUFFER_SIZE)/sampleRate);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToArray();
                drawImage(audiofile.getAbsolutePath());

                //customFragment = new CustomWaveformFragment();
                //getSupportFragmentManager().beginTransaction()
                //        .add(R.id.frameLayout, new CustomWaveformFragment())
                ////        .replace(R.id.frameLayout, customFragment, "waveformFragment")
                ////        .addToBackStack(null)
                //        .commit();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
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

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        //writeToFile("", false);
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);

            //if (i>10000 && i <10100) {
            //    Log.i("Received value:", String.format("Amplitude is:  %d and bytes %d,%d at loop %d", sData[i],bytes[i * 2],bytes[(i * 2) + 1], i));
            //}
            //writeToFile(String.format("%s\n",Long.toString(sData[i] & 0xFFFF)), true);
            sData[i] = 0;
        }
        return bytes;
    }


    private int HEADER_SIZE = 36;

    private void writeWaveFile(File file, int channels, byte[] content) {
        long fileSize = content.length; //file.length();
        long totalSize = fileSize+HEADER_SIZE; //fileSize+36;
        long byteRate = sampleRate * channels * 2; //2 byte per 1 sample for 1 channel.
        byte[] header = generateHeader(fileSize, totalSize, sampleRate, channels, byteRate);
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



    private void writeAudioDataToArray() {
        // Write the output audio in byte


        short sData[] = new short[BUFFER_SIZE];

        recorder.read(sData, 0, BUFFER_SIZE);

        bData = short2byte(sData);

        audiofile = new File(audioFilePath);
        writeWaveFile(file, AUDIO_FORMAT, bData);

        stopRecording();
    }
}