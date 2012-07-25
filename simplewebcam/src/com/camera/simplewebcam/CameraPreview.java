package com.camera.simplewebcam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
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
	private Bitmap bendingLinesBitmap=null;
	
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
    private float adjustedOverlayHeight=0;
    private float adjustedOverlayWidth=0;
    private float ibookHorizontalTranslation=0;
    private float ibookVerticalTranslation=0;
    private float transformedIbookWidth=0;
    private float warningTextHorizontalTranslation=0;
    private float warningTextVerticalTranslation=0;
    private float overlayHorizontalTranslation=0;
    private float overlayVerticalTranslation=0;
    private float overlayBottomCoordinate=0;
    private float overlayTopCoordinate=0;
    private float overlayLeftCoordinate=0;
    private float overlayRightCoordinate=0;
    private float StartXCoordinate;
    private float StartYCoordinate;
    private float EndXCoordinate;
    private float EndYCoordinate;
    private double steeringWheelValue;
  
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
    		       		   
    		   //get steering wheel angle
    		   steeringWheelValue = (float)VehicleMonitoringService.SteeringWheelAngle;
    		   
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
    			   screenToOverlayHeightRatio = (float)0.5*screenHeight/(float)bmpOverlayLines.getHeight();
    			   
    			   //adjust bmpOverlayLines accordingly to screen size
    			   adjustedOverlayWidth = screenToOverlayWidthRatio*bmpOverlayLines.getWidth();
    			   adjustedOverlayHeight = screenToOverlayHeightRatio*bmpOverlayLines.getHeight();
    			   
    			   overlayVerticalTranslation = (float)(0.5*screenHeight)-(float)(0.35*adjustedOverlayHeight);
    			   overlayHorizontalTranslation = (float)(0.5*screenWidth)+(float)(0.5*adjustedOverlayWidth);
    			   
    			   Matrix overlayMatrix = new Matrix();
    			   overlayMatrix.preScale(-screenToOverlayWidthRatio, screenToOverlayHeightRatio);
    			   overlayMatrix.postTranslate(overlayHorizontalTranslation, 
    					   overlayVerticalTranslation);
    			   
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
    			   
    			   //make overlay fade with steering wheel angle
    			   Paint overlayPaint = new Paint();
    			   //overlayPaint.setAlpha(255-(int)steeringWheelValue);
    			   
    			   // draw bitmaps to canvas
    			   canvas.drawBitmap(bmp, cameraFeedMatrix, null);
    			   canvas.drawBitmap(bmpOverlayLines, overlayMatrix, overlayPaint);
    			   canvas.drawBitmap(bmpIbook, ibookMatrix, null);
    			   canvas.drawBitmap(bmpWarningText, warningTextMatrix, null);
    			   
    			   
    			   //get coordinates of overlay
    			   overlayBottomCoordinate = adjustedOverlayHeight + overlayVerticalTranslation;
    			   overlayTopCoordinate = overlayVerticalTranslation;
    			   overlayLeftCoordinate = (float)0.5*(screenWidth-adjustedOverlayWidth);
    			   overlayRightCoordinate = overlayLeftCoordinate + adjustedOverlayWidth;

       			   //Log.i(TAG, "SteeringWheel Angle = " +steeringWheelValue);
    			   
    			   //Set Paint color
       			   Paint linesPaint = new Paint();
       			   linesPaint.setStyle(Paint.Style.STROKE);
       			   linesPaint.setColor(Color.WHITE);
       			   linesPaint.setStrokeWidth(3);
       			   //linesPaint.setAlpha((int)steeringWheelValue*3);
       			   
       			   int z;
       			   int xpixel = (int)overlayLeftCoordinate; 
       			   int numberGreenTransitions=0;
       			   int pixel;
       			   int leftPositionLeftGuideTop=0;
       			   int rightPositionLeftGuideTop=0;
       			   int midpointLeftGuideTop=0;
       			   int[] greenValue;
       			   greenValue = new int[(int)adjustedOverlayWidth];
       			   
       			   for (z=1; z < adjustedOverlayWidth/2; z++){
       			   
       			   pixel = bmpOverlayLines.getPixel(xpixel, (int)overlayTopCoordinate+5);
       			
       			   greenValue[z] = Color.green(pixel);
       			   
       			   Log.w(TAG, "GreenValue = " +greenValue[z] + " @ pixel " + xpixel);
       			   		
       			   		if (greenValue[z] > greenValue[z-1]) {
       			   			
       			   			leftPositionLeftGuideTop = xpixel-1;
       			   		}
       			   		
       			   		
       			   		else if (xpixel == 481) {
       			   			rightPositionLeftGuideTop = xpixel;
       			   		}
       			   
       			   xpixel++;
       			   
       			   }
       			   
       			   midpointLeftGuideTop = (leftPositionLeftGuideTop + rightPositionLeftGuideTop)/2;
       			   Log.w(TAG, "Midpoint = " +midpointLeftGuideTop);

       			   //Log.w(TAG, "Number of Green Transitions = " + numberGreenTransitions);
       			   
       			   StartYCoordinate = overlayBottomCoordinate ;
       			   StartXCoordinate = overlayLeftCoordinate + 20/* + (float)steeringWheelValue*/;
       			   
       			   canvas.drawLine(StartXCoordinate, StartYCoordinate, midpointLeftGuideTop, 
       					   overlayTopCoordinate, linesPaint);
       			   
       			   Path path = new Path();
       			   //PointF point1 = new PointF(StartXCoordinate, StartYCoordinate);
       			   //PointF point2 = new PointF(midpointLeftGuideTop, overlayTopCoordinate);
       			   //PointF point3 = new PointF(midpointLeftGuideTop+50, overlayTopCoordinate+100);
       			   path.cubicTo(StartXCoordinate, StartYCoordinate, midpointLeftGuideTop, overlayTopCoordinate,
       					midpointLeftGuideTop+50, overlayTopCoordinate+100);
       			   
       			   canvas.drawPath(path, linesPaint);
       			   
       			   /*while (EndYCoordinate > overlayTopCoordinate) {
       				   canvas.drawLine(StartXCoordinate, StartYCoordinate, 
   						   EndXCoordinate, EndYCoordinate, linesPaint);
  			
       				   StartXCoordinate = EndXCoordinate;
       				   StartYCoordinate = EndYCoordinate;
       				   //EndXCoordinate = EndXCoordinate + (float)0.000005*x*xchange;
       				   //EndYCoordinate = EndYCoordinate - ychange;
       				   x+=18;
  				   		   
       			 }*/
       			   
       			   
       			   
       			   
       			   
    			   /*int x=10;
    			   float ychange = (float)1;
    			   float xchange = 40//(float)steeringWheelValue;
    			   
    			   //start line
    			   StartYCoordinate = overlayBottomCoordinate ;
    			   StartXCoordinate = overlayLeftCoordinate + 30 + (float)steeringWheelValue;
    			   
    			   //first coordinates
    			   //EndXCoordinate = StartXCoordinate// + (float)0.0001*x*xchange + 1;
     			   //EndYCoordinate = StartYCoordinate// - ychange;
    			   
     			   //draw arc with many lines
    			   
     			   //for(x=0; x<220; x++){
     			  */
    			   
    			
    			   			   
    			   
    			   
    			   
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
			
			bmpOverlayLines = BitmapFactory.decodeResource(getResources(), R.drawable.linesoverlay3); 
		}
		
		if(bmpIbook==null){
			
			bmpIbook = BitmapFactory.decodeResource(getResources(), R.drawable.ibook);
		}
		
		if(bmpWarningText==null){
			
			bmpWarningText = BitmapFactory.decodeResource(getResources(), R.drawable.pleasechecksurroundings);
		}
		
		if(bendingLinesBitmap==null){
			
			bendingLinesBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bendinglines);
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

