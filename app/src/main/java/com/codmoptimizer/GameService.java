package com.codmoptimizer;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.List;

public class GameService extends Service {

    private static final String CHANNEL_ID = "codm_optimizer";
    private static final int NOTIF_ID = 1;

    private final Handler handler =
        new Handler(Looper.getMainLooper());
    private boolean running = false;

    private static final String[] CODM_PKGS = {
        "com.activision.callofduty.shooter",
        "com.garena.game.codm",
        "com.vng.codmvn"
    };

    private static final String[] KILL_LIST = {
        "com.facebook.katana",
        "com.instagram.android",
        "com.twitter.android",
        "com.whatsapp",
        "com.spotify.music",
        "com.snapchat.android",
        "com.netflix.mediaclient",
        "com.google.android.youtube",
        "com.google.android.apps.photos",
        "com.discord",
        "com.reddit.frontpage",
        "org.telegram.messenger"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(
            Intent intent, int flags, int startId) {
        running = true;

        Notification notification =
            new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("CODM Optimizer Active")
                .setContentText("Monitoring game")
                .setSmallIcon(
                    android.R.drawable.ic_menu_manage)
                .setOngoing(true)
                .build();

        startForeground(NOTIF_ID, notification);
        startMonitorLoop();

        return START_STICKY;
    }

    private void startMonitorLoop() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!running) return;

                boolean codmRunning = false;
                for (String pkg : CODM_PKGS) {
                    if (isAppRunning(pkg)) {
                        codmRunning = true;
                        break;
                    }
                }

                if (codmRunning) {
                    cleanMemory();
                    maintainPriority();
                }

                handler.postDelayed(this, 15000);
            }
        }, 10000);
    }

    private void cleanMemory() {
        ActivityManager am =
            (ActivityManager) getSystemService(
                ACTIVITY_SERVICE);

        for (String pkg : KILL_LIST) {
            am.killBackgroundProcesses(pkg);
        }

        runRoot("echo 1 > /proc/sys/vm/drop_caches");
    }

    private void maintainPriority() {
        for (String pkg : CODM_PKGS) {
            String pid = getPid(pkg);
            if (pid != null && !pid.isEmpty()) {
                runRoot("renice -20 " + pid);
                runRoot("echo -1000 > /proc/"
                    + pid + "/oom_score_adj");
                break;
            }
        }
    }

    private boolean isAppRunning(String packageName) {
        try {
            ActivityManager am =
                (ActivityManager) getSystemService(
                    ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> procs =
                am.getRunningAppProcesses();
            if (procs != null) {
                for (ActivityManager.RunningAppProcessInfo p
                        : procs) {
                    for (String s : p.pkgList) {
                        if (s.equals(packageName)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private String getPid(String packageName) {
        try {
            Process p = Runtime.getRuntime().exec(
                new String[]{"su", "-c",
                    "pidof " + packageName}
            );
            byte[] buf = new byte[64];
            int n = p.getInputStream().read(buf);
            p.waitFor();
            if (n > 0) {
                return new String(buf, 0, n).trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void runRoot(String cmd) {
        try {
            Runtime.getRuntime()
                .exec(new String[]{"su", "-c", cmd})
                .waitFor();
        } catch (Exception ignored) {
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                new NotificationChannel(
                    CHANNEL_ID,
                    "CODM Optimizer",
                    NotificationManager.IMPORTANCE_LOW
                );
            NotificationManager nm =
                getSystemService(
                    NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
