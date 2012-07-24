package com.camera.simplewebcam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public static final String ACTION_VEHICLE_UNREVERSED = "com.ford.openxc.VEHICLE_UNREVERSED";
	private static final boolean DEBUG = true;
	protected Context context;
	private SurfaceHolder holder;
    Thread mainLoop = null;
	private Bitmap bmp=null;
	private Bitmap bmpOverlayLines=null;
	private Bitmap bmpIbook=null;
	private Bitmap bmpWarningText=null;
	
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

	// The following variables are used to draw camera images.
    private int winWidth=0;
    private int winHeight=0;
    public float screenHeight=0;
    public float screenWidth=0;
    private float rate=0;
    private float screenToBmpHeightRatio=0;
    private float screenToBmpWidthRatio=0;
    private float screenToOverlayHeightRatio=0;
    private float screenToOverlayWidthRatio=0;
    private float newBmpHeight=0;
    private float newBmpWidth=0;
    private float newOverlayHeight=0;
    private float newOverlayWidth=0;
    private float ibookHorizontalTranslation=0;
    private float ibookVerticalTranslation=0;
    private float transformedIbookWidth=0;
    private float warningTextHorizontalTranslation=0;
    private float warningTextVerticalTranslation=0;
  
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
    		   pixeltobmp(bmp);
         	
    		   Canvas canvas = getHolder().lockCanvas();
    		   
    		   if (canvas != null) {
    			             		
    			  //obtain screen dimensions
    			   DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    			   screenWidth = metrics.widthPixels;
    			   screenHeight= metrics.heightPixels;
    			  
    			   //compute ratio between screen and bmp
    			   screenToBmpWidthRatio = screenWidth/(float)bmp.getWidth();
    			   screenToBmpHeightRatio = screenHeight/(float)bmp.getHeight();
    			   
    			   //adjust bmp to match screen size
    			   newBmpWidth = screenToBmpWidthRatio*bmp.getWidth();
    			   newBmpHeight = screenToBmpHeightRatio*bmp.getHeight();
    			  
    			   //adjust size and flip bmp horizontally with matrix
    			   Matrix cameraFeedMatrix = new Matrix();
    			   cameraFeedMatrix.preScale(-screenToBmpWidthRatio, screenToBmpHeightRatio);
    			   cameraFeedMatrix.postTranslate((float)(0.5*screenWidth)+(float)(0.5*newBmpWidth),
            			(float)(0.5*screenHeight)-(float)(0.5*newBmpHeight));
            	
    			   //compute ratio between screen and overlay lines
    			   screenToOverlayWidthRatio = (float)0.75*screenWidth/(float)bmpOverlayLines.getWidth();
    			   screenToOverlayHeightRatio = (float)0.35*screenHeight/(float)bmpOverlayLines.getHeight();
    			   
    			   //adjust bmpOverlayLines accordingly to screen size
    			   newOverlayWidth = screenToOverlayWidthRatio*bmpOverlayLines.getWidth();
    			   newOverlayHeight = screenToOverlayHeightRatio*bmpOverlayLines.getHeight();
    			   
    			   Matrix overlayMatrix = new Matrix();
    			   overlayMatrix.preScale(-screenToOverlayWidthRatio, screenToOverlayHeightRatio);
    			   overlayMatrix.postTranslate((float)(0.5*screenWidth)+(float)(0.5*newOverlayWidth), 
    					   (float)(0.5*screenHeight)-(float)(0.25*newOverlayHeight));
    			   
    			   //place ibook
    			   Matrix ibookMatrix = new Matrix();
    			   ibookHorizontalTranslation = (float)0.05*screenWidth;
    			   ibookVerticalTranslation = 20;
    			   ibookMatrix.preScale((float)0.5, (float)0.5);
    			   ibookMatrix.postTranslate(ibookHorizontalTranslation, ibookVerticalTranslation);
    			   transformedIbookWidth = (float)0.5*bmpIbook.getWidth();
    			   
    			   //place warning text
    			   Matrix warningTextMatrix = new Matrix();
    			   warningTextHorizontalTranslation = ibookHorizontalTranslation + (float)1.5*transformedIbookWidth;
    			   warningTextVerticalTranslation = ibookVerticalTranslation;
    			   warningTextMatrix.preScale((float)0.5, (float)0.5);
    			   warningTextMatrix.postTranslate(warningTextHorizontalTranslation, warningTextVerticalTranslation);
    			   
    			   // draw bitmaps to canvas
    			   canvas.drawBitmap(bmp, cameraFeedMatrix, null);
    			   canvas.drawBitmap(bmpOverlayLines, overlayMatrix, null);
    			   canvas.drawBitmap(bmpIbook, ibookMatrix, null);
    			   canvas.drawBitmap(bmpWarningText, warningTextMatrix, null);
    			   
    			   getHolder().unlockCanvasAndPost(canvas); 
    		   }

    		   if(shouldStop){
    			   shouldStop = false;  
    			   break;
    		   }	        
    	   }
     	}
       	else {
    	   Log.w(TAG, "No Camera Detected");
       	}
    }

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(DEBUG) Log.d("WebCam", "surfaceCreated");
		if(bmp==null){
			
			bmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);

		}
		if(bmpOverlayLines==null){
			
			bmpOverlayLines = BitmapFactory.decodeResource(getResources(), R.drawable.linesoverlay); 
		}
		
		if(bmpIbook==null){
			
			bmpIbook = BitmapFactory.decodeResource(getResources(), R.drawable.ibook);
		}
		
		if(bmpWarningText==null){
			
			bmpWarningText = BitmapFactory.decodeResource(getResources(), R.drawable.pleasechecksurroundings);
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
				}catch(Exception e){}
			}
		}
		stopCamera();
	}   
}

