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
    static {
        try { System.loadLibrary("odfiz_native"); } catch (UnsatisfiedLinkError e) {}
    }

    private ImageView imgView;
    private TextView tvTime, tvDate, tvDay, tvAddress;
    private EditText etWatermark;
    private Bitmap baseBmp;
    private SharedPreferences prefs;
    private boolean isUserEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        
        prefs = getSharedPreferences("OdfizPrefs", MODE_PRIVATE);
        initViews();
        
        // 1. Load Foto (Pake delay dikit biar UI muncul dulu, anti-blank putih)
        imgView.postDelayed(this::loadOptimizedPhoto, 300);

        // 2. Tombol-tombol
        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveToGallery());
    }

    private void initViews() {
        imgView = findViewById(R.id.imageViewEditor);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvDay = findViewById(R.id.tvDay);
        tvAddress = findViewById(R.id.tvAddress);
        etWatermark = findViewById(R.id.editWatermarkText);

        // Biar bisa diedit manual tanpa ditimpa GPS otomatis
        etWatermark.setOnFocusChangeListener((v, hasFocus) -> isUserEditing = hasFocus);
        
        etWatermark.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUserEditing) { // Update tampilan hanya jika user yang ngetik
                    String[] p = s.toString().split("\\|");
                    if (p.length >= 4) {
                        tvTime.setText(p[0]); tvDate.setText(p[1]);
                        tvDay.setText(p[2]); tvAddress.setText(p[3]);
                    }
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadOptimizedPhoto() {
        String uriStr = getIntent().getStringExtra("PHOTO_URI");
        if (uriStr == null) return;
        try {
            Uri uri = Uri.parse(uriStr);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 2; // Paksa kecilin dikit biar enteng
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap raw = BitmapFactory.decodeStream(is);
            is.close();
            
            if (raw.getWidth() > raw.getHeight()) {
                Matrix m = new Matrix(); m.postRotate(90);
                baseBmp = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
            } else { baseBmp = raw; }
            
            imgView.setImageBitmap(baseBmp);
            updateDateTime();
            startGPS(); // GPS jalan di belakang
        } catch (Exception e) {}
    }

    private void updateDateTime() {
        String t = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String y = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date());
        String a = prefs.getString("last_addr", "Mencari lokasi...");

        tvTime.setText(t); tvDate.setText(d); tvDay.setText(y); tvAddress.setText(a);
        if (!isUserEditing) etWatermark.setText(t + "|" + d + "|" + y + "|" + a);
    }

    private void startGPS() {
        new Thread(() -> {
            try {
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                String provider = lm.getBestProvider(criteria, true);
                
                if (provider != null) {
                    Location loc = lm.getLastKnownLocation(provider);
                    if (loc != null) processLocation(loc);
                }
            } catch (Exception e) {}
        }).start();
    }

    private void processLocation(Location loc) {
        try {
            Geocoder g = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = g.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String addr = addresses.get(0).getAddressLine(0);
                prefs.edit().putString("last_addr", addr).apply();
                runOnUiThread(() -> {
                    if (!isUserEditing) {
                        tvAddress.setText(addr);
                        updateDateTime(); 
                    }
                });
            }
        } catch (Exception e) {}
    }

    private void saveToGallery() {
        if (baseBmp == null) return;
        Toast.makeText(this, "Menyimpan...", Toast.LENGTH_SHORT).show();

        Bitmap out = baseBmp.copy(Bitmap.Config.ARGB_8888, true);
        Canvas cv = new Canvas(out);
        float r = out.getHeight() / 1000f;
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setColor(Color.WHITE);
        pt.setTextSize(60 * r);
        pt.setShadowLayer(3 * r, 0, 0, Color.BLACK);
        
        cv.drawText(tvTime.getText().toString() + " | " + tvAddress.getText().toString(), 40 * r, out.getHeight() - (80 * r), pt);

        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "Odfiz_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        try {
            Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (u != null) {
                OutputStream os = getContentResolver().openOutputStream(u);
                out.compress(Bitmap.CompressFormat.JPEG, 90, os);
                os.close();
                Toast.makeText(this, "Tersimpan!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } catch (Exception e) {}
    }
}
