package com.carterchen247.camerakit.core;

/**
 * Created by kazaf on 2018/1/4.
 */

public interface UserActionCallback {

    void onUsingFrontCamera();

    void onUsingBackCamera();

    void onUsingFlashOnMode();

    void onUsingFlashOffMode();

    void onUsingFlashAutoMode();

    void onCaptureButtonShouldLock();

    void onCaptureButtonShouldUnlock();


}
