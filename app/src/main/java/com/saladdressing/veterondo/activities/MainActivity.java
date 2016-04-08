package com.saladdressing.veterondo.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
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

import com.saladdressing.veterondo.R;
import com.saladdressing.veterondo.adapters.GridDotAdapter;
import com.saladdressing.veterondo.enums.WeatherKind;
import com.saladdressing.veterondo.generators.DotsShapeGenerator;
import com.saladdressing.veterondo.generators.MusicMachine;
import com.saladdressing.veterondo.interfaces.PlaybackListener;
import com.saladdressing.veterondo.pojos.Dot;
import com.saladdressing.veterondo.pojos.OpenCurrentWeather;
import com.saladdressing.veterondo.pojos.WeatherPaletteGenerator;
import com.saladdressing.veterondo.retrofitinterfaces.GetCurrentWeatherInterface;
import com.saladdressing.veterondo.utils.Constants;
import com.saladdressing.veterondo.utils.SPS;
import com.saladdressing.veterondo.utils.SamplePlayer;

import android.os.Process;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainActivity extends AppCompatActivity {


    public static final int REFRESH_PENDING_INTENT_REQUEST_CODE = 1;
    public static final long TIME_TO_REFRESH = 10 * 60 * 1000;
    private static final Handler handler = new Handler();
    private static final int MY_PERMISSION_REQ_CODE = 123;
    static GridView grid;
    static ArrayList<Dot> dots = new ArrayList<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    AlarmManager alarmManager;
    PendingIntent pendingIntent;
    SamplePlayer samplePlayer;
    String condition;
    String[] weatherPalette = WeatherPaletteGenerator.getFunkyPalette();
    GridDotAdapter adapter;
    TextView appTitle;
    TextView weatherDescription;
    TextView location;
    TextView temp;
    Runnable myRunnable;
    WeatherKind mWeatherKind = WeatherKind.SUNNY;
    private Future<?> timingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (dots != null && dots.size() > 0) {
            dots.clear();
        }

        makeFullscreen();
        keepScreenOn();
        initializeDots();

        appTitle = (TextView) findViewById(R.id.app_name);
        grid = (GridView) findViewById(R.id.dot_grid);
        weatherDescription = (TextView) findViewById(R.id.weather_desc);
        location = (TextView) findViewById(R.id.location);
        temp = (TextView) findViewById(R.id.temp);

        samplePlayer = new SamplePlayer(this);

        Typeface titleTypeface = Typeface.createFromAsset(getAssets(), "fonts/Ailerons-Typeface.otf");
        Typeface scriptTypeface = Typeface.createFromAsset(getAssets(), "fonts/RobotoSlab-Light.ttf");

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


        adapter = new GridDotAdapter(this, dots);
        grid.setAdapter(adapter);

        final AdapterView.OnItemClickListener dotListener = new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                for (Dot dot : dots) {
                    dot.setColor(generateRandomColorFromPalette(WeatherPaletteGenerator.getFunkyPalette()));
                }

                /*
                for (int i=0; i< DotsShapeGenerator.returnSunShape().length; i++) {
                    dots.get(i).setColor(DotsShapeGenerator.returnSunShape()[i]);
                }

                */

                adapter.notifyDataSetChanged();
                weatherDescription.setText("FUNKY");

                MusicMachine musicMachine = new MusicMachine(MainActivity.this, mWeatherKind);
                musicMachine.playPattern(400, new PlaybackListener() {


                    @Override
                    public void onPlaybackStarted() {

                    }

                    @Override
                    public void onPlaybackCompleted() {

                    }


                });


            }


        };

        grid.setOnItemClickListener(dotListener);

        retrieveWeather();


        timingTask = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        for (Dot dot : dots) {
                            dot.setColor(generateRandomColorFromPalette(weatherPalette));
                        }


                        adapter.notifyDataSetChanged();
                        weatherDescription.setText(condition);


                    }
                });

            }
        }, 0, 30000, TimeUnit.MILLISECONDS);

        startRegularUpdates();


    }

    private void initializeDots() {
        for (int i = 0; i < 36; i++) {

            Dot dot = new Dot();
            dot.setColor(generateRandomColorFromPalette(WeatherPaletteGenerator.getSunnyPalette()));
            dots.add(dot);
        }
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
        if (alarmManager != null && pendingIntent != null)
            alarmManager.cancel(pendingIntent);
        overridePendingTransition(0, 0);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Process.killProcess(android.os.Process.myPid());

    }

    public ArrayList<Double> getLocation() {

        ArrayList<Double> latlon = new ArrayList<>();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQ_CODE);

            return null;

        } else {

            LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {

                latlon.add(location.getLatitude());
                latlon.add(location.getLongitude());

            }

            if (location == null) {
                latlon.add(0.0);
                latlon.add(0.0);

            }
        }
        return latlon;
    }

    public void retrieveWeather() {


        ArrayList<Double> loc = getLocation();

        if (loc != null) {

            double lat = loc.get(0);
            double lon = loc.get(1);

            RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://api.openweathermap.org/").setLogLevel(RestAdapter.LogLevel.FULL).build();
            GetCurrentWeatherInterface weatherInterface = restAdapter.create(GetCurrentWeatherInterface.class);


            weatherInterface.connect(lat, lon, new Callback<OpenCurrentWeather>() {

                @Override
                public void success(OpenCurrentWeather openCurrentWeather, Response response) {


                    location.setText(openCurrentWeather.getName());


                    long sunriseEpoch = openCurrentWeather.getSys().getSunrise();
                    long sunsetEpoch = openCurrentWeather.getSys().getSunset();

                    int id = openCurrentWeather.getWeather().get(0).getId();
                    boolean isNight = Constants.isNight(sunriseEpoch, sunsetEpoch);


                    evaluateWeatherObject(isNight, id, Constants.kelvinToCelsius(openCurrentWeather.getMain().getTemp()));


                    double temperature = Math.floor(Constants.kelvinToCelsius(openCurrentWeather.getMain().getTemp()));
                    int intTemp = (int) temperature;

                    weatherDescription.setText(condition);
                    temp.setText(intTemp + "°C");

                }


                @Override
                public void failure(RetrofitError error) {

                }
            });

        } else {

            Toast.makeText(MainActivity.this, "Couldn't get location! Enjoy the lightshow anyway!", Toast.LENGTH_LONG).show();
            setPaletteFromWeather(WeatherKind.FUNKY);
        }

    }


    public final String generateRandomColorFromPalette(String[] palette) {

        int paletteSize = palette.length;

        Random rand = new Random();

        return palette[rand.nextInt(paletteSize)];

    }

    private void evaluateWeatherObject(boolean isNight, int id, double celsiusTemp) {

        if (isNight) {

            setPaletteFromWeather(WeatherKind.NIGHTLY);
            mWeatherKind = WeatherKind.NIGHTLY;
            condition = "starry";

        } else {


            if (id >= 200 && id < 300) {

                condition = "Stormy";
                setPaletteFromWeather(WeatherKind.RAINY);
                mWeatherKind = WeatherKind.RAINY;


            }

            if (id >= 300 && id < 400) {
                condition = "Drizzle";
                mWeatherKind = WeatherKind.RAINY;

                setPaletteFromWeather(WeatherKind.RAINY);
            }

            if (id >= 500 && id < 600) {

                condition = "Rainy";
                mWeatherKind = WeatherKind.RAINY;

                setPaletteFromWeather(WeatherKind.RAINY);


            }

            if (id >= 600 && id < 700) {


                condition = "Snowy";
                mWeatherKind = WeatherKind.SNOWY;

                setPaletteFromWeather(WeatherKind.SNOWY);

            }

            if (id >= 700 && id < 800) {

                if (id == 701) {


                    condition = "Misty";
                    mWeatherKind = WeatherKind.CLOUDY;


                }

                if (id == 711) {


                    condition = "Smoky";
                    setPaletteFromWeather(WeatherKind.DUSTY);
                    mWeatherKind = WeatherKind.DUSTY;


                }

                if (id == 721) {


                    condition = "Hazy";


                }

                if (id == 731) {


                    condition = "Dusty";
                    mWeatherKind = WeatherKind.DUSTY;

                    setPaletteFromWeather(WeatherKind.DUSTY);


                }

                if (id == 741) {


                    condition = "Foggy";
                    mWeatherKind = WeatherKind.CLOUDY;

                    setPaletteFromWeather(WeatherKind.CLOUDY);


                }

                if (id == 751) {


                    condition = "Sandy";
                    mWeatherKind = WeatherKind.DUSTY;

                    setPaletteFromWeather(WeatherKind.DUSTY);


                }

                if (id == 761) {


                    condition = "Dusty";
                    mWeatherKind = WeatherKind.DUSTY;

                    setPaletteFromWeather(WeatherKind.DUSTY);


                }

                if (id == 762) {


                    condition = "Ash";
                    mWeatherKind = WeatherKind.CLOUDY;

                    setPaletteFromWeather(WeatherKind.CLOUDY);


                }

                if (id == 771) {


                    condition = "Squalls";


                }

                if (id == 781) {


                    condition = "Tornado";

                }

            }

            if (id == 800) {
                condition = "Clear Sky";

                if (isNight) {

                    condition = "starry";
                    mWeatherKind = WeatherKind.NIGHTLY;

                    setPaletteFromWeather(WeatherKind.NIGHTLY);
                } else {

                    condition = "sunny";

                    if (celsiusTemp >= 18) {
                        mWeatherKind = WeatherKind.SUNNY;

                        setPaletteFromWeather(WeatherKind.SUNNY);
                    } else {
                        mWeatherKind = WeatherKind.SUNNY;
                        setPaletteFromWeather(WeatherKind.SPRINGTIME);
                    }

                }

            }

            if (id > 800 && id < 900) {
                condition = "Cloudy";
                mWeatherKind = WeatherKind.CLOUDY;

                setPaletteFromWeather(WeatherKind.CLOUDY);


            }

            if (id == 900) {

                condition = "Tornado";


            }

            if (id == 901) {

                condition = "Stormy";


            }

            if (id == 902) {

                condition = "Hurricane";

            }

            if (id == 903) {

                condition = "Cold";

            }

            if (id == 904) {


                condition = "Hot";
                setPaletteFromWeather(WeatherKind.HOT);

            }

            if (id == 905) {


                condition = "Windy";

            }

            if (id == 906) {


                condition = "Icy";
                mWeatherKind = WeatherKind.SNOWY;

                setPaletteFromWeather(WeatherKind.SNOWY);

            }


        }
    }

    public String[] setPaletteFromWeather(WeatherKind weatherKind) {

        if (weatherKind == WeatherKind.CLOUDY) {

            weatherPalette = WeatherPaletteGenerator.getCloudyPalette();
            immediatelyApplyPalette();
            return WeatherPaletteGenerator.getCloudyPalette();

        }

        if (weatherKind == WeatherKind.SUNNY) {
            weatherPalette = WeatherPaletteGenerator.getSunnyPalette();
            immediatelyApplyPalette();
            return WeatherPaletteGenerator.getSunnyPalette();
        }

        if (weatherKind == WeatherKind.NIGHTLY) {
            weatherPalette = WeatherPaletteGenerator.getNightlyPalette();
            immediatelyApplyPalette();
            return WeatherPaletteGenerator.getNightlyPalette();

        }

        if (weatherKind == WeatherKind.RAINY) {
            weatherPalette = WeatherPaletteGenerator.getRainPalette();
            immediatelyApplyPalette();
            return WeatherPaletteGenerator.getRainPalette();

        }

        if (weatherKind == WeatherKind.DUSTY) {
            weatherPalette = WeatherPaletteGenerator.getDustyPalette();
            immediatelyApplyPalette();
            return WeatherPaletteGenerator.getDustyPalette();

        }

        if (weatherKind == WeatherKind.HOT) {
            weatherPalette = WeatherPaletteGenerator.getHotPalette();
            immediatelyApplyPalette();
            return WeatherPaletteGenerator.getHotPalette();

        }

        if (weatherKind == WeatherKind.SPRINGTIME) {
            weatherPalette = WeatherPaletteGenerator.getSpringPalette();
            immediatelyApplyPalette();
            return WeatherPaletteGenerator.getSpringPalette();
        }

        if (weatherKind == WeatherKind.FUNKY) {
            weatherPalette = WeatherPaletteGenerator.getFunkyPalette();
            immediatelyApplyPalette();
            return WeatherPaletteGenerator.getFunkyPalette();
        }

        if (weatherKind == WeatherKind.SNOWY) {
            weatherPalette = WeatherPaletteGenerator.getSnowyPalette();
            immediatelyApplyPalette();
            return WeatherPaletteGenerator.getSnowyPalette();

        } else {
            return null;
        }
    }

    public void immediatelyApplyPalette() {

        for (Dot dot : dots) {

            dot.setColor(generateRandomColorFromPalette(weatherPalette));

        }

        adapter.notifyDataSetChanged();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSION_REQ_CODE) {
            int grantResult = grantResults[0];

            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                retrieveWeather();
            } else {

                Toast.makeText(this, "Bummer! Veterondo can't work without yout location!", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void startRegularUpdates() {

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);


        Intent intent = new Intent(this, MainActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        overridePendingTransition(0, 0);

        pendingIntent = PendingIntent.getActivity(this, REFRESH_PENDING_INTENT_REQUEST_CODE, intent, 0);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + TIME_TO_REFRESH, pendingIntent);

    }


}
