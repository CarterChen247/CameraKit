package com.carterchen247.camerakit.core;

import android.app.Activity;
import android.graphics.Point;
import android.hardware.Camera;
import androidx.annotation.NonNull;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import java.util.List;

import com.carterchen247.camerakit.util.CameraLogger;
import com.carterchen247.camerakit.util.CameraUtil;
import com.carterchen247.camerakit.config.CameraConstant;
import com.carterchen247.camerakit.config.ICameraConfig;
import com.carterchen247.camerakit.util.Degrees;


/**
 * Created by kazaf on 2018/1/4.
 */

public class CameraPresenter implements ICameraPresenter, ICameraAction {

    private final String TAG = CameraPresenter.class.getSimpleName();

    private Camera camera;
    private Activity activity;
    private RelativeLayout container;
    private Point containerSize;

    private int displayOrientation;
    private boolean isAutoFocusing;

    private CameraTexturePreview preview;
    private ICameraConfig config;
    private UserActionCallback view;
    private OnPictureCapturedCallback pictureCapturedCallback;

    private boolean isFullScreenPreviewForced;

    public CameraPresenter(Activity activity, RelativeLayout container, ICameraConfig config) {
        this.activity = activity;
        this.container = container;
        this.config = config;
    }

    @Override
    public ICameraConfig getConfig() {
        return config;
    }

    @Override
    public void setUserActionCallback(UserActionCallback view) {
        this.view = view;
    }

    @Override
    public void setOnPictureCapturedCallback(OnPictureCapturedCallback pictureCapturedCallback) {
        this.pictureCapturedCallback = pictureCapturedCallback;
    }

    @Override
    public void startPreview() {
        openCamera();
    }

    public void openCamera() {
        if (null == activity || activity.isFinishing()) return;
        try {
            final int mBackCameraId = config.getBackCamera() != null ? config.getBackCamera() : -1;
            final int mFrontCameraId = config.getFrontCamera() != null ? config.getFrontCamera() : -1;
            if (mBackCameraId == -1 || mFrontCameraId == -1) {
                int numberOfCameras = Camera.getNumberOfCameras();
                if (numberOfCameras == 0) {
                    throwError(new Exception("No cameras are available on this config."));
                    return;
                }

                for (int i = 0; i < numberOfCameras; i++) {
                    //noinspection ConstantConditions
                    if (mFrontCameraId != -1 && mBackCameraId != -1) break;
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mFrontCameraId == -1) {
                        config.setFrontCamera(i);
                    } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK && mBackCameraId == -1) {
                        config.setBackCamera(i);
                    }
                }
            }

            if (view != null) {
                if (config.getCameraPosition() == CameraConstant.CAMERA_POSITION_FRONT) {
                    view.onUsingFrontCamera();
                } else if (config.getCameraPosition() == CameraConstant.CAMERA_POSITION_BACK) {
                    view.onUsingBackCamera();
                }
            }

