package com.saladdressing.veterondo;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import com.saladdressing.veterondo.adapters.GridDotAdapter;
import com.saladdressing.veterondo.pojos.Dot;
import com.saladdressing.veterondo.pojos.WeatherPaletteGenerator;
import android.os.Process;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final Handler handler = new Handler();
    static GridView grid;
    static ArrayList<Dot> dots = new ArrayList<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    GridDotAdapter adapter;
    TextView appTitle;
    TextView weatherDescription;
    TextView location;
    TextView temp;
    Runnable myRunnable;
    private Future<?> timingTask;

    public final String generateRandomColorFromPalette(String[] palette) {

        int paletteSize = palette.length;

        Random rand = new Random();

        return palette[rand.nextInt(paletteSize)];

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        makeFullscreen();
        keepScreenOn();


        appTitle = (TextView) findViewById(R.id.app_name);
        grid = (GridView) findViewById(R.id.dot_grid);
        weatherDescription = (TextView) findViewById(R.id.weather_desc);
        location = (TextView) findViewById(R.id.location);
        temp = (TextView) findViewById(R.id.temp);

        Typeface titleTypeface = Typeface.createFromAsset(getAssets(), "fonts/Ailerons-Typeface.otf");
        Typeface scriptTypeface = Typeface.createFromAsset(getAssets(), "fonts/Junction-light.otf");

        SpannableString span = new SpannableString("::::::::veterondo");
        span.setSpan(new ForegroundColorSpan(Color.parseColor("#F44336")), 9, 10, 0);
        span.setSpan(new ForegroundColorSpan(Color.parseColor("#9C27B0")), 10, 11, 0);
        span.setSpan(new ForegroundColorSpan(Color.parseColor("#2196F3")), 11, 12, 0);
        span.setSpan(new ForegroundColorSpan(Color.parseColor("#64FFDA")), 12, 13, 0);
        span.setSpan(new ForegroundColorSpan(Color.parseColor("#C6FF00")), 13, 14, 0);
        span.setSpan(new ForegroundColorSpan(Color.parseColor("#EF6C00")), 14, 15, 0);
        span.setSpan(new ForegroundColorSpan(Color.parseColor("#FFFF00")), 15, 16, 0);


        appTitle.setTypeface(titleTypeface);
        weatherDescription.setTypeface(titleTypeface);
        location.setTypeface(titleTypeface);
        temp.setTypeface(scriptTypeface);

        appTitle.setText(span);

        for (int i = 0; i < 36; i++) {

            Dot dot = new Dot();
            dot.setColor(generateRandomColorFromPalette(WeatherPaletteGenerator.getSunnyPalette()));
            dots.add(dot);
        }

        adapter = new GridDotAdapter(this, dots);
        grid.setAdapter(adapter);


        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                for (Dot dot : dots) {
                    dot.setColor(generateRandomColorFromPalette(WeatherPaletteGenerator.getFunkyPalette()));
                }


                adapter.notifyDataSetChanged();

            }
        });


        timingTask = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        for (Dot dot : dots) {
                            dot.setColor(generateRandomColorFromPalette(WeatherPaletteGenerator.getCloudyPalette()));
                        }


                        adapter.notifyDataSetChanged();

                    }
                });

            }
        }, 0, 30000, TimeUnit.MILLISECONDS);


    }

    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void makeFullscreen() {
        View decorView = getWindow().getDecorView();

        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(myRunnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Process.killProcess(android.os.Process.myPid());

    }


}
