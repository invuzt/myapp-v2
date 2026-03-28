package com.myapp;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;

public class EditorActivity extends Activity {
    private ImageView imageView;
    private TextView watermarkOverlay;
    private EditText editWatermark;
    private Bitmap originalBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        imageView = findViewById(R.id.imageViewEditor);
        watermarkOverlay = findViewById(R.id.watermarkOverlay);
        editWatermark = findViewById(R.id.editWatermarkText);

        String uriString = getIntent().getStringExtra("PHOTO_URI");
        if (uriString != null) {
            try {
                Bitmap raw = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(uriString));
                // Perbaiki rotasi 90 derajat jika miring
                if (raw.getWidth() > raw.getHeight()) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    originalBitmap = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), matrix, true);
                } else {
                    originalBitmap = raw;
                }
                imageView.setImageBitmap(originalBitmap);
            } catch (Exception e) { e.printStackTrace(); }
        }

        editWatermark.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                watermarkOverlay.setText(s.length() > 0 ? s : "odfiz");
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> processAndSave());
    }

    private void processAndSave() {
        if (originalBitmap == null) return;
        
        // 1. Buat salinan Bitmap untuk digambar watermark secara permanen
        Bitmap workingBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(workingBitmap);
        
        String text = watermarkOverlay.getText().toString();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setAlpha(200);
        // Ukuran teks proporsional terhadap resolusi asli foto
        paint.setTextSize(workingBitmap.getHeight() / 20); 
        paint.setShadowLayer(5f, 0f, 0f, Color.BLACK);

        float x = workingBitmap.getWidth() - paint.measureText(text) - 40;
        float y = workingBitmap.getHeight() - 40;
        
        canvas.drawText(text, x, y, paint);

        // 2. Simpan ke Galeri (DCIM/Camera)
        saveToGallery(workingBitmap);
    }

    private void saveToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Odfiz_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        
        try {
            if (uri != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.close();
                Toast.makeText(this, "Foto tersimpan di Galeri!", Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal simpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
