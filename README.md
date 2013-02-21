OpenXC Backup Camera
======================

This document assumes the user has access to a CAN Translator.

## Purpose

This application, FordBackupCam, seeks to mimic the functionality of the built
in Ford backup camera systems. The built-in systems serve as an aid to the
driver by allowing him/her to see any obstacles that may be behind the vehicle
in order to avoid a collision. This application provides an alternative to the
built in camera systems by combining and OpenXC enabled Android app with a [USB
webcam][].

**Safety Note:** FordBackupCam is intended to be used with a display device that
has a fixed location, preferably permanently, in the vehicle. For example,
mounted on the dash. Disclaimer: Never mount anything on the dash such that the
driver's view is impeded. As this application serves to increase the level of
safety, the tablet should never be mounted in such a way that safety is
sacrificed.

![Backup Camera Screenshot](/images/screenshots/backup_cam_1.png)

## Hardware Needed

1. Android Device (3.2 or later)
1. [USB Webcam][]
1. OpenXC Vehicle Interface with vehicle-specific firmware
1. OpenXC-supported vehicle (full list of [supported
   vehicles](/vehicle-interface/index.html))
1. USB hub

FordBackupCam requires a connection from the Android device to a Vehicle
Interface (installation instructions [here](/vehicle-interface/index.html)). As
both the webcam and the VI require a full-sized USB connection, a
USB hub is required to connect both the CAN Translator and the USB webcam to the
Android device. However, the VI can be connected via bluetooth in
which case you don't need a USB hub as only 1 USB device will be plugged in.

## Installation instructions

1. Install OpenXC Enabler application on android device
1. Run `ndk-build` to compile the native camera library.
1. Install FordBackupCam application on same device
1. Restart tablet (optionally, manually launch application after completing
   steps 4-10. This must only be done once after installation
1. Mount android device in vehicle
1. Mount USB webcam on rear of vehicle
1. Attach USB extension cable to camera and run it up to the front of the
   vehicle inside.
1. Insert USB extension cable into hub
1. Connect CAN Translator to OBD-II port (if not already done)
1. Connect a separate USB cable, one end to the CAN Translator (more detailed
   instructions at openxcplatform.com), one end into the hub (full sized
   end should be in the hub, micro end in the translator)
1. Insert hub into android device.

FordBackupCam is now ready to be used.

![Backup Camera Installed on Focus](/images/screenshots/backup_cam.jpg)

## Functionality

The app continuously reads real-time vehicle data from the VI, specifically:

* `transmission_gear_position`
* `steering_wheel_angle`

### Gear Position Input

Once initially launched and then minimized, the app will automatically respond
when the vehicle is put into `reverse`. Once the vehicle is in `reverse` the
application will launch and show the video feed from the attached USB camera. If
installed correctly, the user will see what is behind the vehicle. In order to
simplify the user-experience, the application mirrors the camera feed such that
an object on the right side behind the vehicle will appear on the right side of
the tablets screen, just like a rear-view mirror. Color coded guidelines are
overlaid on top of the video feed to provide a distance reference for the
driver.

When the vehicle is taken out of reverse, the application will automatically
close, returning the tablet to its previous screen before FordBackupCam was
launched.

### Steering Wheel Angle Input

When the driver turns the steering wheel, the car will obviously not continue
along the straight guiding-lines overlaid on the video feed. When the driver
backs up with the wheel turned left, the vehicle will turn backwards and to the
left accordingly so new guiding lines will appear on the screen in order to
APPROXIMATE the path of the vehicle. The greater the angle of the steering
wheel, the more the vehicle will turn, and thus the new guiding lines will shift
and angle themselves more. This also works (mirrored, of course) if the wheel is
turned to the right.

The user's attention should be drawn to the dynamic guiding lines more as the
magnitude of the angle of the steering wheel increases, so the straight guiding
lines fade away while the dynamic lines become brighter and more opaque. The
opposite is true as the magnitude of the angle of the steering wheel increases.
The dynamic lines vanish when the vehicles steering wheel is straight.

![Backup Camera Sequence](/images/screenshots/backup_cam_sequence.gif)

## Relaunching FordBackupCam

Again with safety as a top priority, the application is designed to close
itself and display a warning message if one of the USB devices is unplugged.
The reason? If the camera accidentally came unplugged, the user would see the
last video frame that was captured. If this occurred while the driver was
reversing, the user might think that nothing was behind the vehicle when in
fact something has entered its path, which would be the result of the video
feed not updating. Or, on the other hand, if the Android device is
disconnected from the CAN Translator, the angle of the steering wheel would
not continue to be updated and thus yield a worse approximation to the
potential path of the vehicle. In addition, the app would not be able to
continue responding to the status of the transmission, and thus the app would
not close/launch as intended. The user might think the tablet or application
has frozen, when in fact a cable was simply unplugged.

Currently, the app must be relaunched manually the first time after a
disconnect in order to restart the VehicleMonitoringService (see below).

It is recommended that you check the enabler or VehicleDashboard in order
to ensure that messages are flowing from the CAN Translator.

## Requirements

* CameraPreview, native implementaiton of a web cam interface using UVC.
* Android SDK
* Android NDK
* OpenXC Enabler application must also be installed on device.
* USB WebCam is UVC camera, and it supports 640x480 resolution with YUYV
format.

* The kernel is V4L2 enabled, e.g.,

    CONFIG_VIDEO_DEV=y
    CONFIG_VIDEO_V4L2_COMMON=y
    CONFIG_VIDEO_MEDIA=y
    CONFIG_USB_VIDEO_CLASS=y
    CONFIG_V4L_USB_DRIVERS=y
    CONFIG_USB_VIDEO_CLASS_INPUT_EVDEV=y

* The permission of /dev/video0 is set 0666 in /ueventd.xxxx.rc

Guaranteed supported platform : Toshiba Thrive running Android 3.2

This application will also work on V4L2-enabled pandaboard and beagleboard.

[USB webcam]: http://www.logitech.com/en-us/product/webcam-C110?crid=34
