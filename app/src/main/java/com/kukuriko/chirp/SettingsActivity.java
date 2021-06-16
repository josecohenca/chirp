package com.kukuriko.chirp;

import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private static String DRAW_WAVE_KEY;
    private static String DRAW_SPEC_KEY;
    private static String APPLY_FILTER_KEY;
    private static String APPLY_CONV_KEY;
    private static String APPLY_ENV_KEY;
    private static String DROPDOWN_KEY;
    private static String TESTFREQ_KEY;
    private static String TESTDUR_KEY;
    private static String FILE_KEY;
    //private static String LOOP_KEY;
    //private static String MAX_LOOP_KEY;
    //private static boolean drawWaveCheck = false;
    //private static boolean drawSpecCheck = false;
    //private static boolean applyFilterCheck = false;
    //private static boolean applyConvCheck = false;
    //private static boolean applyEnvCheck = false;
    //private static boolean fileCheck = false;
    //private static int dropDownValue = 0;
    //private static int testFreqValue = 4000;
    //private static int testDurationValue = 50;
    //private static int iterationNumber = 0;
    //private static int maxLoops = 10;
    private static SharedPreferences p;

    public static boolean getDrawWaveCheck(){
        return MainActivity.drawWaveCheck;
    }

    public static boolean getFileCheck(){
        return MainActivity.fileCheck;
    }

    public static boolean getDrawSpecCheck(){
        return MainActivity.drawSpecCheck;
    }

    public static boolean getApplyFilterCheck(){
        return MainActivity.applyFilterCheck;
    }

    public static boolean getApplyConvCheck(){
        return MainActivity.applyConvCheck;
    }

    public static boolean getApplyEnvCheck(){
        return MainActivity.applyEnvCheck;
    }

    public static int getDropDownValue(){
        return MainActivity.dropDownValue;
    }

    public static int getTestFreqValue(){
        return MainActivity.testFreqValue;
    }

    public static int getTestDurationValue(){
        return MainActivity.testDurationValue;
    }

    //public static int getIterationNumber(){
    //    return iterationNumber;
    //}
//
    //public static int getMaxLoops(){
    //    return maxLoops;
    //}


    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        DRAW_WAVE_KEY=getString(R.string.draw_wave_chart_key);
        DRAW_SPEC_KEY=getString(R.string.draw_spec_chart_key);
        APPLY_FILTER_KEY=getString(R.string.apply_filter_key);
        APPLY_CONV_KEY=getString(R.string.apply_convolution_key);
        APPLY_ENV_KEY=getString(R.string.apply_envelope_key);
        DROPDOWN_KEY=getString(R.string.wave_type_key);
        FILE_KEY=getString(R.string.file_key);
        TESTFREQ_KEY=getString(R.string.test_freq_key);
        TESTDUR_KEY=getString(R.string.test_dur_key);
        //LOOP_KEY=getString(R.string.loop_key);
        //MAX_LOOP_KEY=getString(R.string.max_loops_key);

        p = PreferenceManager.getDefaultSharedPreferences(MainActivity.myContext);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference drawWave = findPreference(DRAW_WAVE_KEY);
            drawWave.setDefaultValue(MainActivity.drawWaveCheck);
            bindPreferenceSummaryToValueBoolean(drawWave,false);

            Preference file = findPreference(FILE_KEY);
            file.setDefaultValue(MainActivity.fileCheck);
            bindPreferenceSummaryToValueBoolean(file,false);

            Preference drawSpec = findPreference(DRAW_SPEC_KEY);
            drawSpec.setDefaultValue(MainActivity.drawSpecCheck);
            bindPreferenceSummaryToValueBoolean(drawSpec,false);

            Preference applyFilter = findPreference(APPLY_FILTER_KEY);
            applyFilter.setDefaultValue(MainActivity.applyFilterCheck);
            bindPreferenceSummaryToValueBoolean(applyFilter,false);

            Preference applyConvolution = findPreference(APPLY_CONV_KEY);
            applyConvolution.setDefaultValue(MainActivity.applyConvCheck);
            bindPreferenceSummaryToValueBoolean(applyConvolution,false);

            Preference applyEnvelope = findPreference(APPLY_ENV_KEY);
            applyEnvelope.setDefaultValue(MainActivity.applyEnvCheck);
            bindPreferenceSummaryToValueBoolean(applyEnvelope,false);

            Preference dropdown = findPreference(DROPDOWN_KEY);
            dropdown.setDefaultValue(MainActivity.dropDownValue);
            bindPreferenceSummaryToValueString(dropdown,"1");

            Preference testFreqPref = findPreference(TESTFREQ_KEY);
            testFreqPref.setDefaultValue(MainActivity.testFreqValue);
            bindPreferenceSummaryToValueString(testFreqPref,"4000");

            Preference testDurPref = findPreference(TESTDUR_KEY);
            testDurPref.setDefaultValue(MainActivity.testDurationValue);
            bindPreferenceSummaryToValueString(testDurPref,"50");

            //ListPreference iteration = findPreference(LOOP_KEY);
            //setListPreferenceData(iteration);
            //iteration.setDefaultValue(iterationNumber);
            //bindPreferenceSummaryToValue(iteration);
