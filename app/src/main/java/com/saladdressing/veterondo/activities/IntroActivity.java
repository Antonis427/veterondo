package com.saladdressing.veterondo.activities;

import android.animation.Animator;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.saladdressing.veterondo.R;
import com.saladdressing.veterondo.enums.WeatherKind;
import com.saladdressing.veterondo.generators.MusicMachine;
import com.saladdressing.veterondo.pojos.Weather;

public class IntroActivity extends AppCompatActivity {

    TextView greeting;
    Typeface robotoSlab;
    ImageView circle;
    ImageView centerDot;
    ImageView leftDot;
    ImageView rightDot;
    TextView instructions;
    View indicator;
    boolean isActivityPendingLaunch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        MusicMachine musicMachine = new MusicMachine(this, WeatherKind.SUNNY);
        musicMachine.playPattern(200);

        final Runnable launchMainActivityRunnable = new Runnable() {
            @Override
            public void run() {

                isActivityPendingLaunch = true;
                Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0,0);

            }
        };


        greeting = (TextView) findViewById(R.id.greeting);
        circle = (ImageView) findViewById(R.id.circle_center);
        centerDot = (ImageView) findViewById(R.id.center_dot);
        leftDot = (ImageView) findViewById(R.id.left_dot);
        rightDot = (ImageView) findViewById(R.id.right_dot);
        instructions = (TextView) findViewById(R.id.instructions);
        indicator = findViewById(R.id.indicator);

        final Handler handler = new Handler();

        centerDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MusicMachine musicMachine = new MusicMachine(IntroActivity.this, WeatherKind.SUNNY);
                musicMachine.playPattern(200);

                handler.postDelayed(launchMainActivityRunnable, 2000);


            }
        });

        leftDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MusicMachine musicMachine = new MusicMachine(IntroActivity.this, WeatherKind.CLOUDY);
                musicMachine.playPattern(200);
                handler.postDelayed(launchMainActivityRunnable, 2000);

            }
        });

        rightDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MusicMachine musicMachine = new MusicMachine(IntroActivity.this, WeatherKind.RAINY);
                musicMachine.playPattern(400);
                handler.postDelayed(launchMainActivityRunnable, 2000);


            }
        });


        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        circle.animate().setListener(null).translationY(-screenHeight).setDuration(0).start();
        centerDot.animate().setListener(null).translationY(-screenHeight).setDuration(0).start();
        leftDot.animate().setListener(null).translationY(-screenHeight).setDuration(0).start();
        rightDot.animate().setListener(null).translationY(-screenHeight).setDuration(0).start();

        robotoSlab = Typeface.createFromAsset(getAssets(), "fonts/RobotoSlab-Light.ttf");
        greeting.setTypeface(robotoSlab);
        instructions.setTypeface(robotoSlab);

        makeFullscreen();
        keepScreenOn();

        createDropAndExpandAnimation(circle, 1000, 3.6f, true);
        createDropAndExpandAnimation(centerDot, 5000, 1f, false);
        createDropAndExpandAnimation(leftDot, 5500, 1f, false);
        createDropAndExpandAnimation(rightDot, 6000, 1f, false);

        instructions.animate().setDuration(700).setStartDelay(7000).alpha(1.0f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        indicator.animate().setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                indicator.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).setDuration(200).setStartDelay(6900).scaleY(1.0f).start();


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


    private void createDropAndExpandAnimation(final View view, final long startDelay, final float scaleFactor, final boolean contract) {

        view.setVisibility(View.VISIBLE);
        view.animate().translationY(0).setDuration(1000).setStartDelay(startDelay).setInterpolator(new AccelerateDecelerateInterpolator()).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                view.animate().setStartDelay(1000).setListener(null).setInterpolator(new OvershootInterpolator()).setDuration(600).start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).start();


        view.animate().setStartDelay(startDelay + 2000).scaleY(scaleFactor).scaleX(scaleFactor).setInterpolator(new OvershootInterpolator()).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {

                if (contract) {
                    view.animate().setStartDelay(2000).setInterpolator(new AccelerateDecelerateInterpolator()).setListener(null).scaleX(0.0f).scaleY(0.0f).setDuration(500).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            greeting.setVisibility(View.GONE);
                        }
                    }).start();
                }

            }
        }).start();
    }
}
