# CameraKit

A SDK that facilitates the rapid development on camera applications.

## Notice

Because the SDK is designed as light-weighted, which means it does not have many features, there are something noteworthy:

- You'll need to handle permission requesting on your own.

- Only few device hardware support full function of Camera API 2, as a result this SDK is written by Camera API 1. If you need advanced features of Camera API 2, you can extend it.

## Usage

Demo application shows the steps of setting up this SDK. The usage of `Camerakit` is very easy:

### Preview

1. Create `CameraPresenter` instance

```java
CameraPresenter cameraPresenter;
```

2. Place startPreview and stopPreview in your app lifecycle, such as `onResume` and `onPause`:
```java

@Override
protected void onResume() {
    super.onResume();
    cameraPresenter.startPreview();
}
    

@Override
protected void onPause() {
    super.onPause();
    cameraPresenter.stopPreview();
}

```


3. Call `releaseCamera` at the end of lifecycle:
```java
@Override
    protected void onDestroy() {
        super.onDestroy();
        cameraPresenter.releaseCamera();
    }
```

### User Actions

```java
cameraPresenter.toggleCameraPosition();
cameraPresenter.toggleFlashMode();
cameraPresenter.capture();
cameraPresenter.focus();

```

### Monitor user action and change UI

```java
cameraPresenter.setUserActionCallback(this);

@Override
public void onUsingFrontCamera() {
    img.setImageResource(R.drawable.icon_camera_front);
}

@Override
public void onUsingBackCamera() {
    img.setImageResource(R.drawable.icon_camera_back);
}

@Override
public void onUsingFlashOnMode() {
    img.setImageResource(R.drawable.icon_flash_on);
}

@Override
public void onUsingFlashOffMode() {
    img.setImageResource(R.drawable.icon_flash_off);
}

@Override
public void onUsingFlashAutoMode() {
    img.setImageResource(R.drawable.icon_flash_auto);
}

@Override
public void onCaptureButtonShouldLock() {
    
}

@Override
public void onCaptureButtonShouldUnlock() {
    
}
```

### Get Result
```java
cameraPresenter.setOnPictureCapturedCallback(this);

@Override
public void onPictureCaptured(byte[] data, final int cameraPosition) {

}
```
 


