package com.odfiz.timemark;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable; // INI YANG TADI KURANG, MAS!
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
        // Layar Hitam Anti-Putih
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        setContentView(R.layout.activity_editor);
        
        prefs = getSharedPreferences("OdfizPrefs", MODE_PRIVATE);
        initViews();
        
        imgView.postDelayed(this::loadOptimizedPhoto, 300);

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

        etWatermark.setOnFocusChangeListener((v, hasFocus) -> isUserEditing = hasFocus);
        etWatermark.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUserEditing) {
                    String[] p = s.toString().split("\\|");
                    if (p.length >= 1) tvTime.setText(p[0]);
                    if (p.length >= 2) tvDate.setText(p[1]);
                    if (p.length >= 3) tvDay.setText(p[2]);
                    if (p.length >= 4) tvAddress.setText(p[3]);
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
            opt.inSampleSize = 2;
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap raw = BitmapFactory.decodeStream(is);
            is.close();
            
            if (raw.getWidth() > raw.getHeight()) {
                Matrix m = new Matrix(); m.postRotate(90);
                baseBmp = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
            } else { baseBmp = raw; }
            
            imgView.setImageBitmap(baseBmp);
            updateDateTime();
            startGPS();
        } catch (Exception e) {}
    }

    private void updateDateTime() {
        String t = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String y = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date());
        String a = prefs.getString("last_addr", "Mencari lokasi...");

        runOnUiThread(() -> {
            tvTime.setText(t); tvDate.setText(d); tvDay.setText(y); tvAddress.setText(a);
            if (!isUserEditing) etWatermark.setText(t + "|" + d + "|" + y + "|" + a);
        });
    }

    private void startGPS() {
        new Thread(() -> {
            try {
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) processLocation(loc);
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
                updateDateTime();
            }
        } catch (Exception e) {}
    }

    private void saveToGallery() {
        if (baseBmp == null) return;
        Toast.makeText(this, "Odfiz Engine: Finalizing...", Toast.LENGTH_SHORT).show();

        Bitmap out = baseBmp.copy(Bitmap.Config.ARGB_8888, true);
        Canvas cv = new Canvas(out);
        float ratio = out.getHeight() / 1000f;
        float padding = 40 * ratio;

        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setColor(Color.WHITE);
        pt.setShadowLayer(4 * ratio, 0, 0, Color.BLACK);
        pt.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        
        // Watermark Waktu
        pt.setTextSize(85 * ratio);
        cv.drawText(tvTime.getText().toString(), padding, out.getHeight() - (180 * ratio), pt);

        // Alamat Auto-Wrap
        pt.setTextSize(25 * ratio);
        pt.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        TextPaint tp = new TextPaint(pt);
        int maxWidth = (int) (out.getWidth() - (padding * 2.5));
        StaticLayout sl = new StaticLayout(tvAddress.getText().toString(), tp, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        
        cv.save();
        cv.translate(padding, out.getHeight() - (150 * ratio));
        sl.draw(cv);
        cv.restore();

        // LOGO ODFIZ di Pojok Kanan Bawah
        pt.setColor(Color.parseColor("#4CAF50")); // Hijau Odfiz
        pt.setTextSize(35 * ratio);
        pt.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
        String logo = "ODFIZ ENGINE";
        float logoWidth = pt.measureText(logo);
        cv.drawText(logo, out.getWidth() - logoWidth - padding, out.getHeight() - padding, pt);

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
