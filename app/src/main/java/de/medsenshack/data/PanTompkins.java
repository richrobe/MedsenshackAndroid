package de.medsenshack.data;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;

import de.fau.lme.plotview.FloatValueList;
import de.fau.lme.plotview.ObjectValueList;

import static de.medsenshack.data.PanTompkins.QRS.QrsArrhythmia;
import static de.medsenshack.data.PanTompkins.QRS.QrsClass;
import static de.medsenshack.data.PanTompkins.QRS.SegmentationStatus;

/**
 * Implements the Pan-Tompkins QRS detection algorithm, including further processing steps like template matching.
 *
 * @author Stefan Gradl
 */
public class PanTompkins extends LmeFilter {

    /**
     * total group delay of the entire filter pipeline
     */
    public static final int TOTAL_DELAY = 24;
    /**
     * LOW-PASS filter
     */
    public static final double[] lp_a = {1.0, 2.0, -1.0};
    public static final double[] lp_b = {0.03125, 0, 0, 0, 0, 0, -0.0625, 0, 0, 0, 0, 0, 0.03125};
    /**
     * HIGH-PASS filter
     */
    public static final double[] hp_a = {1.0, 1.0};
    public static final double[] hp_b = {-0.03125, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0, -1.0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.03125};
    /**
     * DIFF filter
     */
    public static final double[] diff_a = {8.0};
    public static final double[] diff_b = {2.0, 1.0, 0.0, -1.0, -2.0};
    public static int samplingRate = 250;
    /**
     * sampling time in ms
     */
    public static float samplingTime = 1000 / samplingRate;
    public static boolean learning = true;
    public LmeFilter lowpass = new LmeFilter(lp_b, lp_a);
    public LmeFilter highpass = new LmeFilter(hp_b, hp_a);
    public LmeFilter diff = new LmeFilter(diff_b, diff_a);
    public MeanFilter mean;
    public WndIntFilter wndInt;
    public MeanFilter wndMean;
    public double qrsThreshold = 1;
    public int maxQrsSize = 1;
    /**
     * length of the integrator window
     */
    public int wndLength = 1;
    /**
     * amount of samples to copy pre R
     */
    public int preSegment = 1;
    /**
     * amount of samples to copy post R
     */
    public int postSegment = 1;
    public int rPassNum;

    /**
     * Represents a detected QRS complex
     *
     * @author sistgrad
     *
     */
    /**
     * the last 8 QRS complexes
     */
    public ObjectValueList qrsHistory = new ObjectValueList(8);
    public QRS qrsRefTemp1;
    public QRS qrsRefTemp2;
    public StepHistory bandOut;
    public StepHistory intOut;
    public StatFilter heartRateStats = new StatFilter(3);
    public StatFilter qrstaStats = new StatFilter(3);
    public MeanFilter rrMeanLong = new MeanFilter(16);
    public StatFilter rrStats = new StatFilter(8);
    public StdFilter stdStats = new StdFilter(16);
    public LinkedList<Double> rrIntervals = new LinkedList<>();
    public boolean hrvDataReady;
    public LinkedList<Double> standardDeviationRR = new LinkedList<>();
    public LinkedList<Double> successiveDifferences = new LinkedList<>();
    public LinkedList<Double> successiveDifferencesRms = new LinkedList<>();
    public LinkedList<Double> successiveDifferencesStd = new LinkedList<>();
    public int numTotalBeats = 0;
    public int numRr50 = 0;
    public int numRr20 = 0;
    public double pRr50 = 0.0;
    public double pRr20 = 0.0;
    public PeakDetectionFilter risingPeak = new PeakDetectionFilter(3, 0);
    public PeakDetectionFilter rPeak = new PeakDetectionFilter(1, 0);
    public int lastBandPeak;
    public int lastCrossing;
    public MinDetectionFilter qPeak = new MinDetectionFilter(1, 0);
    public MinDetectionFilter sPeak = new MinDetectionFilter(1, 0);
    public int startProcessing = 100;
    public int beatCounter;
    public int timeLastBeat;
    public double wndIntCompensation = 0.85;
    private long mOldTimestamp = 0;

