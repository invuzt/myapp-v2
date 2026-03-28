package com.myapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Langsung cek izin saat app dibuka
        checkPermissions();
    }

    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA, 
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };
        
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, PERMISSION_CODE);
        } else {
            bukaKamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bukaKamera();
        } else {
            finish(); // Tutup app jika izin ditolak
        }
    }

    private void bukaKamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = File.createTempFile("img_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            photoUri = FileProvider.getUriForFile(this, "com.myapp.fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, CAMERA_REQUEST);
        } catch (IOException e) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                // Jika foto sukses, langsung ke Editor (Watermark otomatis muncul di sana)
                Intent intent = new Intent(this, EditorActivity.class);
                intent.putExtra("PHOTO_URI", photoUri.toString());
                startActivity(intent);
                finish(); // Tutup MainActivity agar tidak balik ke layar kosong
            } else {
                finish(); // Jika batal foto, tutup app
            }
        }
    }
}