//
            //EditTextPreference maxLoopsPref = findPreference(MAX_LOOP_KEY);
            //maxLoopsPref.setDefaultValue(maxLoops);
            //bindPreferenceSummaryToValue(maxLoopsPref);

        }
    }
/*
    protected static void setListPreferenceData(ListPreference lp) {
        CharSequence[] entries = new CharSequence[maxLoops];
        CharSequence[] entryValues = new CharSequence[maxLoops];
        List<String> list = new ArrayList<>();
        for(int i=0; i<maxLoops;i++) {
            entries[i] = ((Integer)i).toString();
            entryValues[i] = ((Integer)i).toString();
        }

        lp.setEntries(entries);
        lp.setEntryValues(entryValues);
        lp.setDefaultValue(iterationNumber);
    }

*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private static void bindPreferenceSummaryToValueString(Preference preference, String defaultValue) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, p.getString(preference.getKey(), defaultValue));
    }

    private static void bindPreferenceSummaryToValueBoolean(Preference preference, Boolean defaultValue) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, p.getBoolean(preference.getKey(), defaultValue));
    }
    private static void bindPreferenceSummaryToValueInt(Preference preference, Integer defaultValue) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, p.getInt(preference.getKey(), defaultValue));
    }


    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue = newValue.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof CheckBoxPreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                CheckBoxPreference cbPreference = (CheckBoxPreference) preference;
                if (preference.getKey().equals(DRAW_WAVE_KEY)) {
                    MainActivity.drawWaveCheck = ((Boolean) newValue).booleanValue();
                } else if (preference.getKey().equals(DRAW_SPEC_KEY)) {
                    MainActivity.drawSpecCheck = ((Boolean) newValue).booleanValue();
                } else if (preference.getKey().equals(APPLY_FILTER_KEY)) {
                    MainActivity.applyFilterCheck = ((Boolean) newValue).booleanValue();
                } else if (preference.getKey().equals(APPLY_CONV_KEY)) {
                    MainActivity.applyConvCheck = ((Boolean) newValue).booleanValue();
                } else if (preference.getKey().equals(APPLY_ENV_KEY)) {
                    MainActivity.applyEnvCheck = ((Boolean) newValue).booleanValue();
                } else if (preference.getKey().equals(FILE_KEY)) {
                    MainActivity.fileCheck = ((Boolean) newValue).booleanValue();
                }


            } else if (preference instanceof EditTextPreference) {
                if (preference.getKey().equals(TESTFREQ_KEY)) {
                    // update the changed gallery name to summary filed
                    preference.setSummary(stringValue);
                    MainActivity.testFreqValue = Integer.parseInt(stringValue);
                }
                else if(preference.getKey().equals(TESTDUR_KEY)) {
                    // update the changed gallery name to summary filed
                    preference.setSummary(stringValue);
                    MainActivity.testDurationValue = Integer.parseInt(stringValue);
                }
            } else if (preference instanceof DropDownPreference) {
                //if (preference.getKey().equals(MAX_LOOP_KEY)) {
                //    // update the changed gallery name to summary filed
                //    preference.setSummary(stringValue);
                //    maxLoops=((Integer) newValue).intValue();
                //}
                MainActivity.dropDownValue= (Integer) newValue;
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

}