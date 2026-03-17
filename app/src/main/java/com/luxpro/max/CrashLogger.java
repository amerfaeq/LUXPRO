package com.luxpro.max;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashLogger implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final Context context;

    public CrashLogger(Context context) {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.context = context;
    }

    public static void init(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashLogger(context));
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            File debugFile = new File(Environment.getExternalStorageDirectory(), "LUXPRO_DEBUG.txt");
            FileWriter writer = new FileWriter(debugFile, true);
            PrintWriter pw = new PrintWriter(writer);
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            pw.println("====== CRASH @ " + timestamp + " ======");
            pw.println("Thread: " + thread.getName());
            throwable.printStackTrace(pw);
            pw.println("=========================================\n");
            
            pw.flush();
            pw.close();
            Log.e("LUX_CRASH", "Crash saved to /sdcard/LUXPRO_DEBUG.txt");
        } catch (Exception e) {
            Log.e("LUX_CRASH", "Failed to write crash log: " + e.getMessage());
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            System.exit(2);
        }
    }
}
