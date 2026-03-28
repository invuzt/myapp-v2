package com.myapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
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

        // Ambil URI Foto dari Intent
        String uriString = getIntent().getStringExtra("PHOTO_URI");
        if (uriString != null) {
            Uri photoUri = Uri.parse(uriString);
            try {
                // Load foto asli
                originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                imageView.setImageBitmap(originalBitmap);
            } catch (IOException e) { e.printStackTrace(); }
        }

        // Logika Edit Teks Real-time
        editWatermark.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) watermarkOverlay.setText(s);
                else watermarkOverlay.setText("odfiz"); // Default jika kosong
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> processAndSave());
    }

    private void processAndSave() {
        if (originalBitmap == null) return;
        
        // Buat bitmap baru yang bisa diedit
        Bitmap finalBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
        Canvas canvas = new Canvas(finalBitmap);
        
        // Ambil teks dari TextView overlay
        String textToDraw = watermarkOverlay.getText().toString();

        Paint paint = new Paint();
        paint.setColor(Color.WHITE); // Warna teks
        paint.setAlpha(180); // Transparansi
        paint.setTextSize(finalBitmap.getHeight() / 15); // Ukuran proporsional
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
        
        // Hitung posisi (Kanan Bawah dengan padding)
        float textWidth = paint.measureText(textToDraw);
        float x = finalBitmap.getWidth() - textWidth - (finalBitmap.getWidth() / 20);
        float y = finalBitmap.getHeight() - (finalBitmap.getHeight() / 20);
        
        // Gambar teks ke foto
        canvas.drawText(textToDraw, x, y, paint);

        // SIMPAN KE GALERI (DCIM/Camera)
        simpanKeGaleri(finalBitmap);
    }

    private void simpanKeGaleri(Bitmap bitmap) {
        OutputStream fos;
        try {
            // Gunakan MediaStore untuk Android modern (API 29+)
            Uri imageUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            startActivity(Intent.createChooser(intent, "Bagikan Foto Ber-watermark"));
            finish(); // Tutup editor setelah selesai
        } catch (Exception e) {
            Toast.makeText(this, "Gagal memproses foto", Toast.LENGTH_SHORT).show();
        }
    }
}
