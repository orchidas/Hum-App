package com.example.orchisamadas.analyse_plot;

import android.provider.BaseColumns;
public final class MySQLiteDatabaseContract{
    public MySQLiteDatabaseContract(){}
    public static abstract class TableEntry implements BaseColumns{
        //this table stores the analysis results
        public static final String TABLE_NAME = "analysis_data";
        public static final String COLUMN_NAME = "nameID";
        public static final String COLUMN_DATE =  "dateTime";
        public static final String COLUMN_MAX_SIGNAL = "maximum_signal_frequency";
        public static final String COLUMN_PERCENTAGE_WORSE_CASE = "percentage_worse_Case";
        public static final String COLUMN_RATIO_BACKGROUND_NOSE = "ratio_background_noise";
        public static final String COLUMN_COMMENT = "comments";


        //this table stores the FFT results
        public static final String TABLE_NAME_FFT = "fft_data";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_XVALS = "xvals";
        public static final String COLUMN_NAME_YVALS = "yvals";
        public static final String COLUMN_NAME_IMPULSE = "impluseno";
        public static final String COLUMN_NAME_COMMENT = "comments_fft";


        //be sure to append the impulse number to the COLUMN_NAME_IMPULSE string
        //for example: TableEntry.COLUMN_NAME_IMPULSE + Integer.toString(k)

    }
}