    public PanTompkins(int samplingRate) {
        PanTompkins.samplingRate = samplingRate;
        samplingTime = 1000.0f / samplingRate;

        wndLength = (int) (150.0 * samplingRate / 1000.0);

        preSegment = (int) (120.0 * samplingRate / 1000.0);
        postSegment = (int) (280.0 * samplingRate / 1000.0);

        // buffer for historic values, MUST be > wndLength + filterDelay
        maxQrsSize = preSegment + postSegment;
        if (maxQrsSize < wndLength + TOTAL_DELAY + 2) {
            maxQrsSize = wndLength + TOTAL_DELAY + 2;
        }

        mean = new MeanFilter((int) (350.0 * samplingRate / 1000.0));

        // window integrator width proposed by Pan&Tompkins: 150ms
        wndInt = new WndIntFilter(wndLength);

        Log.d("lme.pants", "sampling: " + samplingRate +
                ", wndLength: " + wndLength);

        // mean of the wnd integrator output over 150ms
        wndMean = new MeanFilter(maxQrsSize);

        bandOut = new StepHistory(maxQrsSize);
        intOut = new StepHistory(maxQrsSize);

        // init QRS history
        for (int i = 0; i < qrsHistory.sizeMax; i++) {
            qrsHistory.values[i] = new QRS(maxQrsSize);
        }

        QRS.template1 = new QRS(maxQrsSize);
        QRS.template2 = new QRS(maxQrsSize);

        QRS.qrsCurrent = (QRS) qrsHistory.next();
        QRS.qrsCurrent.reset();

        QRS.qrsPrevious = null;

        // start processing after 2 seconds
        startProcessing = samplingRate << 1;

        y = new double[12];
        beatCounter = 0;
        learning = true;
    }

    private static double calculateStd(LinkedList<? extends Number> values) {
        double mean = 0.0;
        double meanSquare = 0.0;
        for (int i = 0; i < values.size(); i++) {
            mean += (Double) values.get(i);
            meanSquare += ((Double) values.get(i) * (Double) values.get(i));
        }
        mean /= (double) values.size();
        meanSquare /= (double) values.size();

        return Math.sqrt(meanSquare - mean * mean);
    }

    private static double calculateRMS(LinkedList<? extends Number> values) {
        double meanSquare = 0.0;

        for (int i = 0; i < values.size(); i++) {
            meanSquare += (Double) values.get(i) * (Double) values.get(i);
        }
        meanSquare /= values.size();

        return Math.sqrt(meanSquare);
    }

    /**
     * @param xnow
     * @param timestamp
     * @return
     */

