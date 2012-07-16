package com.camera.simplewebcam;

import com.openxc.VehicleManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class Main extends Activity {
	
	CameraPreview cp;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		cp = new CameraPreview(this);
		setContentView(cp);
		
	}
	
	
	
	
}
