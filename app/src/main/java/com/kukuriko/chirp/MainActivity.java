package com.kukuriko.chirp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
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
//import org.openimaj.image.DisplayUtilities;
//import org.openimaj.image.ImageUtilities;
//import org.openimaj.image.MBFImage;
//import org.openimaj.video.xuggle.XuggleAudio;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_AUDIO_PERMISSION_RESULT = 12345;
    private static final int numChannels = 2;

    protected static String mainActivity_NotificationStr="mainActivity_NotificationStr";

    protected static double[] sample;
    protected static byte[] generatedSnd;


    protected static int maxLoops = 10;
    protected static ArrayBlockingQueue<short[]> allData;

    protected static byte[] bData;
    protected static short[] sData;
    protected static float[][] specData;
    protected static float[][] oData;


    protected static float obstacleSpeed = 0.5f;

    protected static double oldMean = 0;
    protected static double lambda = 0.3;
    protected static double detectionLambda = 0.5;

    protected static float[] distanceCorrection;

    protected static int[][] peakIndex;
    protected static int[][] midPeak;
    protected static float[][] peakWidth;
    protected static int[][] movingPeakIndex;
    protected static float[][] movingPeakDistance;
    protected static float[] movingPeakAngle;
    protected static float[] movingObstacleAngle;
    protected static boolean[] movingObstacleCollision;
    protected static int[][] prevPeakIndex;



    private LineChart chart1;
    private LineChart chart2;
    private LineChart chart3;
    private LineChart chart4;
    private ImageView myImage;

    private Button btStart;
    private Button btStop;
    //private ToggleButton toggleBMP;
    //private ToggleButton toggleFragment;
    //private ToggleButton toggleApplyFilter;
    //private ToggleButton toggleConvolveWithOrig;
    //private ToggleButton toggleEnvelope;
    //private Spinner loopSpinner;
    private Toolbar toolbar;

    //private boolean isToggleBMP = false;
    //private boolean isToggleFragment = false;
    //private boolean isToggleApplyFilter = false;
    //private boolean isToggleConvolveWithOrig = false;
    //private boolean isToggleEnvelope = false;

    private int chartSize = 300;

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

    protected static double inch2mFactor = 0.0254;
    protected static double deviceHeight;
    protected static double deviceWidth;


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

        // includes window decorations (statusbar bar/menu_main bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
            try {
                mWidthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                mHeightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (Exception ignored) {
            }
        }

        // includes window decorations (statusbar bar/menu_main bar)
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

/*
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
*/

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
        lineDataSet.setColor(Color.BLUE);

        lineDataSet.setDrawCircles(false);

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
        barDataSet.setColor(Color.BLUE);
        barDataSet.setBarBorderColor(Color.BLUE);

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
        chart2.clear();
        chart1.setMinimumHeight(0);
        chart2.setMinimumHeight(0);
        if(myData != null) {
            drawImageLineChart(chart1, myData[0]);
            if(MainService.getNumChannels()>1)
                drawImageLineChart(chart2, myData[1]);
        }
    }

    private void drawSpectrogramChart(float[][] myData){
        chart3.clear();
        chart4.clear();
        chart3.setMinimumHeight(0);
        chart4.setMinimumHeight(0);
        if(myData != null) {
            drawImageLineChart(chart3, myData[0]);
            if(MainService.getNumChannels()>1)
                drawImageLineChart(chart4, myData[1]);
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
        chart.setMinimumHeight(chartSize);
        //chart.invalidate();
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
        chart.setMinimumHeight(chartSize);
        //chart.invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (mainActivity_NotificationStr.equals(action)) {
                updateGraphs();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btStart = findViewById(R.id.button1);
        btStop= findViewById(R.id.button9);
        //toggleBMP = findViewById(R.id.button2);
        //toggleFragment = findViewById(R.id.button3);
        //toggleApplyFilter = findViewById(R.id.button6);
        //toggleConvolveWithOrig = findViewById(R.id.button7);
        //toggleEnvelope = findViewById(R.id.button8);
        //loopSpinner = findViewById(R.id.loop_spinner);
        chart1 = findViewById(R.id.chart1);
        chart2 = findViewById(R.id.chart2);
        chart3 = findViewById(R.id.chart3);
        chart4 = findViewById(R.id.chart4);
        //myImage = findViewById(R.id.imageView1);

        List<String> list = new ArrayList<>();
        for(int i=0; i<maxLoops;i++) {
            list.add(((Integer)i).toString());
        }

        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //loopSpinner.setAdapter(adapter);

        //logPath = this.getFilesDir()+"/log.txt";;
        //logFile = new File(logPath);

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
        registerReceiver(receiver, new IntentFilter(mainActivity_NotificationStr));
        //toggleApplyFilter.setChecked(false);
        //toggleConvolveWithOrig.setChecked(false);
        //toggleEnvelope.setChecked(false);



        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent serviceIntent = new Intent(MainActivity.this, MainService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                }

            }
        });


        btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(MainActivity.this, MainService.class);
                stopService(serviceIntent);

            }
        });


    }

    private void updateGraphs(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                Log.d("drawImage", "Update UI");
                //if (isToggleBMP) drawImageBmp();
                //if (isToggleFragment) drawImageFragment();
                if (SettingsActivity.getDrawWaveCheck()) {
                    drawWaveChart(oData);
                }
                if (SettingsActivity.getDrawSpecCheck()) {
                    drawSpectrogramChart(specData);
                }

            }
        });
    }


    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(receiver);
        super.onDestroy();
    }

}