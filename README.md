Rearview Camera with OpenXC
=================================

The Rearview Camera is an Android app that uses OpenXC and a standard USB
webcam to add a rear-facing camera to a car that didn't come equipped with one
from the factory. Charles Reitz developed the first version of this app as a
summer intern at Ford in 2012.

Rearview cameras are undeniably useful, and have been found to minimize
accidents while backing up. They will even be required in all new cars in the
United states, [starting in
2014](http://www.nytimes.com/2012/02/28/business/us-rule-set-for-cameras-at-cars-rear.html?_r=0).
What about all the existing cars on the road? Using OpenXC to determine the
current gear and the support for [USB webcams][USB webcam] in most Android
devices, you can make an aftermarket system relatively inexpensively.

**Disclaimer** This application is intended to be used with a display device
that has a fixed, preferably permanent location in the vehicle (mounted on the
dashboard, for example). Never mount anything on the dash such that the driver's
view is impeded. While the rearview camera application may help increase
visibility, the OpenXC platform does not make any claims of a safety benefit.
This application is intended as a proof-of-concept.

## Dependencies

**Hardware**

* OpenXC-compatible [Android
   Device](http://openxcplatform.com/android/index.html) (3.2 or later) that
   can [access UVC video devices](#android-usb-webcam).
* [USB Webcam][] supporting UVC and a 640x480 resolution in the YUYV format.
* [OpenXC Vehicle
   Interface](https://openxcplatform.com/vehicle-interface/index.html) (VI) with
   vehicle-specific firmware
* [OpenXC-supported
   vehicle](http://openxcplatform.com/vehicle-interface/output-format.html)
* USB hub (if you will be using the VI over USB in addition to the camera)

**Software**

* [OpenXC Android
  Library](http://openxcplatform.com/getting-started/library-installation.html)
* [Android SDK](http://developer.android.com/sdk/index.html)
* [Android NDK](http://developer.android.com/tools/sdk/ndk/index.html)
* [Android Webcam library](https://github.com/openxc/android-webcam)

[USB webcam]: http://www.logitech.com/en-us/product/webcam-C110?crid=34

## Installation instructions

1. Install the [OpenXC
   Enabler](http://openxcplatform.com/getting-started/library-installation.html#enabler) to the device.
1. Run `ndk-build` in the `android-webcam` project folder to compile the native
   camera library.
1. Install the `RearviewCamera` application to the device.
1. Start the app once, then restart device to make sure the
   `VehicleMonitoringService` service is properly registered to begin at bootup
   and start monitoring the gear position.
1. Mount the Android device where it will be secure and visible to the driver
   when backing up, but not in the way of normal driving.
1. Mount the USB webcam on the rear of the vehicle, possible attached to the
   trunk lid or license plate holder. This make take some creativity.
1. Run a USB extension cable from the webcam, through the trunk and up to the
   tablet.
1. Assuming the VI is attached to the OBD-II port, connect the tablet to it via
   USB or Bluetooth.

**With Maven:**

Follow the directions to install the [Android Webcam
library](https://github.com/openxc/android-webcam), then:

    $ cd rearview-camera
    $ mvn package android:deploy android:run

## Functionality

The app continuously reads vehicle data from the VI and watches for changes in
`transmission_gear_position` and the `steering_wheel_angle`.

When the gear position changes to `reverse`, the RearviewCamera app will launch
a video display from the camera. The display includes guide lines indicating
relative distance from objects in the video and steering wheel guide lines. Note
that the video feed is mirrored to mimic the view in the rear view mirror.

The guide lines animate in response to changes in the steering wheel angle. They
illustrate a rough estimate of the path of the vehicle if you continue to move
with the current steering wheel angle. The ratio of brightness between the guide
lines and distance measures shift as you turn the wheel more, drawing your
attention to the more extreme measurements.

When you shift out of `reverse` into `drive` or any other gear, the camera feed
will shut off.

## License

Copyright (c) 2012-2013 Ford Motor Company
Licensed under the BSD license.
