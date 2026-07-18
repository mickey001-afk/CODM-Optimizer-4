package com.codmoptimizer;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    TextView tvPing, tvRam, tvLog;
    Spinner spinnerDNS;
    Button btnBoost, btnLaunch, btnDNS, btnCPU, btnGPU, btnClear, btnKill;

    Handler handler = new Handler();
    StringBuilder logText = new StringBuilder();

    String[] CODM_PKGS = {
        "com.activision.callofduty.shooter",
        "com.garena.game.codm",
        "com.vng.codmvn"
    };

    String[][] DNS = {
        {"Cloudflare Gaming", "1.1.1.2", "1.0.0.2", "security.cloudflare-dns.com"},
        {"Cloudflare", "1.1.1.1", "1.0.0.1", "one.one.one.one"},
        {"Google DNS", "8.8.8.8", "8.8.4.4", "dns.google"},
        {"Quad9", "9.9.9.9", "149.112.112.112", "dns.quad9.net"},
        {"AdGuard", "94.140.14.14", "94.140.15.15", "dns.adguard.com"},
        {"OpenDNS", "208.67.222.222", "208.67.220.220", "doh.opendns.com"},
        {"NextDNS", "45.90.28.0", "45.90.30.0", "dns.nextdns.io"},
        {"AliDNS Asia", "223.5.5.5", "223.6.6.6", "dns.alidns.com"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupDNS();
        setupButtons();
        startMonitors();

        addLog("App started on " + Build.MANUFACTURER + " " + Build.MODEL);
        addLog("Android " + Build.VERSION.RELEASE);
        addLog("Tap BOOST to optimize for CODM");
    }

    void initViews() {
        tvPing    = findViewById(R.id.tvPing);
        tvRam     = findViewById(R.id.tvRam);
        tvLog     = findViewById(R.id.tvLog);
        spinnerDNS = findViewById(R.id.spinnerDNS);
        btnBoost  = findViewById(R.id.btnBoost);
        btnLaunch = findViewById(R.id.btnLaunch);
        btnDNS    = findViewById(R.id.btnDNS);
        btnCPU    = findViewById(R.id.btnCPU);
        btnGPU    = findViewById(R.id.btnGPU);
        btnClear  = findViewById(R.id.btnClear);
        btnKill   = findViewById(R.id.btnKill);
    }

    void setupDNS() {
        List<String> names = new ArrayList<>();
        for (String[] d : DNS) {
            names.add(d[0] + " (" + d[1] + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, names);
        spinnerDNS.setAdapter(adapter);
    }

    void setupButtons() {

        btnBoost.setOnClickListener(v -> {
            btnBoost.setEnabled(false);
            btnBoost.setText("⏳ BOOSTING...");
            new Thread(() -> {
                runBoost();
                runOnUiThread(() -> {
                    btnBoost.setEnabled(true);
                    btnBoost.setText("⚡ ONE-TAP ULTIMATE BOOST");
                    Toast.makeText(this,
                        "✅ Boost Complete!", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        btnLaunch.setOnClickListener(v -> {
            btnLaunch.setEnabled(false);
            new Thread(() -> {
                runBoost();
                runOnUiThread(() -> {
                    launchCODM();
                    btnLaunch.setEnabled(true);
                });
            }).start();
        });

        btnDNS.setOnClickListener(v ->
            new Thread(this::applyDNS).start());

        btnCPU.setOnClickListener(v ->
            new Thread(this::optimizeCPU).start());

        btnGPU.setOnClickListener(v ->
            new Thread(this::optimizeGPU).start());

        btnClear.setOnClickListener(v ->
            new Thread(this::clearRAM).start());

        btnKill.setOnClickListener(v ->
            new Thread(this::killApps).start());
    }

    // ============================================================
    // MONITORS
    // ============================================================

    void startMonitors() {
        // Ping monitor every 5s
        handler.postDelayed(new Runnable() {
            public void run() {
                new Thread(() -> {
                    long p = pingHost("8.8.8.8");
                    runOnUiThread(() -> {
                        if (p > 0) {
                            tvPing.setText(String.valueOf(p));
                            tvPing.setTextColor(
                                p < 50  ? 0xFF00FF88 :
                                p < 100 ? 0xFFFFD700 : 0xFFFF4444);
                        } else {
                            tvPing.setText("?");
                        }
                    });
                }).start();
                handler.postDelayed(this, 5000);
            }
        }, 1000);

        // RAM monitor every 3s
        handler.postDelayed(new Runnable() {
            public void run() {
                long free = getFreeRAM();
                runOnUiThread(() -> {
                    tvRam.setText(String.valueOf(free));
                    tvRam.setTextColor(
                        free > 1000 ? 0xFF00FF88 :
                        free > 500  ? 0xFFFFD700 : 0xFFFF4444);
                });
                handler.postDelayed(this, 3000);
            }
        }, 1000);
    }

    // ============================================================
    // BOOST
    // ============================================================

    void runBoost() {
        addLog("══════════════════════");
        addLog("⚡ BOOST STARTED");
        clearRAM();
        killApps();
        applyDNS();
        optimizeTCP();
        optimizeCPU();
        optimizeGPU();
        optimizeWifi();
        optimizeVM();
        startGameService();
        addLog("══════════════════════");
        addLog("✅ BOOST COMPLETE!");
    }

    // ============================================================
    // DNS
    // ============================================================

    void applyDNS() {
        int i = spinnerDNS.getSelectedItemPosition();
        String p1  = DNS[i][1];
        String p2  = DNS[i][2];
        String doh = DNS[i][3];

        addLog("🔒 DNS → " + DNS[i][0]);

        root("settings put global private_dns_mode hostname");
        root("settings put global private_dns_specifier " + doh);
        root("setprop net.dns1 " + p1);
        root("setprop net.dns2 " + p2);
        root("setprop net.wlan0.dns1 " + p1);
        root("setprop net.wlan0.dns2 " + p2);
        root("setprop dhcp.wlan0.dns1 " + p1);
        root("setprop dhcp.wlan0.dns2 " + p2);
        root("ndc resolver setnetdns wlan0 \"\" " + p1 + " " + p2);

        addLog("✅ DNS: " + p1 + " / " + p2);
    }

    // ============================================================
    // TCP
    // ============================================================

    void optimizeTCP() {
        addLog("📡 Optimizing TCP...");
        String[] cmds = {
            "echo 1 > /proc/sys/net/ipv4/tcp_low_latency",
            "echo 1 > /proc/sys/net/ipv4/tcp_window_scaling",
            "echo 1 > /proc/sys/net/ipv4/tcp_timestamps",
            "echo 1 > /proc/sys/net/ipv4/tcp_sack",
            "echo 1 > /proc/sys/net/ipv4/tcp_fastopen",
            "echo 2 > /proc/sys/net/ipv4/tcp_syn_retries",
            "echo 10 > /proc/sys/net/ipv4/tcp_fin_timeout",
            "echo 1 > /proc/sys/net/ipv4/tcp_tw_reuse",
            "echo cubic > /proc/sys/net/ipv4/tcp_congestion_control",
            "echo 0 > /proc/sys/net/ipv4/tcp_ecn",
            "echo 262144 > /proc/sys/net/core/rmem_max",
            "echo 262144 > /proc/sys/net/core/wmem_max",
            "echo 16384 > /proc/sys/net/core/netdev_max_backlog"
        };
        for (String c : cmds) root(c);
        addLog("✅ TCP optimized");
    }

    // ============================================================
    // CPU
    // ============================================================

    void optimizeCPU() {
        addLog("🔥 CPU → Performance mode...");
        int cores = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < cores; i++) {
            root("echo 1 > /sys/devices/system/cpu/cpu" + i + "/online");
            root("echo performance > /sys/devices/system/cpu/cpu"
                + i + "/cpufreq/scaling_governor");
        }
        root("settings put global low_power 0");
        root("settings put global window_animation_scale 0");
        root("settings put global transition_animation_scale 0");
        root("settings put global animator_duration_scale 0");
        addLog("✅ CPU: " + cores + " cores at max");
    }

    // ============================================================
    // GPU
    // ============================================================

    void optimizeGPU() {
        addLog("🖥️ GPU → Performance mode...");
        root("echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor");
        root("echo 0 > /sys/class/kgsl/kgsl-3d0/bus_split");
        root("echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on");
        root("echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on");
        root("echo 10000 > /sys/class/kgsl/kgsl-3d0/idle_timer");
        root("setprop debug.egl.hw 1");
        root("setprop debug.composition.type gpu");
        addLog("✅ GPU performance mode");
    }

    // ============================================================
    // WIFI
    // ============================================================

    void optimizeWifi() {
        addLog("📶 Optimizing WiFi...");
        root("settings put global wifi_scan_throttle_enabled 0");
        root("settings put global wifi_scan_always_enabled 0");
        root("settings put global wifi_watchdog_on 0");
        root("iw dev wlan0 set power_save off");
        addLog("✅ WiFi optimized");
    }

    // ============================================================
    // VM / MEMORY
    // ============================================================

    void optimizeVM() {
        addLog("💾 Optimizing memory...");
        root("echo 80 > /proc/sys/vm/swappiness");
        root("echo 100 > /proc/sys/vm/vfs_cache_pressure");
        root("echo 10 > /proc/sys/vm/dirty_background_ratio");
        root("echo 20 > /proc/sys/vm/dirty_ratio");
        root("echo 65535 > /proc/sys/fs/file-max");
        root("echo 1000000 > /proc/sys/vm/max_map_count");
        addLog("✅ Memory optimized");
    }

    // ============================================================
    // CLEAR RAM
    // ============================================================

    void clearRAM() {
        addLog("🧹 Clearing RAM...");
        long before = getFreeRAM();

        ActivityManager am =
            (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procs =
            am.getRunningAppProcesses();

        int killed = 0;
        if (procs != null) {
            for (ActivityManager.RunningAppProcessInfo proc : procs) {
                if (proc.importance >
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    boolean protect = false;
                    for (String pkg : proc.pkgList) {
                        if (pkg.contains("activision") ||
                            pkg.contains("garena")     ||
                            pkg.contains("codm")       ||
                            pkg.contains("codmoptimizer") ||
                            pkg.equals("android")      ||
                            pkg.equals("com.android.systemui") ||
                            pkg.equals("com.android.phone")) {
                            protect = true;
                            break;
                        }
                    }
                    if (!protect) {
                        for (String pkg : proc.pkgList) {
                            am.killBackgroundProcesses(pkg);
                            killed++;
                        }
                    }
                }
            }
        }

        root("sync && echo 3 > /proc/sys/vm/drop_caches");
        root("echo 1 > /proc/sys/vm/compact_memory");

        long after = getFreeRAM();
        addLog("✅ Killed " + killed + " procs");
        addLog("✅ Freed: ~" + Math.max(after - before, 0) + " MB");
        addLog("✅ Free RAM: " + after + " MB");
    }

    // ============================================================
    // KILL APPS
    // ============================================================

    void killApps() {
        addLog("❌ Killing heavy apps...");
        String[] apps = {
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
            "org.telegram.messenger",
            "com.zhiliaoapp.musically",
            "com.linkedin.android"
        };

        ActivityManager am =
            (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (String pkg : apps) {
            am.killBackgroundProcesses(pkg);
            root("am force-stop " + pkg);
        }
        addLog("✅ Heavy apps stopped");
    }

    // ============================================================
    // START GAME SERVICE
    // ============================================================

    void startGameService() {
        try {
            Intent i = new Intent(this, GameService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
            addLog("✅ Game service started");
        } catch (Exception e) {
            addLog("⚠️ Service: " + e.getMessage());
        }
    }

    // ============================================================
    // LAUNCH CODM
    // ============================================================

    void launchCODM() {
        for (String pkg : CODM_PKGS) {
            try {
                Intent intent =
                    getPackageManager().getLaunchIntentForPackage(pkg);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    addLog("🎮 CODM launched: " + pkg);
                    return;
                }
            } catch (Exception ignored) {}
        }
        addLog("⚠️ CODM not found on device");
        Toast.makeText(this,
            "CODM not installed!", Toast.LENGTH_LONG).show();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    long pingHost(String host) {
        try {
            Process p = Runtime.getRuntime()
                .exec(new String[]{"ping", "-c", "1", "-W", "3", host});
            BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("time=")) {
                    String t = line.substring(line.indexOf("time=") + 5);
                    t = t.split(" ")[0];
                    p.destroy();
                    return Math.round(Float.parseFloat(t));
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return -1;
    }

    long getFreeRAM() {
        ActivityManager am =
            (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem / (1024 * 1024);
    }

    String root(String cmd) {
        try {
            Process p = Runtime.getRuntime()
                .exec(new String[]{"su", "-c", cmd});
            p.waitFor();
            return "ok";
        } catch (Exception e) {
            return "fail";
        }
    }

    void addLog(String msg) {
        String ts = new SimpleDateFormat(
            "HH:mm:ss", Locale.getDefault()).format(new Date());
        String entry = "[" + ts + "] " + msg + "\n";
        logText.insert(0, entry);
        if (logText.length() > 4000) {
            logText.delete(4000, logText.length());
        }
        runOnUiThread(() -> tvLog.setText(logText.toString()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
