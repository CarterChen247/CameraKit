package com.carterchen247.camerakit.core

import android.app.Activity
import android.graphics.Point
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PictureCallback
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import com.carterchen247.camerakit.config.CameraConstant
import com.carterchen247.camerakit.config.ICameraConfig
import com.carterchen247.camerakit.core.CameraTexturePreview.Companion.newInstance
import com.carterchen247.camerakit.util.CameraLogger
import com.carterchen247.camerakit.util.CameraUtil
import com.carterchen247.camerakit.util.Degrees

class CameraPresenter(
    private val activity: Activity,
    val container: RelativeLayout,
    private val config: ICameraConfig
) : ICameraPresenter, ICameraAction {
    private var view: UserActionCallback? = null
    private var pictureCapturedCallback: OnPictureCapturedCallback? = null
    private var containerSize: Point? = null
    private var camera: Camera? = null
    private var displayOrientation = 0 // Check
    private var preview: CameraTexturePreview? = null
    private var isFullScreenPreviewForced = false
    private var isAutoFocusing = false


    override fun setUserActionCallback(view: UserActionCallback) {
        this.view = view
    }

    override fun setOnPictureCapturedCallback(pictureCapturedCallback: OnPictureCapturedCallback) {
        this.pictureCapturedCallback = pictureCapturedCallback
    }

    override fun startPreview() {
        openCamera()
    }

    private fun openCamera() {
        if (activity.isFinishing) return
        try {
            val mBackCameraId = if (config.backCamera != null) config.backCamera else -1
            val mFrontCameraId = if (config.frontCamera != null) config.frontCamera else -1
            if (mBackCameraId == -1 || mFrontCameraId == -1) {
                val numberOfCameras = Camera.getNumberOfCameras()
                if (numberOfCameras == 0) error("No cameras are available on this config.")
                for (i in 0 until numberOfCameras) {
                    if (mFrontCameraId != -1 && mBackCameraId != -1) break
                    val info = CameraInfo()
                    Camera.getCameraInfo(i, info)
                    if (info.facing == CameraInfo.CAMERA_FACING_FRONT && mFrontCameraId == -1) {
                        config.setFrontCamera(i)
                    } else if (info.facing == CameraInfo.CAMERA_FACING_BACK && mBackCameraId == -1) {
                        config.setBackCamera(i)
                    }
                }
            }
            if (view != null) {
                if (config.cameraPosition == CameraConstant.CAMERA_POSITION_FRONT) {
                    view?.onUsingFrontCamera()
                } else if (config.cameraPosition == CameraConstant.CAMERA_POSITION_BACK) {
                    view?.onUsingBackCamera()
                }
            }
            val toOpen = config.currentCameraId
            camera = Camera.open(if (toOpen == -1) 0 else toOpen)
            onCameraOpened()
        } catch (e: IllegalStateException) {
            CameraLogger.log("exception", e.toString())
            error("Cannot access the camera.")
        } catch (e2: RuntimeException) {
            CameraLogger.log("exception", e2.toString())
            error(Exception("Cannot access the camera, you may need to restart your config.", e2))
        }
    }

    private fun onCameraOpened() {
        if (containerSize == null) {
            createPreviewAutomatically()
        } else {
            createPreview()
        }
    }

    private fun createPreviewAutomatically() {
        container.viewTreeObserver.addOnGlobalLayoutListener(getContainerSizeListener())
    }


    private fun getContainerSizeListener(): ViewTreeObserver.OnGlobalLayoutListener {
        return object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                containerSize = Point(container.getWidth(), container.getHeight());
                createPreview();
                container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }

        };
    }

    override fun stopPreview() {
        closeCamera()
    }

    private fun createPreview() {
        val camera = camera ?: return
        val parameters: Camera.Parameters = camera.parameters
        val previewSize: Camera.Size = getPreviewSize(parameters)
        parameters.setPreviewSize(previewSize.width, previewSize.height)
        val mStillShotSize: Camera.Size = getPictureSize(parameters, previewSize)
        parameters.setPictureSize(mStillShotSize.width, mStillShotSize.height)
        setupCameraDisplayOrientation(parameters)
        camera.parameters = parameters
        val mFlashModes = CameraUtil.getSupportedFlashModes(activity, parameters)
        config.setFlashModes(mFlashModes)
        setupFlashMode()
        initPreviewView(camera)
    }

    private fun getPictureSize(parameters: Camera.Parameters, previewSize: Camera.Size): Camera.Size {
        return CameraUtil.getHighestSupportedStillShotSize(parameters.supportedPictureSizes, previewSize)
            ?: error("Couldn't find any suitable picture size")
    }

    private fun initPreviewView(camera: Camera) {
        val preview = newInstance(activity, camera)
        if (container.childCount > 0 && container.getChildAt(0) is CameraTexturePreview) container.removeViewAt(0)
        container.addView(preview, 0)
        if (isFullScreenPreviewForced) {
            val screenSize = Point()
            activity.windowManager.defaultDisplay.getSize(screenSize)
            preview.setAspectRatio(screenSize.x, screenSize.y)
        } else {
            containerSize?.let {
                // TODO check
                preview.setAspectRatio(it.x, it.y)
            }
        }
        this.preview = preview
    }

    private fun setupCameraDisplayOrientation(parameters: Camera.Parameters) {
        val info = CameraInfo()
        Camera.getCameraInfo(config.currentCameraId, info)
        val deviceOrientation = Degrees.getDisplayRotation(activity)
        displayOrientation = Degrees.getDisplayOrientation(
            info.orientation,
            deviceOrientation,
            info.facing == CameraInfo.CAMERA_FACING_FRONT
        )
        CameraLogger.log("CameraFragment", String.format("Orientations: Sensor = %d˚, CameraConfig = %d˚, Display = %d˚", info.orientation, deviceOrientation, displayOrientation))
        var previewOrientation: Int
        val jpegOrientation: Int
        previewOrientation = displayOrientation
        jpegOrientation = previewOrientation
        if (Degrees.isPortrait(deviceOrientation) && config.cameraPosition == CameraConstant.CAMERA_POSITION_FRONT) previewOrientation = Degrees.mirror(displayOrientation)
        parameters.setRotation(jpegOrientation)
        camera?.setDisplayOrientation(previewOrientation)
    }

    private fun getPreviewSize(parameters: Camera.Parameters): Camera.Size {
        var previewSize = CameraUtil.chooseOptimalSize(parameters.supportedPreviewSizes, containerSize!!.x, containerSize!!.y)
        if (previewSize == null) {
            previewSize = CameraUtil.chooseNearestSize(parameters.supportedPreviewSizes, containerSize!!.x, containerSize!!.y)
            if (previewSize == null) {
                throw java.lang.RuntimeException("Couldn't find any suitable preview size")
            } else {
                if (containerSize == null) {
                    containerSize = Point(previewSize.height, previewSize.width)
                } else {
                    containerSize?.x = previewSize.height
                    containerSize?.y = previewSize.width
                }
            }
        }
        CameraLogger.log("TAG", "previewSize=" + previewSize.width + "," + previewSize.height)
        return previewSize
    }

    private fun setupFlashMode() {
        val camera = camera ?: return
        if (config.cameraPosition == CameraConstant.CAMERA_POSITION_FRONT) {
            return
        }
        var flashMode: String? = null
        when (config.flashMode) {
            CameraConstant.FLASH_MODE_AUTO -> {
                flashMode = Camera.Parameters.FLASH_MODE_AUTO
                view?.onUsingFlashAutoMode()
            }
            CameraConstant.FLASH_MODE_ALWAYS_ON -> {
                flashMode = Camera.Parameters.FLASH_MODE_ON
                view?.onUsingFlashOnMode()
            }
            CameraConstant.FLASH_MODE_OFF -> {
                flashMode = Camera.Parameters.FLASH_MODE_OFF
                view?.onUsingFlashOffMode()
            }
            else -> {
            }
        }
        if (flashMode != null) {
            val parameters: Camera.Parameters = camera.parameters
            parameters.flashMode = flashMode
            camera.parameters = parameters
        }
    }

    override fun releaseCamera() {
        try {
            preview?.surfaceTexture?.release()
        } catch (ignored: Throwable) {
        }
//        container = null // TODO
    }

    override fun toggleCameraPosition() {
        config.toggleCameraPosition()
        reopenCamera()
    }

    private fun reopenCamera() {
        closeCamera()
        openCamera()
    }

    private fun closeCamera() {
        try {
            try {
                camera?.lock()
            } catch (ignored: Throwable) {
            }
            camera?.release()
            camera = null
        } catch (e: java.lang.IllegalStateException) {
//            throwError(java.lang.Exception("Illegal state while trying to close camera.", e)) // TODO
        }
    }

    override fun toggleFlashMode() {
        config.toggleFlashMode()
        setupFlashMode()
    }

    override fun focus() {
        val camera = camera ?: return
        if (isAutoFocusing) return
        try {
            isAutoFocusing = true
            camera.cancelAutoFocus()
            camera.autoFocus { success, _ ->
                isAutoFocusing = false
                if (!success) CameraLogger.log("XXX", "Unable to auto-focus!")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun capture() {
        try {
            view?.onCaptureButtonShouldLock()
            camera?.takePicture(Camera.ShutterCallback { }, PictureCallback { _, _ -> }, createJpegCallback())
        } catch (ex: java.lang.Exception) { // error
        }
    }

    private fun createJpegCallback(): PictureCallback? {
        return PictureCallback { data, _ ->
            view?.onCaptureButtonShouldUnlock()
            pictureCapturedCallback?.onPictureCaptured(data, config.cameraPosition)
        }
    }

    fun forceFullScreenPreview(isFullScreenPreviewForced: Boolean) {
        this.isFullScreenPreviewForced = isFullScreenPreviewForced
    }
}