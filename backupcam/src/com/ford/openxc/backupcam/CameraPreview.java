package com.ford.openxc.backupcam;

/**Creates view to be displayed with camera preview as background feed**/

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

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public static final String ACTION_VEHICLE_UNREVERSED = "com.ford.openxc.VEHICLE_UNREVERSED";
    public static final String NO_CAMERA_DETECTED = "com.ford.openxc.NO_CAMERA_DETECTED";

	private static final boolean DEBUG = true;
	protected Context context;
	private SurfaceHolder holder;
    Thread mainLoop = null;
	private Bitmap bmpVideoFeed=null;
	private Bitmap bmpOverlayLines=null;
	private Bitmap bmpIbook=null;
	private Bitmap bmpWarningText=null;
	private Bitmap bmpDynamicLines=null;
	
	private static final String TAG = "CameraPreview";

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
    	   while (true && cameraExists) {

    		   // obtaining a camera image (pixel data are stored in an array in JNI).
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
    			   
    			   //must draw outline paint first, otherwise yields thick black text with small white inside
    			   drawWarningTextOutline(canvas);
    			   drawWarningText(canvas);
    			   
    			   //unlock canvas
    			   getHolder().unlockCanvasAndPost(canvas); 
    		   }

    		   if(shouldStop){
    			   shouldStop = false;  
    			   break;
    		   }	        
    	   }
     	}
       	else {
    	  sendNoCameraDetectedBroadcast();
       	}
    }
    
	/**bitmap drawing methods**/
	private void drawVideoFeedBitmap(Canvas canvas) {
		canvas.drawBitmap(bmpVideoFeed, createVideoFeedMatrix(), null);
	}

	private void drawOverlayLinesBitmap(Canvas canvas) {
		canvas.drawBitmap(bmpOverlayLines, createOverlayMatrix(), createOverlayPaint());
	}

	private void drawIbookBitmap(Canvas canvas) {
		canvas.drawBitmap(bmpIbook, createIbookMatrix(), null);
	}

	private void drawDynamicLinesBitmap(Canvas canvas) {
		canvas.drawBitmap(bmpDynamicLines, createDynamicLinesMatrix(), createDynamicLinesPaint());
	}
	
	/**matrix creation methods**/
	private Matrix createVideoFeedMatrix() {
		
		Matrix videoFeedMatrix = new Matrix();
		
		videoFeedMatrix.preScale(-computeScreenToFeedWidthRatio(), computeScreenToFeedHeightRatio());
		videoFeedMatrix.postTranslate((float)(0.5*getScreenWidth())+(float)(0.5*computeAdjustedVideoFeedWidth()),
	 		(float)(0.5*getScreenHeight())-(float)(0.5*computeAdjustedVideoFeedHeight()));
			
		return videoFeedMatrix;
	}

	private Matrix createOverlayMatrix() {

		Matrix overlayMatrix = new Matrix();
		
		overlayMatrix.preScale(computeScreenToOverlayWidthRatio(), computeScreenToOverlayHeightRatio());
		overlayMatrix.postTranslate(computeOverlayHorizontalTranslation(), 
				computeOverlayVerticalTranslation());
			
		return overlayMatrix;
	}

	private Matrix createIbookMatrix() {
		
		Matrix ibookMatrix = new Matrix();
		
		ibookMatrix.preScale(computeScreenToIbookWidthRatio(), computeScreenToIbookHeightRatio());
		ibookMatrix.postTranslate(computeIbookHorizontalTranslation(), computeIbookVerticalTranslation());
			
		return ibookMatrix;
	}

	private Matrix createDynamicLinesMatrix() {
		
		//place dynamic lines directly on top of overlay by using same translations/ratios
		float screenToDynamicLinesHeightRatio = computeScreenToOverlayHeightRatio();
		float screenToDynamicLinesWidthRatio = computeScreenToOverlayWidthRatio();
		float dynamicLinesVerticalTranslation = computeOverlayVerticalTranslation();
		float dynamicLinesHorizontalTranslation = computeOverlayHorizontalTranslation();

		Matrix dynamicLinesMatrix = new Matrix();
		
		dynamicLinesMatrix.preScale(screenToDynamicLinesWidthRatio, screenToDynamicLinesHeightRatio);
		dynamicLinesMatrix.postTranslate(dynamicLinesHorizontalTranslation + 3*(float)getSteeringWheelAngle()/2, 
				dynamicLinesVerticalTranslation);
		
		//number divided by must be larger than the maximum absolute value the steering wheel can produce because the x skew
		//must be less than 1
		dynamicLinesMatrix.postSkew(-getSteeringWheelAngle()/480, 0);
			
		return dynamicLinesMatrix;
	}
	
	/**text drawing methods**/
	private void drawWarningTextOutline(Canvas canvas) {
		canvas.drawText("Please Check Surroundings for Safety", getWarningTextXCoordinate(), 
				getWarningTextYCoordinate(), createWarningTextOutlinePaint(createWarningTextPaint()));
	}

	private void drawWarningText(Canvas canvas) {
		canvas.drawText("Please Check Surroundings for Safety", getWarningTextXCoordinate(), 
				getWarningTextYCoordinate(), createWarningTextPaint());
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
		
		if (steeringWheelValue/2 >= 0 && steeringWheelValue/2 <=255) {
			overlayPaint.setAlpha(255-(int)steeringWheelValue/2);
		}
		else if (steeringWheelValue/2 < 0 && steeringWheelValue/2 > -255) {
			overlayPaint.setAlpha(255+(int)steeringWheelValue/2);
		}
		else {
			overlayPaint.setAlpha(0);
		}
			
		return overlayPaint;
	}

	private Paint createDynamicLinesPaint() {
		
		float steeringWheelValue = getSteeringWheelAngle(); 
		Paint dynamicLinesPaint = new Paint();
		
		if (steeringWheelValue >= 0 && steeringWheelValue < 255){
			dynamicLinesPaint.setAlpha((int)steeringWheelValue);
		}
		else if (steeringWheelValue < 0 && steeringWheelValue > -255){
			dynamicLinesPaint.setAlpha(-(int)steeringWheelValue);
		}
		else {
			dynamicLinesPaint.setAlpha(255);
		}
		
		return dynamicLinesPaint;
	}
	
	/**steering wheel angle retrieval method**/
	private float getSteeringWheelAngle() {
		float steeringWheelValue = (float)VehicleMonitoringService.SteeringWheelAngle;
		return steeringWheelValue;
	}
	
	/**coordinate retrieval methods**/
	private float getWarningTextYCoordinate() {
		float warningTextYCoordinate = computeIbookVerticalTranslation() + computeAdjustedIbookHeight();
		return warningTextYCoordinate;
	}

	private float getWarningTextXCoordinate() {
		float warningTextXCoordinate = computeIbookHorizontalTranslation() + (float)1.5*computeAdjustedIbookWidth();
		return warningTextXCoordinate;
	}

	/**ibook translation computation methods**/
	private float computeIbookVerticalTranslation() {
		float ibookVerticalTranslation = (float)(0.02*getScreenHeight());
			return ibookVerticalTranslation;
	}
	
	private float computeIbookHorizontalTranslation() {
		float ibookHorizontalTranslation = (float)0.02*getScreenWidth();
			return ibookHorizontalTranslation;
	}
	
	/**overlay translation computation methods**/
	private float computeOverlayHorizontalTranslation() {
		float overlayHorizontalTranslation = (float)(0.5*getScreenWidth())-(float)(0.5*computeAdjustedOverlayWidth());
			return overlayHorizontalTranslation;
	}
	
	private float computeOverlayVerticalTranslation() {
		float overlayVerticalTranslation = (float)(0.5*getScreenHeight())-(float)(0.45*computeAdjustedOverlayHeight());
		return overlayVerticalTranslation;
	}
	
	/**screen to video feed ratio computation methods**/
	private float computeScreenToFeedHeightRatio() {
		float screenToFeedHeightRatio = getScreenHeight()/(float)bmpVideoFeed.getHeight();
			return screenToFeedHeightRatio;
	}
	
	private float computeScreenToFeedWidthRatio() {
		float screenToFeedWidthRatio = getScreenWidth()/(float)bmpVideoFeed.getWidth();
			return screenToFeedWidthRatio;
	}
	
	/**screen to overlay ratio computation methods**/
	private float computeScreenToOverlayHeightRatio() {
		float screenToOverlayHeightRatio = (float)0.65*getScreenHeight()/(float)bmpOverlayLines.getHeight();
			return screenToOverlayHeightRatio;
	}
	
	private float computeScreenToOverlayWidthRatio() {
		float screenToOverlayWidthRatio = (float)0.85*getScreenWidth()/(float)bmpOverlayLines.getWidth();
			return screenToOverlayWidthRatio;
	}
	
	/**screen to ibook ratio computation methods**/
	private float computeScreenToIbookWidthRatio() {
		float screenToIbookWidthRatio = getScreenWidth()/bmpIbook.getWidth()/20;
			return screenToIbookWidthRatio;
	}
	
	private float computeScreenToIbookHeightRatio() {
		float screenToIbookHeightRatio = getScreenHeight()/bmpIbook.getHeight()/20;
			return screenToIbookHeightRatio;
	}

	/**adjusted video feed dimensions computation methods**/
	private float computeAdjustedVideoFeedHeight() {
		float adjustedVideoFeedHeight = computeScreenToFeedHeightRatio()*bmpVideoFeed.getHeight();
			return adjustedVideoFeedHeight;
	}
	
	private float computeAdjustedVideoFeedWidth() {
		float adjustedVideoFeedWidth = computeScreenToFeedWidthRatio()*bmpVideoFeed.getWidth();
			return adjustedVideoFeedWidth;
	}	
	
	/**adjusted overlay dimensions computation methods**/
	private float computeAdjustedOverlayHeight() {
		float adjustedOverlayHeight = computeScreenToOverlayHeightRatio()*bmpOverlayLines.getHeight();
			return adjustedOverlayHeight;
	}
	
	private float computeAdjustedOverlayWidth() {
		float adjustedOverlayWidth = computeScreenToOverlayWidthRatio()*bmpOverlayLines.getWidth();
			return adjustedOverlayWidth;
	}
	
	/**adjusted ibook dimensions computation methods**/
	private float computeAdjustedIbookHeight() {
		float adjustedIbookHeight = computeScreenToIbookHeightRatio()*bmpIbook.getHeight();
			return adjustedIbookHeight;
	}
		
	private float computeAdjustedIbookWidth() {
		float adjustedIbookWidth = computeScreenToIbookWidthRatio()*bmpIbook.getWidth();
			return adjustedIbookWidth;
	}
		
	/**get screen dimensions methods**/
	private float getScreenHeight() {
		float screenHeight= context.getResources().getDisplayMetrics().heightPixels;
			return screenHeight;
	}
	
	private float getScreenWidth() {
		float screenWidth = context.getResources().getDisplayMetrics().widthPixels;
			return screenWidth;
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
			
			bmpVideoFeed = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);

		}
		if(bmpOverlayLines==null){
			
			bmpOverlayLines = BitmapFactory.decodeResource(getResources(), R.drawable.overlay); 
		}
		
		if(bmpIbook==null){
			
			bmpIbook = BitmapFactory.decodeResource(getResources(), R.drawable.ibook);
		}
		
		if(bmpDynamicLines==null){
			
			bmpDynamicLines = BitmapFactory.decodeResource(getResources(), R.drawable.dynamiclines);
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
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(DEBUG) Log.d("WebCam", "surfaceChanged");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(DEBUG) Log.d("WebCam", "surfaceDestroyed");
		if(cameraExists){
			shouldStop = true;
			while(shouldStop){
				try{ 
					Thread.sleep(100); // wait for thread stopping
				}
				catch(Exception e){}
			}
		}
		stopCamera();
	}   
}