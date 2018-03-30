package me.kazaf.camerakit.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.kazaf.camerakit.config.CameraConstant;
import me.kazaf.camerakit.core.CameraPresenter;

/**
 * Created by kazaf on 2018/1/4.
 */

public class CameraUtil {

    public static List<Integer> getSupportedFlashModes(
            Context context, Camera.Parameters parameters) {
        //check has system feature for flash
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            List<String> modes = parameters.getSupportedFlashModes();
            if (modes == null
                    || (modes.size() == 1 && modes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF))) {
                return null; //not supported
            } else {
                ArrayList<Integer> flashModes = new ArrayList<>();
                for (String mode : modes) {
                    switch (mode) {
                        case Camera.Parameters.FLASH_MODE_AUTO:
                            if (!flashModes.contains(CameraConstant.FLASH_MODE_AUTO))
                                flashModes.add(CameraConstant.FLASH_MODE_AUTO);
                            break;
                        case Camera.Parameters.FLASH_MODE_ON:
                            if (!flashModes.contains(CameraConstant.FLASH_MODE_ALWAYS_ON))
                                flashModes.add(CameraConstant.FLASH_MODE_ALWAYS_ON);
                            break;
                        case Camera.Parameters.FLASH_MODE_OFF:
                            if (!flashModes.contains(CameraConstant.FLASH_MODE_OFF))
                                flashModes.add(CameraConstant.FLASH_MODE_OFF);
                            break;
                        default:
                            break;
                    }
                }
                return flashModes;
            }
        } else {
            return null; //not supported
        }
    }

    public static class CompareSizesByArea implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height - (long) rhs.width * rhs.height);
        }
    }

    public static class CompareSizesByAreaDesc implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            return Long.signum(- (long) lhs.width * lhs.height +  (long) rhs.width * rhs.height);
        }

    }

    public static Camera.Size getHighestSupportedStillShotSize(List<Camera.Size> supportedPictureSizes, Camera.Size sizePreview) {
        double aspectRatioPreview = (double) sizePreview.height / sizePreview.width;

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        List<Camera.Size> alternatives = new ArrayList<>();

        for (Camera.Size sizePicture : supportedPictureSizes) {
            double aspectRatioPicture = (double) sizePicture.height / sizePicture.width;
            Log.e("camera", "preview, picture=" + aspectRatioPreview + "," + aspectRatioPreview);
            if (aspectRatioPicture == aspectRatioPreview && sizePicture.width >= sizePreview.width && sizePicture.height >= sizePreview.height) {
                bigEnough.add(sizePicture);
                Log.e("camera", "add picture size=" + sizePicture.height + "," + sizePicture.width);
            } else {
                alternatives.add(sizePicture);
            }
        }

        // Pick the biggest of those, assuming we found any
        if (bigEnough.size() > 0) {
            Camera.Size maxSize = Collections.max(bigEnough, new CameraUtil.CompareSizesByArea());
            Log.d("CameraFragment", "Using resolution: " + maxSize.width + "x" + maxSize.height);
            return maxSize;

        } else {
            Log.e(CameraUtil.class.getSimpleName(), "Couldn't find any suitable picture size");

            // do comparison from big to small
            Collections.sort(alternatives,  new CameraUtil.CompareSizesByAreaDesc());

            if (alternatives.size() == 0) {

                // unfortunately don't expect to see it, will solve it in future
                return null;

            } else {

                Camera.Size aspectRatioNearest;

                if (alternatives.size() == 1) {
                    aspectRatioNearest = alternatives.get(0);
                } else {

                    // assume the 1st element is the nearest one
                    aspectRatioNearest = alternatives.get(0);
                    double distanceEucilidean = Math.abs(aspectRatioPreview - ((double) aspectRatioNearest.height / aspectRatioNearest.width));

                    for (int i = 1; i < alternatives.size(); i++) {

                        Camera.Size candidate = alternatives.get(i);
                        double aspectRatio = ((double) candidate.height / candidate.width);
                        double distanceCompare = Math.abs(aspectRatioPreview - aspectRatio);

                        if (distanceCompare < distanceEucilidean) {

                            // update most relevant one
                            distanceEucilidean = distanceCompare;
                            aspectRatioNearest = candidate;

                        }
                    }
                }

                Log.e(CameraUtil.class.getSimpleName(), " Use alternative size height=" + aspectRatioNearest.height + ", width=" + aspectRatioNearest.width);
                return aspectRatioNearest;
            }
        }
    }

    public static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int cameraHeight, int cameraWidth) {

        double aspectRatioFrame = (double) cameraHeight / cameraWidth;

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        List<Camera.Size> notBigEnough = new ArrayList<>();

        for (Camera.Size option : choices) {
            double aspectRatioPreview = (double) option.height / option.width;
            Log.e("camera", "frame, preview=" + aspectRatioFrame + "," + aspectRatioPreview);
            Log.e("camera", "preview=" + option.height + "," + option.width);

            if (aspectRatioPreview != aspectRatioFrame) {
                continue;
            }
            if (option.width >= cameraWidth && option.height >= cameraHeight) {
                bigEnough.add(option);
            } else {
                notBigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CameraUtil.CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CameraUtil.CompareSizesByArea());
        } else {
            Log.e(CameraPresenter.class.getSimpleName(), "Couldn't find any suitable preview size");
            return null;
        }
    }

    public static Camera.Size chooseNearestSize(List<Camera.Size> choices, int cameraHeight, int cameraWidth) {

        double aspectRatioFrame = (double) cameraHeight / cameraWidth;

        // find nearest aspect ratio
        double aspectRatioNearest = (double) choices.get(0).height / choices.get(0).width;
        double distanceNearest = Math.abs(aspectRatioFrame - aspectRatioNearest);
        for (Camera.Size option : choices) {
            double aspectRatioPreview = (double) option.height / option.width;
            double distance = Math.abs(aspectRatioFrame - aspectRatioPreview);
            if (distance < distanceNearest) {
                distanceNearest = distance;
                aspectRatioNearest = aspectRatioPreview;
            }
        }

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        List<Camera.Size> notBigEnough = new ArrayList<>();

        for (Camera.Size option : choices) {
            double aspectRatioPreview = (double) option.height / option.width;
            Log.e("camera", "frame, preview=" + aspectRatioFrame + "," + aspectRatioPreview);
            Log.e("camera", "preview=" + option.height + "," + option.width);

            if (aspectRatioPreview != aspectRatioNearest) {
                continue;
            }
            if (option.width >= cameraWidth && option.height >= cameraHeight) {
                bigEnough.add(option);
            } else {
                notBigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CameraUtil.CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CameraUtil.CompareSizesByArea());
        } else {
            Log.e(CameraPresenter.class.getSimpleName(), "Couldn't find any suitable preview size");
            return null;
        }
    }
}
