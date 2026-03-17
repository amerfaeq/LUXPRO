package com.luxpro.max;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;

import androidx.core.app.NotificationCompat;

public class Launcher extends Service {

    Menu menu;

    //When this Class is called the code in this function will be executed
    @Override
    public void onCreate() {
        super.onCreate();

        // Create Notification Channel for Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("LUX_MENU", "LUX Menu Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, "LUX_MENU")
                .setContentTitle("LUX PRO MAX")
                .setContentText("Mod Menu is active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        startForeground(1, notification);

        menu = new Menu(this);
        menu.SetWindowManagerWindowService();
        menu.ShowMenu();

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
               Thread();
                handler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void Thread() {
        // Keep menu visible while service is running
        if (menu != null) {
            menu.setVisibility(View.VISIBLE);
        }
    }

    //Destroy our View
    public void onDestroy() {
        super.onDestroy();
        menu.onDestroy();
    }

    //Same as above so it wont crash in the background and therefore use alot of Battery life
    public void onTaskRemoved(Intent intent) {
        super.onTaskRemoved(intent);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopSelf();
    }

    //Override our Start Command so the Service doesnt try to recreate itself when the App is closed
    public int onStartCommand(Intent intent, int i, int i2) {
        return Service.START_NOT_STICKY;
    }
}
