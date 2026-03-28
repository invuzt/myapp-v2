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

    public native String helloFromRust();

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
        imgView = findViewById(R.id.imageViewEditor);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvDay = findViewById(R.id.tvDay);
        tvAddress = findViewById(R.id.tvAddress);
        etWatermark = findViewById(R.id.editWatermarkText);

        loadOptimizedPhoto();

        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveToGallery());

        try {
            String s = helloFromRust();
            if (s != null) Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {}
    }

    private void loadOptimizedPhoto() {
        String uriStr = getIntent().getStringExtra("PHOTO_URI");
        if (uriStr == null) return;
        try {
            Uri uri = Uri.parse(uriStr);
            // Trik Resolusi: Cek ukuran tanpa muat ke RAM dulu
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream is = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            // Hitung sample size (biar nggak blank putih)
            options.inSampleSize = calculateInSampleSize(options, 1280, 1280);
            options.inJustDecodeBounds = false;
            
            is = getContentResolver().openInputStream(uri);
            Bitmap raw = BitmapFactory.decodeStream(is, null, options);
            is.close();
            
            // Perbaiki Rotasi
            if (raw.getWidth() > raw.getHeight()) {
                Matrix m = new Matrix(); m.postRotate(90);
                baseBmp = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
            } else { baseBmp = raw; }
            
            imgView.setImageBitmap(baseBmp);
            setupLabels();
        } catch (Exception e) { finish(); }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqW, int reqH) {
        final int h = options.outHeight;
        final int w = options.outWidth;
        int inSampleSize = 1;
        if (h > reqH || w > reqW) {
            final int halfH = h / 2;
            final int halfW = w / 2;
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void setupLabels() {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String day = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date());
        String addr = prefs.getString("last_addr", "Mencari lokasi...");

        tvTime.setText(time); tvDate.setText(date); tvDay.setText(day); tvAddress.setText(addr);
        etWatermark.setText(time + "|" + date + "|" + day + "|" + addr);
    }

    private void saveToGallery() {
        if (baseBmp == null) return;
        Toast.makeText(this, "Memproses Gambar...", Toast.LENGTH_SHORT).show();

        Bitmap out = baseBmp.copy(Bitmap.Config.ARGB_8888, true);
        Canvas cv = new Canvas(out);
        float r = out.getHeight() / 1000f;
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setColor(Color.WHITE);
        pt.setTextSize(70 * r);
        pt.setShadowLayer(5 * r, 0, 0, Color.BLACK);
        
        cv.drawText(tvTime.getText().toString() + " " + tvDate.getText().toString(), 40 * r, out.getHeight() - (100 * r), pt);

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
                Toast.makeText(this, "Berhasil Simpan!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } catch (Exception e) {}
    }
}