    public double next(double xnow, long timestamp) {

        y[1] = xnow;

        // LOW PASS (5 samples delay)
        y[2] = lowpass.next(y[1]);

        // HIGH PASS (16 samples delay
        y[3] = highpass.next(y[2]);

        // save original ECG after bandpass filtering
        bandOut.add(y[3]);

        // DIFFERENTIATOR (2 samples delay)
        y[4] = diff.next(y[3]);

        // SQUARING
        y[5] = y[4] * y[4];

        // WND INTEGRATOR
        y[6] = wndInt.next(y[5]);

        // save value in history
        intOut.add(y[6]);

        //wndOut-mean
        y[7] = wndMean.next(y[6]);

        // all further processing is only done after an initial timeout,
        // is only used to handle display issues with the plot views

        if (startProcessing <= 0) {
            // check for potential cardiac arrest
            if (timeLastBeat > 3500) {
                QRS.qrsCurrent.rIdx = 0;
                QRS.qrsCurrent.rTimestamp = timestamp;
                QRS.qrsCurrent.rAmplitude = y[3];
                QRS.qrsCurrent.classification = QrsClass.VIRTUAL;
                QRS.qrsCurrent.arrhythmia = QrsArrhythmia.CARDIAC_ARREST;
                QRS.qrsCurrent.feat_width = timeLastBeat;
                QRS.qrsCurrent.segState = QRS.SegmentationStatus.FINISHED;
                return y[6];
            }

            qrsThreshold = y[7];

            // is intOut or bandOut above threshold?
            if (y[3] > qrsThreshold || y[6] > qrsThreshold ||
                    QRS.qrsCurrent.segState == SegmentationStatus.R_FOUND) {

                lastCrossing++;

                if (QRS.qrsCurrent.segState == SegmentationStatus.INVALID) {

                    // initialize R peak detector
                    rPeak.reset();
                    rPeak.next(bandOut.history.getPastValue(2));
                    rPeak.next(bandOut.history.getPastValue(1));
                    rPeak.next(y[3]);

                    lastCrossing = 0;
                    QRS.qrsCurrent.segState = SegmentationStatus.THRESHOLD_CROSSED;
                }

                if (QRS.qrsCurrent.segState == SegmentationStatus.THRESHOLD_CROSSED) {
                    if (lastCrossing > preSegment &&
                            QRS.template2.classification == QrsClass.NORMAL) {

                        // if lastCrossing is larger than preSegment samples but
                        // no R peak was found it was an aberrant beat.
                        // It is only considered if we already have two template beats
                        Log.d("lme.pants", "abb beat " + lastCrossing);
                        QRS.qrsCurrent.rIdx = 0;
                        QRS.qrsCurrent.rTimestamp = timestamp;
                        QRS.qrsCurrent.rAmplitude = y[3];
                        QRS.qrsCurrent.classification = QrsClass.ABERRANT;
                        QRS.qrsCurrent.arrhythmia = QrsArrhythmia.ARTIFACT;
                        QRS.qrsCurrent.feat_width = lastCrossing;
                        QRS.qrsCurrent.segState = SegmentationStatus.FINISHED;

                    }
                }
            } else {

                // below all thresholds
                if (lastCrossing > 0) {
                    lastCrossing--;
                }

                if (QRS.qrsCurrent.segState == SegmentationStatus.PROCESSED) {
                    // QRS was processed, reset
                    QRS.qrsCurrent = (QRS) qrsHistory.next();
                    QRS.qrsCurrent.reset();
                }
            }

            // check for mean crossing
            if (QRS.qrsCurrent.segState == SegmentationStatus.THRESHOLD_CROSSED) {

                // R peak detector
                rPeak.next(y[3]);

                // rising peak detector
                risingPeak.reset();
                risingPeak.next(y[6]);

                qPeak.reset();
                sPeak.reset();

                if (rPeak.peakIdx != -1) {

                    // R peak found, check if intOut is above threshold
                    if (y[6] < qrsThreshold) {
                        if (lastCrossing > 0) {
                            rPeak.reset();
                            QRS.qrsCurrent.segState = SegmentationStatus.THRESHOLD_CROSSED;
                            lastCrossing = (int) (-1000 * samplingTime);
                            return y[6];
                        }
                    }

                    // reverse copy all QRS values
                    int i;
                    for (i = 0; i <= preSegment; i++) {

                        // from bandfiltered signal
                        y[8] = bandOut.history.getPastValue(preSegment - i);

                        // to current QRS object
                        QRS.qrsCurrent.values.add((float) y[8]);

                        // find Q only if it hasn't been found yet
                        if (QRS.qrsCurrent.qIdx == -1) {

                            // find q-min
                            qPeak.next(bandOut.history.getPastValue(i));
                            if (qPeak.peakIdx != -1) {
                                QRS.qrsCurrent.qAmplitude = qPeak.peakValue;
                                QRS.qrsCurrent.qIdx = preSegment - i;
                            }
                        }
                    }

                    // if no Q has been found, we use the first sample
                    if (QRS.qrsCurrent.qIdx == -1) {
                        QRS.qrsCurrent.qAmplitude = QRS.qrsCurrent.values.values[0];
                        QRS.qrsCurrent.qIdx = 0;
                    }

                    // R peak in filtered signal
                    QRS.qrsCurrent.rIdx = QRS.qrsCurrent.values.head - rPeak.peakIdx;
                    QRS.qrsCurrent.rAmplitude = rPeak.peakValue;
                    QRS.qrsCurrent.rTimestamp = (long) (timestamp - rPeak.peakIdx * samplingTime);
                    rPassNum = 1;

                    // check if the amplitudes are valid
                    if (QRS.qrsCurrent.rAmplitude - QRS.qrsCurrent.qAmplitude <
                            bandOut.range * 0.1) {

                        Log.d("lme.pants", "Amplitude validation error: " +
                                (QRS.qrsCurrent.rAmplitude - QRS.qrsCurrent.qAmplitude));
                        // probably misdetected
                        QRS.qrsCurrent.reset();

                    } else {
                        // wait for S min
                        lastBandPeak = 0;
                        QRS.qrsCurrent.segState = SegmentationStatus.R_FOUND;

                        // pre-initialize sPeak detector
                        sPeak.next(y[3]);
                    }
                }
            }

            // ==============================================
            // == R peak found... looking for S min
            // ====>
            else if (QRS.qrsCurrent.segState == SegmentationStatus.R_FOUND) {

                // R has been found, we wait for S min
                QRS.qrsCurrent.values.add((float) y[3]);

                // continue looking for rising peak
                if (rPassNum > 0) {

                    rPassNum++;
                    risingPeak.next(y[6]);

                    if (risingPeak.peakIdx != -1) {

                        // rising peak of integration window found
                        // the length of the ridge equals the
                        // width of the QRS complex
                        QRS.qrsCurrent.feat_width =
                                (long) (rPassNum * wndIntCompensation * samplingTime);
                        rPassNum = 0;

                    }
                }

                lastBandPeak++;

                // find S
                if (QRS.qrsCurrent.sIdx == -1) {

                    // find S as min
                    sPeak.next(y[3]);
                    if (sPeak.peakIdx != -1) {
                        QRS.qrsCurrent.sAmplitude = sPeak.peakValue;
                        QRS.qrsCurrent.sIdx = QRS.qrsCurrent.values.head
                                - sPeak.peakIdx;
                    }
                }

                // check for max range
                if (lastBandPeak >= postSegment) {

                    // ==============================================
                    // == segmentation finished
                    // ====>
                    QRS.qrsCurrent.segState = SegmentationStatus.FINISHED;

                    // is no S has been found, we use the last sample
                    if (QRS.qrsCurrent.sIdx == -1) {

                        QRS.qrsCurrent.sAmplitude = y[3];
                        QRS.qrsCurrent.sIdx = QRS.qrsCurrent.values.head;

                    }

                    QRS.qrsPrevious = (QRS) qrsHistory.getPastValue(1);

                    // make sure that we have a width
                    if (QRS.qrsCurrent.feat_width < 1) {

                        // substitute width estimation
                        QRS.qrsCurrent.feat_width = (long)
                                ((QRS.qrsCurrent.sIdx - QRS.qrsCurrent.qIdx) *
                                        wndIntCompensation * samplingTime);
                    }

                    // find a template
                    if (QRS.template1.classification == QrsClass.INVALID ||
                            QRS.template2.classification == QrsClass.INVALID) {

                        // no templates, wait for 6 beats
                        beatCounter++;
                        if (QRS.qrsCurrent.classify() == QrsClass.INVALID) {
                            beatCounter--;
                        }
                        if (beatCounter == 6) {

                            ArrayList<Integer> sortList = new ArrayList<>(6);

                            // 6 beats encountered, choose the templates
                            double avg = 0.0;
                            for (int i = 0; i < 6; i++) {
                                qrsRefTemp1 = (QRS) qrsHistory.getPastValue(1);
                                avg += qrsRefTemp1.feat_qrsta;
                            }
                            avg /= 6;

                            sortList.add(0);

                            // sort the indices in ascending order
                            for (int i = 0; i < 6; i++) {

                                qrsRefTemp1 = (QRS) qrsHistory.getPastValue(i);

                                for (int n = 0; n < sortList.size(); n++) {

                                    qrsRefTemp2 = (QRS) qrsHistory.getPastValue(sortList.get(n));

                                    if (qrsRefTemp1.feat_qrsta < avg &&
                                            qrsRefTemp2.feat_qrsta < avg &&
                                            qrsRefTemp2.feat_qrsta > qrsRefTemp1.feat_qrsta) {

                                        sortList.add(n, i);
                                        break;

                                    } else if (n == sortList.size() - 1) {

                                        sortList.add(i);
                                        break;
                                    }
                                }
                            }

                            // select
                            for (int i = 0; i < 3; i++) {

                                qrsRefTemp1 = (QRS) qrsHistory.getPastValue(sortList.get(i));
                                qrsRefTemp2 = (QRS) qrsHistory.getPastValue(sortList.get(i + 1));

                                if (qrsRefTemp1.maxCorr(qrsRefTemp2) > 0.9) {

                                    // take those two as templates
                                    QRS.template1.copy(qrsRefTemp1);
                                    QRS.template2.copy(qrsRefTemp2);
                                    QRS.template1.classification = QrsClass.NORMAL;
                                    QRS.template2.classification = QrsClass.NORMAL;
                                }
                            }

                            // see if we have two templates
                            if (QRS.template2.classification != QrsClass.NORMAL) {

                                // no, only one template, so take the two smallest
                                QRS.template1.copy((QRS) qrsHistory.getPastValue(sortList.get(0)));
                                QRS.template2.copy((QRS) qrsHistory.getPastValue(sortList.get(1)));
                                QRS.template1.classification = QrsClass.NORMAL;
                                QRS.template2.classification = QrsClass.NORMAL;
                            }

                            // end learning time
                            learning = false;
                        }
                    } else {

                        // classify current QRS and only proceed if beat is not invalid
                        if (QRS.qrsCurrent.classify() != QrsClass.INVALID) {

                            // missed beat?
                            if (QRS.qrsCurrent.classification == QrsClass.ESCAPE) {

                                // insert copy of current beat between current and last beat
                                QRS.qrsPrevious = QRS.qrsCurrent;
                                QRS.qrsCurrent = (QRS) qrsHistory.next();
                                QRS.qrsCurrent.copy(QRS.qrsPrevious);

                                QRS.qrsPrevious.classification = QrsClass.VIRTUAL;

                                // estimate the timestamps of the inserted (missed/virtual) beat
                                QRS.qrsPrevious.estimateMissedTimestamps();

                                // reclassify the beat
                                QRS.qrsPrevious.classify();

                                // make sure it is not classified normal, since it certainly
                                // is the escape beat
                                if (QRS.qrsCurrent.classification == QrsClass.NORMAL) {
                                    QRS.qrsCurrent.classification = QrsClass.ESCAPE;
                                }
                            } else if (QRS.qrsCurrent.classification == QrsClass.NORMAL) {

                                if (QRS.qrsCurrent.feat_cct1 > QRS.qrsCurrent.feat_cct2) {

                                    // replace template 1
                                    QRS.template1.copy(QRS.qrsCurrent);
                                } else {
                                    // replace template 2
                                    QRS.template2.copy(QRS.qrsCurrent);
                                }
                            }

                            // calculate averages
                            rrMeanLong.next(QRS.qrsCurrent.feat_rr);

                            if (QRS.qrsCurrent.feat_rr > 180 &&
                                    QRS.qrsCurrent.feat_rr < 4000) {
                                if (!PanTompkins.learning) {
                                    numTotalBeats++;
                                }

                                long currTimestamp = (long) (((double) QRS.qrsCurrent.rTimestamp / (double) samplingRate) * 1000);
                                if (mOldTimestamp == 0) {
                                    mOldTimestamp = currTimestamp;
                                }

                                if ((currTimestamp - mOldTimestamp) >= 10 * 1000) {
                                    hrvDataReady = true;
                                    mOldTimestamp = currTimestamp;
                                    standardDeviationRR.add(calculateStd(rrIntervals));
                                    successiveDifferencesRms
                                            .add(calculateRMS(successiveDifferences));
                                    successiveDifferencesStd
                                            .add(calculateStd(successiveDifferences));
                                    successiveDifferences.clear();
                                    rrIntervals.clear();
                                }

                                rrIntervals.add((double) QRS.qrsCurrent.feat_rr);
                                successiveDifferences.add((double) Math.abs(QRS.qrsCurrent.feat_rr
                                        - QRS.qrsPrevious.feat_rr));
                                //Log.e("PANTS", "successive difference: " + successiveDifferences.getLast());
                                if (successiveDifferences.getLast() >= 50.0) {
                                    numRr50++;
                                    pRr50 = (double) numRr50 / (double) numTotalBeats;
                                }
                                if (successiveDifferences.getLast() >= 20.0) {
                                    numRr20++;
                                    pRr20 = (double) numRr20 / (double) numTotalBeats;
                                }

                                rrStats.next(QRS.qrsCurrent.feat_rr);
                                stdStats.next(QRS.qrsCurrent.feat_rr);

                                // calculate heart rate
                                heartRateStats.next(60000 / rrStats.value);
                                qrstaStats.next(QRS.qrsCurrent.feat_qrsta);
                            }
                        }
                    }

                    // <====
                    // ==============================================
                }
            }
            // <====
            // ==============================================
        } else {
            startProcessing--;
        }

        return y[6];
    }

