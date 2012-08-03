FORDBACKUPCAM : Android Application


Warning:  This document assumes the user has access to a CAN Translator.  


Purpose:

This application seeks to mimic the functionality of the built in Ford backup camera systems.  The built in systems serve
as an aid to the driver by allowing him/her to see any obstacles that may be behind the vehicle in order to circumvent a
collisional incident.  This application provides an alternative to the built in camera systems i.e. an aftermarket option.  


Safety:

FordBackupCam is intended to be used on a device that has a fixed location, preferably permanently, in the vehicle.  For example,
mounted on the dash.  Disclaimer: Never mount anything on the dash such that the view of the driver is impeeded.  As this application 
serves to increase the level of safety, the tablet should never be mounted in such a way that safety is sacrificed.  


Hardware Needed:

1) Android Device (3.2 or later)
2) CAN Translator, OpenXC Platform
3) USB Webcam
4) Ford Vehicle (list of supported vehicles can be found at openxcplatform.com)
5) USB splitter

  -FordBackupCam requires a connection via USB from the Android device to a CAN Translator (instructions on how to install a 
    CAN Translator in a vehicle can be found at openxcplatform.com).  As both the webcam and the translator require a full-sized USB
    connection, a USB splitter is required to connect both the CAN Translator and the USB webcam to the Android device.  


Installation instructions:
  1)  Install OpenXC Enabler application on android device
  2)  Install FordBackupCam application on same device
  3)  Restart tablet (optionally, manually launch application after completing steps 4-10).  This must only be done once 
	after installation
  4)  Mount android device in vehicle
  5)  Mount USB webcam on rear of vehicle
  6)  Attach USB extension cable to camera and run it up to the front of the vehicle inside.
  7)  Insert USB extension cable into splitter
  8)  Connect CAN Translator to OBD-II port (if not already done)  
  9)  Connect a separate USB cable, one end to the CAN Translator (more detailed instructions at openxcplatform.com), one end 
	into the splitter (full sized end should be in the splitter)
  10) Insert splitter into android device.

FordBackupCam is now ready to be used.  


Functionality/use:  

  Through the use of the CAN Translator, FordBackupCam continually reads real-time vehicle data.  The app monitors two pieces of 
    data:  1)  The gear position of the transmission.
	   2)  The angle of the steering wheel.
       
  1) By monitoring the gear position data, the application can respond in such a way that mimics the Ford built in backup camera.  If 
      the tablet has been restarted or launched manually (we recommend the restart), the app will respond to the vehicle being put in
      reverse.  When the vehicle is put in reverse, the application will launch and show the video feed from the attached USB camera.  If
      installed correctly, the user will see what is behind the vehicle.  In order to make use of the camera simpler and require less 
      active thought from the user, the application mirrors the camera feed such that an object on the right side behind the vehicle will 
      appear on the right side of the tablets screen, despite the fact that the rear-facing camera "sees" that the object is on its left.  

  When the vehicle is taken out of reverse, the application will automatically close, returning the tablet to its previous screen before
    FordBackupCam was launched.  

  2)  By monitoring the angle of the steering wheel, the application can again mimic the Ford built in backup camera.  When the driver
	turns the steering wheel, the car will obviously not continue along the straight, guiding lines that appear over top of the video
	feed. Instead, the vehicle will curve to the side.  When the vehicle turns the wheel to the left, the vehicle will turn backwards 
	and to the left when in reverse, and new guiding lines will appear on the screen in order to APPROXIMATE the path of the vehicle.  The 
	greater the angle of the steering wheel, the more the vehicle will turn, and thus the new guiding lines will shift and angle 
	themselves more.  This also works (mirrored, of course) if the wheel is turned to the right.  

The user's attention should be drawn to the dynamic guiding lines more as the magnitude of the angle of the steering wheel increases, 
so the straight guiding lines fade away while the dynamic lines become brighter and more opaque.  The opposite is true as the 
magnitude of the angle of the steering wheel increases.  The dynamic lines vanish when the vehicles steering wheel is straight.  



Relaunching FordBackupCam:

  Again with safety as a top priority, the application is designed to close itself and display a warning message if one of the 
    USB devices is unplugged.  The reason? If the camera accidentally came unplugged, the user would see the last video frame that was
    captured.  If this occurred while the driver was reversing, the user might think that nothing was behind the vehicle when in fact 
    something has entered its path, which would be the result of the video feed not updating.  Or, on the other hand, if the Android
    device is disconnected from the CAN Translator, the angle of the steering wheel would not continue to be updated and thus yield 
    a worse approximation to the potential path of the vehicle.  In addition, the app would not be able to continue responding to the
    status of the transmission, and thus the app would not close/launch as intended.  The user might think the tablet or application 
    has frozen, when in fact a cable was simply unplugged.  



Code intricacies:

