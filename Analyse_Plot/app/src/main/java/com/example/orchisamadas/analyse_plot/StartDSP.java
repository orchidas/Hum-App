package com.example.orchisamadas.analyse_plot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder.AudioSource;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.orchisamadas.analyse_plot.MySQLiteDatabaseContract.TableEntry;



public class StartDSP extends ActionBarActivity {
    TextView TextHandleRemainingImpulses;
    AudioRecord recorder;
    CaptureAudio captureAudio;
    TextView textViewTime;
    String title;
    EditText comment;
    ImageButton done;
    final CounterClass timer = new CounterClass(5000, 1000);
    private static final double REFSPL = 0.00002;
    private MediaPlayer mPlayer = null;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaPlayer = MediaPlayer.create(this, R.raw.boo);
        setContentView(R.layout.activity_start_dsp);
        MySQLiteDatabaseHelper databaseHelper = new
                MySQLiteDatabaseHelper(StartDSP.this);
        //open or create database
        SQLiteDatabase db = openOrCreateDatabase(Environment.getExternalStorageDirectory() + File.separator + databaseHelper.NAME, MODE_PRIVATE, null);
        databaseHelper.onCreate(db);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_start_ds, menu);
        //allow user to add description
        return super.onCreateOptionsMenu(menu);
    }


    public void startPlaying(){
        mPlayer=new MediaPlayer();
        try{
            mPlayer.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath()+
                    "/MySounds/Chirp_50_1000Hz.wav");
            mPlayer.prepare();
            mPlayer.start();
        }
        catch(IOException e){}
    }


    //play chirp when play button is pressed
    public boolean onOptionsItemSelected(MenuItem item){
        //Handle presses on the action bar items
        switch(item.getItemId()){
            case R.id.GenerateChirp:startPlaying();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class CounterClass extends CountDownTimer {
        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            textViewTime.setText("Captured");
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long millis = millisUntilFinished;
            String hms = String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            System.out.println(hms);
            textViewTime.setText(hms);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        //allow user to enter title
        comment = (EditText) findViewById(R.id.Addcomment);
        done = (ImageButton) findViewById(R.id.Enter);
        Toast.makeText(StartDSP.this, "Add a small description of the noise you're hearing", Toast.LENGTH_SHORT).show();

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                title = comment.getText().toString();
                if(title == null)
                    title = " ";
                //close virtual keyboard
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                Toast.makeText(StartDSP.this, "Description saved", Toast.LENGTH_SHORT).show();
                comment.setVisibility(View.INVISIBLE);
                done.setVisibility(View.INVISIBLE);

            TextHandleRemainingImpulses = (TextView)
                    findViewById(R.id.remaining_impulses);
            TextHandleRemainingImpulses.setText(getResources().getString
                    (R.string.remaining_impulse_leadtext) + Integer.toString(getResources().getInteger(R.integer.num_impulses)));
            textViewTime = (TextView)findViewById(R.id.textViewTime);
            captureAudio = new CaptureAudio(); captureAudio.execute();
            }
        });
    }


    @Override
    protected void onPause(){
        if(captureAudio != null)
            captureAudio.cancel(false);
        super.onPause();
        finish();
    }


    private class CaptureAudio extends AsyncTask<Void, Integer, Integer>{

        protected void onPreExecute(){
            int bufferSize=2*AudioRecord.getMinBufferSize(getResources().getInteger(R.integer.sample_rate),
                    getResources().getInteger(R.integer.num_channels),AudioFormat.ENCODING_PCM_16BIT);
            recorder= new AudioRecord(AudioSource.MIC,getResources().getInteger(R.integer.sample_rate),
                    getResources().getInteger(R.integer.num_channels),AudioFormat.ENCODING_PCM_16BIT,bufferSize);
            if(recorder.getState()!=AudioRecord.STATE_INITIALIZED){
                Toast.makeText(StartDSP.this, getResources().getString(R.string.recorder_init_fail),Toast.LENGTH_LONG).show();
                recorder.release();
                recorder=null;
                return;}
        }


        protected Integer doInBackground(Void ... params) {
            if (recorder == null) {
                return -1;
            }
            int remainingImpulses =
                    getResources().getInteger(R.integer.num_impulses);
            int detectBufferLength = getResources().getInteger(R.integer.detect_buffer_length); //length = sampleRate * recordTime
            int sampleBufferLength = getResources().getInteger(R.integer.sample_rate) * getResources().getInteger(R.integer.capture_time);
            sampleBufferLength =
                    nearestPow2Length(sampleBufferLength);
            short[] detectBuffer = new short[detectBufferLength];
            short[][] sampleBuffer = new short[remainingImpulses][sampleBufferLength];
            recorder.startRecording();

            while (remainingImpulses > 0) {
                //...Listing 8A Article 3 Lines 516 to 525
                publishProgress(-1, -1, -1, -1);
                int samplesRead = 0;

                while (samplesRead < detectBufferLength)
                    samplesRead += recorder.read(detectBuffer, samplesRead, detectBufferLength - samplesRead);

                if (detectImpulse(detectBuffer)) {
                    remainingImpulses--;
                    publishProgress(0, remainingImpulses, -1, -1);
                    System.arraycopy(detectBuffer, 0, sampleBuffer[remainingImpulses], 0, detectBufferLength);
                    samplesRead = detectBufferLength;
                    while (samplesRead < sampleBufferLength)
                        samplesRead += recorder.read(sampleBuffer[remainingImpulses], samplesRead, sampleBufferLength - samplesRead);
                }
                if (isCancelled()) {
                    detectBuffer = null;
                    sampleBuffer = null;
                    return -1;
                }
            }//end while(remainingImpulses > 0)
            detectBuffer = null;
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
            if (!isCancelled()) {
                publishProgress(-1, -1, 0, -1);
            }
            //save recorded audio to an external file in memory card to enable playback option
            saveRecord(sampleBuffer, sampleBufferLength);


            //analysing data
            final int numImpulses =
                    getResources().getInteger(R.integer.num_impulses);
            double[][] samples = new double[numImpulses][sampleBufferLength];
            //normalizing time domain data
            for (int k = 0; k < numImpulses; k++) {
                double max = 0;
                for (int n = 0; n < sampleBufferLength; n++) {
                    samples[k][n] = (double) sampleBuffer[k][n];
                    if (max < samples[k][n]) {
                        max = samples[k][n];
                    }
                }
                for (int h = 0; h < sampleBufferLength; h++) {
                    samples[k][h] /= max;
                }
            }
            sampleBuffer = null;

            //we apply a slight smoothing effect to the edge of the sample to improve
            //our result
            applyBasicWindow(samples, numImpulses, sampleBufferLength);
            //do FFT
            int error = doubleFFT(samples, numImpulses, sampleBufferLength);
            if (error == -1) {
                if (!isCancelled()) {
                    publishProgress(-1, -1, -1, 0);
                }
                sampleBuffer = null;
                return -1;
            }


            /*samples[][] contains the result of FFT.
            Store the FFT results into table fft_data.
            Here we average all the samples to compute the averaged data set*/
            double[] toStorage = new double[sampleBufferLength];
            for(int k = 0; k < numImpulses; k++)
            {
                for(int n = 0; n < sampleBufferLength; n++)
                    toStorage[n] += samples[k][n]/REFSPL;
            }
            for(int n = 0; n < sampleBufferLength; n++)
            {
                toStorage[n] /= numImpulses;
                //toStorage[n] = 20*Math.log10(toStorage[n] / REFSPL);
            }

            if(isCancelled())
                return -1;

            //reduce the size of our sample so the graph can load in a normal amount of time
            int samplesPerPoint = getResources().getInteger(R.integer.samples_per_bin);
            int width = toStorage.length / samplesPerPoint / 2;
            double maxYval = 0;
            double[] tempBuffer = new double[width];

            for(int k = 0; k < tempBuffer.length; k++)
            {
                for(int n = 0; n < samplesPerPoint; n++)
                    tempBuffer[k] += toStorage[k*samplesPerPoint + n];

                tempBuffer[k] /= (double)samplesPerPoint;

                if(maxYval < tempBuffer[k])
                    maxYval = tempBuffer[k];
            }
            ContentValues vals = new ContentValues();
            ContentValues values = new ContentValues();
            MySQLiteDatabaseHelper databaseHelper = new
                    MySQLiteDatabaseHelper(StartDSP.this);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();

            //we're going to save every single impulse separately ======================================
            //NOTE: if the num impulses changes the associated table MUST be deleted and remade.
            //tempBuffer has the average time domain values of all impulses whereas tempImpBuffer
            // has the frequency domain power values of each impulse.
            for(int i = 0; i < numImpulses; i++)
            {
                double maxTemp = 0;

                double[] tempImpBuffer = new double[width];
                for(int k = 0; k < tempImpBuffer.length; k++)
                {
                    for(int n = 0; n < samplesPerPoint; n++)
                        //tempImpBuffer[k] += 20*Math.log10(samples[i][k*samplesPerPoint + n] / REFSPL);
                        tempImpBuffer[k] += (samples[i][k*samplesPerPoint + n] / REFSPL);

                    tempImpBuffer[k] /= (double)samplesPerPoint;

                    if(maxTemp < tempImpBuffer[k])
                        maxTemp = tempImpBuffer[k];
                }

                /*double sf = maxTemp - maxYval;

                for(int k = 0; k < tempImpBuffer.length; k++)
                    tempImpBuffer[k] -= sf;*/

                ByteBuffer byteImpBuffer = ByteBuffer.allocate(width*8);
                for(int k = 0; k < width; k++)
                    byteImpBuffer.putDouble(tempImpBuffer[k]);
                vals.put(TableEntry.COLUMN_NAME_IMPULSE + Integer.toString(i), byteImpBuffer.array());
            }


            double[] xVals = new double[tempBuffer.length];
            double sampleRate = getResources().getInteger(R.integer.sample_rate);
            for(int k = 0; k < xVals.length; k++)
                xVals[k] = k* sampleRate / (2*xVals.length);

            ByteBuffer byteBufferY = ByteBuffer.allocate(tempBuffer.length*8);
            for(int k = 0; k < tempBuffer.length; k++)
                byteBufferY.putDouble(tempBuffer[k]);
            vals.put(TableEntry.COLUMN_NAME_YVALS, byteBufferY.array());

            ByteBuffer byteBufferX = ByteBuffer.allocate(xVals.length*8);
            for(int k = 0; k < xVals.length; k++)
                byteBufferX.putDouble(xVals[k]);
            vals.put(TableEntry.COLUMN_NAME_XVALS, byteBufferX.array());
            byteBufferY = null;
            byteBufferX = null;


            String date = DateFormat.format("LLL dd, yyyy HH:mm", new Date()).toString();
            vals.put(TableEntry.COLUMN_NAME_DATE, date);
            vals.put(TableEntry.COLUMN_NAME_COMMENT, " - " + title);
            db.insert(TableEntry.TABLE_NAME_FFT, null, vals);


            //==========================================================================================

            //Do DSP here
            for (int i = 0; i < numImpulses; i++) {
                //Generating average over 1 Hz
                double averageOver = 1 / (double) getResources().getInteger(R.integer.averageOverDenominator);
                //sampleBufferLength = numPts in Matlab =32768
                double freqDeltaF = (double) (sampleRate) / sampleBufferLength;
                int ptsAverageOverFreq = (int) Math.floor(averageOver / freqDeltaF);
                int numPtsAfterAverage = (int) Math.floor(sampleBufferLength / ptsAverageOverFreq);
                //we only want to keep values till 300Hz for our analysis
                int upperLimitFreq = 300;
                double freqDeltaFAfterAverage = (double) (sampleRate) / numPtsAfterAverage;
                int ptsTillUpperLimit = (int) Math.floor((double) (upperLimitFreq) / freqDeltaFAfterAverage);
                double[] arrayOfFFTAverages = new double[ptsTillUpperLimit];
                double[] arrayOfFreqAverages = new double[ptsTillUpperLimit];


                for (int n = 0; n < ptsTillUpperLimit; n++) {
                    for (int k = 0; k < ptsAverageOverFreq; k++) {
                        arrayOfFFTAverages[n] += samples[i][n * ptsAverageOverFreq + k];

                    }
                    arrayOfFFTAverages[n] /= ptsAverageOverFreq;
                }

                for (int k = 0; k < ptsTillUpperLimit; k++) {
                    arrayOfFreqAverages[k] = ((double) (sampleRate) / (numPtsAfterAverage)) * k;
                }

                //breaking into frequency bands
                int numPtsInEachBand = (int) Math.floor(15 / freqDeltaFAfterAverage);
                double[][] freqBandYvals = new double[32][numPtsInEachBand];
                double[][] freqBandXvals = new double[32][numPtsInEachBand];

                for (int n = 0; n <= 31; n++) {
                    int startFreq = 2 + (5 * n);
                    int startingPt = (int) Math.floor(startFreq / freqDeltaFAfterAverage);
                    for (int k = 0; k < numPtsInEachBand; k++) {
                        //freqBandYvals[n][k] = 20 * Math.log10(arrayOfFFTAverages[startingPt + k] / REFSPL);
                        freqBandYvals[n][k] = (arrayOfFFTAverages[startingPt + k] / REFSPL);
                        freqBandXvals[n][k] = arrayOfFreqAverages[startingPt + k];
                    }
                }

                //identify strongest signal and average band power
                double[] avgBandPower = new double[32];
                double strongestSignal = 0;
                double strongestSignalFreq = 0;


                for (int n = 0; n <= 31; n++) {
                    for (int k = 0; k < numPtsInEachBand; k++) {
                        if (freqBandYvals[n][k] > strongestSignal) {
                            strongestSignal = freqBandYvals[n][k];
                            strongestSignalFreq = freqBandXvals[n][k];
                        }
                        avgBandPower[n] += freqBandYvals[n][k];
                    }
                    avgBandPower[n] /= numPtsInEachBand;
                }


                //calculating percentage worse case -- these are the frequenices which have maximum power.
                // Using ArrayList is much simpler
                /*strongestSignal - freqBandYvals(n,k) < 0.5 * avg deviation of
                    freqBandYvals from strongestSignal*/
                double dev = 0;
                for (int n = 0; n <= 31; n++) {
                    for (int k = 0; k < numPtsInEachBand; k++)
                        dev += Math.pow(freqBandYvals[n][k] - strongestSignal,2);
                }
                dev /= (32*numPtsInEachBand);
                dev = Math.sqrt(dev);


                double threshold = 1 -(0.5 * dev)/strongestSignal;
                List<Double> percentageWorseCase = new ArrayList<Double>();
                for (int n = 0; n <= 31; n++) {
                    for (int k = 0; k < numPtsInEachBand; k++) {
                        if (freqBandYvals[n][k] / strongestSignal >= threshold)
                            percentageWorseCase.add(freqBandXvals[n][k]);
                    }
                }


                //removing repeated frequencies and sorting arrayList
                Set<Double> unique = new HashSet<Double>();
                unique.addAll(percentageWorseCase);
                percentageWorseCase.clear();
                percentageWorseCase.addAll(unique);
                Collections.sort(percentageWorseCase);
                Log.d("ADebugTag", "Final Percentage Worse Case Frequencies  " + percentageWorseCase);


               /*calculating Ratio Background Noise
               calculating ratio of power across 1Hz bandwidth of signal to average band
               power and finding out the weakest signals
               for weak signals, avgBandPower(n) - freqBandYvals(n,k) > 1.5 * standard deviation of freqBandYvals(n,:)*/

                double [] std = new double[32];
                double weakThreshold =0;
                for (int n = 0; n <= 31; n++) {
                    for (int k = 0; k < numPtsInEachBand; k++)
                        std[n] += Math.pow(freqBandYvals[n][k] - avgBandPower[n], 2);

                    std[n] /= numPtsInEachBand;
                    std[n] = Math.sqrt(std[n]);
                }

                int[] numberWeakPeaks = new int[32];
                for (int n = 0; n <= 31; n++) {
                    weakThreshold = 1 - (1.5 * std[n])/avgBandPower[n];
                    for (int k = 0; k < numPtsInEachBand; k++) {
                        if (freqBandYvals[n][k] / avgBandPower[n] <= weakThreshold)
                            numberWeakPeaks[n]++;
                    }
                }

                //calculating frequency band with least power
                int lowestPeakBand = 0;
                double avgPowerLowestPeakBands = 0;
                int min = numberWeakPeaks[0];
                for (int n = 0; n <= 31; n++) {
                    if (numberWeakPeaks[n] <= min) {
                        min = numberWeakPeaks[n];
                        lowestPeakBand = n;
                    }
                }

                //calculating average power of lowestPeakBand
                for (int k = 0; k < numPtsInEachBand; k++)
                    avgPowerLowestPeakBands += freqBandYvals[lowestPeakBand][k];
                avgPowerLowestPeakBands /= numPtsInEachBand;

                /*for signals that have peaks above background noise, freqBandvals(n,k) - avgPower_lowestPeakBand >
                avg deviation of freqBandYvals from avgPower_lowestPeakBand */

                dev = 0;
                for (int n = 0; n <= 31; n++) {
                    for (int k = 0; k < numPtsInEachBand; k++)
                        dev += Math.pow(freqBandYvals[n][k] - avgPowerLowestPeakBands,2);
                }
                dev /= (32*numPtsInEachBand);
                dev = Math.sqrt(dev);
                double limit = 1 + (dev)/avgPowerLowestPeakBands; //should be greater than 1

                List<Double> ratioBackgroundNoise = new ArrayList<Double>();
                for (int n = 0; n <= 31; n++) {
                    for (int k = 0; k < numPtsInEachBand; k++) {
                        if (freqBandYvals[n][k] / avgPowerLowestPeakBands >= limit)
                            ratioBackgroundNoise.add(freqBandXvals[n][k]);
                    }
                }

                Set<Double> uniqueElements = new HashSet<Double>();
                uniqueElements.addAll(ratioBackgroundNoise);
                ratioBackgroundNoise.clear();
                ratioBackgroundNoise.addAll(uniqueElements);
                Collections.sort(ratioBackgroundNoise);
                Log.d("ADebugTag", "Ratio background noise Frequencies  " + ratioBackgroundNoise);

                //========================================================================================================


                //inserting into database
                db = databaseHelper.getWritableDatabase();
                int minimum;
                int maximum;
                if (percentageWorseCase.size() > ratioBackgroundNoise.size()) {
                    maximum = percentageWorseCase.size();
                    minimum = ratioBackgroundNoise.size();
                } else {
                    maximum = ratioBackgroundNoise.size();
                    minimum = percentageWorseCase.size();
                }

                for (int n = 0; n < minimum; n++) {
                    values.put(TableEntry.COLUMN_NAME, "IMPULSE" + Integer.toString(i));
                    values.put(TableEntry.COLUMN_DATE, date);
                    values.put(TableEntry.COLUMN_COMMENT, " - " + title);
                    values.put(TableEntry.COLUMN_MAX_SIGNAL, strongestSignalFreq);
                    values.put(TableEntry.COLUMN_PERCENTAGE_WORSE_CASE, percentageWorseCase.get(n));
                    values.put(TableEntry.COLUMN_RATIO_BACKGROUND_NOSE, ratioBackgroundNoise.get(n));
                    db.insert(TableEntry.TABLE_NAME, null, values);
                }

                for (int n = minimum; n < maximum; n++) {
                    values.put(TableEntry.COLUMN_NAME, "IMPULSE" + Integer.toString(i));
                    values.put(TableEntry.COLUMN_DATE, date);
                    values.put(TableEntry.COLUMN_COMMENT, " - " +title);
                    values.put(TableEntry.COLUMN_MAX_SIGNAL, strongestSignalFreq);
                    if (maximum == ratioBackgroundNoise.size()) {
                        values.put(TableEntry.COLUMN_PERCENTAGE_WORSE_CASE, 0.0);
                        values.put(TableEntry.COLUMN_RATIO_BACKGROUND_NOSE, ratioBackgroundNoise.get(n));
                    } else {
                        values.put(TableEntry.COLUMN_PERCENTAGE_WORSE_CASE, percentageWorseCase.get(n));
                        values.put(TableEntry.COLUMN_RATIO_BACKGROUND_NOSE, 0.0);

                    }
                    db.insert(TableEntry.TABLE_NAME, null, values);
                }

                int prog = (int) 100 * (i + 1) / numImpulses;
                publishProgress(-1, -1, prog, -1);
            }
            db.close();

            if (isCancelled())
                return -1;
            else
                return 0;
        }

//======================================================================================================================

        protected void onProgressUpdate(Integer ... data){
            if(data[0] == 0) {timer.start();}
            if(data[1] != -1)
                TextHandleRemainingImpulses.setText(getResources().getString (R.string.remaining_impulse_leadtext) + Integer.toString(data[1]));
            if(data[2] != -1){
                TextHandleRemainingImpulses.setVisibility(TextView.INVISIBLE);
                ProgressBar temp = (ProgressBar) findViewById(R.id.computation_progress);
                temp.setVisibility(View.VISIBLE); temp.setProgress(data[2]);
                TextView showProgress = (TextView) findViewById(R.id.analysing);
                showProgress.setText("Analysing...");
                showProgress.setVisibility(View.VISIBLE);
            }
            if(data[3] != -1)
                Toast.makeText(StartDSP.this, getResources().getString(R.string.computation_error), Toast.LENGTH_LONG).show();}


        protected void onPostExecute(Integer data){
            if(recorder != null){ recorder.release(); recorder = null;}
            if(data == -1){
                Toast.makeText(StartDSP.this, getResources().getString(R.string.error), Toast.LENGTH_LONG).show();
            }
            else {

                        //allowing user to playback on pressing a button
                        ImageButton playback = (ImageButton) findViewById(R.id.playback);
                        playback.setVisibility(View.VISIBLE);
                        playback.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                playbackAudio();
                            }
                            });


                        TextView showProgress = (TextView) findViewById(R.id.analysing);
                        showProgress.setText("Analysis Complete");

                        //Start the DisplayGraph activity on click of a button
                        //Button 1 displays FFT graph
                        Button FFTbutton = (Button) findViewById(R.id.btnDisplayGraph);
                        FFTbutton.setVisibility(View.VISIBLE);
                        FFTbutton.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View arg0) {
                                String which_button_pressed = "1";
                                Bundle bundle = new Bundle();
                                bundle.putString("button_pressed", which_button_pressed);
                                Intent intent = new Intent(StartDSP.this, DisplayGraph.class);
                                intent.putExtras(bundle);
                                startActivity(intent);
                            }
                        });

                        //Button 2 displays Analysis Histogram
                        Button Histbutton = (Button) findViewById(R.id.btnDisplayHistogram);
                        Histbutton.setVisibility(View.VISIBLE);
                        Histbutton.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View arg0) {
                                String which_button_pressed = "2";
                                Bundle bundle = new Bundle();
                                bundle.putString("button_pressed", which_button_pressed);
                                Intent intent = new Intent(StartDSP.this, DisplayGraph.class);
                                intent.putExtras(bundle);
                                startActivity(intent);
                            }
                        });

                        return;
                        }
                }


        protected void onCancelled(){
            if(recorder != null){recorder.release();recorder = null;}}


        protected boolean detectImpulse(short[] samples){
            int threshold = getResources().getInteger(R.integer.detect_threshold);
            for(int k = 0; k < samples.length; k++){
                if(samples[k] >= threshold){return true;}}
            return false;}

        protected int doubleFFT(double[][] samples, int numImpulses, int sampleSize){
            double[] real = new double[sampleSize]; double[] imag = new double[sampleSize];
            for(int k = 0; k < numImpulses; k++){
                System.arraycopy(samples[k], 0, real, 0, sampleSize);
                for(int n = 0; n < sampleSize; n++)
                    imag[n] = 0; int error = FFTbase.fft(real, imag, true);
                if(error == -1) {return -1;}
                for(int n = 0; n < sampleSize; n++)
                    samples[k][n] = Math.sqrt(real[n]*real[n] + imag[n]*imag[n]);
                if(isCancelled()) {return -1;}
            }
            return 0;}
    }

    protected void applyBasicWindow(double[][] samples, int numImpulses, int sampleLength)
    {
			/*The main goal is too ensure that the edges are smooth.
			 * Windowing is used because the DFT calculations operate on the infinite periodic
			 * extension of the input signal. Since many actual signals are either not periodic at all,
			 * or are sampled over an interval different from their actual period, this can produce
			 * false frequency components at the artificial 'edge' between repeated intervals,
			 * called leakage. By first multiplying the time-domain signal by a windowing function
			 * which goes to zero at both ends, you create a smooth transition between repeated intervals
			 * in the infinite periodic extension, thus mitigating the creation of these artificial
			 * frequency components when we then take the DFT.*/

        for(int k = 0; k < numImpulses; k++)
        {
            samples[k][0] *= 0.0625;
            samples[k][1] *= 0.125;
            samples[k][2] *= 0.25;
            samples[k][3] *= 0.5;
            samples[k][4] *= 0.75;
            samples[k][5] *= 0.875;
            samples[k][6] *= 0.9375;

            samples[k][sampleLength - 7] *= 0.9375;
            samples[k][sampleLength - 6] *= 0.875;
            samples[k][sampleLength - 5] *= 0.75;
            samples[k][sampleLength - 4] *= 0.5;
            samples[k][sampleLength - 3] *= 0.25;
            samples[k][sampleLength - 2] *= 0.125;
            samples[k][sampleLength - 1] *= 0.0625;
        }
        return;
    }



    public static int nearestPow2Length(int length){
        int temp = (int) (Math.log(length) / Math.log(2.0) + 0.5);length = 1;
        for(int n = 1; n <= temp; n++) {length = length * 2;}
        return length;}


    //save recorded data in an external file to enable user to playback
    public void saveRecord(short sampleBuffer[][], int sampleBufferLength){
        final int numImpulses =
                getResources().getInteger(R.integer.num_impulses);
        File file = new File(Environment.getExternalStorageDirectory(), "recordedSound.wav");
        if (file.exists())
            file.delete();
        try {
            file.createNewFile();
        } catch (IOException e) {
            Log.e("create file:", e.toString());
        }
        try {
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);

            for (int k = numImpulses -1; k >= 0; k--) {
                for (int n = 0; n < sampleBufferLength; n++)
                    dos.writeShort(sampleBuffer[k][n]);
            }
        }
        catch(IOException e){}
    }


    //playback record
    public void playbackAudio(){
        File file = new File(Environment.getExternalStorageDirectory(), "recordedSound.wav");
        // Get the length of the audio stored in the file (16 bit so 2 bytes per short)
        // and create a short array to store the recorded audio.
        int audioLength = (int)(file.length()/2);
        short [] audio = new short[audioLength];
        try {
            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int n = 0;
            while (dis.available() > 0) {
                audio[n] = dis.readShort();
                n++;
            }

            dis.close();
        }
        catch(IOException e){}

        // Create a new AudioTrack object using the same parameters as the AudioRecord
        // object used to create the file.
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioLength,
                AudioTrack.MODE_STREAM);
        // Start playback
        audioTrack.play();
        // Write the audio buffer to the AudioTrack object
        audioTrack.write(audio, 0, audioLength);
    }
}


