package com.odfiz.timemark;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.location.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditorActivity extends Activity {
    private ImageView imgView;
    private TextView tvTime, tvDate, tvDay, tvAddress;
    private EditText etWatermark;
    private Bitmap baseBmp;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        
        prefs = getSharedPreferences("OdfizPrefs", MODE_PRIVATE);
        initViews();
        loadInitialData();
        
        findViewById(R.id.btnCancel).setOnClickListener(v -> restartApp());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveProcessedImage());
    }

    private void initViews() {
        imgView = findViewById(R.id.imageViewEditor);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvDay = findViewById(R.id.tvDay);
        tvAddress = findViewById(R.id.tvAddress);
        etWatermark = findViewById(R.id.editWatermarkText);

        etWatermark.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String[] p = s.toString().split("\\|");
                if (p.length >= 4) {
                    tvTime.setText(p[0]); tvDate.setText(p[1]);
                    tvDay.setText(p[2]); tvAddress.setText(p[3]);
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadInitialData() {
        String uriStr = getIntent().getStringExtra("PHOTO_URI");
        if (uriStr == null) { finish(); return; }

        try {
            Bitmap raw = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(uriStr));
            if (raw.getWidth() > raw.getHeight()) {
                Matrix m = new Matrix(); m.postRotate(90);
                baseBmp = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
            } else { baseBmp = raw; }
            imgView.setImageBitmap(baseBmp);
        } catch (Exception e) { finish(); }

        // Load metadata (Time & Last Address)
        SimpleDateFormat st = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat sd = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sy = new SimpleDateFormat("EEE", Locale.getDefault());
        
        String time = st.format(new Date());
        String date = sd.format(new Date());
        String day = sy.format(new Date());
        String addr = prefs.getString("last_addr", "Mencari lokasi...");

        tvTime.setText(time); tvDate.setText(date); tvDay.setText(day); tvAddress.setText(addr);
        etWatermark.setText(time + "|" + date + "|" + day + "|" + addr);

        // Update lokasi di background agar tidak lag
        new Thread(() -> fetchLocation(time, date, day)).start();
    }

    private void fetchLocation(String time, String date, String day) {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                Geocoder g = new Geocoder(this, Locale.getDefault());
                List<Address> a = g.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (a != null && !a.isEmpty()) {
                    String newAddr = a.get(0).getAddressLine(0);
                    prefs.edit().putString("last_addr", newAddr).apply();
                    runOnUiThread(() -> {
                        tvAddress.setText(newAddr);
                        etWatermark.setText(time + "|" + date + "|" + day + "|" + newAddr);
                    });
                }
            }
        } catch (Exception e) {}
    }

    private void saveProcessedImage() {
        if (baseBmp == null) return;
        Bitmap out = baseBmp.copy(Bitmap.Config.ARGB_8888, true);
        Canvas cv = new Canvas(out);
        float r = out.getHeight() / 1000f;
        float p = 45 * r;

        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setShadowLayer(6 * r, 0, 0, Color.BLACK);
        pt.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        
        // Render Jam (Taller scale)
        pt.setColor(Color.WHITE);
        pt.setTextSize(95 * r);
        pt.setTextScaleX(0.82f); 
        Rect b = new Rect();
        String ts = tvTime.getText().toString();
        pt.getTextBounds(ts, 0, ts.length(), b);
        float yB = out.getHeight() - (180 * r);
        float yT = yB - b.height();
        cv.drawText(ts, p, yB, pt);

        // Render Garis Kuning
        pt.setColor(Color.parseColor("#FFD700"));
        pt.setStrokeWidth(6 * r);
        float xG = p + pt.measureText(ts) + (18 * r);
        cv.drawLine(xG, yT, xG, yB, pt);

        // Render Tanggal & Hari
        pt.setColor(Color.WHITE);
        pt.setTextSize(26 * r);
        pt.setTextScaleX(0.9f);
        pt.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        cv.drawText(tvDate.getText().toString(), xG + (18 * r), yT + (24 * r), pt);
        cv.drawText(tvDay.getText().toString(), xG + (18 * r), yB - (2 * r), pt);

        // Render Alamat
        pt.setTextSize(23 * r);
        TextPaint tp = new TextPaint(pt);
        StaticLayout sl = new StaticLayout(tvAddress.getText().toString(), tp, (int)(out.getWidth() - (p * 2)), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        cv.save();
        cv.translate(p, yB + (25 * r));
        sl.draw(cv);
        cv.restore();

        saveToGallery(out);
    }

    private void saveToGallery(Bitmap b) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "Odfiz_" + System.currentTimeMillis());
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");
        try {
            Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (u != null) {
                OutputStream os = getContentResolver().openOutputStream(u);
                b.compress(Bitmap.CompressFormat.JPEG, 95, os);
                os.close();
                restartApp();
            }
        } catch (Exception e) {}
    }

    private void restartApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
