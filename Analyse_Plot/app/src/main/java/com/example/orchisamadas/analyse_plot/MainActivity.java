package com.example.orchisamadas.analyse_plot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn1 = (Button) findViewById(R.id.buttonNotAgree);
        btn1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Toast.makeText(MainActivity.this, "Consent not given. Closing app", Toast.LENGTH_SHORT).show();
                finish();
                System.exit(0);
            }
        });
    }

    public void StartApp(View v){
            Intent intent = new Intent(MainActivity.this, StartApp.class);
            startActivity(intent);
    }

    public void TermsAndConditions(View v){
        Intent intent = new Intent(MainActivity.this, TermsAndConditions.class);
        startActivity(intent);
    }
}


