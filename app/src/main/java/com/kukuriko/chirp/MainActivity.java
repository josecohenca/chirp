package com.kukuriko.chirp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.semantive.waveformandroid.waveform.Segment;
import com.semantive.waveformandroid.waveform.WaveformFragment;
//import org.openimaj.image.DisplayUtilities;
//import org.openimaj.image.ImageUtilities;
//import org.openimaj.image.MBFImage;
//import org.openimaj.video.xuggle.XuggleAudio;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_AUDIO_PERMISSION_RESULT = 12345;
    private final int duration = 50;
    private final int maxRange = 10;
    private final int speedOfSound = 343;
    private final int maxDelayRTT = 2*(1000*maxRange)/speedOfSound;
    private final int SAMPLING_RATE_IN_HZ = 192000;
    private int filterSize = 1024;
    private final double nyqRate = SAMPLING_RATE_IN_HZ/2.0;
    private final int numSample = duration * SAMPLING_RATE_IN_HZ / 1000;
    double sample[] = new double[numSample];
    double testFreq = 8000;
    double freq1 = 18000;
    double freq2 = 23000;
    double guardFreq = 100;
    double testFreqNorm = 8000/nyqRate;
    double freq1Norm = 18000/nyqRate;
    double freq2Norm = 23000/nyqRate;
    private final int numChannels = 2;
    byte[] generatedSnd = new byte[numChannels * numSample];

    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO; // same as (CHANNEL_IN_LEFT | CHANNEL_IN_RIGHT)
    // other MIC combinations: CHANNEL_IN_FRONT_BACK , CHANNEL_IN_X_AXIS , CHANNEL_IN_Y_AXIS , CHANNEL_IN_Z_AXIS
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BPP = 16; //bits per sample
    private final float floatScale = 1 << (RECORDER_BPP - 1);
    private final int recordingDuration = duration + maxDelayRTT;
    private final int BUFFER_SIZE = recordingDuration * numChannels * SAMPLING_RATE_IN_HZ / 1000;

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private int accDetection;
    private int loopSpinnerSelection = 0;

    private int loops;
    private int maxLoops = 10;
    private short[][] allData;

    byte[] bData;
    short[] sData;
    float[][] specData;
    float[][] fData;
    float[][] oData;
    Handler handler = new Handler();
    private CustomWaveformFragment customFragment;
    private FourierTransform ft;
    private BandPassFilter bp;

    private float obstacleSpeed = 0.5f;

    private double oldMean = 0;
    private double lambda = 0.3;

    private float[] distanceCorrection;

    private int[][] peakIndex;
    private int[][] midPeak;
    private float[][] peakWidth;
    private int[][] movingPeakIndex;
    private float[][] movingPeakDistance;
    private float[] movingPeakAngle;
    private float[] movingObstacleAngle;
    private boolean[] movingObstacleCollision;
    private int[][] prevPeakIndex;


    public enum WaveType {
        SINE,
        CHIRP
    }

    public static final float FREQ_LP_BEAT = 150.0f;
    public static final float T_FILTER = (float) (1.0f / (2.0f * Math.PI * FREQ_LP_BEAT));
    public static final float BEAT_RTIME = 0.02f;

    private LineChart chart1;
    private LineChart chart2;
    private BarChart chart3;
    private BarChart chart4;
    private ImageView myImage;

    private Button btStart;
    //private ToggleButton toggleBMP;
    //private ToggleButton toggleFragment;
    private ToggleButton toggleChart;
    private ToggleButton toggleSpectrogram;
    private ToggleButton toggleApplyFilter;
    private ToggleButton toggleConvolveWithOrig;
    private ToggleButton toggleEnvelope;
    private Spinner loopSpinner;

    //private boolean isToggleBMP = false;
    //private boolean isToggleFragment = false;
    private boolean isToggleChart = false;
    private boolean isToggleSpectrogram = false;
    private boolean isToggleApplyFilter = false;
    private boolean isToggleConvolveWithOrig = false;
    private boolean isToggleEnvelope = false;

    private double inch2mFactor = 0.0254;
    private double deviceHeight;
    private double deviceWidth;



    /** The maximum signal value */
    private float maxValue = 100f;

    /** Whether to automatically determine the x scalar */
    private boolean autoFit = true;

    /** Whether to automatically determine the y scalar */
    private boolean autoScale = true;

    /** The scalar in the x direction */
    private float xScale = 1f;

    //private File logFile;
    //private static String logPath;
    private File audioFile;
    private static String audioFilePath;
    private File imageFile;
    private static String imageFilePath;

    private WaveType waveType = WaveType.CHIRP;

    public static String getAudioFilePath(){
        return audioFilePath;
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

    private void setRealDeviceSize() {
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);


        // since SDK_INT = 1;
        int mWidthPixels = displayMetrics.widthPixels;
        int mHeightPixels = displayMetrics.heightPixels;

        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
            try {
                mWidthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                mHeightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (Exception ignored) {
            }
        }

        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                Point realSize = new Point();
                Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
                mWidthPixels = realSize.x;
                mHeightPixels = realSize.y;
            } catch (Exception ignored) {
            }
        }

        deviceWidth = inch2mFactor*mWidthPixels/displayMetrics.xdpi;//displayMetrics.density * 160; //displayMetrics.scaledDensity * 160;
        deviceHeight = inch2mFactor*mHeightPixels/displayMetrics.ydpi;
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

    private void drawImageBmp() {
        Log.d("drawImageBmp", "Started process");

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels/2;
        int width = displayMetrics.widthPixels;

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);

        float startX = 0;
        float startY = 0;
        float stopX = 0;
        float stopY = 0;

        // Work out the y scalar
        float m = this.maxValue;
        if (this.autoScale)
            m = (float) Math.max(
                    Math.abs(minValue(sData)),
                    Math.abs(maxValue(sData)));

        final int nc = 2; //STEREO
        final int channelHeight = height / nc;
        final float scalar = height / (m * 2 * nc);
        final int h = height;

        // Work out the xscalar
        if (this.autoFit)
            this.xScale = width / (sData.length / (float) nc);

        // Plot the wave form
        for (int c = 0; c < nc; c++) {

            final int yOffset = channelHeight * c + channelHeight / 2;
            int lastX = 0;
            int lastY = yOffset;

            for (int i = 0; i < sData.length / nc; i += nc) {

                if(i==1000)
                    Log.i("Made it","Here");
                final int x = (int) (i * this.xScale);
                final int y = (int) (sData[i * nc + c] / Integer.MAX_VALUE * scalar + yOffset);

                //canvas.drawPoint((float) y, (float) x, paint);
                canvas.drawLine(lastX, lastY, x, h - y, paint);
                lastX = x;
                lastY = h - y;

            }

        }

        try (FileOutputStream out = new FileOutputStream(imageFilePath)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("drawImageBmp", "Finished process");

        myImage.setImageBitmap(bitmap);
    }

    private void drawImageFragment(){
        customFragment = new CustomWaveformFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.frameLayout, new CustomWaveformFragment())
                //        .replace(R.id.frameLayout, customFragment, "waveformFragment")
                //        .addToBackStack(null)
                .commit();
    }

    private class LineChartListener implements OnChartValueSelectedListener {
        @Override
        public void onValueSelected(Entry e, Highlight h) {
            Log.i("Entry selected", e.toString());
            Log.i("LOW HIGH", "low: " + chart1.getLowestVisibleX() + ", high: " + chart1.getHighestVisibleX());
            Log.i("MIN MAX", "xMin: " + chart1.getXChartMin() + ", xMax: " + chart1.getXChartMax() + ", yMin: " + chart1.getYChartMin() + ", yMax: " + chart1.getYChartMax());
        }

        @Override
        public void onNothingSelected() {
            Log.i("Nothing selected", "Nothing selected.");
        }
    }

    private void setupChartStyle(LineChart c){

        // background color
        c.setBackgroundColor(Color.WHITE);

        // disable description text
        c.getDescription().setEnabled(false);

        // enable touch gestures
        c.setTouchEnabled(true);

        //// set listeners
        //LineChartListener l = new LineChartListener();
        //chart.setOnChartValueSelectedListener(l);

        c.setDrawGridBackground(false);

        // create marker to display box when values are selected
        MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);

        // Set the marker to the chart
        mv.setChartView(c);
        c.setMarker(mv);

        // enable scaling and dragging
        c.setDragEnabled(true);
        c.setScaleEnabled(true);
        // chart.setScaleXEnabled(true);
        // chart.setScaleYEnabled(true);

        // force pinch zoom along both axis
        c.setPinchZoom(true);


        //XAxis xAxis;
        //{   // // X-Axis Style // //
        //    xAxis = c.getXAxis();
