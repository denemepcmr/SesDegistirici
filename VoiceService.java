package com.yusuf.sesdegistirici;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;

public class VoiceService extends Service {
    public static volatile boolean running = false;
    public static volatile int effect = 0;
    // 0 Erkek, 1 Kadın, 2 Çocuk, 3 Robot, 4 Canavar
    private static final float[] RATES = {0.78f, 1.35f, 1.65f, 1.0f, 0.58f};

    private Thread worker;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String ch = "ses_kanal";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(ch, "Ses Değiştirici", NotificationManager.IMPORTANCE_LOW));
        }
        Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, ch) : new Notification.Builder(this);
        Notification n = nb.setContentTitle("Ses Değiştirici çalışıyor")
                .setContentText("Mikrofon değiştirilmiş sese çevriliyor")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(1, n);
        }
        if (!running) {
            running = true;
            worker = new Thread(new Runnable() {
                @Override public void run() { loop(); }
            });
            worker.start();
        }
        return START_NOT_STICKY;
    }

    private void loop() {
        final int sr = 44100;
        final int chunk = 512;
        final int enc = AudioFormat.ENCODING_PCM_16BIT;
        AudioRecord rec = null;
        AudioTrack track = null;
        try {
            int minRec = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, enc);
            int minPlay = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, enc);
            rec = new AudioRecord(MediaRecorder.AudioSource.MIC, sr,
                    AudioFormat.CHANNEL_IN_MONO, enc, Math.max(minRec, chunk * 4));
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(enc).setSampleRate(sr)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(Math.max(minPlay, chunk * 4))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build();
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) return;

            PitchShifter ps = new PitchShifter();
            short[] in = new short[chunk];
            short[] out = new short[chunk];
            long t = 0;
            rec.startRecording();
            track.play();
            while (running) {
                int r = rec.read(in, 0, chunk);
                if (r <= 0) continue;
                int e = effect;
                if (e < 0 || e >= RATES.length) e = 0;
                float rate = RATES[e];
                for (int i = 0; i < r; i++) {
                    float x = in[i] / 32768f;
                    float y = ps.process(x, rate);
                    if (e == 3) {
                        y = y * (float) Math.sin(2.0 * Math.PI * 55.0 * (t++) / sr);
                    }
                    y *= 1.6f;
                    if (y > 1f) y = 1f;
                    if (y < -1f) y = -1f;
                    out[i] = (short) (y * 32767f);
                }
                track.write(out, 0, r);
            }
        } catch (Exception ex) {
            // sessizce durdur
        } finally {
            running = false;
            try { if (rec != null) { rec.stop(); rec.release(); } } catch (Exception ignored) { }
            try { if (track != null) { track.stop(); track.release(); } } catch (Exception ignored) { }
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
