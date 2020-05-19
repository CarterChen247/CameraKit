package com.carterchen247.camerademo;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;

import com.carterchen247.camerakit.util.ImageUtil;

/**
 * Created by kazaf on 2018/1/4.
 */

public class ResultActivity extends AppCompatActivity {

    ImageView img;
    private static Bitmap mBitmap;
    String path;
    boolean isFrontCamera;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank);
        img = (ImageView) findViewById(R.id.img);
        path = getIntent().getExtras().getString(Constant.FILE);
        isFrontCamera = getIntent().getBooleanExtra(Constant.IS_FRONT_CAMERA, false);

        img.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                setImageBitmap();
                                img.getViewTreeObserver().removeOnPreDrawListener(this);
                                return true;
                            }
                        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /** Sets bitmap to ImageView widget */
    private void setImageBitmap() {
        final int width = img.getMeasuredWidth();
        final int height = img.getMeasuredHeight();

        // TODO IMPROVE MEMORY USAGE HERE, ESPECIALLY ON LOW-END DEVICES.
        if (mBitmap == null)
            mBitmap = ImageUtil.getRotatedBitmap(Uri.parse(path).getPath(), width, height, isFrontCamera);

        img.setImageBitmap(mBitmap);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBitmap != null && !mBitmap.isRecycled()) {
            try {
                mBitmap.recycle();
                mBitmap = null;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