    /**
     * @author Falling
     */
    public static class QRS {
        /**
         * Static reference to the template slot 1
         */
        public static QRS template1 = null;
        /**
         * Static reference to the template slot 2
         */
        public static QRS template2 = null;
        /**
         * Static reference to the current QRS
         */
        public static QRS qrsCurrent = null;
        /**
         * Static reference to the previous QRS
         */
        public static QRS qrsPrevious = null;
        /**
         * Segmentation state of this QRS
         */
        public SegmentationStatus segState = SegmentationStatus.INVALID;
        /**
         * Timestamp of the R-deflection in milliseconds
         */
        public long rTimestamp = -1;
        public double qAmplitude, rAmplitude, sAmplitude;
        public int qIdx = -1, rIdx = -1, sIdx = -1;
        /**
         * mean value of all values
         */
        public double mean = 0;
        /**
         * QRS width in milliseconds
         */
        public long feat_width;
        /**
         * R-R interval to last QRS in milliseconds
         */
        public long feat_rr;
        /**
         * QR amplitude, currently not used
         */
        public double feat_qra;
        /**
         * RS amplitude, currently not used
         */
        public double feat_rsa;
        /**
         * area total
         */
        public double feat_qrsta;
        /**
         * CC normalized to templates
         */
        public double feat_cct1, feat_cct2;
        /**
         * ArDiff to templates
         */
        public double feat_arT1diff, feat_arT2diff;
        public QrsClass classification = QrsClass.INVALID;
        public QrsArrhythmia arrhythmia = QrsArrhythmia.NONE;
        /**
         * QRS from filtered signal
         */
        public FloatValueList values = null;
        private transient int _i;
        private transient double _x, _y, _sumx, _sumy;
        private transient double _cc, _maxcc;
        private transient int _n;

