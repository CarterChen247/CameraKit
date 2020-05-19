package com.carterchen247.camerakit.config;

import java.util.List;

/**
 * Created by kazaf on 2018/1/4.
 */

public class CameraConfig implements ICameraConfig {

    private Integer idCameraFront, idCameraBack;
    private Integer positionCameraCurrent = CameraConstant.CAMERA_POSITION_BACK;

    private List<Integer> mFlashModes;
    private Integer mFlashMode = CameraConstant.FLASH_MODE_OFF;


    @Override
    public void setFrontCamera(int camera) {
        idCameraFront = camera;
    }

    @Override
    public Integer getFrontCamera() {
        return idCameraFront;
    }

    @Override
    public void setBackCamera(int camera) {
        idCameraBack = camera;
    }
    @Override
    public Integer getBackCamera() {
        return idCameraBack;
    }

    @Override
    public void setCameraPosition(int position) {
        positionCameraCurrent = position;
    }

    @Override
    public int getCameraPosition() {
        return positionCameraCurrent;
    }

    @Override
    public int getCurrentCameraId() {
        if (positionCameraCurrent== CameraConstant.CAMERA_POSITION_FRONT){
            return idCameraFront;
        }else{
            return idCameraBack;
        }
    }

    @Override
    public void toggleCameraPosition() {
        if (getCameraPosition() == CameraConstant.CAMERA_POSITION_FRONT) {
            // Front, go to back if possible
            if (getBackCamera() != null) setCameraPosition(CameraConstant.CAMERA_POSITION_BACK);
        } else {
            // Back, go to front if possible
            if (getFrontCamera() != null) setCameraPosition(CameraConstant.CAMERA_POSITION_FRONT);
        }
    }

    @Override
    public void toggleFlashMode() {
        if (mFlashModes != null) {
            mFlashMode = mFlashModes.get((mFlashModes.indexOf(mFlashMode) + 1) % mFlashModes.size());
        }
    }

    @Override
    public void setFlashModes(List<Integer> modes) {
        mFlashModes = modes;
    }

    @Override
    public int getFlashMode() {
        return mFlashMode;
    }




}
