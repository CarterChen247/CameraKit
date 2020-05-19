package com.carterchen247.camerademo;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import java.io.File;

import com.carterchen247.camerakit.core.CameraPresenter;
import com.carterchen247.camerakit.core.UserActionCallback;

import com.carterchen247.camerakit.config.CameraConfig;
import com.carterchen247.camerakit.core.OnPictureCapturedCallback;
import com.carterchen247.camerakit.util.ImageUtil;

/**
 * Created by kazaf on 2018/1/4.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener, UserActionCallback, OnPictureCapturedCallback {

    private final String TAG = MainActivity.class.getSimpleName();

    ImageButton facing;
    ImageButton flash;
    ImageButton capture;

    CameraPresenter cameraPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        facing = (ImageButton) findViewById(R.id.facing);
        flash = (ImageButton) findViewById(R.id.flash);
        capture = (ImageButton) findViewById(R.id.capture);
        final RelativeLayout preview = (RelativeLayout) findViewById(R.id.rootFrame);
        facing.setOnClickListener(this);
        flash.setOnClickListener(this);
        capture.setOnClickListener(this);
        preview.setOnClickListener(this);

        CameraConfig config = new CameraConfig();
        cameraPresenter = new CameraPresenter(this, preview, config);
        cameraPresenter.forceFullScreenPreview(false);
        cameraPresenter.setUserActionCallback(this);
        cameraPresenter.setOnPictureCapturedCallback(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraPresenter.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraPresenter.stopPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraPresenter.releaseCamera();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.facing:
                cameraPresenter.toggleCameraPosition();
                break;
            case R.id.flash:
                cameraPresenter.toggleFlashMode();
                break;
            case R.id.capture:
                cameraPresenter.capture();
                break;
            case R.id.rootFrame:
                cameraPresenter.focus();
        }
    }

    @Override
    public void onUsingFrontCamera() {
        facing.setImageResource(R.drawable.icon_camera_front);
    }

    @Override
    public void onUsingBackCamera() {
        facing.setImageResource(R.drawable.icon_camera_back);
    }

    @Override
    public void onUsingFlashOnMode() {
        flash.setImageResource(R.drawable.icon_flash_on);
    }

    @Override
    public void onUsingFlashOffMode() {
        flash.setImageResource(R.drawable.icon_flash_off);
    }

    @Override
    public void onUsingFlashAutoMode() {
        flash.setImageResource(R.drawable.icon_flash_auto);
    }

    @Override
    public void onCaptureButtonShouldLock() {

    }

    @Override
    public void onCaptureButtonShouldUnlock() {

    }

    @Override
    public void onPictureCaptured(byte[] data, final int cameraPosition) {

        final File outputFile = ImageUtil.makeTempFile(this, ImageUtil.createSaveDir("photo"), "camera", ".jpg");

        ImageUtil.saveToDiskAsync(data, outputFile, new ImageUtil.ICallback() {
            @Override
            public void done(Exception exception) {
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra(Constant.FILE, outputFile.getAbsolutePath());
                intent.putExtra(Constant.IS_FRONT_CAMERA, cameraPosition);
                startActivity(intent);
            }
        });

    }

}