            final int toOpen = config.getCurrentCameraId();
            camera = Camera.open(toOpen == -1 ? 0 : toOpen);
            onCameraOpened();

        } catch (IllegalStateException e) {
            CameraLogger.log("exception", e.toString());
            throwError(new Exception("Cannot access the camera.", e));
        } catch (RuntimeException e2) {
            CameraLogger.log("exception", e2.toString());
            throwError(new Exception("Cannot access the camera, you may need to restart your config.", e2));
        }
    }

    private void onCameraOpened() {
        if (containerSize == null) {
            createPreviewAutomatically();
        } else {
            createPreview();
        }
    }

    private void createPreview() {

        Camera.Parameters parameters = camera.getParameters();

        Camera.Size previewSize = getPreviewSize(parameters);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        Camera.Size mStillShotSize = getPictureSize(parameters, previewSize);
        parameters.setPictureSize(mStillShotSize.width, mStillShotSize.height);

        setupCameraDisplayOrientation(parameters);

        camera.setParameters(parameters);

        List<Integer> mFlashModes = CameraUtil.getSupportedFlashModes(activity, parameters);
        config.setFlashModes(mFlashModes);
        setupFlashMode();

        initPreviewView();
    }

    private Camera.Size getPreviewSize(Camera.Parameters parameters) {
        Camera.Size previewSize = CameraUtil.chooseOptimalSize(parameters.getSupportedPreviewSizes(), containerSize.x, containerSize.y);
        if (previewSize == null) {
            previewSize = CameraUtil.chooseNearestSize(parameters.getSupportedPreviewSizes(), containerSize.x, containerSize.y);
            if (previewSize == null) {
                throw new RuntimeException("Couldn't find any suitable preview size");
            } else {
                if (containerSize == null) {
                    containerSize = new Point(previewSize.height, previewSize.width);
                } else {
                    containerSize.x = previewSize.height;
                    containerSize.y = previewSize.width;
                }
            }
        }
        CameraLogger.log(TAG, "previewSize=" + previewSize.width + "," + previewSize.height);
        return previewSize;
    }

    private Camera.Size getPictureSize(Camera.Parameters parameters, Camera.Size previewSize) {
        Camera.Size mStillShotSize = CameraUtil.getHighestSupportedStillShotSize(parameters.getSupportedPictureSizes(), previewSize);
        if (mStillShotSize == null) {
            throw new RuntimeException("Couldn't find any suitable picture size");
        }
        return mStillShotSize;
    }

    private void setupCameraDisplayOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(config.getCurrentCameraId(), info);
        final int deviceOrientation = Degrees.getDisplayRotation(activity);
        displayOrientation = Degrees.getDisplayOrientation(
                info.orientation,
                deviceOrientation,
                info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
        );
        CameraLogger.log("CameraFragment", String.format("Orientations: Sensor = %d˚, CameraConfig = %d˚, Display = %d˚", info.orientation, deviceOrientation, displayOrientation));

        int previewOrientation;
        int jpegOrientation;
        jpegOrientation = previewOrientation = displayOrientation;

        if (Degrees.isPortrait(deviceOrientation) && config.getCameraPosition() == CameraConstant.CAMERA_POSITION_FRONT)
            previewOrientation = Degrees.mirror(displayOrientation);

        parameters.setRotation(jpegOrientation);
        camera.setDisplayOrientation(previewOrientation);
    }

    private void setupFlashMode() {
        if (config.getCameraPosition() == CameraConstant.CAMERA_POSITION_FRONT) {
            return;
        }
        String flashMode = null;
        switch (config.getFlashMode()) {
            case CameraConstant.FLASH_MODE_AUTO:
                flashMode = Camera.Parameters.FLASH_MODE_AUTO;
                if (view != null) {
                    view.onUsingFlashAutoMode();
                }
                break;
            case CameraConstant.FLASH_MODE_ALWAYS_ON:
                flashMode = Camera.Parameters.FLASH_MODE_ON;
                if (view != null) {
                    view.onUsingFlashOnMode();
                }
                break;
            case CameraConstant.FLASH_MODE_OFF:
                flashMode = Camera.Parameters.FLASH_MODE_OFF;
                if (view != null) {
                    view.onUsingFlashOffMode();
                }
                break;
            default:
                break;
        }
        if (flashMode != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(flashMode);
            camera.setParameters(parameters);
        }
    }


    private void initPreviewView() {
        if (activity == null) return;
        preview = new CameraTexturePreview(activity, camera);
        if (container.getChildCount() > 0 && container.getChildAt(0) instanceof CameraTexturePreview)
            container.removeViewAt(0);
        container.addView(preview, 0);

        if (!isFullScreenPreviewForced) {
            preview.setAspectRatio(containerSize.x, containerSize.y);
        } else {
            Point screenSize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(screenSize);
            preview.setAspectRatio(screenSize.x, screenSize.y);
        }
    }

    private void createPreviewAutomatically() {
        container.getViewTreeObserver().addOnGlobalLayoutListener(getContainerSizeListener());
    }

    @Override
    public void stopPreview() {
        closeCamera();
    }

    @Override
    public void releaseCamera() {
        try {
            preview.getSurfaceTexture().release();
        } catch (Throwable ignored) {

        }
        container = null;
    }

    @Override
    public void toggleCameraPosition() {
        config.toggleCameraPosition();
        reopenCamera();
    }

    @Override
    public void toggleFlashMode() {
        config.toggleFlashMode();
        setupFlashMode();
    }

    @Override
    public void focus() {
        if (camera == null || isAutoFocusing) return;
        try {
            isAutoFocusing = true;
            camera.cancelAutoFocus();
            camera.autoFocus(
                    new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            isAutoFocusing = false;
                            if (!success)
                                CameraLogger.log(TAG, "Unable to auto-focus!");
                        }
                    });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void capture() {
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        //            // We could have configurable shutter sound here
        //            camera.enableShutterSound(false);
        //        }
        try {

            if (view != null) {
                view.onCaptureButtonShouldLock();
            }
            camera.takePicture(createShutterCallback(), createRawCallback(), createJpegCallback());

        } catch (Exception ex) {

            // error

        }
    }

    @NonNull
    private Camera.PictureCallback createJpegCallback() {
        return new Camera.PictureCallback() {
            public void onPictureTaken(final byte[] data, Camera camera) {
                if (view != null) {
                    view.onCaptureButtonShouldUnlock();
                }
                if (pictureCapturedCallback != null) {
                    pictureCapturedCallback.onPictureCaptured(data, config.getCameraPosition());
                }
            }
        };
    }

    @NonNull
    private Camera.PictureCallback createRawCallback() {
        return new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                //CameraLogger.log(TAG, "onPictureTaken - raw. Raw is null: " + (data == null));
            }
        };
    }

    @NonNull
    private Camera.ShutterCallback createShutterCallback() {
        return new Camera.ShutterCallback() {
            public void onShutter() {
                //CameraLogger.log(TAG, "onShutter'd");
            }
        };
    }

    private void throwError(Exception e) {
        CameraLogger.log(TAG, "error=" + e.toString());
    }

    private void reopenCamera() {
        closeCamera();
        openCamera();
    }

    private void closeCamera() {
        try {
            if (camera != null) {
                try {
                    camera.lock();
                } catch (Throwable ignored) {
                }
                camera.release();
                camera = null;
            }
        } catch (IllegalStateException e) {
            throwError(new Exception("Illegal state while trying to close camera.", e));
        }
    }

    private ViewTreeObserver.OnGlobalLayoutListener getContainerSizeListener() {
        return new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                containerSize = new Point(container.getWidth(), container.getHeight());
                createPreview();
                container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        };
    }


    public void forceFullScreenPreview(boolean isFullScreenPreviewForced) {
        this.isFullScreenPreviewForced = isFullScreenPreviewForced;
    }
}
