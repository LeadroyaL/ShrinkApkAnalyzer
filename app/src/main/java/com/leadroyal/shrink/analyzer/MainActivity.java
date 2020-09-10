package com.leadroyal.shrink.analyzer;

import android.os.Bundle;

import com.android.tools.apk.analyzer.ApkAnalyzerCli;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                ApkAnalyzerCli.main(new String[]{});
                ApkAnalyzerCli.main(new String[]{"manifest", "print", getPackageName()});
                ApkAnalyzerCli.main(new String[]{"apk", "summary", getApplicationInfo().publicSourceDir});
                ApkAnalyzerCli.main(new String[]{"manifest", "print", getApplicationInfo().publicSourceDir});
                ApkAnalyzerCli.main(new String[]{"manifest", "application-id", getApplicationInfo().publicSourceDir});
                ApkAnalyzerCli.main(new String[]{"manifest", "version-name", getApplicationInfo().publicSourceDir});
                ApkAnalyzerCli.main(new String[]{"manifest", "version-code", getApplicationInfo().publicSourceDir});
                ApkAnalyzerCli.main(new String[]{"manifest", "min-sdk", getApplicationInfo().publicSourceDir});
                ApkAnalyzerCli.main(new String[]{"manifest", "target-sdk", getApplicationInfo().publicSourceDir});
                ApkAnalyzerCli.main(new String[]{"manifest", "debuggable", getApplicationInfo().publicSourceDir});
                ApkAnalyzerCli.main(new String[]{"resources", "xml", getApplicationInfo().publicSourceDir, "--file", "AndroidManifest.xml"});
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}