        public QRS(int size) {
            values = new FloatValueList(size, true, true);
        }

        public void copy(QRS source) {
            segState = source.segState;
            rTimestamp = source.rTimestamp;
            rAmplitude = source.rAmplitude;
            rIdx = source.rIdx;
            qAmplitude = source.qAmplitude;
            qIdx = source.qIdx;
            sAmplitude = source.sAmplitude;
            sIdx = source.sIdx;
            mean = source.mean;
            feat_width = source.feat_width;
            feat_qra = source.feat_qra;
            feat_rsa = source.feat_rsa;
            feat_qrsta = source.feat_qrsta;
            feat_cct1 = source.feat_cct1;
            feat_cct2 = source.feat_cct2;
            feat_rr = source.feat_rr;
            classification = source.classification;
            arrhythmia = source.arrhythmia;
            values.copy(source.values);
        }

        public void reset() {
            values.clear();
            rTimestamp = qIdx = rIdx = sIdx = -1;
            mean = feat_cct1 = feat_cct2 =
                    feat_qra = feat_qrsta = feat_rsa =
                            feat_rr = feat_width = 0;

            segState = SegmentationStatus.INVALID;
            classification = QrsClass.INVALID;
            arrhythmia = QrsArrhythmia.NONE;
        }

        /**
         * Estimates the timestamps of a missed/virtual beat
         */
        public void estimateMissedTimestamps() {
            if (feat_rr < 1 || feat_rr > 6000)
                return;

            feat_rr /= 2;
            rTimestamp -= feat_rr;
        }

