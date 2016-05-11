package com.example.orchisamadas.analyse_plot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

public class StartApp extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_app);
    }

    public void StartDSP(View v){
        Intent intent=new Intent(this,StartDSP.class);
        startActivity(intent);
    }

    /*Starts the activity DisplayGraph to view previous graphs
    We can either view previous FFT graphs or previous analysis histograms
    depending on which button is pressed */
    public void gotoGraphFFT(View v)
    {
        Bundle bundle = new Bundle();
        bundle.putString("button_pressed", "1");
        Intent intent = new Intent(this, DisplayGraph.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void gotoHistogram(View v)
    {
        Bundle bundle = new Bundle();
        bundle.putString("button_pressed", "2");
        Intent intent = new Intent(this, DisplayGraph.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

}