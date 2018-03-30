package me.kazaf.camerakit.core;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.TextureView;

import me.kazaf.camerakit.util.CameraLogger;

class CameraTexturePreview extends TextureView implements TextureView.SurfaceTextureListener {

  private static final String TAG = CameraTexturePreview.class.getSimpleName();

  private final Camera mCamera;
  private int mRatioWidth = 0;
  private int mRatioHeight = 0;

  public CameraTexturePreview(Context context, Camera camera) {
    super(context);
    mCamera = camera;
    setSurfaceTextureListener(this);
  }



  /**
   * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
   * calculated from the parameters. Note that the actual sizes of parameters don't matter, that is,
   * calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
   *
   * @param width Relative horizontal size
   * @param height Relative vertical size
   */
  public void setAspectRatio(int width, int height) {
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Size cannot be negative.");
    }
    mRatioWidth = width;
    mRatioHeight = height;
    requestLayout();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    setMeasuredDimension(mRatioWidth, mRatioHeight);
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
    try {
      mCamera.setPreviewTexture(surfaceTexture);
      mCamera.startPreview();
    } catch (Throwable e) {
      CameraLogger.log(TAG, "Error setting camera preview: " + e.getMessage());
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
    return true;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

  }
}
