package com.example.orchisamadas.analyse_plot;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.example.orchisamadas.analyse_plot.
        MySQLiteDatabaseContract.TableEntry;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

//upgrade to GraphView 4.0.1
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


public class DisplayGraph extends ActionBarActivity {
    //if received date is null, current/previous graph is loaded
    public static String RECEIVED_DATE = null;
    public static String CURRENT_DATE =null;
    public static String which_button_pressed = null;
    public static final int request = 1;
    //Shows results of all recordings
    public static String SHOW_ALL = "NO";
    public static String SHOW_TOTAL = "NO";
    public static String COMPARE = "NO";
    //setting x axis labels to allow zoom in and out in histograms
    public static String [] xLabels = new String [12];


    /* you save the state of the application in a bundle (typically non-persistent, dynamic data in onSaveInstanceState),
    it can be passed back to onCreate if the activity needs to be recreated (e.g., orientation change)
    so that you don't lose this prior information. If no data was supplied, savedInstanceState is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //default xaxis labels
        for(int n = 0;n < xLabels.length;n++)
            xLabels[n] = Integer.toString(20 + (20*n));
        //RECEIVED_DATE should always point to last recorded/current data unless History is selected
        RECEIVED_DATE = null;
        Bundle bundle = getIntent().getExtras();
        which_button_pressed = bundle.getString("button_pressed");
        if (which_button_pressed.equals("1"))
            loadFFT(RECEIVED_DATE);
        else if (which_button_pressed.equals("2"))
            loadHistogram(RECEIVED_DATE);
    }


    //Options menu start ==============================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_graph, menu);
        return true;
    }

    //disabling compare and Show FFT options for FFT graph. Disabling Show Histogram option for
    //Histogram graph. Enabling Show Histogram and Show FFT options for compare Histogram graph.
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        if (which_button_pressed.equals("1")) {
            menu.findItem(R.id.compare).setEnabled(false);
            menu.findItem(R.id.show_hist).setEnabled(true);
            menu.findItem(R.id.show_FFT).setEnabled(false);
        }
        else {
            if (COMPARE.equals("YES"))
                menu.findItem(R.id.show_hist).setEnabled(true);
            else
                    menu.findItem(R.id.show_hist).setEnabled(false);
                menu.findItem(R.id.compare).setEnabled(true);
                menu.findItem(R.id.show_FFT).setEnabled(true);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.delete_database) {
            //delete all entries
            MySQLiteDatabaseHelper mydb = new MySQLiteDatabaseHelper(this);
            SQLiteDatabase db = mydb.getWritableDatabase();
            mydb.deleteAllEntries(db, TableEntry.TABLE_NAME_FFT);
            mydb.deleteAllEntries(db, TableEntry.TABLE_NAME);
            //reload the graph so it displays the "no data" screen
            if(which_button_pressed.equals("1"))
                loadFFT(RECEIVED_DATE);
            else
                loadHistogram(RECEIVED_DATE);

            db.close();
            db = null;
            mydb = null;

            //Confirm that all entries were deleted.
            Toast.makeText(this, getResources().getString(R.string.deleted_database), Toast.LENGTH_LONG)
                    .show();
            return true;
        }

        if(id == R.id.about)
        {
             if(which_button_pressed.equals("1"))
                    Toast.makeText(DisplayGraph.this, getResources().getString(R.string.about_fft), Toast.LENGTH_LONG)
                        .show();
                else {
                    //put in a loop for the display to last longer
                    for (int n = 0; n < 3; n++)
                        Toast.makeText(DisplayGraph.this, getResources().getString(R.string.about_hist), Toast.LENGTH_LONG)
                                .show();
                }
            return true;
        }


        if(id == R.id.history) {
            //start the activity which displays a list of previous entries
            //and allows the user to choose one to display

            //disable show-total if enabled
            SHOW_TOTAL = "NO";
            Intent intent = new Intent(this, PickHistory.class);
            Bundle bundle = new Bundle();
            bundle.putString("button_pressed", which_button_pressed);
            intent.putExtras(bundle);
            startActivityForResult(intent, request);
        }


        if(id == R.id.record_data)
        {
            //starts the StartDSP activity to record more data
            Intent intent = new Intent(this, StartDSP.class);
            startActivity(intent);
            finish();
            return true;
        }


        if(id == R.id.show_all)
        {
            //show all the impulses of current recording
            if(which_button_pressed.equals("1")) {
                SHOW_ALL = SHOW_ALL.equals("YES")?"NO":"YES";
                loadFFT(RECEIVED_DATE);
            }
            //show histogram of all data recorded so far
            else {
                SHOW_TOTAL = "YES";
                loadHistogram(RECEIVED_DATE);
            }
        }


        if(id == R.id.show_FFT) {
            //on choosing this option, the FFT graph is displayed
            loadFFT(RECEIVED_DATE);
            which_button_pressed = "1";
        }


        if(id == R.id.show_hist) {
            //on choosing this option, the histogram is displayed

            /*if initially histograms are being compared, then on clicking
            show histogram button, we want to display the reference histogram,i.e,
            one corresponding to CURRENT_DATE */
            if(COMPARE.equals("YES"))
                RECEVIED_DATE = CURRENT_DATE;

