package com.luxpro.vip;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateDownloadService extends Service {

    public static final String EXTRA_APK_URL = "apk_url";
    private static final String CHANNEL_ID = "lux_update_channel";
    private static final int NOTIF_ID = 9001;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getStringExtra(EXTRA_APK_URL) == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String apkUrl = intent.getStringExtra(EXTRA_APK_URL);

        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification(0));

        new Thread(() -> downloadAndInstall(apkUrl)).start();

        return START_NOT_STICKY;
    }

    private void downloadAndInstall(String apkUrl) {
        try {
            File cacheDir = getExternalCacheDir() != null ? getExternalCacheDir() : getCacheDir();
            File apkFile = new File(cacheDir, "lux_update.apk");

            URL url = new URL(apkUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            int fileLength = conn.getContentLength();

            // Try-with-resources: streams guaranteed closed even if exception occurs
            try (InputStream input = conn.getInputStream();
                 FileOutputStream output = new FileOutputStream(apkFile)) {
                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        updateNotificationProgress(progress);
                    }
                }
                output.flush();
            }

            installApk(apkFile);

        } catch (Exception e) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(getString(R.string.error))
                    .setContentText(getString(R.string.update_fail_msg))
                    .setAutoCancel(true);
            if (nm != null) nm.notify(NOTIF_ID + 1, builder.build());
        }
        stopSelf();
    }

    private void installApk(File apkFile) {
        Uri apkUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", apkFile);

        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Check if can install unknown sources
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean canInstall = getPackageManager().canRequestPackageInstalls();
            if (!canInstall) {
                // Open settings to allow installation
                Intent settingsIntent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settingsIntent);
                return;
            }
        }

        startActivity(installIntent);
    }

    private Notification buildNotification(int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(getString(R.string.update_running_title))
                .setContentText(progress > 0 ? getString(R.string.update_progress_msg, progress) : getString(R.string.update_start_msg))
                .setProgress(100, progress, progress == 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true);
        return builder.build();
    }

    private void updateNotificationProgress(int progress) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(progress));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.update_channel_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.update_channel_desc));
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
