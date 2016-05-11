package com.example.orchisamadas.analyse_plot;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.orchisamadas.analyse_plot.MySQLiteDatabaseContract.TableEntry;

public class MySQLiteDatabaseHelper extends SQLiteOpenHelper{
    public static final String NAME="DataAnalysis3.db";
    public static final int VERSION=1;
    public static Context mContext;
    public MySQLiteDatabaseHelper(Context context){
        super(context,NAME,null,VERSION);mContext=context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //this table stores analysis results
        String create = "CREATE TABLE IF NOT EXISTS " + TableEntry.TABLE_NAME + " (" + TableEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TableEntry.COLUMN_NAME+ " TEXT, "
                + TableEntry.COLUMN_COMMENT + " TEXT, "
                + TableEntry.COLUMN_DATE + " TEXT, "
                + TableEntry.COLUMN_MAX_SIGNAL + " REAL, "
                + TableEntry.COLUMN_PERCENTAGE_WORSE_CASE + " REAL, "
                + TableEntry.COLUMN_RATIO_BACKGROUND_NOSE + " REAL";
        create = create + ")";
        db.execSQL(create);


        //this table stores FFT results
       create = "CREATE TABLE IF NOT EXISTS " + TableEntry.TABLE_NAME_FFT + " ("
                + TableEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TableEntry.COLUMN_NAME_DATE + " TEXT, "
               + TableEntry.COLUMN_NAME_COMMENT+ " TEXT, "
                + TableEntry.COLUMN_NAME_XVALS + " BLOB, "
                + TableEntry.COLUMN_NAME_YVALS + " BLOB";

        int numImpulses = mContext.getResources().getInteger(R.integer.num_impulses);
        for(int k = 0; k < numImpulses; k++)
            create = create + ", " + TableEntry.COLUMN_NAME_IMPULSE + Integer.toString(k) + " BLOB";

        create = create + ")";

        //this command executes an SQLite command, in this case
        //it creates our table.
        db.execSQL(create);



    }
    public void deleteTable(SQLiteDatabase db, String tableName){
        final String delete="DROP TABLE IF EXISTS "+tableName;
        db.execSQL(delete);
    }
    public void deleteAllEntries(SQLiteDatabase db,String tableName){
        db.delete(tableName, null, null);}

    public void deleteDatabase(){mContext.deleteDatabase(NAME);}

    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        if (newVersion <= oldVersion)
            return;
        deleteDatabase();
        onCreate(db);
        return;
    }
}


