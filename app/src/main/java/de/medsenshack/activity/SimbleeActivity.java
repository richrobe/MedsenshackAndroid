package de.medsenshack.activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.util.Locale;

import de.fau.lme.plotview.Plot;
import de.fau.lme.plotview.PlotView;
import de.fau.lme.sensorlib.dataframe.SimbleeMedhackDataFrame;
import de.medsenshack.R;
import de.medsenshack.StreamingActivity;
import de.medsenshack.data.ActivityClass;
import de.medsenshack.data.storage.AnnotationWriter;


public class SimbleeActivity extends StreamingActivity implements ActionBar.TabListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = SimbleeActivity.class.getSimpleName();


    private ToggleButton mToggleButton1;
    private ToggleButton mToggleButton2;
    private ToggleButton mToggleButton3;
    private ToggleButton mToggleButton4;

    private AnnotationWriter annotationWriter = null;


    //private TextView mReceiveTextView;
    //private Button mSendButton;
    //private EditText mEditText;
    private PlotView mLiveAccPlotView;
    private static Plot mLiveAccXPlot;
    private static Plot mLiveAccYPlot;
    private static Plot mLiveAccZPlot;


    private PlotView mLiveEcgPlotView;
    private static Plot mLiveEcgPlot;
    private PlotView mLiveGsrPlotView;
    private static Plot mLiveGsrPlot;

    private static final int SECTION_COUNT = 3;
    private static Context sContext;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v13.app.FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    /**
     * The {@link android.support.v4.view.ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simblee);
        setActivity(this, true);
        sContext = this;
        createUiElements();

    }

    @Override
    protected void createUiElements() {
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(SECTION_COUNT - 1);

        if (mActionBar != null) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            // When swiping between different sections, select the corresponding
            // tab. We can also use ActionBar.Tab#select() to do this if we have
            // a reference to the Tab.
            mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    mActionBar.setSelectedNavigationItem(position);
                }
            });
            // For each of the sections in the app, add a tab to the action bar.
            for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
                // Create a tab with text corresponding to the page title defined by
                // the adapter. Also specify this Activity object, which implements
                // the TabListener interface, as the callback (listener) for when
                // this tab is selected.
                mActionBar.addTab(
                        mActionBar.newTab()
                                .setText(mSectionsPagerAdapter.getPageTitle(i))
                                .setTabListener(this));
            }
        }

        mToggleButton1 = (ToggleButton) findViewById(R.id.toggle_1);
        mToggleButton2 = (ToggleButton) findViewById(R.id.toggle_2);
        mToggleButton3 = (ToggleButton) findViewById(R.id.toggle_3);
        mToggleButton4 = (ToggleButton) findViewById(R.id.toggle_4);
        mToggleButton1.setOnCheckedChangeListener(this);
        mToggleButton2.setOnCheckedChangeListener(this);
        mToggleButton3.setOnCheckedChangeListener(this);
        mToggleButton4.setOnCheckedChangeListener(this);

//        mReceiveTextView = (TextView) findViewById(R.id.tv_receive);
//        mSendButton = (Button) findViewById(R.id.button_send);
//        mSendButton.setOnClickListener(this);
//        mSendButton.setEnabled(false);
//        mEditText = (EditText) findViewById(R.id.edit_text);

        /*mLiveAccPlotView = (PlotView) findViewById(R.id.pv_live_acc);
        mLiveEcgPlotView = (PlotView) findViewById(R.id.pv_live_ecg);
        mLiveGsrPlotView = (PlotView) findViewById(R.id.pv_live_gsr);

        if (mLiveAccXPlot == null) {
            int color = getResources().getColor(R.color.status_bar_connected);
            mLiveAccXPlot = new SamplingPlot("", Plot.generatePlotPaint(5f, 255,
                    ((color >> 16) & 0xFF), ((color >> 8) & 0xFF), (color & 0xFF)),
                    Plot.PlotStyle.LINE, 250000);
            ((SamplingPlot) mLiveAccXPlot).setViewport(250, 10);
            mLiveAccXPlot.hideAxis(true);

            color = getResources().getColor(R.color.status_bar_connecting);
            mLiveAccYPlot = new SamplingPlot("", Plot.generatePlotPaint(5f, 255,
                    ((color >> 16) & 0xFF), ((color >> 8) & 0xFF), (color & 0xFF)),
                    Plot.PlotStyle.LINE, 250000);
            ((SamplingPlot) mLiveAccYPlot).setViewport(250, 10);
            mLiveAccYPlot.hideAxis(true);

            color = getResources().getColor(R.color.status_bar_simulating);
            mLiveAccZPlot = new SamplingPlot("", Plot.generatePlotPaint(5f, 255,
                    ((color >> 16) & 0xFF), ((color >> 8) & 0xFF), (color & 0xFF)),
                    Plot.PlotStyle.LINE, 250000);
            ((SamplingPlot) mLiveAccZPlot).setViewport(250, 10);
            mLiveAccZPlot.hideAxis(true);

            color = getResources().getColor(R.color.colorAccentDark);
            mLiveEcgPlot = new SamplingPlot("", Plot.generatePlotPaint(5f, 255,
                    ((color >> 16) & 0xFF), ((color >> 8) & 0xFF), (color & 0xFF)),
                    Plot.PlotStyle.LINE, 250000);
            ((SamplingPlot) mLiveEcgPlot).setViewport(250, 10);
            mLiveEcgPlot.hideAxis(true);

            color = getResources().getColor(R.color.colorPrimaryDark);
            mLiveGsrPlot = new SamplingPlot("", Plot.generatePlotPaint(5f, 255,
                    ((color >> 16) & 0xFF), ((color >> 8) & 0xFF), (color & 0xFF)),
                    Plot.PlotStyle.LINE, 250000);
            ((SamplingPlot) mLiveGsrPlot).setViewport(250, 10);
            mLiveGsrPlot.hideAxis(true);
        } else {
            mLiveAccXPlot.clear();
            mLiveAccYPlot.clear();
            mLiveAccZPlot.clear();
            mLiveEcgPlot.clear();
            mLiveGsrPlot.clear();
            mLiveAccPlotView.requestRedraw(false);
            mLiveEcgPlotView.requestRedraw(false);
            mLiveGsrPlotView.requestRedraw(false);
        }
        mLiveAccPlotView.setVisibility(View.VISIBLE);
        mLiveAccPlotView.attachPlot(mLiveAccXPlot);
        mLiveAccPlotView.attachPlot(mLiveAccYPlot);
        mLiveAccPlotView.attachPlot(mLiveAccZPlot);
        mLiveAccPlotView.setMaxRedrawRate(40);

        mLiveEcgPlotView.setVisibility(View.VISIBLE);
        mLiveEcgPlotView.attachPlot(mLiveEcgPlot);
        mLiveEcgPlotView.setMaxRedrawRate(40);

        mLiveGsrPlotView.setVisibility(View.VISIBLE);
        mLiveGsrPlotView.attachPlot(mLiveGsrPlot);
        mLiveGsrPlotView.setMaxRedrawRate(40);*/

        mFABToggle = false;
        mPauseButtonPressed = false;
    }

    @Override
    protected void clearUi() {
        /*mLiveAccXPlot.clear();
        mLiveAccYPlot.clear();
        mLiveAccZPlot.clear();
        mLiveEcgPlot.clear();
        mLiveGsrPlot.clear();*/
    }

    @Override
    public void onSensorConnected() {
        super.onSensorConnected();
        //mSendButton.setEnabled(true);
    }

    @Override
    public void onSensorDisconnected() {
        super.onSensorDisconnected();
        //mSendButton.setEnabled(false);
    }

    @Override
    public void onSegmentationFinished() {

    }

    @Override
    public void onStartStreaming() {
        super.onStartStreaming();
        annotationWriter = new AnnotationWriter("annotation");
        annotationWriter.prepareWriter();
    }

    @Override
    public void onStopStreaming() {
        super.onStopStreaming();
        annotationWriter.completeWriter();
        annotationWriter = null;
    }

    @Override
    public void onDataReceived(SimbleeMedhackDataFrame data) {

        /*if (mLiveAccXPlot != null) {
            if (data instanceof SimbleeMedhackAccDataFrame) {
                SimbleeMedhackAccDataFrame tmp = (SimbleeMedhackAccDataFrame) data;
                ((SamplingPlot) mLiveAccXPlot).addValue((float) tmp.accX / 2, tmp.timeStamp);

                ((SamplingPlot) mLiveAccYPlot).addValue((float) tmp.accY, tmp.timeStamp);

                ((SamplingPlot) mLiveAccZPlot).addValue((float) tmp.accZ, tmp.timeStamp);
            }

        }*/
    }

    @Override
    public void onPlotMarker(Plot.PlotMarker marker, int signalId, int index) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //case R.id.button_send:
            //mService.sendSimblee(mEditText.getText().toString());
            //break;
            case R.id.fab_button:
                if (!mFABToggle && !mStreaming) {
                    startDailyHeart();
                }
                animateFAB();
                break;
            case R.id.button_stop:
                onStopButtonClick();
                break;
            case R.id.button_pause:
                onPauseButtonClick();
                break;
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
            compoundButton.setTextColor(getResources().getColor(R.color.colorAccent));
            switch (compoundButton.getId()) {
                case R.id.toggle_1:
                    mToggleButton2.setChecked(false);
                    mToggleButton3.setChecked(false);
                    mToggleButton4.setChecked(false);
                    if(annotationWriter!=null) {
                        annotationWriter.writeData(ActivityClass.SIT);
                    }
                    break;
                case R.id.toggle_2:
                    mToggleButton1.setChecked(false);
                    mToggleButton3.setChecked(false);
                    mToggleButton4.setChecked(false);
                    if(annotationWriter!=null) {
                        annotationWriter.writeData(ActivityClass.WALK);
                    }
                    break;
                case R.id.toggle_3:
                    mToggleButton1.setChecked(false);
                    mToggleButton2.setChecked(false);
                    mToggleButton4.setChecked(false);
                    if(annotationWriter!=null) {
                        annotationWriter.writeData(ActivityClass.STAIRS_UP);
                    }
                    break;
                case R.id.toggle_4:
                    mToggleButton1.setChecked(false);
                    mToggleButton2.setChecked(false);
                    mToggleButton3.setChecked(false);
                    if(annotationWriter!=null) {
                        annotationWriter.writeData(ActivityClass.RUN);
                    }
                    break;
            }
        } else {
            compoundButton.setTextColor(getResources().getColor(R.color.grey_800));
            if(annotationWriter!=null) {
                annotationWriter.writeData(ActivityClass.IDLE);
            }
        }
    }

    /**
     * A {@link android.app.Fragment} showing general information.
     */
    public static class GeneralFragment extends Fragment {

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static GeneralFragment newInstance(int sectionNumber) {
            GeneralFragment fragment = new GeneralFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_general, container, false);

            return rootView;
        }

        /**
         * Updates all Views of this {@link android.app.Fragment}.
         */
        public void update() {

        }

        /**
         * Resets all Views of this {@link android.app.Fragment}.
         */
        public void resetUi() {
        }
    }


    /**
     * A {@link android.app.Fragment} showing general information.
     */
    public static class AccGyrFragment extends Fragment {

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static AccGyrFragment newInstance(int sectionNumber) {
            AccGyrFragment fragment = new AccGyrFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_acc_gyr, container, false);

            return rootView;
        }

        /**
         * Updates all Views of this {@link android.app.Fragment}.
         */
        public void update() {

        }

        /**
         * Resets all Views of this {@link android.app.Fragment}.
         */
        public void resetUi() {
        }
    }

    /**
     * A {@link android.app.Fragment} showing general information.
     */
    public static class EcgGsrFragment extends Fragment {

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static EcgGsrFragment newInstance(int sectionNumber) {
            EcgGsrFragment fragment = new EcgGsrFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_ecg_gsr, container, false);

            return rootView;
        }

        /**
         * Updates all Views of this {@link android.app.Fragment}.
         */
        public void update() {

        }

        /**
         * Resets all Views of this {@link android.app.Fragment}.
         */
        public void resetUi() {
        }
    }


    /**
     * A {@link android.support.v13.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SparseArray<Fragment> registeredFragments = new SparseArray<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return GeneralFragment.newInstance(position + 1);
                case 1:
                    return AccGyrFragment.newInstance(position + 1);
                case 2:
                    return EcgGsrFragment.newInstance(position + 1);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return SECTION_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return SimbleeActivity.sContext.getString(R.string.title_overview).toUpperCase(l);
                case 1:
                    return SimbleeActivity.sContext.getString(R.string.title_accgyr).toUpperCase(l);
                case 2:
                    return SimbleeActivity.sContext.getString(R.string.title_ecggsr).toUpperCase(l);
            }
            return null;
        }
    }
}