        /**
         * Specific QRS template to use for correlation classification
         *
         * @return the class most likely to fit this QRS
         */
        public QrsClass classify() {
            if (rIdx == -1) {
                // no R peak found
                classification = QrsClass.INVALID;

            } else {

                // current rr-time
                feat_rr = (long) ((rTimestamp - qrsPrevious.rTimestamp) * samplingTime);

                feat_qra = rAmplitude - qAmplitude;
                feat_rsa = rAmplitude - sAmplitude;

                mean = values.getMean();

                // CC feature variables
                feat_qrsta = _sumx = _sumy = 0;

                // calculate qrsta
                for (_i = 0; _i < values.num; _i++) {
                    if (values.values[_i] > 0)
                        feat_qrsta += values.values[_i];
                    else
                        feat_qrsta -= values.values[_i];
                }

                // check for templates
                if (template1.classification == QrsClass.INVALID ||
                        template2.classification == QrsClass.INVALID) {

                    // no templates yet, unknown and return
                    classification = QrsClass.UNKNOWN;

                    return classification;
                }

                // calculate correlation to templates
                feat_cct1 = maxCorr(template1);
                feat_cct2 = maxCorr(template2);

                feat_arT1diff = arDiff(template1);
                feat_arT2diff = arDiff(template2);

                // normal QRS duration is 60-120 ms
                if (feat_width > 130) {
                    // possibly bundle branch block
                    classification = QrsClass.BB_BLOCK;

                } else if (feat_width < 45) {

                    classification = QrsClass.PVC;

                } else {

                    classification = QrsClass.NORMAL;

                }


                // template matchings

                // template CC tests
                if (feat_cct1 < 0.2 || feat_cct2 < 0.2) {
                    arrhythmia = QrsArrhythmia.ARTIFACT;
                }
                if (feat_cct1 < 0.3 || feat_cct2 < 0.3) {

                    classification = QrsClass.ABERRANT;

                } else if (feat_cct1 < 0.6 || feat_cct2 < 0.6) {

                    classification = QrsClass.PVC_ABERRANT;

                } else if (feat_cct1 < 0.9 && feat_cct2 < 0.9) {

                    classification = QrsClass.PVC;

                } else if (feat_cct1 < 0.98 && feat_cct2 < 0.98) {

                    if (feat_arT1diff > 0.7 || feat_arT2diff > 0.7) {

                        classification = QrsClass.ABERRANT;

                    } else if (feat_arT1diff > 0.5 || feat_arT2diff > 0.5) {

                        classification = QrsClass.PVC_ABERRANT;

                    } else if (feat_arT1diff > 0.2 && feat_arT2diff > 0.2) {

                        classification = QrsClass.PVC;

                    }
                }

                // RR tests
                // -|----|-----------|--
                if ((feat_rr >= qrsPrevious.feat_rr * 1.5 && feat_rr > 800) ||
                        feat_rr > 1700) {

                    arrhythmia = QrsArrhythmia.AV_BLOCK;

                    // escape beat
                    if (classification == QrsClass.NORMAL) {
                        classification = QrsClass.APC;
                    }
                }
                // -|-|--
                else if (feat_rr > 1 && feat_rr < 460) {

                    // premature and fusion types
                    if (feat_rr > qrsPrevious.feat_rr * 0.92f) {
                        // could be "normal" heart rate change
                        if (classification == QrsClass.NORMAL &&
                                (feat_cct1 < 0.96 || feat_cct2 < 0.96)) {
                            classification = QrsClass.APC;
                        }

                    } else {
                        arrhythmia = QrsArrhythmia.FUSION;
                    }

                    if (feat_rr < 400) {

                        if (feat_cct1 < 0.6 || feat_cct2 < 0.6) {
                            classification = QrsClass.APC_ABERRANT;
                        } else {
                            classification = QrsClass.APC;
                        }

                    }
                }
                // -|-------------|----|--
                else if (qrsPrevious.feat_rr > 800 &&
                        feat_rr < qrsPrevious.feat_rr * 0.6f) {

                    classification = QrsClass.ESCAPE;

                } else if (classification == QrsClass.NORMAL && feat_width > 10 &&
                        feat_width < qrsPrevious.feat_width * 0.6f &&
                        (feat_arT1diff > 0.1 || feat_arT2diff > 0.1)) {

                    classification = QrsClass.PREMATURE;

                }

                /*Log.d("s", "   " + classification
                        + String.format(" [%.3f %.3f] [%.3f %.3f]",
                        feat_cct1, feat_cct2, feat_arT1diff, feat_arT2diff)
                        + String.format(" [%d %d  %.3f] ",
                        feat_width, feat_rr, feat_qrsta));
                        */
            }
            return classification;
        }

