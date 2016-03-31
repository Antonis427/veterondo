package com.saladdressing.veterondo.utils;

public class Constants {

    public static final String OWM_API_KEY = "0d931dce668ff170f3e22d3ff29ff35c";


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
