package com.odfiz.timemark;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.location.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
        // Trik Anti-Putih: Set background gelap dulu
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        setContentView(R.layout.activity_editor);
        
        prefs = getSharedPreferences("OdfizPrefs", MODE_PRIVATE);
        imgView = findViewById(R.id.imageViewEditor);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvDay = findViewById(R.id.tvDay);
        tvAddress = findViewById(R.id.tvAddress);
        etWatermark = findViewById(R.id.editWatermarkText);

        // Jalankan loading di Thread terpisah agar UI tidak freeze
        new Thread(this::loadPhotoTask).start();

        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveProcessedImage());
    }

    private void loadPhotoTask() {
        String uriStr = getIntent().getStringExtra("PHOTO_URI");
        if (uriStr == null) return;
        try {
            Uri uri = Uri.parse(uriStr);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 2; // Hemat RAM agar tidak blank putih
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap raw = BitmapFactory.decodeStream(is);
            is.close();
            
            if (raw.getWidth() > raw.getHeight()) {
                Matrix m = new Matrix(); m.postRotate(90);
                baseBmp = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
            } else { baseBmp = raw; }
            
            runOnUiThread(() -> {
                imgView.setImageBitmap(baseBmp);
                updateLabels();
            });
        } catch (Exception e) {}
    }

    private void updateLabels() {
        String t = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String y = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date());
        String a = prefs.getString("last_addr", "Lokasi...");

        tvTime.setText(t); tvDate.setText(d); tvDay.setText(y); tvAddress.setText(a);
        etWatermark.setText(t + "|" + d + "|" + y + "|" + a);
    }

    private void saveProcessedImage() {
        if (baseBmp == null) return;
        
        // Simpan apa adanya (Tanpa Watermark tambahan di file)
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "Odfiz_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        try {
            Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (u != null) {
                OutputStream os = getContentResolver().openOutputStream(u);
                baseBmp.compress(Bitmap.CompressFormat.JPEG, 95, os);
                os.close();
                Toast.makeText(this, "Tersimpan di Galeri!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
