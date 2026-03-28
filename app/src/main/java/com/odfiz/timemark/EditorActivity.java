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
        // Paksa layar hitam sejak awal agar tidak ada putih-putih
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        setContentView(R.layout.activity_editor);
        
        prefs = getSharedPreferences("OdfizPrefs", MODE_PRIVATE);
        imgView = findViewById(R.id.imageViewEditor);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvDay = findViewById(R.id.tvDay);
        tvAddress = findViewById(R.id.tvAddress);
        etWatermark = findViewById(R.id.editWatermarkText);

        // Langsung muat foto
        loadPhotoImmediate();
        
        // Panggil lokasi (Ambil yang terakhir tersimpan agar cepat)
        String lastAddr = prefs.getString("last_addr", "Mencari lokasi...");
        tvAddress.setText(lastAddr);
        updateTimeLabels();

        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveWithWatermarkFinal());
    }

    private void loadPhotoImmediate() {
        String uriStr = getIntent().getStringExtra("PHOTO_URI");
        if (uriStr == null) return;
        try {
            Uri uri = Uri.parse(uriStr);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 2; // Kecilkan resolusi agar RAM aman
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap raw = BitmapFactory.decodeStream(is);
            is.close();
            
            if (raw.getWidth() > raw.getHeight()) {
                Matrix m = new Matrix(); m.postRotate(90);
                baseBmp = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
            } else { baseBmp = raw; }
            
            imgView.setImageBitmap(baseBmp);
        } catch (Exception e) {}
    }

    private void updateTimeLabels() {
        String t = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String y = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date());
        tvTime.setText(t); tvDate.setText(d); tvDay.setText(y);
        etWatermark.setText(t + "|" + d + "|" + y + "|" + tvAddress.getText());
    }

    private void saveWithWatermarkFinal() {
        if (baseBmp == null) return;
        Toast.makeText(this, "Menyimpan ke Galeri...", Toast.LENGTH_SHORT).show();

        // 1. Buat salinan Bitmap untuk digambar
        Bitmap out = baseBmp.copy(Bitmap.Config.ARGB_8888, true);
        Canvas cv = new Canvas(out);
        float ratio = out.getHeight() / 1000f;
        float padding = 40 * ratio;

        // 2. Setting Paint (Teks)
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setColor(Color.WHITE);
        pt.setShadowLayer(5 * ratio, 0, 0, Color.BLACK);
        
        // Gambar Waktu (Besar)
        pt.setTextSize(90 * ratio);
        pt.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        cv.drawText(tvTime.getText().toString(), padding, out.getHeight() - (220 * ratio), pt);

        // Gambar Alamat (Kecil & Rapih)
        pt.setTextSize(28 * ratio);
        pt.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        TextPaint tp = new TextPaint(pt);
        StaticLayout sl = new StaticLayout(tvAddress.getText().toString(), tp, (int)(out.getWidth() - (padding*2)), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        
        cv.save();
        cv.translate(padding, out.getHeight() - (180 * ratio));
        sl.draw(cv);
        cv.restore();

        // 3. Proses Simpan ke Galeri
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
                Toast.makeText(this, "Berhasil!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } catch (Exception e) {}
    }
}
