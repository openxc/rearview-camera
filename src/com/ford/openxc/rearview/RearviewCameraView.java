package com.ford.openxc.rearview;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.ford.openxc.webcam.WebcamPreview;
import com.openxc.NoValueException;
import com.openxc.VehicleManager;
import com.openxc.measurements.SteeringWheelAngle;
import com.openxc.measurements.UnrecognizedMeasurementTypeException;

/** Creates bitmap image showing video feed with graphic overlays.
 *
 * The camera feed is converted to a bitmap using code based on the
 * simple-web-cam project (https://bitbucket.org/neuralassembly/simplewebcam).
 *
 * The graphical overlays include a PNG showing red, yellow and green distance
 * measures. The steering wheel angle guide lines are generated and drawn on the
 * screen dynamically.
 *
 * To increase portability to devices with different screen sizes, most of the
 * methods of this class take the screen size as a parameter. The proportions of
 * all elements on the screen should remain the same regardless of screen size.
 *
 * The JNI implementation is based off of the "simple-web-cam" project:
 * https://bitbucket.org/neuralassembly/simplewebcam from neuralassembly.
*/
public class RearviewCameraView extends WebcamPreview {

    private static String TAG = "RearviewCameraView";

    public static final String NO_CAMERA_DETECTED = "com.ford.openxc.NO_CAMERA_DETECTED";

    private Bitmap bmpVideoFeed=null;
    private Bitmap bmpOverlayLines=null;
    private Bitmap bmpDynamicLines=null;
    private VehicleManager mVehicleManager;

