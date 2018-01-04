package me.kazaf.camerakit.core;

/**
 * Created by kazaf on 2018/1/4.
 */

public interface ICameraViewCallback {

    void onUsingFrontCamera();

    void onUsingBackCamera();

    void onUsingFlashOnMode();

    void onUsingFlashOffMode();

    void onUsingFlashAutoMode();

    void onCaptureButtonShouldLock();

    void onCaptureButtonShouldUnlock();


}