        public double maxCorr(QRS qrs) {
            _maxcc = 0.0;

            for (_n = -8; _n < 8; _n++) {
                _x = _y = _sumx = _sumy = _cc = 0.0;

                for (_i = 0; _i < values.num; _i++) {
                    _x = (values.values[_i] - mean);
                    _y = (qrs.values.getIndirect(_n + _i) - qrs.mean);

                    _cc += _x * _y;

                    _sumx += _x * _x;
                    _sumy += _y * _y;
                }

                if (_cc != 0) {
                    _cc = _cc / Math.sqrt(_sumx * _sumy);
                    if (_cc > _maxcc)
                        _maxcc = _cc;
                }
            }

            return _maxcc;
        }

        public double arDiff(QRS qrs) {

            if (qrs.feat_qrsta == 0.0)
                return 0.0;

            return Math.abs((feat_qrsta - qrs.feat_qrsta) / qrs.feat_qrsta);
        }

        public enum SegmentationStatus {
            INVALID, THRESHOLD_CROSSED, R_FOUND, FINISHED, PROCESSED
        }

        public enum QrsClass {
            /**
             * Rejection Class
             */
            UNKNOWN,
            /**
             * No valid QRS complex, probably detection failure
             */
            INVALID,
            /**
             * Normal QRS morphology
             */
            NORMAL,
            /**
             * Premature Ventricular Contraction recognized in QRS (cc small)
             */
            PVC,
            /**
             * PVC like aberrant beat (cc very small)
             */
            PVC_ABERRANT,
            /**
             * Bundle branch block (q->s >130ms)
             */
            BB_BLOCK,
            /**
             * Various escape beats (> 600ms no QRS)
             */
            ESCAPE,
            /**
             * Artial premature complexes/beats
             */
            APC,
            /**
             * Aberrated artrial premature beats
             */
            APC_ABERRANT,
            /**
             * Various premature beats, unspecified, possibly junctional premature
             */
            PREMATURE,
            /**
             * waveform differs significantly, potentially a/v flutter or fibrillation (cc < 0.4)
             */
            ABERRANT,
            /**
             * Virtual beats, inserted for missed beats
             */
            VIRTUAL
        }

