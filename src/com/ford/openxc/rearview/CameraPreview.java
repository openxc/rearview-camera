package com.ford.openxc.rearview;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** Creates bitmap image showing video feed with graphic overlays.
 *
 * The camera feed is converted to a bitmap using code based on the
 * simple-web-cam project (https://bitbucket.org/neuralassembly/simplewebcam).
 *
 * The graphical overlays include a PNG showing red, yellow and green distance
 * measures and a PNG icon of a book (TODO is the book necessary? do we have a
 * license for that?).
 *
 * Text is drawn to the screen with a black outline to increase visibility.
 *
 * The steering wheel angle guide lines are generated and drawn on the screen
 * dynamically.
 *
 * To increase portability to devices with different screen sizes, most of the
 * methods of this class take the screen size as a parameter. The proportions of
 * all elements on the screen should remain the same regardless of screen size.
 *
 * The JNI implementation is based off of the "simple-web-cam" project:
 * https://bitbucket.org/neuralassembly/simplewebcam from neuralassembly.
*/
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,
        Runnable {

    public static final String ACTION_VEHICLE_UNREVERSED = "com.ford.openxc.VEHICLE_UNREVERSED";
    public static final String NO_CAMERA_DETECTED = "com.ford.openxc.NO_CAMERA_DETECTED";
    private static final String TAG = "CameraPreview";

    private static final boolean DEBUG = true;
    protected Context context;
    private SurfaceHolder holder;
    Thread mainLoop = null;
    private Bitmap bmpVideoFeed=null;
    private Bitmap bmpOverlayLines=null;
    private Bitmap bmpIbook=null;
    private Bitmap bmpWarningText=null;
    private Bitmap bmpDynamicLines=null;

    private boolean cameraExists=false;
    private boolean shouldStop=false;

    // /dev/videox (x=cameraId+cameraBase) is used.
    // In some omap devices, system uses /dev/video[0-3],
    // so users must use /dev/video[4-].
    // In such a case, try cameraId=0 and cameraBase=4
    private int cameraId=0;
    private int cameraBase=0;

    // This definition also exists in ImageProc.h.
    // Webcam must support the resolution 640x480 with YUYV format.
    static final int IMG_WIDTH=640;
    static final int IMG_HEIGHT=480;

    // JNI functions
    public native int prepareCamera(int videoid);
    public native int prepareCameraWithBase(int videoid, int camerabase);
    public native void processCamera();
    public native void stopCamera();
    public native void pixeltobmp(Bitmap bitmap);
    static {
        System.loadLibrary("ImageProc");
    }

    CameraPreview(Context context) {
        super(context);
        this.context = context;
        if(DEBUG) Log.d("WebCam","CameraPreview constructed");
        setFocusable(true);

        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    @Override
    public void run() {
        if (cameraExists) {
            while (!shouldStop && cameraExists) {

                // obtaining a camera image (pixel data are stored in an array in
                // JNI).
                processCamera();

                // camera image to bmp
                pixeltobmp(bmpVideoFeed);

                Canvas canvas = getHolder().lockCanvas();

                if (canvas != null) {
                    // draw bitmaps to canvas
                    drawVideoFeedBitmap(canvas);
                    drawOverlayLinesBitmap(canvas);
                    drawIbookBitmap(canvas);
                    drawDynamicLinesBitmap(canvas);

                    //must draw outline paint first, otherwise yields thick black
                    //text with small white inside
                    drawWarningTextOutline(canvas);
                    drawWarningText(canvas);

                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        } else {
            sendNoCameraDetectedBroadcast();
        }
    }

    /**bitmap drawing methods**/
    private void drawVideoFeedBitmap(Canvas canvas) {
        canvas.drawBitmap(bmpVideoFeed, createVideoFeedMatrix(), null);
    }

    private void drawOverlayLinesBitmap(Canvas canvas) {
        canvas.drawBitmap(bmpOverlayLines, createOverlayMatrix(),
                createOverlayPaint());
    }

    private void drawIbookBitmap(Canvas canvas) {
        canvas.drawBitmap(bmpIbook, createIbookMatrix(), null);
    }

    private void drawDynamicLinesBitmap(Canvas canvas) {
        canvas.drawBitmap(bmpDynamicLines, createDynamicLinesMatrix(),
                createDynamicLinesPaint());
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

    private Matrix createIbookMatrix() {

        Matrix ibookMatrix = new Matrix();

        ibookMatrix.preScale(computeScreenToIbookWidthRatio(),
                computeScreenToIbookHeightRatio());
        ibookMatrix.postTranslate(computeIbookHorizontalTranslation(),
                computeIbookVerticalTranslation());

        return ibookMatrix;
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
        dynamicLinesMatrix.postSkew(-getSteeringWheelAngle()/480, 0);

        return dynamicLinesMatrix;
    }

    /**text drawing methods**/
    private void drawWarningTextOutline(Canvas canvas) {
        canvas.drawText("Please Check Surroundings for Safety",
                getWarningTextXCoordinate(), getWarningTextYCoordinate(),
                createWarningTextOutlinePaint(createWarningTextPaint()));
    }

    private void drawWarningText(Canvas canvas) {
        canvas.drawText("Please Check Surroundings for Safety",
                getWarningTextXCoordinate(), getWarningTextYCoordinate(),
                createWarningTextPaint());
    }

    /**paint creation methods**/
    private Paint createWarningTextOutlinePaint(Paint warningTextPaint) {
        Paint warningTextOutlinePaint = new Paint();

        warningTextOutlinePaint.setStrokeWidth(4);
        warningTextOutlinePaint.setStyle(Paint.Style.STROKE);
        warningTextOutlinePaint.setTextSize(warningTextPaint.getTextSize());

        return warningTextOutlinePaint;
    }

    private Paint createWarningTextPaint() {
        Paint warningTextPaint = new Paint();

        warningTextPaint.setColor(Color.WHITE);
        warningTextPaint.setTextSize(50);

        return warningTextPaint;
    }

    private Paint createOverlayPaint(){

        float steeringWheelValue = getSteeringWheelAngle();
        Paint overlayPaint = new Paint();

        if (steeringWheelValue == 0) {
            overlayPaint.setAlpha(255);
        } else if (steeringWheelValue/2 > 0 && steeringWheelValue/2 <=255) {
            overlayPaint.setAlpha(255-(int)steeringWheelValue/2);
        } else if (steeringWheelValue/2 < 0 && steeringWheelValue/2 >= -255) {
            overlayPaint.setAlpha(255+(int)steeringWheelValue/2);
        } else {
            overlayPaint.setAlpha(0);
        }

        return overlayPaint;
    }

    private Paint createDynamicLinesPaint() {
        float steeringWheelValue = getSteeringWheelAngle();
        Paint dynamicLinesPaint = new Paint();

        if (steeringWheelValue >= 0 && steeringWheelValue < 255){
            dynamicLinesPaint.setAlpha((int)steeringWheelValue);
        } else if (steeringWheelValue < 0 && steeringWheelValue > -255){
            dynamicLinesPaint.setAlpha(-(int)steeringWheelValue);
        } else {
            dynamicLinesPaint.setAlpha(255);
        }

        return dynamicLinesPaint;
    }

    /**steering wheel angle retrieval method**/
    private float getSteeringWheelAngle() {
        return (float) VehicleMonitoringService.SteeringWheelAngle;
    }

    /**coordinate retrieval methods**/
    private float getWarningTextYCoordinate() {
        return computeIbookVerticalTranslation() + computeAdjustedIbookHeight();
    }

    private float getWarningTextXCoordinate() {
        return computeIbookHorizontalTranslation() +
                (float)1.5*computeAdjustedIbookWidth();
    }

    /**ibook translation computation methods**/
    private float computeIbookVerticalTranslation() {
        return (float)(0.02*getScreenHeight());
    }

    private float computeIbookHorizontalTranslation() {
        return (float)0.02*getScreenWidth();
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

    /**screen to ibook ratio computation methods**/
    private float computeScreenToIbookWidthRatio() {
        float screenToIbookWidthRatio = getScreenWidth()/bmpIbook.getWidth()/20;
            return screenToIbookWidthRatio;
    }

    private float computeScreenToIbookHeightRatio() {
        return getScreenHeight()/bmpIbook.getHeight()/20;
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

    /**adjusted ibook dimensions computation methods**/
    private float computeAdjustedIbookHeight() {
        return computeScreenToIbookHeightRatio()*bmpIbook.getHeight();
    }

    private float computeAdjustedIbookWidth() {
        return computeScreenToIbookWidthRatio()*bmpIbook.getWidth();
    }

    /**get screen dimensions methods**/
    private float getScreenHeight() {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    private float getScreenWidth() {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**send broadcast method**/
    private void sendNoCameraDetectedBroadcast() {
        Intent noCameraDetectedIntent = new Intent(NO_CAMERA_DETECTED);
             context.sendBroadcast(noCameraDetectedIntent);
             Log.i(TAG, "No Camera Detected Intent Broadcasted");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(DEBUG) Log.d("WebCam", "surfaceCreated");
        //if null, create each bitmap
        if(bmpVideoFeed==null){
            bmpVideoFeed = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT,
                    Bitmap.Config.ARGB_8888);
        }
        if(bmpOverlayLines==null){
            bmpOverlayLines = BitmapFactory.decodeResource(getResources(),
                    R.drawable.overlay);
        }

        if(bmpIbook==null){
            bmpIbook = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ibook);
        }

        if(bmpDynamicLines==null){
            bmpDynamicLines = BitmapFactory.decodeResource(getResources(),
                    R.drawable.dynamiclines);
        }

        // /dev/videox (x=cameraId + cameraBase) is used
        int ret = prepareCameraWithBase(cameraId, cameraBase);

        if(ret!=-1) {
            cameraExists = true;
        }

        mainLoop = new Thread(this);
        mainLoop.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        if(DEBUG) Log.d("WebCam", "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(DEBUG) Log.d("WebCam", "surfaceDestroyed");
        if(cameraExists){
            shouldStop = true;
            while(shouldStop) {
                try{
                    Thread.sleep(100); // wait for thread stopping
                }
                catch(Exception e){}
            }
        }
        stopCamera();
    }
}
