package me.kazaf.camerakit.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static me.kazaf.camerakit.util.Degrees.DEGREES_270;
import static me.kazaf.camerakit.util.Degrees.DEGREES_90;

/**
 * Created by tomiurankar on 06/03/16.
 */
public class ImageUtil {
    /**
     * Saves byte[] array to disk
     *
     * @param input    byte array
     * @param output   path to output file
     * @param callback will always return in originating thread
     */
    public static void saveToDiskAsync(
            final byte[] input, final File output, final ICallback callback) {
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                try {
                    FileOutputStream outputStream = new FileOutputStream(output);
                    CameraLogger.log("saveToDiskAsync", "file=" + output.getAbsolutePath());
                    outputStream.write(input);
                    outputStream.flush();
                    outputStream.close();

                    handler.post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    callback.done(null);
                                }
                            });
                } catch (final Exception e) {
                    handler.post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    callback.done(e);
                                }
                            });
                }
            }
        }.start();
    }

    public static String createSaveDir(String folder) {
        return new File(Environment.getExternalStorageDirectory(), folder).getAbsolutePath();
    }

    public static File makeTempFile(
            @NonNull Context context, @Nullable String saveDir, String prefix, String extension) {
        if (saveDir == null) saveDir = context.getExternalCacheDir().getAbsolutePath();
        final String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final File dir = new File(saveDir);
        dir.mkdirs();
        return new File(dir, prefix + timeStamp + extension);
    }

    /**
     * Rotates the bitmap per their EXIF flag. This is a recursive function that will be called again
     * if the image needs to be downsized more.
     *
     * @param inputFile Expects an JPEG file if corrected orientation wants to be set.
     * @return rotated bitmap or null
     */
    @Nullable
    public static Bitmap getRotatedBitmap(String inputFile, int reqWidth, int reqHeight, boolean isFlip) {
        final int rotationInDegrees = getExifDegreesFromJpeg(inputFile);

        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(inputFile, opts);
        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight, rotationInDegrees);
        opts.inJustDecodeBounds = false;

        final Bitmap origBitmap = BitmapFactory.decodeFile(inputFile, opts);

        if (origBitmap == null) return null;

        Matrix matrix = new Matrix();
        matrix.preRotate(rotationInDegrees);
        CameraLogger.log("degree", "rotationInDegrees=" + rotationInDegrees);
        if (isFlip) {
            if (rotationInDegrees % 180 == 0) {
                matrix.preScale(-1, 1);
            } else if (rotationInDegrees % 180 == 90){
                matrix.preScale(1, -1);
            }
        }

        // we need not check if the rotation is not needed, since the below function will then return the same bitmap. Thus no memory loss occurs.

        return Bitmap.createBitmap(
                origBitmap, 0, 0, origBitmap.getWidth(), origBitmap.getHeight(), matrix, true);
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight, int rotationInDegrees) {

        // Raw height and width of image
        final int height;
        final int width;
        int inSampleSize = 1;

        // Check for rotation
        if (rotationInDegrees == DEGREES_90 || rotationInDegrees == DEGREES_270) {
            width = options.outHeight;
            height = options.outWidth;
        } else {
            height = options.outHeight;
            width = options.outWidth;
        }

        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private static int getExifDegreesFromJpeg(String inputFile) {
        try {
            final ExifInterface exif = new ExifInterface(inputFile);

            final String result =
                    exif.getAttribute(ExifInterface.TAG_ORIENTATION);

            CameraLogger.log("exif", "result=" + result);

            final int exifOrientation =
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }

            CameraLogger.log("exif", "exifOrientation=" + exifOrientation);
        } catch (IOException e) {
            CameraLogger.log("exif", "Error when trying to get exif data from : " + inputFile);
            CameraLogger.log("exif", "e="+e.toString());
        }
        return 0;
    }

    public interface ICallback {
        /**
         * It is called when the background operation completes. If the operation is successful, {@code
         * exception} will be {@code null}.
         */
        void done(Exception exception);
    }

}
