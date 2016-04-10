package com.saladdressing.veterondo.utils;

public class Constants {

    public static final String OWM_API_KEY = "0d931dce668ff170f3e22d3ff29ff35c";
    public static final String INTRO_PLAYED = "introPlayed";
    public static final String IS_WINDY = "isWindy";
    public static final String IS_RAINY = "isRainy";
    public static final String DOT_CHOSEN_INTRO = "dotChosenIntro";
    public static final String FROM_INTRO = "fromIntro";

    public static boolean isNight(long sunrise, long sunset) {


        long epochTime = System.currentTimeMillis() / 1000;

        if (epochTime > sunrise && epochTime < sunset) {
            return false;
        } else {
            return true;
        }


    }


    public static double kelvinToCelsius(double kelvinTemp){
        return kelvinTemp - 273.15;
    }


    public static double kelvinToFarhenheit(double kelvinTemp) {
        return kelvinTemp * 9/5 - 459.67;

    }


}
