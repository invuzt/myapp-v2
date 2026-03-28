package com.odfiz.timemark;

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
    private static final int PERM_CODE = 100;
    private static final int REQ_CAM = 1888;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPerms();
    }

    private void checkPerms() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.CAMERA, 
                Manifest.permission.ACCESS_FINE_LOCATION
            }, PERM_CODE);
        } else {
            openCam();
        }
    }

    private void openCam() {
        Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File f = File.createTempFile("odf_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            photoUri = FileProvider.getUriForFile(this, "com.odfiz.timemark.fileprovider", f);
            it.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(it, REQ_CAM);
        } catch (IOException e) { finish(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_CAM && res == RESULT_OK) {
            Intent it = new Intent(this, EditorActivity.class);
            it.putExtra("PHOTO_URI", photoUri.toString());
            startActivity(it);
            finish();
        } else { finish(); }
    }
}
