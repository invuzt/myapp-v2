package com.myapp;

import android.app.Activity;
import android.content.ContentValues;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

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
                            tvTime.setText(parts[0]);
                            tvDate.setText(parts[1]);
                            tvDay.setText(parts[2]);
                            tvAddress.setText(parts[3]);
                        } else {
                            tvAddress.setText(s);
                        }
                    } catch (Exception e) {}
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
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
        String sAddress = "GPS: Mencari alamat...";
        
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    sAddress = addresses.get(0).getAddressLine(0);
                }
            }
        } catch (Exception e) { sAddress = "GPS/Internet bermasalah"; }

        tvTime.setText(sTime);
        tvDate.setText(sDate);
        tvDay.setText(sDay);
        tvAddress.setText(sAddress);
        editWatermark.setText(sTime + "|" + sDate + "|" + sDay + "|" + sAddress);
    }

    private void processAndSave() {
        if (originalBitmap == null) return;
        Bitmap finalBmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(finalBmp);
        
        float bW = finalBmp.getWidth();
        float bH = finalBmp.getHeight();
        
        // --- PERHITUNGAN GAYA WATERMARK ODFIZ (SESUAI CONTOH) ---
        // Koordinat dinaikkan agar aman dari pemotongan Galeri
        float paddingL = bW / 18; 
        float paddingB = bH / 10; 

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(8f, 0f, 0f, Color.BLACK); 
        // Pakai font SANS_SERIF bawaan Android agar lebih mirip contoh
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        // 1. Teks Jam (Besar, Ramping)
        String sTime = tvTime.getText().toString();
        paint.setTextSize(bH / 12); // Jam Besar
        float xJam = paddingL;
        // Y Jam dihitung agar berada di atas alamat
        float yJam = bH - paddingB - (bH / 18); 
        canvas.drawText(sTime, xJam, yJam, paint);

        // 2. Garis Vertikal Pemisah (WARNA KUNING)
        float timeWidth = paint.measureText(sTime);
        float xGaris = xJam + timeWidth + paddingL / 2;
        float yGarisTop = yJam - (bH / 12); // Sama tinggi jam
        float yGarisBot = yJam;
        paint.setColor(Color.parseColor("#FFD700")); // Kuning Emas
        paint.setStrokeWidth(6f);
        canvas.drawLine(xGaris, yGarisTop, xGaris, yGarisBot, paint);

        // 3. Teks Tanggal & Hari (Kecil, Putih)
        paint.setColor(Color.WHITE); // Reset warna ke putih
        paint.setStrokeWidth(1f); 
        paint.setTextSize(bH / 38); // Tanggal Kecil
        float xDate = xGaris + paddingL / 2;
        canvas.drawText(tvDate.getText().toString(), xDate, yGarisTop + (paint.getTextSize() * 1.3f), paint);
        canvas.drawText(tvDay.getText().toString(), xDate, yGarisBot - (paint.getTextSize() * 0.3f), paint);

        // 4. Teks Alamat (Di bawah jam, Putih, Bungkus otomatis)
        paint.setTextSize(bH / 45); // Alamat Lebih Kecil
        float xAddr = paddingL;
        // Y Alamat tepat di bawah Jam
        float yAddrTop = yJam + (paint.getTextSize() * 1.6f);
        
        TextPaint tp = new TextPaint(paint);
        int targetWidth = (int)(bW - paddingL * 2);
        StaticLayout sl = new StaticLayout(tvAddress.getText().toString(), tp, targetWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        
        // Proteksi: Jika alamat sangat panjang, naikkan container jam agar alamat tidak terpotong bawah
        float actualYAddr = yAddrTop;
        if (yAddrTop + sl.getHeight() > bH - (bH/50)) {
             actualYAddr = bH - sl.getHeight() - (bH/50);
        }

        canvas.save();
        canvas.translate(xAddr, actualYAddr);
        sl.draw(canvas);
        canvas.restore();

        saveImage(finalBmp);
    }

    private void saveImage(Bitmap bmp) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "Odfiz_Timemark_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        try {
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri != null) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, os);
                os.close();
                Toast.makeText(this, "Tersimpan di DCIM/Camera!", Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (Exception e) { Toast.makeText(this, "Gagal simpan", Toast.LENGTH_SHORT).show(); }
    }
}
