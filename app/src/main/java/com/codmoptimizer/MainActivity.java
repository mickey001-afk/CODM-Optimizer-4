package com.codmoptimizer;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    private TextView tvPing;
    private TextView tvRam;
    private TextView tvLog;
    private Spinner spinnerDNS;
    private Button btnBoost;
    private Button btnLaunch;
    private Button btnDNS;
    private Button btnCPU;
    private Button btnGPU;
    private Button btnClear;
    private Button btnKill;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final StringBuilder logText = new StringBuilder();

    private static final String[] CODM_PKGS = {
        "com.activision.callofduty.shooter",
        "com.garena.game.codm",
        "com.vng.codmvn"
    };

    private static final String[][] DNS = {
        {"Cloudflare Gaming", "1.1.1.2", "1.0.0.2",
            "security.cloudflare-dns.com"},
        {"Cloudflare",        "1.1.1.1", "1.0.0.1",
            "one.one.one.one"},
        {"Google DNS",        "8.8.8.8", "8.8.4.4",
            "dns.google"},
        {"Quad9",             "9.9.9.9", "149.112.112.112",
            "dns.quad9.net"},
        {"AdGuard",           "94.140.14.14", "94.140.15.15",
            "dns.adguard.com"},
        {"OpenDNS",           "208.67.222.222", "208.67.220.220",
            "doh.opendns.com"},
        {"NextDNS",           "45.90.28.0", "45.90.30.0",
            "dns.nextdns.io"},
        {"AliDNS Asia",       "223.5.5.5", "223.6.6.6",
            "dns.alidns.com"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupDNSSpinner();
        setupButtons();
        startPingMonitor();
        startRamMonitor();

        addLog("CODM Optimizer Ready");
        addLog("Device: " + Build.MANUFACTURER + " " + Build.MODEL);
        addLog("Android: " + Build.VERSION.RELEASE);
        addLog("Tap BOOST to start");
    }

    // =========================================================
    // INIT
    // =========================================================

    private void initViews() {
        tvPing     = findViewById(R.id.tvPing);
        tvRam      = findViewById(R.id.tvRam);
        tvLog      = findViewById(R.id.tvLog);
        spinnerDNS = findViewById(R.id.spinnerDNS);
        btnBoost   = findViewById(R.id.btnBoost);
        btnLaunch  = findViewById(R.id.btnLaunch);
        btnDNS     = findViewById(R.id.btnDNS);
        btnCPU     = findViewById(R.id.btnCPU);
        btnGPU     = findViewById(R.id.btnGPU);
        btnClear   = findViewById(R.id.btnClear);
        btnKill    = findViewById(R.id.btnKill);
    }

    private void setupDNSSpinner() {
        List<String> names = new ArrayList<>();
        for (String[] d : DNS) {
            names.add(d[0] + " (" + d[1] + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            names
        );
        spinnerDNS.setAdapter(adapter);
    }

    private void setupButtons() {
        btnBoost.setOnClickListener(v -> {
            btnBoost.setEnabled(false);
            btnBoost.setText("BOOSTING...");
            new Thread(() -> {
                runFullBoost();
                runOnUiThread(() -> {
                    btnBoost.setEnabled(true);
                    btnBoost.setText("ONE-TAP ULTIMATE BOOST");
                    Toast.makeText(
                        MainActivity.this,
                        "Boost Complete!",
                        Toast.LENGTH_SHORT
                    ).show();
                });
            }).start();
        });

        btnLaunch.setOnClickListener(v -> {
            btnLaunch.setEnabled(false);
            new Thread(() -> {
                runFullBoost();
                runOnUiThread(() -> {
                    launchCODM();
                    btnLaunch.setEnabled(true);
                });
            }).start();
        });

        btnDNS.setOnClickListener(v ->
            new Thread(this::applyDNS).start()
        );

        btnCPU.setOnClickListener(v ->
            new Thread(this::optimizeCPU).start()
        );

        btnGPU.setOnClickListener(v ->
            new Thread(this::optimizeGPU).start()
        );

        btnClear.setOnClickListener(v ->
            new Thread(this::clearRAM).start()
        );

        btnKill.setOnClickListener(v ->
            new Thread(this::killApps).start()
        );
    }

    // =========================================================
    // MONITORS
    // =========================================================

    private void startPingMonitor() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    long ping = doPing("8.8.8.8");
                    runOnUiThread(() -> {
                        if (ping > 0) {
                            tvPing.setText(String.valueOf(ping));
                            if (ping < 50) {
                                tvPing.setTextColor(0xFF00FF88);
                            } else if (ping < 100) {
                                tvPing.setTextColor(0xFFFFD700);
                            } else {
                                tvPing.setTextColor(0xFFFF4444);
                            }
                        } else {
                            tvPing.setText("--");
                        }
                    });
                }).start();
                handler.postDelayed(this, 5000);
            }
        }, 2000);
    }

    private void startRamMonitor() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long free = getFreeRam();
                runOnUiThread(() -> {
                    tvRam.setText(String.valueOf(free));
                    if (free > 1000) {
                        tvRam.setTextColor(0xFF00FF88);
                    } else if (free > 500) {
                        tvRam.setTextColor(0xFFFFD700);
                    } else {
                        tvRam.setTextColor(0xFFFF4444);
                    }
                });
                handler.postDelayed(this, 3000);
            }
        }, 2000);
    }

    // =========================================================
    // FULL BOOST
    // =========================================================

    private void runFullBoost() {
        addLog("==========================");
        addLog("BOOST STARTED");
        addLog("==========================");
        clearRAM();
        killApps();
        applyDNS();
        optimizeTCP();
        optimizeCPU();
        optimizeGPU();
        optimizeWifi();
        optimizeVM();
        startGameService();
        addLog("==========================");
        addLog("BOOST COMPLETE");
        addLog("==========================");
    }

    // =========================================================
    // DNS
    // =========================================================

    private void applyDNS() {
        int i = spinnerDNS.getSelectedItemPosition();
        String primary   = DNS[i][1];
        String secondary = DNS[i][2];
        String hostname  = DNS[i][3];

        addLog("Applying DNS: " + DNS[i][0]);
        runRoot("settings put global private_dns_mode hostname");
        runRoot("settings put global private_dns_specifier " + hostname);
        runRoot("setprop net.dns1 " + primary);
        runRoot("setprop net.dns2 " + secondary);
        runRoot("setprop net.wlan0.dns1 " + primary);
        runRoot("setprop net.wlan0.dns2 " + secondary);
        runRoot("setprop dhcp.wlan0.dns1 " + primary);
        runRoot("setprop dhcp.wlan0.dns2 " + secondary);
        addLog("DNS set: " + 
