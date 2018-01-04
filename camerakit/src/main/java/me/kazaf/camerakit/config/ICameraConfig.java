package me.kazaf.camerakit.config;

import java.util.List;

/**
 * Created by kazaf on 2018/1/4.
 */

public interface ICameraConfig {

    Integer getBackCamera();

    Integer getFrontCamera();

    void setFrontCamera(int camera);

    void setBackCamera(int camera);

    void setCameraPosition(int position);

    int getCameraPosition();

    int getCurrentCameraId();

    void toggleCameraPosition();

    void toggleFlashMode();

    void setFlashModes(List<Integer> modes);

    int getFlashMode();

}