            //disable comparison histogram
            COMPARE = "NO";
            //disable show-total
            SHOW_TOTAL = "NO";
            loadHistogram(RECEIVED_DATE);
            which_button_pressed = "2";
        }


        if(id == R.id.compare){
            /*compares current histogram with another previous histogram as selected by user
            from PickHistory Activity */
            COMPARE = "YES";
            Intent intent = new Intent(this, PickHistory.class);
            Bundle bundle = new Bundle();
            bundle.putString("button_pressed", which_button_pressed);
            intent.putExtras(bundle);
            startActivityForResult(intent, request);
        }


        //to zoom in and out just change the xaxis labels of graph
        if(id == R.id.zoomIn){
            for(int n = 0;n < xLabels.length ;n++)
                xLabels[n] = Integer.toString(50 + (10*n));
            if(which_button_pressed.equals("1"))
                loadFFT(RECEIVED_DATE);
            else{
                if(COMPARE == "YES")
                    compareHistogram(CURRENT_DATE, RECEIVED_DATE);
                else
                    loadHistogram(RECEIVED_DATE);
            }
        }


        if(id == R.id.zoomOut){
            for(int n = 0;n < xLabels.length ;n++)
                xLabels[n] = Integer.toString(20 + (20*n));
            if(which_button_pressed.equals("1"))
                loadFFT(RECEIVED_DATE);
            else{
                if(COMPARE == "YES")
                    compareHistogram(CURRENT_DATE, RECEIVED_DATE);
                else
                    loadHistogram(RECEIVED_DATE);
            }
        }

        return super.onOptionsItemSelected(item);
    }


    //result from PickHistory activity which allows user to choose a date
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == request) {
            if (resultCode == RESULT_OK)
                RECEIVED_DATE = data.getStringExtra("RESULT_STRING");
            if (which_button_pressed.equals("1"))
                loadFFT(RECEIVED_DATE);
            else {
                if (COMPARE.equals("NO")) {
                    loadHistogram(RECEIVED_DATE);
                    /*CURRENT_DATE is the reference date with respect to which
                    comparisons with other dates are made*/
                    CURRENT_DATE = RECEIVED_DATE;
                }
                else {
                    compareHistogram(CURRENT_DATE, RECEIVED_DATE);
                }
            }
        }
        else
            return;
    }


