package com.myapp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
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
        String sAddress = "Mencari lokasi...";
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                Geocoder geo = new Geocoder(this, Locale.getDefault());
                List<Address> addrs = geo.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addrs != null && !addrs.isEmpty()) sAddress = addrs.get(0).getAddressLine(0);
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
        
        // --- SCALE SYNC (Menyamakan Layar & Foto) ---
        float ratio = bH / 1000f; // Dasar skala foto
        float padding = 40 * ratio;
        
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(5 * ratio, 0, 0, Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        // 1. Jam Besar
        paint.setTextSize(85 * ratio); 
        float yJam = bH - (150 * ratio);
        canvas.drawText(tvTime.getText().toString(), padding, yJam, paint);

        // 2. Garis Kuning
        float timeWidth = paint.measureText(tvTime.getText().toString());
        float xGaris = padding + timeWidth + (15 * ratio);
        paint.setColor(Color.parseColor("#FFD700"));
        paint.setStrokeWidth(4 * ratio);
        canvas.drawLine(xGaris, yJam - (75 * ratio), xGaris, yJam, paint);

        // 3. Tanggal & Hari
        paint.setColor(Color.WHITE);
        paint.setTextSize(25 * ratio);
        paint.setStrokeWidth(1);
        canvas.drawText(tvDate.getText().toString(), xGaris + (15 * ratio), yJam - (45 * ratio), paint);
        canvas.drawText(tvDay.getText().toString(), xGaris + (15 * ratio), yJam - (10 * ratio), paint);

        // 4. Alamat (Multi-line)
        paint.setTextSize(22 * ratio);
        TextPaint tp = new TextPaint(paint);
        StaticLayout sl = new StaticLayout(tvAddress.getText().toString(), tp, (int)(bW - (padding * 2)), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        canvas.save();
        canvas.translate(padding, yJam + (30 * ratio));
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
                Toast.makeText(this, "Tersimpan!", Toast.LENGTH_SHORT).show();
                
                // --- BALIK KE KAMERA LAGI ---
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } catch (Exception e) {}
    }
}
