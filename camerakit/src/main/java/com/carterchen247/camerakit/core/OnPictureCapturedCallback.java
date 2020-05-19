package com.carterchen247.camerakit.core;

/**
 * Created by kazaf on 2018/1/5.
 */

public interface OnPictureCapturedCallback {

    void onPictureCaptured(byte[] data, int cameraPosition);
}