//=================================================================================================================
    //plotting FFT graphs
    private void loadFFT(String received_date) {

        setContentView(R.layout.activity_display_graph);
        MySQLiteDatabaseHelper databaseHelper = new MySQLiteDatabaseHelper(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        final int numImpulses = getResources().getInteger(R.integer.num_impulses);
        String[] projection = null;
        GraphView graph = null;

        if (SHOW_ALL.equals("NO")) {
            //If we're displaying the average, we only want the average Y vals.
            projection = new String[]{
                    TableEntry.COLUMN_NAME_XVALS,
                    TableEntry.COLUMN_NAME_YVALS,
                    TableEntry.COLUMN_NAME_DATE,
                    TableEntry.COLUMN_NAME_COMMENT};
        }

        else {
            //If we're displaying all impulses, we need to load all the impulses
            // - the average one.
            projection = new String[3 + numImpulses];

            for (int k = 0; k < numImpulses; k++)
                projection[k] = TableEntry.COLUMN_NAME_IMPULSE + Integer.toString(k);

            projection[numImpulses] = TableEntry.COLUMN_NAME_XVALS;
            projection[numImpulses + 1] = TableEntry.COLUMN_NAME_DATE;
            projection[numImpulses + 2] = TableEntry.COLUMN_NAME_COMMENT;
        }
        Cursor c = null;


        //handle the case where we want to load a specific data set. we do this
        //using the given received_date.
        if (received_date != null) {

            final String where = TableEntry.COLUMN_NAME_DATE + " = '" + received_date + "'";
            c = db.query(TableEntry.TABLE_NAME_FFT,
                    projection,
                    where,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        //handle the case where we want to load the latest data
        //or where our previous code fails for some reason
        if (received_date == null || c.getCount() == 0) {
            c = db.query(TableEntry.TABLE_NAME_FFT,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        if (c.getCount() == 0) {
            Toast.makeText(DisplayGraph.this, getResources().getString(R.string.no_entries), Toast.LENGTH_LONG).show();
            return;
        }


        //find the length of the BLOB
        int numBytes = getResources().getInteger(R.integer.sample_rate) * getResources().getInteger(R.integer.capture_time);

        //check to make sure that the sampled length buffer is a power of two
        numBytes = StartDSP.nearestPow2Length(numBytes);

        //  * 8 due to num bytes in a double
        //  / 2 due to the fact that we are only using half the original sample
        //  = * 4
        numBytes = numBytes * 4 / getResources().getInteger(R.integer.samples_per_bin);

        byte[] tempByte = new byte[numBytes];
        double[] xData = new double[numBytes / 8];
        double[] yData = new double[numBytes / 8];

		/*I'm using an array for the min and max values so that it can be passed between
		 * functions without losing data. Since java does not support pass by reference,
		 * this is one way we can get extra return values. */
        int[] minmax = new int[2];

        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        //minmax[0] = min, minmax[1] = max

        //get the latest data set based on our search criteria
        c.moveToLast();

        //This Xdata set is common to all our Ydata sets, so we can load it once
        //before everything else.
        tempByte = c.getBlob(c.getColumnIndex(TableEntry.COLUMN_NAME_XVALS));
        ByteBuffer byteBuffer = ByteBuffer.allocate(tempByte.length);
        byteBuffer.put(tempByte);
        byteBuffer.rewind();

        for (int k = 0; k < xData.length; k++)
            xData[k] = byteBuffer.getDouble();


        String title = c.getString(c.getColumnIndex(TableEntry.COLUMN_NAME_DATE)) + c.getString(c.getColumnIndex(TableEntry.COLUMN_NAME_COMMENT));
        graph = (GraphView) findViewById(R.id.FFTgraph);
        
        if (SHOW_ALL.equals("NO")) {
            //grab the average blob, and convert it back into it's original form (double)
            tempByte = c.getBlob(c.getColumnIndex(TableEntry.COLUMN_NAME_YVALS));
            byteBuffer.clear();
            byteBuffer.rewind();
            byteBuffer.put(tempByte);
            byteBuffer.rewind();

            for (int k = 0; k < yData.length; k++) {
                yData[k] = byteBuffer.getDouble();
                if(minmax[1] < yData[k])
                    minmax[1] = (int) Math.ceil(yData[k]);

                if(minmax[0] > yData[k])
                    minmax[0] = (int) Math.floor(yData[k]);
            }

            //add the dataset to the graph
            DataPoint[] values = new DataPoint[yData.length];
            for (int n = 0; n < yData.length; n++) {
                DataPoint v = new DataPoint(xData[n], yData[n]);
                values[n] = v;
            }
            LineGraphSeries<DataPoint> data = new LineGraphSeries<DataPoint>(values);
            graph.addSeries(data);
            min = (int) minmax[0];
            max = (int) minmax[1];
        }

        else {
            //we load each set of data separately
            for (int i = 0; i < numImpulses; i++) {
                //grab the associated impulse blob and convert it back into doubles
                tempByte = c.getBlob(c.getColumnIndex(TableEntry.COLUMN_NAME_IMPULSE + Integer.toString(i)));
                byteBuffer.clear();
                byteBuffer.rewind();
                byteBuffer.put(tempByte);
                byteBuffer.rewind();

                for (int k = 0; k < yData.length; k++) {
                    yData[k] = byteBuffer.getDouble();
                    if (minmax[1] < yData[k])
                        minmax[1] = (int) Math.ceil(yData[k]);

                    if (minmax[0] > yData[k])
                        minmax[0] = (int) Math.floor(yData[k]);
                }

                //add the impulse data to the series
                DataPoint[] values = new DataPoint[yData.length];
                for (int n = 0; n < yData.length; n++) {
                    DataPoint v = new DataPoint(xData[n], yData[n]);
                    values[n] = v;
                }
                LineGraphSeries<DataPoint> data = new LineGraphSeries<DataPoint>(values);
                graph.addSeries(data);

                if (min > minmax[0])
                    min = (int) minmax[0];
                if (max < minmax[1])
                    max = (int) minmax[1];
                int color = 0;

		/*Don't ask what all these colors are, I don't even really know.
		 * The point is that they are all different.
		 * They are in the form of ALPHA RED GREEN BLUE, meaning the first
		 * byte is the alpha value (opacity or see-through-ness and in this
		 * case is always 0xff), the second byte is red and so forth.*/

                switch (i) {
                    case -1:
                        color = Color.BLUE;
                        break;
                    case 0:
                        color = 0xff0099cc;
                        break;
                    case 1:
                        color = 0xff9933cc;
                        break;
                    case 2:
                        color = 0xff669900;
                        break;
                    case 3:
                        color = 0xffff8800;
                        break;
                    case 4:
                        color = 0xffcc0000;
                        break;
                    case 5:
                        color = 0xff33b5e5;
                        break;
                    case 6:
                        color = 0xffaa66cc;
                        break;
                    case 7:
                        color = 0xff99cc00;
                        break;
                    case 8:
                        color = 0xffffbb33;
                        break;
                    case 9:
                        color = 0xff4444;
                        break;
                    default:
                        color = Color.BLUE;
                        break;
                }
                data.setColor(color);
            }
        }

        db.close();
        db = null;

        minmax[0] = min; //-2^31
        minmax[1] = max;// 2^ 31 - 1

        //set up graph so it displays the way we want
        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        staticLabelsFormatter.setVerticalLabels(GenerateVerticalLabels(minmax));
        staticLabelsFormatter.setHorizontalLabels(null);

        graph.setTitle(title);
        //allows the user to zoom in and scroll
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);
        graph.getViewport().setMinY(minmax[0]);
        graph.getViewport().setMaxY(minmax[1]);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(1000);
        //graph.getGridLabelRenderer().setHorizontalAxisTitle("Frequency in Hz");
        graph.getGridLabelRenderer().setVerticalAxisTitle(null);
    }

        private String[] GenerateVerticalLabels(int[] minmax)
        {
		/*we need to truncate the last digit so everything is a nice multiple of 10
		 * because Dr. Smith likes nice clean multiples of 10.*/
            if(minmax[0] >= 0)
            {
                minmax[0] = minmax[0] / 10;
                minmax[0] = minmax[0] * 10;
            }
            else
            {
                minmax[0] = minmax[0] / 10;
                minmax[0] = (minmax[0] - 1) * 10;
            }

            if(minmax[1] >= 0)
            {
                minmax[1] = minmax[1] / 10;
                minmax[1] = (minmax[1] + 1) * 10;
            }
            else
            {
                minmax[1] = minmax[1] / 10;
                minmax[1] = minmax[1] * 10;
            }

            int numIntervals = 0;
            int stride = 0;

            if( (minmax[1] - minmax[0]) <= 100)
            {
                numIntervals = (minmax[1] - minmax[0]) / 10  + 1;
                stride = 10;
            }
            else
            {
                numIntervals = 11;
                stride = (minmax[1] - minmax[0]) / 10;
                //make stride a multiple of 5
                stride = stride / 5;
                stride = (stride + 1) * 5;

                //max must therefore be slightly larger than before
                minmax[1] = minmax[0] + stride * (numIntervals - 1);
            }

            String[] labels = new String[numIntervals];

            for(int k =0; k < numIntervals; k++)
                labels[k] = Integer.toString(minmax[0] + (numIntervals - k - 1) * stride);

            return labels;
        }


//=================================================================================================================

    //plotting Histogram
    private void loadHistogram(String received_date) {

        setContentView(R.layout.activity_display_histogram);
        //access database to get values
        MySQLiteDatabaseHelper databaseHelper = new MySQLiteDatabaseHelper(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String[] projection = new String[]{TableEntry.COLUMN_NAME, TableEntry.COLUMN_DATE, TableEntry.COLUMN_COMMENT, TableEntry.COLUMN_PERCENTAGE_WORSE_CASE,
                TableEntry.COLUMN_RATIO_BACKGROUND_NOSE};
        Cursor cursor = null;
        String where, date, title;
        //if no history is picked, load current/previous result
        if (received_date == null && SHOW_TOTAL.equals("NO")){
            cursor = db.query(TableEntry.TABLE_NAME, projection, null, null, null, null, null, null);

            //check to see if there are any entries. If there are none
            //then tell the user that there is no data to display.
            if (cursor.getCount() == 0) {
                Toast.makeText(DisplayGraph.this, getResources().getString(R.string.no_entries), Toast.LENGTH_LONG).show();
                return;
            }

            //getting values of current recording only
            cursor.moveToLast();
            date = cursor.getString(cursor.getColumnIndex(TableEntry.COLUMN_DATE));
            where = TableEntry.COLUMN_DATE + " = '" + date + "'";
            //we are passing received date as a parameter to plotHist. It cannot be null.
            received_date = date;
        }
        //show all results over time
        else if(SHOW_TOTAL.equals("YES"))
            where = null;

        //if history is picked
        else
            where = TableEntry.COLUMN_DATE + " = '" + received_date + "'";

        cursor = db.query(TableEntry.TABLE_NAME, projection, where, null, null, null, null, null);
        List<Double> percentage_worse_case = new ArrayList<Double>();
        List<Double> ratio_background_noise = new ArrayList<Double>();
        cursor.moveToFirst();
        title = cursor.getString(cursor.getColumnIndex(TableEntry.COLUMN_COMMENT));

        while (!cursor.isAfterLast()) {
            if (cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_PERCENTAGE_WORSE_CASE)) != 0)
                percentage_worse_case.add(cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_PERCENTAGE_WORSE_CASE)));
            if (cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_RATIO_BACKGROUND_NOSE)) != 0)
                ratio_background_noise.add(cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_RATIO_BACKGROUND_NOSE)));
            cursor.moveToNext();
        }

        db.close();
        Log.d("ADebugTag", "Percentage worse case - " + percentage_worse_case);
        Log.d("ADebugTag", "Ratio background noise - " + ratio_background_noise);

        plotHist("Percentage Worse Case ", title, received_date, percentage_worse_case);
        plotHist("Ratio Background Noise ", title, received_date, ratio_background_noise);
    }

    protected void plotHist(String GraphTitle, String description, String date, List<Double> frequencies) {

        GraphView graph = null;
        if (GraphTitle.startsWith("Percentage Worse Case"))
            graph = (GraphView) findViewById(R.id.percentage_worse_case);
        else
            graph = (GraphView) findViewById(R.id.ratio_background_noise);

        //clear graph before it is called again
        if (graph != null)
            graph.removeAllSeries();

        //if list is empty
        if (frequencies.size() == 0) {
            Toast.makeText(DisplayGraph.this, getResources().getString(R.string.empty_list) + " in " + GraphTitle, Toast.LENGTH_LONG).show();
            return;
        }

        //create frequency intervals of 10Hz
        int class_interval = 10;
        int minimum = (class_interval * (int) Math.round(Collections.min(frequencies) / class_interval)) - (2*class_interval);
        int maximum = (class_interval * (int) Math.round(Collections.max(frequencies) / class_interval)) + (2*class_interval);

        int[] countXvals = new int[(maximum - minimum) / class_interval];
        int[] countYvals = new int[(maximum - minimum) / class_interval - 1];
        int count = 0, n;

        for (n = minimum; n <= maximum - class_interval; n += class_interval) {
            countXvals[count++] = n;
        }

        //get frequency of occurrence of different frequencies to plot histogram
        for (n = 0; n < countYvals.length; n++) {
            for (Double key : frequencies) {
                if (key >= countXvals[n] && key <= countXvals[n + 1])
                    countYvals[n]++;
            }
        }

        DataPoint[] values = new DataPoint[countYvals.length];
        for (n = 0; n < countYvals.length; n++) {
            DataPoint v = new DataPoint((countXvals[n] + countXvals[n + 1]) / 2, countYvals[n]);
            values[n] = v;
        }

        BarGraphSeries<DataPoint> data = new BarGraphSeries<DataPoint>(values);
        graph.addSeries(data);
        data.setSpacing(10);
        //If total result over all time is displayed
        if (SHOW_TOTAL.equals("YES")) {
            data.setColor(Color.MAGENTA);
            graph.setTitle(GraphTitle +" - Total");
        } else {
            data.setColor(Color.BLUE);
            graph.setTitle(GraphTitle + " " + date + description);
        }

        //generating labels for X axis
        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        staticLabelsFormatter.setHorizontalLabels(xLabels);
        //setting x axis bounds
        graph.getViewport().setXAxisBoundsManual(true);
        double minRange = Double.parseDouble(xLabels[0]);
        double maxRange = Double.parseDouble(xLabels[xLabels.length -1]);
        graph.getViewport().setMinX(minRange);
        graph.getViewport().setMaxX(maxRange);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Frequency in Hz");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Num of occurrence");
        graph.getViewport().setScrollable(true);
    }



    //================================================================================================================
    /*comparing results of two histograms*/
    private void compareHistogram( String currentDate, String selectedDate) {

        setContentView(R.layout.activity_display_histogram);
        MySQLiteDatabaseHelper databaseHelper = new MySQLiteDatabaseHelper(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String[] projection = new String[]{TableEntry.COLUMN_NAME, TableEntry.COLUMN_DATE, TableEntry.COLUMN_COMMENT, TableEntry.COLUMN_PERCENTAGE_WORSE_CASE,
                TableEntry.COLUMN_RATIO_BACKGROUND_NOSE};
        Cursor cursor = null;
        Cursor cursorDescription =null;
        String where = null;
        String currentTitle, selectedTitle;

        if(currentDate != null)
            where = TableEntry.COLUMN_DATE + " = '" + currentDate + "'";
        else {
            cursor = db.query(TableEntry.TABLE_NAME, projection, null, null, null, null, null, null);
            cursor.moveToLast();
            currentDate = cursor.getString(cursor.getColumnIndex(TableEntry.COLUMN_DATE));
            where = TableEntry.COLUMN_DATE + " = '" + currentDate + "'";
        }

        cursor = db.query(TableEntry.TABLE_NAME, projection, where, null, null, null, null, null);
        List<Double> percentage_worse_case_current = new ArrayList<Double>();
        List<Double> ratio_background_noise_current = new ArrayList<Double>();
        cursor.moveToFirst();
        String legend_current = cursor.getString(cursor.getColumnIndex(TableEntry.COLUMN_COMMENT));

        while (!cursor.isAfterLast()) {
            if (cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_PERCENTAGE_WORSE_CASE)) != 0)
                percentage_worse_case_current.add(cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_PERCENTAGE_WORSE_CASE)));
            if (cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_RATIO_BACKGROUND_NOSE)) != 0)
                ratio_background_noise_current.add(cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_RATIO_BACKGROUND_NOSE)));
            cursor.moveToNext();
        }

        where = TableEntry.COLUMN_DATE + " = '" + selectedDate + "'";
        cursor = db.query(TableEntry.TABLE_NAME, projection, where, null, null, null, null, null);
        List<Double> percentage_worse_case_selected = new ArrayList<Double>();
        List<Double> ratio_background_noise_selected = new ArrayList<Double>();
        cursor.moveToFirst();
        String legend_selected = cursor.getString(cursor.getColumnIndex(TableEntry.COLUMN_COMMENT));

        while (!cursor.isAfterLast()) {
            if (cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_PERCENTAGE_WORSE_CASE)) != 0)
                percentage_worse_case_selected.add(cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_PERCENTAGE_WORSE_CASE)));
            if (cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_RATIO_BACKGROUND_NOSE)) != 0)
                ratio_background_noise_selected.add(cursor.getDouble(cursor.getColumnIndex(TableEntry.COLUMN_RATIO_BACKGROUND_NOSE)));
            cursor.moveToNext();
        }


        plotCompareHistogram("Percentage Worse Case" , legend_current, legend_selected, currentDate, selectedDate, percentage_worse_case_current, percentage_worse_case_selected);
        plotCompareHistogram("Ratio background noise" , legend_current, legend_selected, currentDate, selectedDate, ratio_background_noise_current, ratio_background_noise_selected);

        db.close();
    }


    private void plotCompareHistogram (String GraphTitle, String legend_current, String legend_selected, String currentDate, String selectedDate, List<Double> currentFrequencies, List<Double> selectedFrequencies) {

        GraphView graphCompare = null;
        if(GraphTitle.equalsIgnoreCase("Percentage Worse Case"))
            graphCompare = (GraphView) findViewById (R.id.percentage_worse_case);
        else
            graphCompare = (GraphView) findViewById (R.id.ratio_background_noise);

        //clear graph before it is called again
        if(graphCompare != null)
            graphCompare.removeAllSeries();

        if(currentDate.equals(selectedDate)){
            Toast.makeText(DisplayGraph.this, "Cannot compare with itself", Toast.LENGTH_LONG).show();
            return;
        }

        if(currentFrequencies.size() == 0){
            Toast.makeText(DisplayGraph.this, getResources().getString(R.string.empty_list) + " in " + GraphTitle + " " + currentDate , Toast.LENGTH_LONG).show();
            return;
        }
        else if(selectedFrequencies.size() == 0){
            Toast.makeText(DisplayGraph.this, getResources().getString(R.string.empty_list) + " in " + GraphTitle + " " + selectedDate , Toast.LENGTH_LONG).show();
            return;
        }

        //creating frequency interval of 10Hz
        int class_interval = 10;
        double minCol,maxCol;
        if(Collections.min(currentFrequencies) < Collections.min(selectedFrequencies))
            minCol = Collections.min(currentFrequencies);
        else
            minCol = Collections.min(selectedFrequencies);

        if(Collections.max(currentFrequencies) > Collections.max(selectedFrequencies))
            maxCol = Collections.max(currentFrequencies);
        else
            maxCol = Collections.max(selectedFrequencies);

        int minimum = class_interval * (int) Math.round(minCol / class_interval) - (2*class_interval);
        int maximum = class_interval * (int) Math.round(maxCol / class_interval) + (2*class_interval);


        int[] countXvals = new int[(maximum - minimum) / class_interval];
        int[] countYvalsCurrent = new int[(maximum - minimum) / class_interval - 1];
        int [] countYvalsSelected = new int[(maximum - minimum) / class_interval - 1];
        int count = 0, n;

        for (n = minimum; n <= maximum - class_interval; n += class_interval) {
            countXvals[count++] = n;
        }

        //get frequency of occurrence of different frequencies to plot histogram
        for (n = 0; n < countYvalsCurrent.length; n++) {
            for (Double key : currentFrequencies) {
                if (key >= countXvals[n] && key <= countXvals[n + 1])
                    countYvalsCurrent[n]++;
            }
            for (Double key : selectedFrequencies){
                if (key >= countXvals[n] && key <= countXvals[n + 1])
                    countYvalsSelected[n]++;
            }
        }

        DataPoint [] valuesCurrent = new DataPoint [countYvalsCurrent.length];
        DataPoint [] valuesSelected = new DataPoint [countYvalsSelected.length];
        for(n = 0; n < countYvalsCurrent.length; n++) {
            DataPoint v = new DataPoint((countXvals[n] + countXvals[n+1])/2, countYvalsCurrent[n]);
            valuesCurrent[n] = v;

        }

        for(n = 0; n < countYvalsSelected.length; n++) {
            DataPoint v = new DataPoint((countXvals[n] + countXvals[n+1])/2, countYvalsSelected[n]);
            valuesSelected[n] = v;
        }


        BarGraphSeries<DataPoint> dataCurrent = new BarGraphSeries<DataPoint>(valuesCurrent);
        BarGraphSeries<DataPoint> dataSelected = new BarGraphSeries<DataPoint>(valuesSelected);
        graphCompare.addSeries(dataCurrent);
        //dataCurrent.setSpacing(10);
        dataCurrent.setColor(Color.BLUE );
        graphCompare.addSeries(dataSelected);
        dataSelected.setColor(Color.RED);


        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graphCompare);
        staticLabelsFormatter.setHorizontalLabels(xLabels);
        //setting x axis bounds
        double minRange = Double.parseDouble(xLabels[0]);
        double maxRange = Double.parseDouble(xLabels[xLabels.length -1]);
        graphCompare.getViewport().setXAxisBoundsManual(true);
        graphCompare.getViewport().setMinX(minRange);
        graphCompare.getViewport().setMaxX(maxRange);
        graphCompare.setTitle(GraphTitle);
        graphCompare.getGridLabelRenderer().setHorizontalAxisTitle("Frequency in Hz");
        graphCompare.getGridLabelRenderer().setVerticalAxisTitle("Num occurrence");
        graphCompare.getViewport().setScrollable(true);
        //setting legend
        dataCurrent.setTitle(currentDate  + legend_current);
        dataSelected.setTitle(selectedDate + legend_selected);

        if(GraphTitle.equals("Percentage Worse Case")) {
            graphCompare.getLegendRenderer().setVisible(true);
            graphCompare.getLegendRenderer().setTextSize(16);
            graphCompare.getLegendRenderer().setTextColor(Color.BLACK);
            graphCompare.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
            //graphCompare.getLegendRenderer().setFixedPosition(5, 5);
        }

    }
}