The android project contains four main classes:
1) BackupCameraActivity
2) BootupReceiver
3) CameraPreview
4) VehicleMonitoringService

  1)  BackupCameraActivity

    -This is the main activity.  
    -When created, it starts the VehicleMonitoringService.
    -It contains two receivers:  1) receiver that listens for a USB device being detached.  When this intent is received, it builds
				     a dialog that informs the user of what has happened, and forces them to close the acttivity.
				  2) receiver that listens for a closing intent from the VehicleMonitoringService.  When this intent
				     is received, the app closes by calling is finish() method.   
				     
    -Includes a method that monitors whether the activity is active or not, called isRunning().  This method is accessed by the
	VehicleMonitoringService to determine whether or not the activity needs to be launched/closed (see VehicleMonitoringService, #4).

  2)  BootupReceiver
  
    -This is a receiver whose purpose is to listen for an intent sent by the Android system that the device has been booted.  When 
	received, the receiver launches the VehicleMonitoringService.  The purpose of this is that it gives the app the ability to 
	monitor the status of the transmission without the need for the application to be manually launched.  The user can simply
	turn on the tablet and it's ready to go.

   3) CameraPreview
    
      -This is the view to which the screen is set.  
      -Every item displayed on the screen is created as a bitmap in this view.  
      -The camera feed is converted to a bitmap throug the use of an ImageProc.c native jni file that grabs the camera, prepares it
	  with base, etc.  
      -A png image containing the red, yellow, and green straight overlay lines is located in res/drawable.  CameraPreview accesses
	  this file and converts it to a bitmap.  This bitmap is drawn to the canvas after the video feed, thus overlaying it on top
	  of the video feed.  
      -A png image of an ibook icon is also located in res/drawable and accessed by CameraPreview to be converted to a bitmap.  This
	  is also drawn to the canvas after the video feed, thus overlaying it.  
      -To the right of the ibook icon is a warning message that reads "Please Check Surroundings for Safety".  This is drawn with the
	  canvas.drawText method.  In order to make this text visible regardless of what is behind it in the video feed, an outline is 
	  drawn with black, slightly larger font around the text.
      -The last bitmap that is drawn to the canvas is one that contains the dynamic lines.  As previously discussed, the dynamic lines bend and 
	  change opacity based on the angle of the steering wheel.  The angle of the steering wheel is accessed through the 
	  VehicleMonitoringService (see #4).
       
      -In order to make the app function on all screen sizes, CameraPreview contains several methods that relate the screen size to
       the size of the bitmaps drawn.  Each bitmap drawn is manipulated through the use of a matrix that is scaled relative to the 
       dimensions of the screen, thus allowing the proportions of all items displayed to remain constant from screen to screen, 
       independent of size.  This is done by finding the ratio between the native size of the bitmap and the size of the screen and 
       multiplying by that ratio.  Proportions are thus preserved on every screen.  
       
       -When the surface is destroyed, the stopCamera() method is called, which can be found in the ImageProc.c jni file.  
       
  4) VehicleMonitoringService
    
      -This service is launched both on bootup by BootupReceiver (see #2) and when BackupCameraActivity (see #1) is launched.  
      -The purpose of this service is to bind with the VehicleManager, a service performed by the OpenXC Enabler application.  The
	  service implements two listeners, one for the steering wheel angle and one for the transmission gear position.  
      -The service also monitors the status of the activity (whether is is running or not).  By monitoring both the status of the
	  transmission and the status of the activity, the service can launch the activity appropriately.  When the service detects
	  that the vehicle has been put into reverse, it checks to see if the activity is running or not through the isRunning() 
	  method in BackupCameraActivity.  If the activity is not running, then both conditions are satisfied for it to launch the 
	  activity.  In addition, if the service detects that the vehicle is no longer in reverse, it checks whether the activity 
	  is running or not through the same method.  If it is running and the car is not in reverse, it sends an intent to 
	  BackupCameraActivity.  When that intent is received, BackupCameraActivity calls its finish() method, which closes the 
	  application.  


Native Functions:

  -As previously mentioned, there are two native function files: a header file (ImageProc.h) and function file (ImageProc.c).  These 
    files handle the tasks of dealing with the camera, including checking the camera base, opening the device, initializing the device, 
    stopping the device, etc.  
  
  -NOTE:  If changes are made to these files, the project must be rebuilt: simply reinstalling the application on an Android device
	      does not implement these changes.  This is easiest done with installling the 
	      NDK (http://developer.android.com/tools/sdk/ndk/index.html)
    

    
Dependencies:

  1)openxc is a library.  


Requirements:
  
  1) OpenXC Enabler application must also be installed on device. 

  2) USB WebCam is UVC camera, and it supports 640x480 resolution with YUYV format.

  3) The kernel is V4L2 enabled, e.g.,

	CONFIG_VIDEO_DEV=y

	CONFIG_VIDEO_V4L2_COMMON=y

	CONFIG_VIDEO_MEDIA=y

	CONFIG_USB_VIDEO_CLASS=y

	CONFIG_V4L_USB_DRIVERS=y

	CONFIG_USB_VIDEO_CLASS_INPUT_EVDEV=y

  4) The permission of /dev/video0 is set 0666 in /ueventd.xxxx.rc


Guaranteed supported platform : Toshiba Thrive running Android 3.2
		     

This application will also work on V4L2-enabled pandaboard and beagleboard.



Last Modified:  8/3/2012

