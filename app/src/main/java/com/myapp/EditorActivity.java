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
    private View watermarkOverlay; // Diganti View (Container)
    private TextView tvTime, tvDate, tvDay, tvAddress;
    private EditText editWatermark;
    private Bitmap originalBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        imageView = findViewById(R.id.imageViewEditor);
        watermarkOverlay = findViewById(R.id.watermarkOverlay);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvDay = findViewById(R.id.tvDay);
        tvAddress = findViewById(R.id.tvAddress);
        editWatermark = findViewById(R.id.editWatermarkText);

        loadPhoto();
        generateAutomaticMetadata();

        // Agar EditText juga bisa merubah tampilan real-time
        editWatermark.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    try {
                        String[] parts = s.toString().split("\|");
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
        String sAddress = "GPS: Menentukan lokasi...";
        
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
        } catch (Exception e) { sAddress = "GPS/Internet tidak tersedia"; }

        // Tampilkan di layar
        tvTime.setText(sTime);
        tvDate.setText(sDate);
        tvDay.setText(sDay);
        tvAddress.setText(sAddress);

        // Set di EditText (untuk diedit, pisahkan dengan '|')
        editWatermark.setText(sTime + "|" + sDate + "|" + sDay + "|" + sAddress);
    }

    private void processAndSave() {
        if (originalBitmap == null) return;
        Bitmap finalBmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(finalBmp);
        
        // --- LOGIKA GAYA WATERMARK ODFIZ ---
        float bW = finalBmp.getWidth();
        float bH = finalBmp.getHeight();
        float padding = bW / 30; // Proporsional

        // 1. Cat dasar teks
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(5f, 0f, 0f, Color.BLACK); // Biar keliatan di background terang
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // 2. Teks Jam (Besar)
        String sTime = tvTime.getText().toString();
        paint.setTextSize(bH / 15); // Jam Besar
        float xJam = padding;
        float yJam = bH - padding * 3;
        canvas.drawText(sTime, xJam, yJam, paint);

        // 3. Garis Vertikal Pemisah
        float timeWidth = paint.measureText(sTime);
        float xGaris = xJam + timeWidth + padding / 2;
        float yGarisTop = yJam - (bH / 15); // Sama tinggi jam
        float yGarisBot = yJam;
        paint.setStrokeWidth(4f);
        canvas.drawLine(xGaris, yGarisTop, xGaris, yGarisBot, paint);

        // 4. Teks Tanggal & Hari (Kecil, di samping jam)
        paint.setStrokeWidth(1f); // Reset stroke
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(bH / 45); // Kecil
        float xDate = xGaris + padding / 2;
        float yDate = yGarisTop + (paint.getTextSize()); // Agak atas
        canvas.drawText(tvDate.getText().toString(), xDate, yDate, paint);

        float yDay = yGarisBot - (paint.getTextSize()); // Agak bawah
        canvas.drawText(tvDay.getText().toString(), xDate, yDay, paint);

        // 5. Teks Alamat (Kecil, di bawah jam)
        paint.setTextSize(bH / 50); // Lebih kecil lagi
        float xAddr = padding;
        float yAddr = yJam + (paint.getTextSize() * 2);
        
        // Handle Alamat Panjang (Bungkus Teks)
        TextPaint tp = new TextPaint(paint);
        StaticLayout sl = new StaticLayout(tvAddress.getText().toString(), tp, (int)(bW - padding * 2), Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        canvas.save();
        canvas.translate(xAddr, yAddr);
        sl.draw(canvas);
        canvas.restore();

        saveImage(finalBmp);
    }

    private void saveImage(Bitmap bmp) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "Odfiz_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        try {
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri != null) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, os);
                os.close();
                Toast.makeText(this, "Tersimpan di Galeri!", Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (Exception e) { Toast.makeText(this, "Error simpan", Toast.LENGTH_SHORT).show(); }
    }
}
