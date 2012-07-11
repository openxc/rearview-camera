package com.camera.simplewebcam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Runnable {

	private static final boolean DEBUG = true;
	protected Context context;
	private SurfaceHolder holder;
    Thread mainLoop = null;
	private Bitmap bmp=null;
	private Bitmap bmp2=null;
	
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
    private RectF rect;
    private int dw, dh;
    public float screenHeight;
    public float screenWidth;
    private float rate;
    private float screenToBmpHeightRatio;
    private float screenToBmpWidthRatio;
    private float newWidth;
    private float newHeight;
  
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
    			   
    			   Log.w(TAG, "Screen Height is " + screenHeight);
    			   Log.w(TAG, "Bmp Height is " + bmp.getHeight());
    			   Log.w(TAG, "Height Ratio is " + screenToBmpHeightRatio);
    			   
    			   Log.w(TAG, "Screen Width is " + screenWidth);
    			   Log.w(TAG, "Bmp Width is " + bmp.getWidth());
    			   Log.w(TAG, "Width Ratio is " + screenToBmpWidthRatio);
    			   
    			   //adjust bmp to match screen size
    			   newWidth = screenToBmpWidthRatio*bmp.getWidth();
    			   newHeight = screenToBmpHeightRatio*bmp.getHeight();
    			  
    			   //adjust size and flip bmp horizontally with matrix
    			   Matrix matrix = new Matrix();
    			   matrix.preScale(-screenToBmpWidthRatio, screenToBmpHeightRatio);
    			   matrix.postTranslate((float)(0.5*screenWidth)+(float)(0.5*newWidth),
            			(float)(0.5*screenHeight)-(float)(0.5*newHeight));
            	
    			   // draw camera bmp on canvas
    			   canvas.drawBitmap(bmp, matrix, null);
    			   getHolder().unlockCanvasAndPost(canvas); 
    		   }

    		   if(shouldStop){
    			   shouldStop = false;  
    			   break;
    		   }	        
    	   }
     	}
       	else {
    	   Log.w(TAG, "No Camera Attached");
       	}
    }

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(DEBUG) Log.d("WebCam", "surfaceCreated");
		if(bmp==null){
			
			bmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);
						
		}
		// /dev/videox (x=cameraId + cameraBase) is used
		int ret = prepareCameraWithBase(cameraId, cameraBase);
		
		if(ret!=-1) cameraExists = true;
		
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
