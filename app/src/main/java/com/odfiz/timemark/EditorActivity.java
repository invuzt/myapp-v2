package com.odfiz.timemark;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import java.io.*;

public class EditorActivity extends Activity {
    static {
        try { System.loadLibrary("odfiz_native"); } catch (UnsatisfiedLinkError e) {}
    }

    // Fungsi sakti untuk kirim data mentah ke Rust
    public native boolean processPhotoNative(byte[] data);
    public native String helloFromRust();

    private ImageView imgView;
    private Bitmap baseBmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        
        imgView = findViewById(R.id.imageViewEditor);
        loadFastData();

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            // Panggil mesin Rust untuk olah cepat
            new Thread(() -> {
                // Simulasi kirim data mentah ke Rust
                // processPhotoNative(rawByteData); 
                runOnUiThread(() -> Toast.makeText(this, "Olah Rust Selesai!", Toast.LENGTH_SHORT).show());
            }).start();
        });
    }

    private void loadFastData() {
        String uriStr = getIntent().getStringExtra("PHOTO_URI");
        try {
            // Pakai optimasi inSampleSize agar load preview tidak lag
            InputStream is = getContentResolver().openInputStream(Uri.parse(uriStr));
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 2; // Perkecil preview biar RAM aman
            baseBmp = BitmapFactory.decodeStream(is, null, opt);
            imgView.setImageBitmap(baseBmp);
        } catch (Exception e) { finish(); }
    }

    private void restartApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
