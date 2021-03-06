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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
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

    // Time that it takes for MainActivity to launch IconShowcaseActivity
    public static final long TIME_TO_REFRESH = 5 * 60 * 1000;

    private static final Handler handler = new Handler();
    private static final int MY_PERMISSION_REQ_CODE = 123;
    static GridView grid;
    static ArrayList<Dot> dots = new ArrayList<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    int classTempFahr = 0;
    int classTempCelsius = 0;
    int iconToSendToShowcaseActivity = R.drawable.cloud_refresh;
    String descriptionToSendToShowcaseActivity = "nothing to see here";
    boolean fromIntro;
    ImageView wraps;
    ImageView circle;
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
    double windSpeed = 0.0;
    boolean isWindy;
    boolean isRainy;
    SPS sps;
    private Future<?> timingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sps = new SPS(this);
        if (!sps.getPrefs().getBoolean(Constants.INTRO_PLAYED, false)) {
            Intent intent = new Intent(this, IntroActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            startActivity(intent);
            overridePendingTransition(0, 0);

        }

        setContentView(R.layout.activity_main);

        fromIntro = getIntent().getBooleanExtra(Constants.FROM_INTRO, false);

        if (dots != null && dots.size() > 0) {
            dots.clear();
        }

        makeFullscreen();
        keepScreenOn();
        initializeDots();

        circle = (ImageView) findViewById(R.id.circle);
        wraps = (ImageView) findViewById(R.id.wraps);
        appTitle = (TextView) findViewById(R.id.app_name);
        grid = (GridView) findViewById(R.id.dot_grid);
        weatherDescription = (TextView) findViewById(R.id.weather_desc);
        location = (TextView) findViewById(R.id.location);
        temp = (TextView) findViewById(R.id.temp);

        float scaleFactor = getResources().getDisplayMetrics().heightPixels / 40 * 2;
        Handler handlerz = new Handler();

        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sps.getPrefs().getString(Constants.TEMP_UNIT, "celsius").equalsIgnoreCase("celsius")) {
                    sps.getEditor().putString(Constants.TEMP_UNIT, "fahrenheit").apply();
                    animateCircleOnTap();

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            temp.setText(classTempFahr + "°F");

                        }
                    }, 200);
                } else {
                    sps.getEditor().putString(Constants.TEMP_UNIT, "celsius").apply();
                    animateCircleOnTap();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            temp.setText(classTempCelsius + "°C");

                        }
                    }, 200);
                }

            }
        });


        /*
        The following three if-statements change the color of the expanded circle overlay according
        to the color chosen in IntroActivity when tapping one of the three circles.
         */
        if (sps.getPrefs().getString(Constants.DOT_CHOSEN_INTRO, "").equalsIgnoreCase("Y")) {
            wraps.setImageResource(R.drawable.circle_yellow);

        }

        if (sps.getPrefs().getString(Constants.DOT_CHOSEN_INTRO, "").equalsIgnoreCase("B")) {
            wraps.setImageResource(R.drawable.circle_blue);
        }

        if (sps.getPrefs().getString(Constants.DOT_CHOSEN_INTRO, "").equalsIgnoreCase("P")) {
            wraps.setImageResource(R.drawable.layout);
        }

        if (fromIntro)
            wraps.setVisibility(View.VISIBLE);
        wraps.animate().setDuration(0).scaleX(scaleFactor).scaleY(scaleFactor).start();

        samplePlayer = new SamplePlayer(this);

        Typeface titleTypeface = Typeface.createFromAsset(getAssets(), "fonts/Ailerons-Typeface.otf");
        Typeface scriptTypeface = Typeface.createFromAsset(getAssets(), "fonts/RobotoSlab-Light.ttf");

        // make logo multi-colored
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
        // grid.setAdapter(adapter);

        // change colored dots to the funky palette when a dot is clicked and play
        // random chord
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


        // changes color of dots in adapter within a certain palette
        timingTask = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.i("TIMING TASK ", "running...");
                        for (Dot dot : dots) {
                            dot.setColor(generateRandomColorFromPalette(weatherPalette));
                        }


                        adapter.notifyDataSetChanged();
                        weatherDescription.setText(condition);


                    }
                });

            }
        }, 0, 30000, TimeUnit.MILLISECONDS);


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
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }


    @Override
    protected void onResume() {
        grid.setAdapter(adapter);
        super.onResume();
    }

    @Override
    protected void onPause() {

        handler.removeCallbacks(myRunnable);

        // remove handler callbacks in GridDotAdapter
        adapter.removeHandlerCallbacks();
        //   timingTask.cancel(true);

        overridePendingTransition(0, 0);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(myRunnable);

        // remove handler callbacks in GridDotAdapter
        adapter.removeHandlerCallbacks();
        super.onDestroy();

    }


    @Override
    public void onBackPressed() {
        Process.killProcess(android.os.Process.myPid());

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

            // when in doubt, travel to the center of the globe.
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


                    wraps.animate().setDuration(500).scaleX(0.0f).scaleY(0.0f).start();
                    location.setText(openCurrentWeather.getName());


                    long sunriseEpoch = openCurrentWeather.getSys().getSunrise();
                    long sunsetEpoch = openCurrentWeather.getSys().getSunset();

                    int id = openCurrentWeather.getWeather().get(0).getId();
                    boolean isNight = Constants.isNight(sunriseEpoch, sunsetEpoch);


                    evaluateWeatherObject(isNight, id, Constants.kelvinToCelsius(openCurrentWeather.getMain().getTemp()), windSpeed);


                    double temperature = Math.floor(Constants.kelvinToCelsius(openCurrentWeather.getMain().getTemp()));
                    double temperatureFahr = Math.floor(Constants.kelvinToFarhenheit(openCurrentWeather.getMain().getTemp()));

                    int intTemp = (int) temperature;
                    int intTempFahr = (int) temperatureFahr;

                    classTempCelsius = intTemp;
                    classTempFahr = intTempFahr;


                    weatherDescription.setText(condition);

                    if (sps.getPrefs().getString(Constants.TEMP_UNIT, "celsius").equalsIgnoreCase("celsius")) {
                        temp.setText(intTemp + "°C");

                    } else {
                        temp.setText(intTempFahr + "°F");
                    }
                    scheduleShowcaseActivityLaunch();

                }


                @Override
                public void failure(RetrofitError error) {

                }
            });

        } else {

            setPaletteFromWeather(WeatherKind.FUNKY);
        }

    }

    // IconShowcaseActivity will be launching every TIME_TO_REFRESH minutes
    private void scheduleShowcaseActivityLaunch() {
        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(returnShowcaseIntent());
            }
        }, TIME_TO_REFRESH);
    }


    public final String generateRandomColorFromPalette(String[] palette) {

        int paletteSize = palette.length;

        Random rand = new Random();

        return palette[rand.nextInt(paletteSize)];

    }

    private void evaluateWeatherObject(boolean isNight, int id, double celsiusTemp, double windSpeed) {

        sps.getEditor().putBoolean(Constants.IS_WINDY, false).apply();
        sps.getEditor().putBoolean(Constants.IS_RAINY, false).apply();


        isWindy = false;
        isRainy = false;

        if (windSpeed > 10.0) {

            isWindy = true;
            sps.getEditor().putBoolean(Constants.IS_WINDY, true).apply();

        } else {
            isWindy = false;
            sps.getEditor().putBoolean(Constants.IS_WINDY, false).apply();
        }


        if (isNight) {

            setPaletteFromWeather(WeatherKind.NIGHTLY);
            mWeatherKind = WeatherKind.NIGHTLY;
            condition = "starry";
            iconToSendToShowcaseActivity = Constants.MOON_ICON;
            descriptionToSendToShowcaseActivity = "starry";

        } else {


            if (id >= 200 && id < 300) {

                condition = "Stormy";
                setPaletteFromWeather(WeatherKind.RAINY);
                mWeatherKind = WeatherKind.RAINY;
                sps.getEditor().putBoolean(Constants.IS_RAINY, true).apply();
                iconToSendToShowcaseActivity = Constants.CLOUD_LIGHTNING_ICON;
                descriptionToSendToShowcaseActivity = "stormy";


            }

            if (id >= 300 && id < 400) {
                condition = "Drizzle";
                mWeatherKind = WeatherKind.RAINY;

                setPaletteFromWeather(WeatherKind.RAINY);
                sps.getEditor().putBoolean(Constants.IS_RAINY, true).apply();
                iconToSendToShowcaseActivity = Constants.DRIZZLE_ICON;
                descriptionToSendToShowcaseActivity = "drizzle";


            }

            if (id >= 500 && id < 600) {

                condition = "Rainy";
                mWeatherKind = WeatherKind.RAINY;

                setPaletteFromWeather(WeatherKind.RAINY);
                iconToSendToShowcaseActivity = Constants.RAIN_ICON;
                descriptionToSendToShowcaseActivity = "rainy";
                sps.getEditor().putBoolean(Constants.IS_RAINY, true).apply();


            }

            if (id >= 600 && id < 700) {


                condition = "Snowy";
                mWeatherKind = WeatherKind.SNOWY;
                iconToSendToShowcaseActivity = Constants.CLOUD_SNOW_ICON;
                descriptionToSendToShowcaseActivity = "snowy";

                setPaletteFromWeather(WeatherKind.SNOWY);

            }

            if (id >= 700 && id < 800) {

                if (id == 701) {


                    condition = "Misty";
                    mWeatherKind = WeatherKind.CLOUDY;
                    descriptionToSendToShowcaseActivity = "misty";
                    iconToSendToShowcaseActivity = Constants.FOG_ICON;


                }

                if (id == 711) {


                    condition = "Smoky";
                    setPaletteFromWeather(WeatherKind.DUSTY);
                    mWeatherKind = WeatherKind.DUSTY;
                    descriptionToSendToShowcaseActivity = "smoke";
                    iconToSendToShowcaseActivity = Constants.CLOUD_ICON;


                }

                if (id == 721) {


                    condition = "Hazy";
                    descriptionToSendToShowcaseActivity = "hazy";
                    iconToSendToShowcaseActivity = Constants.FOG_ICON;


                }

                if (id == 731) {


                    condition = "Dusty";
                    mWeatherKind = WeatherKind.DUSTY;
                    descriptionToSendToShowcaseActivity = "dusty";
                    iconToSendToShowcaseActivity = Constants.CLOUD_ICON;

                    setPaletteFromWeather(WeatherKind.DUSTY);


                }

                if (id == 741) {


                    condition = "Foggy";
                    mWeatherKind = WeatherKind.CLOUDY;
                    iconToSendToShowcaseActivity = Constants.FOG_ICON;
                    descriptionToSendToShowcaseActivity = "foggy";

                    setPaletteFromWeather(WeatherKind.CLOUDY);


                }

                if (id == 751) {


                    condition = "Sandy";
                    mWeatherKind = WeatherKind.DUSTY;

                    iconToSendToShowcaseActivity = Constants.FOG_ICON;
                    descriptionToSendToShowcaseActivity = "sandy";
                    setPaletteFromWeather(WeatherKind.DUSTY);


                }

                if (id == 761) {


                    condition = "Dusty";
                    mWeatherKind = WeatherKind.DUSTY;
                    iconToSendToShowcaseActivity = Constants.FOG_ICON;
                    descriptionToSendToShowcaseActivity = "dusty";

                    setPaletteFromWeather(WeatherKind.DUSTY);


                }

                if (id == 762) {


                    condition = "Ash";
                    mWeatherKind = WeatherKind.CLOUDY;
                    descriptionToSendToShowcaseActivity = "ash";
                    iconToSendToShowcaseActivity = Constants.CLOUD_ICON;

                    setPaletteFromWeather(WeatherKind.CLOUDY);


                }

                if (id == 771) {


                    condition = "Squalls";
                    descriptionToSendToShowcaseActivity = "squalls";
                    iconToSendToShowcaseActivity = Constants.WIND_ICON;
                    sps.getEditor().putBoolean(Constants.IS_WINDY, true).apply();

                    setPaletteFromWeather(WeatherKind.CLOUDY);


                }

                if (id == 781) {


                    condition = "Tornado";
                    descriptionToSendToShowcaseActivity = "tornado";
                    iconToSendToShowcaseActivity = Constants.TORNADO_ICON;

                }

            }

            if (id == 800) {
                condition = "Clear Sky";

                if (isNight) {

                    condition = "starry";
                    mWeatherKind = WeatherKind.NIGHTLY;
                    descriptionToSendToShowcaseActivity = "starry";
                    iconToSendToShowcaseActivity = Constants.MOON_ICON;
                    setPaletteFromWeather(WeatherKind.NIGHTLY);
                } else {

                    condition = "sunny";
                    descriptionToSendToShowcaseActivity = "sunny";
                    iconToSendToShowcaseActivity = Constants.SUN_ICON;

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
                descriptionToSendToShowcaseActivity = "cloudy";
                iconToSendToShowcaseActivity = Constants.CLOUD_ICON;
                setPaletteFromWeather(WeatherKind.CLOUDY);


            }

            if (id == 900) {

                condition = "Tornado";
                iconToSendToShowcaseActivity = Constants.TORNADO_ICON;
                descriptionToSendToShowcaseActivity = "tornado";
                setPaletteFromWeather(WeatherKind.CLOUDY);


            }

            if (id == 901) {

                condition = "Stormy";
                iconToSendToShowcaseActivity = Constants.CLOUD_LIGHTNING_ICON;
                descriptionToSendToShowcaseActivity = "stormy";
                setPaletteFromWeather(WeatherKind.CLOUDY);


            }

            if (id == 902) {

                condition = "Hurricane";
                descriptionToSendToShowcaseActivity = "hurricane";
                iconToSendToShowcaseActivity = Constants.TORNADO_ICON;
                setPaletteFromWeather(WeatherKind.CLOUDY);


            }

            if (id == 903) {

                condition = "Cold";
                descriptionToSendToShowcaseActivity = "cold";
                iconToSendToShowcaseActivity = Constants.COLD_ICON;

            }

            if (id == 904) {


                condition = "Hot";
                descriptionToSendToShowcaseActivity = "hot";
                iconToSendToShowcaseActivity = Constants.HOT_ICON;
                setPaletteFromWeather(WeatherKind.HOT);

            }

            if (id == 905) {


                condition = "Windy";
                isWindy = true;
                descriptionToSendToShowcaseActivity = "windy";
                iconToSendToShowcaseActivity = Constants.WIND_ICON;
                sps.getEditor().putBoolean(Constants.IS_WINDY, true).apply();

            }

            if (id == 906) {


                condition = "Icy";
                mWeatherKind = WeatherKind.SNOWY;
                descriptionToSendToShowcaseActivity = "icy";
                iconToSendToShowcaseActivity = Constants.COLD_ICON;

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


    // create the Intent object for launching IconShowcaseActivity with updated info
    public Intent returnShowcaseIntent() {

        Intent intent = new Intent(this, IconShowcaseActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(Constants.ICON_TO_SHOW, iconToSendToShowcaseActivity);
        intent.putExtra(Constants.DESC_TO_SHOW, descriptionToSendToShowcaseActivity);

        Log.e("DESC = ", descriptionToSendToShowcaseActivity);
        Log.e("ICON = ", iconToSendToShowcaseActivity + "");
        sps.getEditor().putString(Constants.DESC_TO_SHOW, descriptionToSendToShowcaseActivity).apply();
        sps.getEditor().putInt(Constants.ICON_TO_SHOW, iconToSendToShowcaseActivity).apply();

        overridePendingTransition(0, 0);

        return intent;
    }

    public void animateCircleOnTap() {

        circle.animate().setDuration(200).scaleX(0.0f).scaleY(0.0f).setInterpolator(new AccelerateDecelerateInterpolator()).withEndAction(new Runnable() {

            @Override
            public void run() {
                circle.animate().setStartDelay(10).scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
            }
        });

    }

}
