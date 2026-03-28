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
    private View watermarkOverlay;
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

        editWatermark.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    try {
                        // Menggunakan Quoted Pattern agar aman dari error escape
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
        String sAddress = "Mencari alamat...";
        
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
        } catch (Exception e) { sAddress = "GPS/Internet mati"; }

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
        float padding = bW / 30;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(8f, 0f, 0f, Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Jam (Style Besar)
        String sTime = tvTime.getText().toString();
        paint.setTextSize(bH / 12); 
        float xJam = padding;
        float yJam = bH - padding * 4;
        canvas.drawText(sTime, xJam, yJam, paint);

        // Garis Vertikal
        float timeWidth = paint.measureText(sTime);
        float xGaris = xJam + timeWidth + padding / 1.5f;
        float yGarisTop = yJam - (bH / 12);
        float yGarisBot = yJam;
        paint.setStrokeWidth(6f);
        canvas.drawLine(xGaris, yGarisTop, xGaris, yGarisBot, paint);

        // Tanggal & Hari
        paint.setStrokeWidth(1f);
        paint.setTextSize(bH / 35);
        float xDate = xGaris + padding / 1.5f;
        canvas.drawText(tvDate.getText().toString(), xDate, yGarisTop + (paint.getTextSize() * 1.2f), paint);
        canvas.drawText(tvDay.getText().toString(), xDate, yGarisBot - (paint.getTextSize() * 0.2f), paint);

        // Alamat (Di bawah Jam)
        paint.setTextSize(bH / 45);
        float yAddr = yJam + (paint.getTextSize() * 2.5f);
        TextPaint tp = new TextPaint(paint);
        StaticLayout sl = new StaticLayout(tvAddress.getText().toString(), tp, (int)(bW - padding * 2), Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        canvas.save();
        canvas.translate(padding, yAddr);
        sl.draw(canvas);
        canvas.restore();

        saveImage(finalBmp);
    }

    private void saveImage(Bitmap bmp) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "Odfiz_Style_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        try {
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri != null) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, os);
                os.close();
                Toast.makeText(this, "Berhasil simpan ke Galeri!", Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (Exception e) { Toast.makeText(this, "Gagal simpan", Toast.LENGTH_SHORT).show(); }
    }
}