//
        //    // vertical grid lines
        //    xAxis.enableGridDashedLine(10f, 10f, 0f);
        //}
//
        //YAxis yAxis;
        //{   // // Y-Axis Style // //
        //    yAxis = c.getAxisLeft();
//
        //    // disable dual axis (only use LEFT axis)
        //    chart.getAxisRight().setEnabled(false);
//
        //    // horizontal grid lines
        //    yAxis.enableGridDashedLine(10f, 10f, 0f);
//
        //    // axis range
        //    yAxis.setAxisMaximum(200f);
        //    yAxis.setAxisMinimum(-200f);
        //}

    }

    private void setupChartStyle(BarChart c){

        // background color
        c.setBackgroundColor(Color.WHITE);

        // disable description text
        c.getDescription().setEnabled(false);

        // enable touch gestures
        c.setTouchEnabled(true);

        //// set listeners
        //LineChartListener l = new LineChartListener();
        //chart.setOnChartValueSelectedListener(l);

        c.setDrawGridBackground(false);

        // create marker to display box when values are selected
        MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);

        // Set the marker to the chart
        mv.setChartView(c);
        c.setMarker(mv);

        // enable scaling and dragging
        c.setDragEnabled(true);
        c.setScaleEnabled(true);
        // chart.setScaleXEnabled(true);
        // chart.setScaleYEnabled(true);

        // force pinch zoom along both axis
        c.setPinchZoom(true);

    }

    private void setupLineDataSetStyle(LineDataSet lineDataSet){

        lineDataSet.setDrawIcons(false);

        //// draw dashed line
        //lineDataSet.enableDashedLine(10f, 5f, 0f);

        // black lines and points
        lineDataSet.setColor(Color.BLACK);
        //lineDataSet.setCircleColor(Color.BLACK);

        // line thickness and point size
        lineDataSet.setLineWidth(1f);
        //lineDataSet.setCircleRadius(2f);

        // draw points as solid circles
        //lineDataSet.setDrawCircleHole(false);

        // customize legend entry
        lineDataSet.setFormLineWidth(1f);
        //lineDataSet.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
        lineDataSet.setFormSize(15.f);

        // text size of values
        lineDataSet.setValueTextSize(9f);

        //// draw selection line as dashed
        //lineDataSet.enableDashedHighlightLine(10f, 5f, 0f);
//
        //// set the filled area
        //lineDataSet.setDrawFilled(true);
        //lineDataSet.setFillFormatter(new IFillFormatter() {
        //    @Override
        //    public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
        //        return lineChart.getAxisLeft().getAxisMinimum();
        //    }
        //});
//
        //// set color of filled area
        //if (Utils.getSDKInt() >= 18) {
        //    // drawables only supported on api level 18 and above
        //    Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
        //    lineDataSet.setFillDrawable(drawable);
        //} else {
        //    lineDataSet.setFillColor(Color.BLACK);
        //}
    }


    private void setupBarDataSetStyle(BarDataSet barDataSet){

        barDataSet.setDrawIcons(false);

        // black lines and points
        barDataSet.setColor(Color.BLACK);
        barDataSet.setBarBorderColor(Color.BLACK);

        // line thickness and point size
        barDataSet.setBarBorderWidth(1f);

        // customize legend entry
        barDataSet.setFormLineWidth(1f);
        barDataSet.setFormSize(15.f);

        // text size of values
        barDataSet.setValueTextSize(9f);

    }

    private void drawWaveChart(float[][] myData){
        chart1.clear();
        if(myData.length>1) chart2.clear();
        if(oData != null) {
            drawImageLineChart(chart1, myData[0]);
            if(myData.length>1) drawImageLineChart(chart2, myData[1]);
        }
    }

    private void drawSpectrogramChart(float[][] myData){
        chart3.clear();
        if(myData.length>1) chart4.clear();
        if(myData != null) {
            drawImageBarChart(chart3, myData[0]);
            if(myData.length>1) drawImageBarChart(chart4, myData[1]);
        }
    }

    private void drawImageLineChart(LineChart chart, float[] data){
        setupChartStyle(chart);

        ArrayList<Entry> values = new ArrayList<>();

        for (int i = 0; i < data.length; i ++) {
            values.add(new Entry(i, data[i], this.getDrawable(R.drawable.star)));
        }

        LineDataSet set = new LineDataSet(values, "DataSet");

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set); // add the data setsz

        // create a data object with the data sets
        LineData lineData = new LineData(dataSets);

        setupLineDataSetStyle(set);

        // set data
        chart.setData(lineData);
    }

    private void drawImageBarChart(BarChart chart, float[] data){
        setupChartStyle(chart);

        ArrayList<BarEntry> values = new ArrayList<>();

        for (int i = 0; i < data.length; i ++) {
            values.add(new BarEntry(i, data[i], this.getDrawable(R.drawable.star)));
        }

        BarDataSet set = new BarDataSet(values, "DataSet");

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set); // add the data setsz

        // create a data object with the data sets
        BarData barData = new BarData(dataSets);

        setupBarDataSetStyle(set);

        // set data
        chart.setData(barData);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btStart = findViewById(R.id.button1);
        //toggleBMP = findViewById(R.id.button2);
        //toggleFragment = findViewById(R.id.button3);
        toggleChart = findViewById(R.id.button4);
        toggleSpectrogram = findViewById(R.id.button5);
        toggleApplyFilter = findViewById(R.id.button6);
        toggleConvolveWithOrig = findViewById(R.id.button7);
        toggleEnvelope = findViewById(R.id.button8);
        loopSpinner = findViewById(R.id.loop_spinner);
        chart1 = findViewById(R.id.chart1);
        chart2 = findViewById(R.id.chart2);
        chart3 = findViewById(R.id.chart3);
        chart4 = findViewById(R.id.chart4);
        myImage = findViewById(R.id.imageView1);

        List<String> list = new ArrayList<>();
        for(int i=0; i<maxLoops;i++) {
            list.add(((Integer)i).toString());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        loopSpinner.setAdapter(adapter);

        //logPath = this.getFilesDir()+"/log.txt";;
        //logFile = new File(logPath);
        audioFilePath = this.getFilesDir()+"/voice8K16bitstereo.wav";
        imageFilePath = this.getFilesDir()+"/voice8K16bitstereo.png";

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

        setRealDeviceSize();

        this.ft = new FourierTransform(numChannels);
        this.bp = new BandPassFilter( filterSize, (float)((freq2+guardFreq)/nyqRate), (float)((freq1-guardFreq)/nyqRate), numChannels, true);


        try {
            genTone();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        toggleChart.setChecked(false);
        toggleSpectrogram.setChecked(false);
        toggleApplyFilter.setChecked(false);
        toggleConvolveWithOrig.setChecked(false);
        toggleEnvelope.setChecked(false);


        loopSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                loopSpinnerSelection = Integer.parseInt((String)parent.getItemAtPosition(position));
                calculateCurrentLoopVal();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                loopSpinnerSelection = 0;
            }
        });


        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread startRecording = new Thread(new Runnable(){
                    public void run() {
                        handler.post(new Runnable() {
                            public void run() {
                                startRecordLoop();
                            }
                        });
                    }
                });
                startRecording.start();
            }
        });

        toggleChart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isToggleChart=true;
                    drawWaveChart(oData);
                } else {
                    isToggleChart=false;
                    chart1.clear();
                    if(oData.length>1) chart2.clear();
                }
            }
        });


        toggleSpectrogram.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isToggleSpectrogram=true;
                    drawSpectrogramChart(specData);
                } else {
                    isToggleSpectrogram=false;
                    chart3.clear();
                    if(specData.length>1) chart4.clear();
                }
            }
        });


        toggleApplyFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isToggleApplyFilter=true;
                } else {
                    isToggleApplyFilter=false;
                }
            }
        });

        toggleConvolveWithOrig.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isToggleConvolveWithOrig=true;
                } else {
                    isToggleConvolveWithOrig=false;
                }
            }
        });

        toggleEnvelope.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isToggleEnvelope=true;
                } else {
                    isToggleEnvelope=false;
                }
            }
        });

    }


    private void startRecordLoop(){
        loops = 0;
        allData = new short[maxLoops][];

        while(loops<maxLoops) {

            Thread threadPlay = new Thread(new Runnable() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            playSound();
                        }
                    });

                }
            });
            threadPlay.start();

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

            try {
                threadPlay.join();
                threadRecord.join();
            } catch (Exception e) {
                e.printStackTrace();
            }

            loops++;
        }

        calculateCurrentLoopVal();
    }

    private void calculateCurrentLoopVal(){
        sData = allData[loopSpinnerSelection];

        oData = new float[numChannels][];
        for( int c = 0; c < numChannels; c++ ) {
            oData[c] = new float[sData.length/numChannels];
            for (int i = 0; i < sData.length/numChannels; i += numChannels) {
                final float y = (sData[i * numChannels + c]) / floatScale;
                oData[c][i]=y;
            }

        }

        this.ft.process( sData );
        float[][] fftData = this.ft.getLastFFT();
        specData = new float[numChannels][];
        for (int c = 0; c < numChannels / 4; c++) {
            specData[c] = new float[fftData.length / 4];
            for (int i = 0; i < fftData.length / 4; i++) {
                final float re = fftData[c][i * 2];
                final float im = fftData[c][i * 2 + 1];
                specData[c][i] = (float) Math.log(Math.sqrt(re * re + im * im) + 1) / 50f;
            }
        }

        accDetection = 0;
        if (isToggleApplyFilter) applyFilter(sData);
        if (isToggleConvolveWithOrig) applyCorrelation(sData);
        if (isToggleEnvelope){
            if(loopSpinnerSelection>0) {
                for (int i = 0; i <= loopSpinnerSelection; i++) {
                    if (applyEnvelope(allData[i]))
                        accDetection++;
                }
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                Log.d("drawImage", "Update UI");
                //if (isToggleBMP) drawImageBmp();
                //if (isToggleFragment) drawImageFragment();
                if (isToggleChart) drawWaveChart(oData);
                if (isToggleSpectrogram) drawSpectrogramChart(specData);

                if(loopSpinnerSelection>= 9 && accDetection >= 5){
                    Toast.makeText(MainActivity.this, "Obstacle!",
                            Toast.LENGTH_SHORT).show();
                }
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
            //if ((i % 1000) == 0) {
            //    Log.i("Current Freq:", String.format("Freq is:  %f at loop %d of %d", instfreq, i, numSample));
            //}
            Double s = 0.0;

            if(waveType==WaveType.SINE) {
                s = Math.pow(Math.sin(Math.PI * i / (numSample - 1)), 2) * Math.sin(2 * Math.PI * i / (SAMPLING_RATE_IN_HZ / instfreq));
            }
            else if (waveType==WaveType.CHIRP)
                s = Math.sin(Math.PI*testFreq*i/SAMPLING_RATE_IN_HZ);

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
            audioTrack = new AudioTrack(AudioManager.STREAM_ALARM, SAMPLING_RATE_IN_HZ, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length, AudioTrack.MODE_STATIC);
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

        int bufferSizeFactor = 2;
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT) * bufferSizeFactor;

        recorder = new AudioRecord(MediaRecorder.AudioSource.UNPROCESSED, SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();
        isRecording = true;

        try {
            Thread.sleep(recordingDuration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToArray();
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



    private void writeAudioDataToArray() {
        // Write the output audio in byte

        sData = new short[BUFFER_SIZE];

        allData[loops] = sData.clone();

        recorder.read(sData, 0, BUFFER_SIZE);

        bData = short2byte(sData);

        //audioFile = new File(audioFilePath);
        //writeWaveFile(audioFile, AUDIO_FORMAT, bData);

        stopRecording();
    }

    private void applyFilter(short[] myData){
        sData = this.bp.process( myData, numChannels );
        oData = new float[numChannels][];
        for( int c = 0; c < numChannels; c++ ) {
            oData[c] = new float[sData.length/numChannels];
            for (int i = 0; i < sData.length/numChannels; i += numChannels) {
                oData[c][i] = sData[i * numChannels + c];
            }
        }

        this.ft.process( sData );
        float[][] fftData = this.ft.getLastFFT();
        specData = new float[numChannels][];
        for (int c = 0; c < numChannels / 4; c++) {
            specData[c] = new float[fftData.length / 4];
            for (int i = 0; i < fftData.length / 4; i++) {
                final float re = fftData[c][i * 2];
                final float im = fftData[c][i * 2 + 1];
                specData[c][i] = (float) Math.log(Math.sqrt(re * re + im * im) + 1) / 50f;
            }
        }
    }

    private void applyCorrelation(short[] myData){
        sData = MyUtils.convertDoubleShort(Convolution.correlate(MyUtils.convertShortDouble(myData),
                    MyUtils.convertShortDouble(byte2short(generatedSnd)), numChannels));
        oData = new float[numChannels][];
        for( int c = 0; c < numChannels; c++ ) {
            oData[c] = new float[sData.length/numChannels];
            for (int i = 0; i < sData.length/numChannels; i += numChannels) {
                oData[c][i] = (float)sData[i * numChannels + c];
            }
        }

        this.ft.process( sData );
        float[][] fftData = this.ft.getLastFFT();
        specData = new float[numChannels][];
        for (int c = 0; c < numChannels / 4; c++) {
            specData[c] = new float[fftData.length / 4];
            for (int i = 0; i < fftData.length / 4; i++) {
                final float re = fftData[c][i * 2];
                final float im = fftData[c][i * 2 + 1];
                specData[c][i] = (float) Math.log(Math.sqrt(re * re + im * im) + 1) / 50f;
            }
        }

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

        if(oldMean==0) oldMean=mean;
        else oldMean = mean*lambda+(1-lambda)*oldMean;

        distanceCorrection = new float[numChannels];
        peakIndex = new int[numChannels][5];
        peakWidth = new float[numChannels][5];
        midPeak = new int[numChannels][5];
        movingPeakIndex = new int[numChannels][5];
        movingPeakDistance = new float[numChannels][5];
        movingPeakAngle = new float[5];
        movingObstacleAngle = new float[5];
        movingObstacleCollision = new boolean[5];

        oData = new float[numChannels][];
        specData = new float[numChannels][];

        for( int c = 0; c < numChannels; c++ ) {
            filter1Out = 0;
            filter2Out = 0;
            peakEnv = 0.0f;
            peakNum = 0;
            beatPulse = false;
            beatTrigger = false;
            prevBeatPulse = false;
            oData[c] = new float[myData.length / numChannels];
            specData[c] = new float[myData.length / numChannels];
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

                oData[c][i] = peakEnv; // or EnvIn ?

                // Step 3 : Schmitt trigger
                if( !beatTrigger )
                {
                    if( peakEnv > oldMean ){
                        beatTrigger = true;
                        if(peakNum<5) peakIndex[c][peakNum] = i;
                        peakNum++;
                    }
                }
                else
                {
                    if( peakEnv < oldMean*0.5 ){
                        beatTrigger = false;
                        peakWidth[c][peakNum-1] = (i-peakIndex[c][peakNum-1]) * distFactor;
                        midPeak[c][peakNum-1] = (i+peakIndex[c][peakNum-1])/2;
                    }
                }

                //// Step 4 : rising edge detector
                //beatPulse = false;
                //if( (beatTrigger) && (!prevBeatPulse) ) {
                //    beatPulse = true;
                //}
                //prevBeatPulse = beatTrigger;

                specData[c][i] = (beatTrigger ? peakNum : 0);
            }

            distanceCorrection[c] = (peakIndex[c][1] - peakIndex[c][0]) * distFactor;

            if(prevPeakIndex != null) {
                for (int i = 0; i < peakIndex[c].length; i++) {
                    if ((prevPeakIndex[c][i] - peakIndex[c][i]) * distFactor > obstacleSpeed) {
                        movingPeakIndex[c][i] = peakIndex[c][i];
                        movingPeakDistance[c][i] = (peakIndex[c][i] - peakIndex[c][0]) * distFactor;
                    } else {
                        movingPeakIndex[c][i] = -1;
                        movingPeakDistance[c][i] = -1;
                    }
                }
            }
        }
        prevPeakIndex=peakIndex;

        boolean applyDistanceCorrection = false;
        if(numChannels>1) {
            for (int i = 0; i < movingPeakDistance[0].length; i++) {
                if (movingPeakDistance[0][i] > -1 && movingPeakDistance[1][i] > -1) {
                    double d1 = movingPeakDistance[0][i];
                    double d2 = movingPeakDistance[1][i];
                    double d3 = distanceCorrection[1];
                    double lmic = deviceHeight;
                    if (applyDistanceCorrection) d2 -= d3;

                    double peakAngleNum = Math.pow(d1, 2) + Math.pow(lmic, 2) - Math.pow(d2, 2);
                    double peakAngleDenom = 2 * d1 * lmic;
                    movingPeakAngle[i] = (float) Math.toDegrees(Math.PI - Math.acos(peakAngleNum / peakAngleDenom));  //theta

                    double df = d1 + (double) peakWidth[0][i] / (2 * d1);

                    movingObstacleAngle[i] = (float) Math.toDegrees((Math.acos(df / d1)));  //delta
                    movingObstacleCollision[i] = movingPeakAngle[i] <= movingObstacleAngle[i];
                    if(movingObstacleCollision[i]) ret = true;
                }
            }
        }

        return ret;
    }
}