        public enum QrsArrhythmia {
            /**
             * no arrhythmia, normal pace
             */
            NONE,
            /**
             * Very likely a normal beat, shows several deviations, but not enough to classify as ectopic
             */
            ARTIFACT,
            /**
             * Fusion of two beats (rr < 0.65 * prev && no APC)
             */
            FUSION,
            /**
             * atrioventricular block, generic (rr > 1.6 * prev => ESCAPE if nothing else)
             */
            AV_BLOCK,
            /**
             * Heart rate > 130 bpm
             */
            TACHYCARDIA,
            /**
             * Heart rate < 40 bpm
             */
            BRADYCARDIA,
            /**
             * a/v flutter or fibrillation (ABERRANT && rr <<)
             */
            FIBRILLATION,
            /**
             * Heart failure
             */
            CARDIAC_ARREST
        }
    }

    public static class StepHistory {

        public FloatValueList history = null;
        public MeanFilter peakAverage = new MeanFilter(8);
        public double peakOverall = 0.0,
                peakSignal = 0.0, peakNoise = 0.0,
                threshold1 = 0.0, threshold2 = 0.0,
                min = Float.MAX_VALUE, max = Float.MIN_VALUE,
                range = 0.0;

        public StepHistory(int size) {
            history = new FloatValueList(size);
        }

        public void add(double value) {
            // check for min/max
            if (value > max) {

                max = value;
                peakOverall = max;
                range = Math.abs(max) - Math.abs(min);

            } else if (value < min) {

                min = value;
                range = Math.abs(max) - Math.abs(min);

            }

            history.add((float) value);
        }

        /**
         * @return the current threshold for an assumed R-peak
         */
        public double threshold() {
            return min + range * 0.15;
        }
    }
}
