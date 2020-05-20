package com.carterchen247.camerakit.core

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.AttributeSet
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import com.carterchen247.camerakit.util.CameraLogger

internal class CameraTexturePreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(
    context,
    attrs,
    defStyleAttr
), SurfaceTextureListener {
    private lateinit var camera: Camera
    private var ratioWidth = 0
    private var ratioHeight = 0

    init {
        surfaceTextureListener = this
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that is,
     * calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        require(width >= 0 && height >= 0) { "Size cannot be negative." }
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(ratioWidth, ratioHeight)
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
        try {
            camera.setPreviewTexture(surfaceTexture)
            camera.startPreview()
        } catch (e: Throwable) {
            CameraLogger.log(TAG, "Error setting camera preview: " + e.message)
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

    companion object {
        private val TAG = CameraTexturePreview::class.java.simpleName

        @JvmStatic
        fun newInstance(context: Context, camera: Camera): CameraTexturePreview {
            return CameraTexturePreview(context).apply {
                this.camera = camera
            }
        }
    }
}