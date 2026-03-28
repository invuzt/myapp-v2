package com.myapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;

    // --- EDIT WATERMARK DI SINI ---
    private static final String WATERMARK_TEXT = "odfiz";
    // -----------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView1);
        Button photoButton = findViewById(R.id.button1);

        photoButton.setOnClickListener(v -> {
            // Cek Izin Otomatis
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                bukaKamera();
            }
        });
    }

    private void bukaKamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin diberikan", Toast.LENGTH_LONG).show();
                bukaKamera();
            } else {
                Toast.makeText(this, "Izin ditolak", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            
            // Tambahkan Watermark
            Bitmap watermarkedPhoto = addWatermark(photo, WATERMARK_TEXT);
            
            // Langsung View
            imageView.setImageBitmap(watermarkedPhoto);
            
            Toast.makeText(this, "Foto berhasil diambil dengan Watermark", Toast.LENGTH_SHORT).show();
        }
    }

    // Fungsi untuk menambahkan watermark teks
    private Bitmap addWatermark(Bitmap src, String watermark) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE); // Warna teks watermark
        paint.setAlpha(150); // Transparansi (0-255)
        paint.setTextSize(h / 15); // Ukuran teks proporsional dengan tinggi foto
        paint.setAntiAlias(true);
        paint.setUnderlineText(false);
        
        // Posisi watermark (sudut kanan bawah dengan padding)
        float x = w - paint.measureText(watermark) - (w / 20);
        float y = h - (h / 20);
        
        canvas.drawText(watermark, x, y, paint);
        
        return result;
    }
}
