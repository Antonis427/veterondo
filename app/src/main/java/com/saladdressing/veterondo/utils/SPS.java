package com.saladdressing.veterondo.utils;


import android.content.Context;
import android.content.SharedPreferences;

public class SPS {

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    public static final String FILENAME = "veterondoPrefs";
    public static final int MODE = Context.MODE_PRIVATE;
    Context context;

    public SPS(Context context) {

        this.context = context;
        sharedPrefs = context.getSharedPreferences(FILENAME, MODE);
        editor = sharedPrefs.edit();


    }


    public SharedPreferences getPrefs() {
        return sharedPrefs;
    }

    public SharedPreferences.Editor getEditor() {
        return editor;
    }





}
