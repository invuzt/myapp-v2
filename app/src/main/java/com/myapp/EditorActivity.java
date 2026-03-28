package com.myapp;

import android.app.Activity;
import android.content.ContentValues;
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

        loadPhoto();
        generateAutomaticMetadata();

        editWatermark.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                watermarkOverlay.setText(s.length() > 0 ? s : "");
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
        String waktu = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        String infoGps = "GPS: Menentukan lokasi...";
        
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                double lat = loc.getLatitude();
                double lon = loc.getLongitude();
                infoGps = String.format("Lat: %.5f, Lon: %.5f", lat, lon);
                
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    infoGps += "\n" + addresses.get(0).getAddressLine(0);
                }
            }
        } catch (Exception e) { infoGps = "GPS tidak tersedia"; }

        String fullText = waktu + "\n" + infoGps + "\nodfiz";
        editWatermark.setText(fullText);
        watermarkOverlay.setText(fullText);
    }

    private void processAndSave() {
        if (originalBitmap == null) return;
        Bitmap finalBmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(finalBmp);
        
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(finalBmp.getHeight() / 35); // Lebih kecil karena teks banyak
        paint.setShadowLayer(10f, 0f, 0f, Color.BLACK);

        String[] lines = watermarkOverlay.getText().toString().split("\n");
        float y = finalBmp.getHeight() - (lines.length * paint.getTextSize()) - 60;
        
        for (String line : lines) {
            float x = finalBmp.getWidth() - paint.measureText(line) - 50;
            canvas.drawText(line, x, y, paint);
            y += paint.getTextSize() + 10;
        }

        saveImage(finalBmp);
    }

    private void saveImage(Bitmap bmp) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "Timemark_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        try {
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri != null) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, os);
                os.close();
                Toast.makeText(this, "Tersimpan di DCIM/Camera", Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (Exception e) { Toast.makeText(this, "Error simpan", Toast.LENGTH_SHORT).show(); }
    }
}
