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
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditorActivity extends Activity {
    private ImageView imgView;
    private TextView tvTime, tvAddress;
    private Bitmap baseBmp;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        
        prefs = getSharedPreferences("OdfizPrefs", MODE_PRIVATE);
        imgView = findViewById(R.id.imageViewEditor);
        tvTime = findViewById(R.id.tvTime);
        tvAddress = findViewById(R.id.tvAddress);

        // Load foto standar (tanpa aneh-aneh)
        String uriStr = getIntent().getStringExtra("PHOTO_URI");
        if (uriStr != null) {
            try {
                baseBmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(uriStr)));
                imgView.setImageBitmap(baseBmp);
            } catch (Exception e) { finish(); }
        }

        // Tampilkan waktu & lokasi terakhir yang tersimpan
        String time = new SimpleDateFormat("HH:mm | dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String addr = prefs.getString("last_addr", "Lokasi...");
        tvTime.setText(time);
        tvAddress.setText(addr);

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveFile());
    }

    private void saveFile() {
        if (baseBmp == null) return;
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
                Toast.makeText(this, "Tersimpan!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {}
    }
}
