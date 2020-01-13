package com.kukuriko.chirp;

import com.semantive.waveformandroid.waveform.Segment;
import com.semantive.waveformandroid.waveform.WaveformFragment;

import java.util.Arrays;
import java.util.List;
import android.graphics.Color;

public class CustomWaveformFragment extends WaveformFragment {

    /**
     * Provide path to your audio file.
     *
     * @return
     */
    @Override
    protected String getFileName() {
        return MainActivity.getAudioFilePath();
    }


    ///**
    // * Optional - provide list of segments (start and stop values in seconds) and their corresponding colors
    // *
    // * @return
    // */
    //@Override
    //protected List<Segment> getSegments() {
    //    return Arrays.asList(
    //            new Segment(55.2, 55.8, Color.rgb(238, 23, 104)),
    //            new Segment(56.2, 56.6, Color.rgb(238, 23, 104)),
    //            new Segment(58.4, 59.9, Color.rgb(184, 92, 184)));
    //}
}