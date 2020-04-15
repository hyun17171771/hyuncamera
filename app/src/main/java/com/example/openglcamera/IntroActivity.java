package com.example.openglcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class IntroActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = IntroActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_CODE = 100;

    String[] REQUIRED_PERMISSION = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Handler mHandler;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        mContext = this;
        mHandler = new Handler();

        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED &&
                cameraPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "## checkPermission - true");

            goToMain();

        } else {


            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, PERMISSION_REQUEST_CODE);

        }

    }

    private void goToMain() {

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();

            }
        }, 700);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grandResults) {

        if (requestCode == PERMISSION_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSION.length) {

            boolean checkResult = true;

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    checkResult = false;
                    break;
                }
            }


            if (checkResult) {
                goToMain();
            } else {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSION[0]) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSION[1])) {

                    Toast.makeText(mContext, "권한을 허용한 경우에만 앱 이용이 가능합니다", Toast.LENGTH_SHORT).show();
                    finish();

                } else {

                    Toast.makeText(mContext, "권한이 거부되었습니다. 설정에서 권한을 허용해야 합니다", Toast.LENGTH_SHORT).show();
                    finish();

                }

            }

        }

    }
}
