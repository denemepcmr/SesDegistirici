package com.yusuf.sesdegistirici;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends Activity {
    private Button toggle;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Ses Değiştirici");
        title.setTextSize(24);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(14);
        status.setPadding(0, pad / 2, 0, pad / 2);
        status.setText("Kulaklık veya Bluetooth hoparlör kullanın (geri besleme/ıslık olmasın).");
        root.addView(status);

        RadioGroup rg = new RadioGroup(this);
        String[] names = {"Erkek (kalın)", "Kadın", "Çocuk", "Robot", "Canavar"};
        for (int i = 0; i < names.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(names[i]);
            rb.setId(1000 + i);
            rg.addView(rb);
        }
        rg.check(1000 + VoiceService.effect);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup g, int id) {
                VoiceService.effect = id - 1000;
            }
        });
        root.addView(rg);

        toggle = new Button(this);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (VoiceService.running) {
                    stopService(new Intent(MainActivity.this, VoiceService.class));
                    VoiceService.running = false;
                    refresh();
                } else {
                    askPermissionsAndStart();
                }
            }
        });
        root.addView(toggle);
        setContentView(root);
        refresh();
    }

    private void refresh() {
        toggle.setText(VoiceService.running ? "DURDUR" : "BAŞLAT");
    }

    private void askPermissionsAndStart() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        } else {
            startVoice();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoice();
        } else {
            status.setText("Mikrofon izni gerekli.");
        }
    }

    private void startVoice() {
        Intent i = new Intent(this, VoiceService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        toggle.postDelayed(new Runnable() {
            @Override public void run() { refresh(); }
        }, 400);
    }
}
