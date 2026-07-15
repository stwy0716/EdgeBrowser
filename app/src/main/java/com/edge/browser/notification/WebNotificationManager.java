package com.edge.browser.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class WebNotificationManager {

    private static final String CHANNEL_ID = "web_notifications";
    private static final String CHANNEL_NAME = "Web Notifications";
    private static final String CHANNEL_DESC = "Notifications from websites";

    private static WebNotificationManager instance;
    private NotificationManager notificationManager;
    private Context appContext;

    private WebNotificationManager() {}

    public static synchronized WebNotificationManager getInstance() {
        if (instance == null) {
            instance = new WebNotificationManager();
        }
        return instance;
    }

    public void init(Context context) {
        appContext = context.getApplicationContext();
        createNotificationChannel(appContext);
    }

    public void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void showNotification(String title, String body, String origin, int id) {
        if (notificationManager == null || appContext == null) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setSubText(origin);
        builder.setAutoCancel(true);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        notificationManager.notify(id, builder.build());
    }

    public void cancelNotification(int id) {
        if (notificationManager != null) {
            notificationManager.cancel(id);
        }
    }
}