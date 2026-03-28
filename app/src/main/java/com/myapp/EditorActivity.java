package com.myapp;

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
import android.view.View;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditorActivity extends Activity {
    private ImageView imageView;
    private TextView tvTime, tvDate, tvDay, tvAddress;
    private EditText editWatermark;
    private Bitmap originalBitmap;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        prefs = getSharedPreferences("OdfizPrefs", MODE_PRIVATE);
        imageView = findViewById(R.id.imageViewEditor);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvDay = findViewById(R.id.tvDay);
        tvAddress = findViewById(R.id.tvAddress);
        editWatermark = findViewById(R.id.editWatermarkText);

        loadPhoto();
        generateAutomaticMetadata();

        editWatermark.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    try {
                        String[] parts = s.toString().split(java.util.regex.Pattern.quote("|"));
                        if (parts.length >= 4) {
                            tvTime.setText(parts[0]); tvDate.setText(parts[1]);
                            tvDay.setText(parts[2]); tvAddress.setText(parts[3]);
                        }
                    } catch (Exception e) {}
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.btnSave).setOnClickListener(v -> processAndSave());
    }

    private void loadPhoto() {
        String uriString = getIntent().getStringExtra("PHOTO_URI");
        if (uriString != null) {
            try {
                Bitmap raw = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(uriString));
                if (raw.getWidth() > raw.getHeight()) {
                    Matrix m = new Matrix(); m.postRotate(90);
                    originalBitmap = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
                } else { originalBitmap = raw; }
                imageView.setImageBitmap(originalBitmap);
            } catch (Exception e) {}
        }
    }

    private void generateAutomaticMetadata() {
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfDay = new SimpleDateFormat("EEE", Locale.getDefault());
        String sTime = sdfTime.format(new Date());
        String sDate = sdfDate.format(new Date());
        String sDay = sdfDay.format(new Date());
        
        // Ambil alamat terakhir yang tersimpan di memori sebagai cadangan
        String lastAddress = prefs.getString("last_address", "Lokasi tidak diketahui");
        String sAddress = lastAddress;

        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            // Coba ambil lokasi dari GPS atau Network
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (loc != null) {
                Geocoder geo = new Geocoder(this, Locale.getDefault());
                List<Address> addrs = geo.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addrs != null && !addrs.isEmpty()) {
                    sAddress = addrs.get(0).getAddressLine(0);
                    // Simpan alamat baru ini ke memori untuk penggunaan berikutnya (offline)
                    prefs.edit().putString("last_address", sAddress).apply();
                }
            }
        } catch (Exception e) {}

        tvTime.setText(sTime); tvDate.setText(sDate); tvDay.setText(sDay); tvAddress.setText(sAddress);
        editWatermark.setText(sTime + "|" + sDate + "|" + sDay + "|" + sAddress);
    }

    private void processAndSave() {
        if (originalBitmap == null) return;
        Bitmap finalBmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(finalBmp);
        float bW = finalBmp.getWidth();
        float bH = finalBmp.getHeight();
        float ratio = bH / 1000f; 
        float padding = 45 * ratio;
        
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(6 * ratio, 0, 0, Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        // 1. Tinggi Jam
        float jamSize = 85 * ratio;
        paint.setTextSize(jamSize);
        Rect jamBounds = new Rect();
        String timeStr = tvTime.getText().toString();
        paint.getTextBounds(timeStr, 0, timeStr.length(), jamBounds);
        float yBaselineJam = bH - (180 * ratio);
        float jamTop = yBaselineJam - jamBounds.height();
        float jamBottom = yBaselineJam;

        // 2. Gambar Jam
        canvas.drawText(timeStr, padding, yBaselineJam, paint);

        // 3. Garis Kuning
        float xGaris = padding + paint.measureText(timeStr) + (18 * ratio);
        paint.setColor(Color.parseColor("#FFD700"));
        paint.setStrokeWidth(5 * ratio);
        canvas.drawLine(xGaris, jamTop, xGaris, jamBottom, paint);

        // 4. Tanggal & Hari
        paint.setColor(Color.WHITE);
        paint.setTextSize(26 * ratio);
        paint.setStrokeWidth(1);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        canvas.drawText(tvDate.getText().toString(), xGaris + (18 * ratio), jamTop + (24 * ratio), paint);
        canvas.drawText(tvDay.getText().toString(), xGaris + (18 * ratio), jamBottom - (2 * ratio), paint);

        // 5. Alamat
        paint.setTextSize(23 * ratio);
        TextPaint tp = new TextPaint(paint);
        StaticLayout sl = new StaticLayout(tvAddress.getText().toString(), tp, (int)(bW - (padding * 2)), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        canvas.save();
        canvas.translate(padding, jamBottom + (25 * ratio));
        sl.draw(canvas);
        canvas.restore();

        saveImage(finalBmp);
    }

    private void saveImage(Bitmap bmp) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "Odfiz_" + System.currentTimeMillis());
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");
        try {
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri != null) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, os);
                os.close();
                Toast.makeText(this, "Foto Tersimpan!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } catch (Exception e) {}
    }
}
