package me.kazaf.camerakit.core;

import me.kazaf.camerakit.config.ICameraConfig;

/**
 * Created by kazaf on 2018/1/4.
 */

public interface ICameraPresenter {

    void setUserActionCallback(UserActionCallback view);

    void setOnPictureCapturedCallback(OnPictureCapturedCallback pictureCapturedCallback);

    void startPreview();

    void stopPreview();

    void releaseCamera();

    ICameraConfig getConfig();


}
