package com.odfiz.timemark;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditorActivity extends Activity {
    private ImageView imgView;
    private Bitmap baseBmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        
        imgView = findViewById(R.id.imageViewEditor);
        TextView tvTime = findViewById(R.id.tvTime);
        TextView tvAddr = findViewById(R.id.tvAddress);
        SharedPreferences prefs = getSharedPreferences("OdfizPrefs", MODE_PRIVATE);

        // 1. Ambil Foto
        String uriStr = getIntent().getStringExtra("PHOTO_URI");
        if (uriStr != null) {
            try {
                // Pakai sample size 2 biar HP nggak berat
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inSampleSize = 2;
                baseBmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(uriStr)), null, opt);
                imgView.setImageBitmap(baseBmp);
            } catch (Exception e) { finish(); }
        }

        // 2. Set Teks (Ambil yang ada saja)
        tvTime.setText(new SimpleDateFormat("HH:mm | dd-MM-yyyy", Locale.getDefault()).format(new Date()));
        tvAddr.setText(prefs.getString("last_addr", "Lokasi Terdeteksi"));

        // 3. Tombol Batal
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        // 4. Tombol Simpan (Murni simpan apa adanya)
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            if (baseBmp == null) return;
            saveToGallery();
        });
    }

    private void saveToGallery() {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "Odfiz_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");
        try {
            Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (u != null) {
                OutputStream os = getContentResolver().openOutputStream(u);
                baseBmp.compress(Bitmap.CompressFormat.JPEG, 90, os);
                os.close();
                Toast.makeText(this, "Berhasil Simpan!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {}
    }
}