    public RearviewCameraView(Context context) {
        super(context);
        context.bindService(new Intent(context, VehicleManager.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    public RearviewCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        context.bindService(new Intent(context, VehicleManager.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void run() {
        while(mRunning) {
            synchronized(mServiceSyncToken) {
                if(mWebcamManager == null) {
                    try {
                        mServiceSyncToken.wait();
                    } catch(InterruptedException e) {
                        break;
                    }
                }

                if(!mWebcamManager.cameraAttached()) {
                    mRunning = false;
                    sendNoCameraDetectedBroadcast();
                }

                bmpVideoFeed = mWebcamManager.getImage();
                Canvas canvas = mHolder.lockCanvas();
                if(canvas != null) {
                    canvas.drawBitmap(bmpVideoFeed, createVideoFeedMatrix(), null);
                    canvas.drawBitmap(bmpOverlayLines, createOverlayMatrix(),
                            createOverlayPaint());
                    canvas.drawBitmap(bmpDynamicLines, createDynamicLinesMatrix(),
                            createDynamicLinesPaint());
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        if(bmpOverlayLines == null){
            bmpOverlayLines = BitmapFactory.decodeResource(getResources(),
                    R.drawable.overlay);
        }

        if(bmpDynamicLines == null){
            bmpDynamicLines = BitmapFactory.decodeResource(getResources(),
                    R.drawable.dynamiclines);
        }
    }

    /**matrix creation methods**/
    private Matrix createVideoFeedMatrix() {
        Matrix videoFeedMatrix = new Matrix();

        videoFeedMatrix.preScale(-computeScreenToFeedWidthRatio(),
                computeScreenToFeedHeightRatio());
        videoFeedMatrix.postTranslate((float)(0.5*getScreenWidth()) +
                    (float)(0.5*computeAdjustedVideoFeedWidth()),
                (float)(0.5*getScreenHeight()) -
                    (float)(0.5*computeAdjustedVideoFeedHeight()));

        return videoFeedMatrix;
    }

    private Matrix createOverlayMatrix() {
        Matrix overlayMatrix = new Matrix();

        overlayMatrix.preScale(computeScreenToOverlayWidthRatio(),
                computeScreenToOverlayHeightRatio());
        overlayMatrix.postTranslate(computeOverlayHorizontalTranslation(),
                computeOverlayVerticalTranslation());

        return overlayMatrix;
    }

    private Matrix createDynamicLinesMatrix() {
        //place dynamic lines directly on top of overlay by using same
        //translations/ratios
        float screenToDynamicLinesHeightRatio =
                computeScreenToOverlayHeightRatio();
        float screenToDynamicLinesWidthRatio =
                computeScreenToOverlayWidthRatio();
        float dynamicLinesVerticalTranslation =
                computeOverlayVerticalTranslation();
        float dynamicLinesHorizontalTranslation =
                computeOverlayHorizontalTranslation();

        Matrix dynamicLinesMatrix = new Matrix();

        dynamicLinesMatrix.preScale(screenToDynamicLinesWidthRatio,
                screenToDynamicLinesHeightRatio);
        dynamicLinesMatrix.postTranslate(dynamicLinesHorizontalTranslation +
                3*(float)getSteeringWheelAngle()/2,
                dynamicLinesVerticalTranslation);

        //number divided by must be larger than the maximum absolute value the
        //steering wheel can produce because the x skew must be less than 1
        dynamicLinesMatrix.postSkew((float)-getSteeringWheelAngle()/480, 0);

        return dynamicLinesMatrix;
    }

    private Paint createOverlayPaint(){
        double steeringWheelAngle = getSteeringWheelAngle();
        Paint overlayPaint = new Paint();

        if (steeringWheelAngle == 0) {
            overlayPaint.setAlpha(255);
        } else if (steeringWheelAngle / 2 > 0 && steeringWheelAngle / 2 <= 255) {
            overlayPaint.setAlpha(255 - (int)steeringWheelAngle / 2);
        } else if (steeringWheelAngle / 2 < 0 && steeringWheelAngle/2 >= -255) {
            overlayPaint.setAlpha(255 + (int)steeringWheelAngle / 2);
        } else {
            overlayPaint.setAlpha(0);
        }

        return overlayPaint;
    }

    private double getSteeringWheelAngle() {
        double steeringWheelAngle = 0;
        try {
            if(mVehicleManager != null) {
                steeringWheelAngle = ((SteeringWheelAngle)mVehicleManager.get(
                            SteeringWheelAngle.class)).getValue().doubleValue();
            }
        } catch(UnrecognizedMeasurementTypeException e) {
        } catch(NoValueException e) {
        }
        return steeringWheelAngle;
    }

    private Paint createDynamicLinesPaint() {
        double steeringWheelAngle = getSteeringWheelAngle();
        Paint paint = new Paint();

        if (steeringWheelAngle >= 0 && steeringWheelAngle < 255){
            paint.setAlpha((int)steeringWheelAngle);
        } else if (steeringWheelAngle < 0 && steeringWheelAngle > -255){
            paint.setAlpha(-(int)steeringWheelAngle);
        } else {
            paint.setAlpha(255);
        }
        return paint;
    }

    /**overlay translation computation methods**/
    private float computeOverlayHorizontalTranslation() {
        return (float)(0.5*getScreenWidth()) -
                (float)(0.5*computeAdjustedOverlayWidth());
    }

    private float computeOverlayVerticalTranslation() {
        return (float)(0.5*getScreenHeight()) -
                (float)(0.3*computeAdjustedOverlayHeight());
    }

    /**screen to video feed ratio computation methods**/
    private float computeScreenToFeedHeightRatio() {
         return getScreenHeight() / bmpVideoFeed.getHeight();
    }

    private float computeScreenToFeedWidthRatio() {
        return getScreenWidth()/(float)bmpVideoFeed.getWidth();
    }

    /**screen to overlay ratio computation methods**/
    private float computeScreenToOverlayHeightRatio() {
        return (float)0.5*getScreenHeight()/(float)bmpOverlayLines.getHeight();
    }

    private float computeScreenToOverlayWidthRatio() {
        return (float)0.85*getScreenWidth()/(float)bmpOverlayLines.getWidth();
    }

    /**adjusted video feed dimensions computation methods**/
    private float computeAdjustedVideoFeedHeight() {
        return computeScreenToFeedHeightRatio()*bmpVideoFeed.getHeight();
    }

    private float computeAdjustedVideoFeedWidth() {
        return computeScreenToFeedWidthRatio()*bmpVideoFeed.getWidth();
    }

    /**adjusted overlay dimensions computation methods**/
    private float computeAdjustedOverlayHeight() {
        return computeScreenToOverlayHeightRatio()*bmpOverlayLines.getHeight();
    }

    private float computeAdjustedOverlayWidth() {
        return computeScreenToOverlayWidthRatio()*bmpOverlayLines.getWidth();
    }

    /**get screen dimensions methods**/
    private float getScreenHeight() {
        return mContext.getResources().getDisplayMetrics().heightPixels;
    }

    private float getScreenWidth() {
        return mContext.getResources().getDisplayMetrics().widthPixels;
    }

    /**send broadcast method**/
    private void sendNoCameraDetectedBroadcast() {
        Intent noCameraDetectedIntent = new Intent(NO_CAMERA_DETECTED);
        mContext.sendBroadcast(noCameraDetectedIntent);
        Log.i(TAG, "No Camera Detected Intent Broadcasted");
    }

    ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            mVehicleManager = ((VehicleManager.VehicleBinder)service
                    ).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleService disconnected unexpectedly");
            mVehicleManager = null;
        }
    };
}
