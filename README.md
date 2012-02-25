
HOW TO CONNECT TO A REAL PHONE:

sudo vim /etc/udev/rules.d/51-android.rules
# add here
SUBSYSTEM=="usb", ATTR{idVendor}=="0fce", MODE="0666", GROUP="plugdev"
# "0fce" is for Sony Ericsson Xperia


sudo /etc/init.d/udev restart
cd $ANDROID_SDKS/platform-tools
./adb kill-server
./adb start-server
./adb devices
