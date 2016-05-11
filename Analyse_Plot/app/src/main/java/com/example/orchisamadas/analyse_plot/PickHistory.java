package com.example.orchisamadas.analyse_plot;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.orchisamadas.analyse_plot.MySQLiteDatabaseContract.TableEntry;

//Using most of Nick's code here as well
/*This class defines the list that shows all the dates and allows
 * you to pick one. It is called from DisplayGraph? Since query tasks can be long, I probably should not
 * be doing this operation on the UI thread. Should probably be implemented
 * with a cursorLoader.*/

public class PickHistory extends ListActivity {

    private SimpleCursorAdapter mAdapter;
    private MySQLiteDatabaseHelper databaseHelper;
    private SQLiteDatabase db;
    private static String[] dateBuffer;
    private String which_button_pressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pick_history);
        databaseHelper = new MySQLiteDatabaseHelper(this);
        Bundle bundle = getIntent().getExtras();
        which_button_pressed = bundle.getString("button_pressed");

        if (which_button_pressed.equals("1"))
            loadListGraph();
        else
            loadListHistogram();

        /*Registers this ViewGroup for use with the context menu. This means that all
		 * view which are part of the ListView group (ie. all items in the list) are
		 * attached to the context menu, and on a long click will cause the context menu
		 * to appear.*/
        registerForContextMenu((ListView) findViewById(android.R.id.list));
    }


    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
        //we need to return to the last activity using a listener and close this activity
        //the return value should be idBuffer[position];

		/*used in this way, setResult sets what the onActivityResult resultCode will be
		 * when the current activity terminates and goes back to the previous activity.
		 * In this case it is the date of the item clicked on in the list. */
        Intent intent = new Intent();
        intent.putExtra("RESULT_STRING", dateBuffer[pos]);
        setResult(RESULT_OK, intent);
        finish();
    }


    //Context Menu =========================================================
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		/*This function is called when the context menu is created (when the user
		 * makes a long click I believe). The menu inflater sets up the menu view
		 * which is filled with the menu view which we want to display. */
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
		/*The context menu adapter contains the info on everything in the list*/
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

		/*Here we decide what to do with the context menu item that was selected.
		 * This happens after the context menu is created so we're dealing with
		 * the possible options available in the context menu. */
        switch (item.getItemId()) {
            
            case R.id.delete_entry:
                //find the entry and delete it
                db = databaseHelper.getWritableDatabase();
                db.delete(TableEntry.TABLE_NAME, TableEntry.COLUMN_DATE + " = '" + dateBuffer[info.position] + "'", null);
                db.delete(TableEntry.TABLE_NAME_FFT, TableEntry.COLUMN_NAME_DATE + " = '" + dateBuffer[info.position] + "'", null);
                Toast.makeText(PickHistory.this, "Entry deleted", Toast.LENGTH_SHORT).show();
                db.close();

                //reload the list so the deleted entry is no longer there
                if (which_button_pressed.equals("1"))
                    loadListGraph();
                else
                    loadListHistogram();
                return true;

            default:
                //do the default task
                return super.onContextItemSelected(item);
        }
    }
//============================================================================================================================

    //loads FFT graph history
    private void loadListGraph() {
        db = databaseHelper.getReadableDatabase();
        String PROJECTION[] = new String[]{TableEntry._ID, TableEntry.COLUMN_NAME_DATE, TableEntry.COLUMN_NAME_COMMENT};

		/*Loads all entries of the table. The projection
		 * defines which columns to load which in this case
		 * is the _ID column and date column. */
        Cursor c = db.query(TableEntry.TABLE_NAME_FFT,
                PROJECTION,
                null,
                null,
                null,
                null,
                null,
                null);


		/*We need to fill an array with the identities of all the table entries. since this
		 * information is not shown to the user we must handle it ourselves.*/
        dateBuffer = new String[c.getCount()];
        c.moveToFirst();
        for (int k = 0; k < c.getCount(); k++) {
            dateBuffer[k] = c.getString(c.getColumnIndex(TableEntry.COLUMN_NAME_DATE));
            c.moveToNext();
        }


		/*this changes all the dates that we loaded to something usable by the list view.
		 * Essentially, all the dates get turned into textViews as defined by "date_entry"
		 * and then are sent to the list view. */

        startManagingCursor(c);
        mAdapter = new SimpleCursorAdapter(this, R.layout.layout_pick_history, c,
                new String[]{TableEntry.COLUMN_NAME_DATE,TableEntry.COLUMN_NAME_COMMENT}, new int[]{R.id.date_entry,R.id.comment_entry}, 0);

        //to view both date and comment, we need to bind them together with ViewBinder
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                boolean result = false;
                if(view.getId() == R.id.date_entry) {
                    TextView date = (TextView) view.findViewById(R.id.date_entry);
                    date.setText(cursor.getString(cursor.getColumnIndex(TableEntry.COLUMN_NAME_DATE)));
                    result = true;
                }
                else if(view.getId() == R.id.comment_entry){
                    TextView comment = (TextView) view.findViewById(R.id.comment_entry);
                    comment.setText(cursor.getString(cursor.getColumnIndex(TableEntry.COLUMN_NAME_COMMENT)));
                    result = true;
                }
                    return result;
            }
            });

            db.close();

            //load the adapter into the list
            setListAdapter(mAdapter);
            return;
        }

//==================================================================================================================

//loads histogram history

    private void loadListHistogram() {
        db = databaseHelper.getReadableDatabase();
        //We only select distinct dates from the table along with the row ids of those dates
        String select = "SELECT DISTINCT " + TableEntry.COLUMN_DATE + " as " + TableEntry._ID + ", " + TableEntry.COLUMN_DATE + ", " + TableEntry.COLUMN_COMMENT +
                " FROM " + TableEntry.TABLE_NAME;
        Cursor c = db.rawQuery(select, null);


        /*We need to fill an array with the dates of all the table entries. since this
		 * information is not shown to the user we must handle it ourselves.
        idBuffer = new String[c.getCount()];*/
        dateBuffer = new String[c.getCount()];
        c.moveToFirst();
        for (int k = 0; k < c.getCount(); k++) {
            dateBuffer[k] = c.getString(c.getColumnIndex(TableEntry.COLUMN_DATE));
            c.moveToNext();
        }

		/*this changes all the dates that we loaded to something usable by the list view.
		 * Essentially, all the dates get turned into textViews as defined by "date_entry"
		 * and then are sent to the list view.
        //SimpleCursorAdapter - An easy adapter to map columns from a cursor to TextViews or ImageViews defined in an XML file */
        mAdapter = new SimpleCursorAdapter(this, R.layout.layout_pick_history, c,
                new String[]{TableEntry.COLUMN_DATE,TableEntry.COLUMN_COMMENT}, new int[]{R.id.date_entry,R.id.comment_entry}, 0);

        //to view both date and comment, we need to bind them together with ViewBinder
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                boolean result = false;
                if(view.getId() == R.id.date_entry) {
                    TextView date = (TextView) view.findViewById(R.id.date_entry);
                    date.setText(cursor.getString(cursor.getColumnIndex(TableEntry.COLUMN_DATE)));
                    result = true;
                }
                else if(view.getId() == R.id.comment_entry){
                    TextView comment = (TextView) view.findViewById(R.id.comment_entry);
                    comment.setText(cursor.getString(cursor.getColumnIndex(TableEntry.COLUMN_COMMENT)));
                    result = true;
                }
                return result;
            }
        });



        //load the adapter into the list
        setListAdapter(mAdapter);
        db.close();
        return;
    }
}




