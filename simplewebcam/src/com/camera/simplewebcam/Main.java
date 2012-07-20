package com.camera.simplewebcam;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class Main extends Activity {
	
	private final static String TAG = "MainCamera";
   
	CameraPreview cp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		cp = new CameraPreview(this);
		setContentView(cp);

		Log.w(TAG, "In onCreate");

	}
}
	 
	

