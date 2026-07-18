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

import androidx.core.app.NotificationCompat;

import java.util.List;

public class GameService extends Service {

    private static final String CHANNEL_ID = "codm_channel";
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;

    private final String[] CODM_PKGS = {
        "com.activision.callofduty.shooter",
        "com.garena.game.codm",
        "com.vng.codmvn"
    };

    private final String[] KILL_LIST = {
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
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true;

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚡ CODM Optimizer")
            .setContentText("Monitoring game performance...")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();

        startForeground(1, n);
        startMonitorLoop();

        return START_STICKY;
    }

    void startMonitorLoop() {
        handler.postDelayed(new Runnable() {
            public void run() {
                if (!running) return;

                boolean codmRunning = false;
                for (String pkg : CODM_PKGS) {
                    if (isRunning(pkg)) {
                        codmRunning = true;
                        break;
                    }
                }

                if (codmRunning) {
                    cleanForGame();
                    keepCODMPriority();
                }

                handler.postDelayed(this, 15000);
            }
        }, 10000);
    }

    void cleanForGame() {
        ActivityManager am =
            (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        for (String pkg : KILL_LIST) {
            am.killBackgroundProcesses(pkg);
        }

        exec("echo 1 > /proc/sys/vm/drop_caches");
        exec("sync");
    }

    void keepCODMPriority() {
        for (String pkg : CODM_PKGS) {
            String pid = getPID(pkg);
            if (pid != null && !pid.isEmpty()) {
                exec("renice -20 " + pid);
                exec("echo -1000 > /proc/" + pid + "/oom_score_adj");
                break;
            }
        }
    }

    boolean isRunning(String pkg) {
        try {
            ActivityManager am =
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> procs =
                am.getRunningAppProcesses();
            if (procs != null) {
                for (ActivityManager.RunningAppProcessInfo p : procs) {
                    for (String s : p.pkgList) {
                        if (s.equals(pkg)) return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    String getPID(String pkg) {
        try {
            Process p = Runtime.getRuntime()
                .exec(new String[]{"su", "-c", "pidof " + pkg});
            byte[] buf = new byte[64];
            int n = p.getInputStream().read(buf);
            p.waitFor();
            return n > 0 ? new String(buf, 0, n).trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    void exec(String cmd) {
        try {
            Runtime.getRuntime()
                .exec(new String[]{"su", "-c", cmd})
                .waitFor();
        } catch (Exception ignored) {}
    }

    void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "CODM Optimizer",
                NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm =
                getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
