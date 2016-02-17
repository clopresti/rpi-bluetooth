# Using Bluetooth With The Raspberry Pi

The following instructions show how to setup and use a USB Bluetooth adapter with the Raspberry Pi.
Please note that these instructions only apply to the latest Raspbian Jessie distribution. 
If you are using a different distribution or an older Raspbian build such as Wheezy some of the instructions will need to be changed as the Bluetooth stack and utilities have changed with the Jessie release and BlueZ 5.23. 
  
A lot of the instructions I found online were relating to the older wheezy builds and it took some extra time to find the correct way to do things with Jessie. 
There were also a few configuration changes that needed to be made in order to work around some known issues/bugs relating to the RFCOMM (serial over Bluetooth) communication on the Pi. 
I have documented all this and hopefully I can save you some time if you are trying to do a similar things.

I am using a USB Bluetooth 4.0 adapter with a Broadcom BCM20702 chipset but there are many other supported Bluetooth adapters including those with CSR (Cambridge Silicon Radio) chipsets. I tested on a Raspberry Pi 2 but all this should work with any version of the Raspberry Pi as long as you are running the latest Raspbian Jessie distribution.

**Video Tutorial:** https://www.amazon.com/review/RANPHRIBO5H6L/ref=cm_cr_rdp_perm

## Part 1. Setup

**First make sure your Pi is up to date:**
```
> sudo apt-get update
> sudo apt-get upgrade
> sudo rpi-update
> sudo reboot
```

**Insert your USB Bluetooth adapter into one of the USB ports on the Pi and enter:**
```
> lsusb
```
You should see output similar to the following:
```
< Bus 001 Device 00x: ID xxxx:xxxx Broadcom Corp.
```
If you used an adapter with a different chipset like CSR you might see 'Cambridge Silicon Radio' insead of 'Broadcom Corp'. 
If you do not see something similar to this it might mean that there is something wrong with your adapter or it is not recognized/supported.

**Next install the bluez and bluetooth manager packages:**
```
> sudo apt-get install bluetooth blueman bluez python-gobject python-gobject-2
```
**Now you can check you your adapter state:**
```
> sudo hciconfig
```
You should see output similar to the following:
```
BD Address: XX:XX:XX:XX:XX:XX
UP RUNNING PSCAN ISCAN
```
The [XX:XX:XX:XX:XX:XX] part is the MAC address of your Bluetooth adapter
If you do not see "UP RUNNING" enter the following to turn on Bluetooth:
```
> sudo hciconfig hci0 up
```

## Part 2. Paring and Connecting Devices

**Start the command line Bluetooth manager**
```
> bluetoothctl
```
You should see a line similar to:
```
Controller XX:XX:XX:XX:XX:XX raspberrypi [default] 
```
__*'raspberrypi'*__ is the default name you will see when pairing/connecting to the Pi from other devices

Next Enter:
```
> agent on
```
You should see:
```
< Agent Registered
```
Next Enter:
```
> default-agent
```
You should see:
```
< Default agent request successful
```
Start scanning for devices:
```
> scan on
```
You should see:
```
< Discovery started
```
Turn on your device and enter your device into pairing mode (this is device dependent).

You should see any devices that are found
```
< Device XX:XX:XX:XX:XX:XX [Device Name/Type]
```
Find the MAC address of the device you want to pair (XX:XX:XX:XX:XX:XX) and enter:
```
> pair [MAC ADDRESS] 
```
Substitute [MAC ADDRESS] with the MAC address of your device

tip - you can use tab completion to enter MAC addresses

__*If your device requires a pin to pair enter the displayed pin next*__

You should now see:
```
< Pairing successful
```
Now you can connect to the device:
```
> connect [MAC ADDRESS] 
```
Again substitute [MAC ADDRESS] with the MAC address of your device

You should see:
```
< Connection successful
```
You can see a list of all paired devices by entering:
```
> paired-devices
```
You should see all your paired  device(s) listed

If you want to automatically connect to a device whenever it is turned on you need to trust it:
```
> trust [MAC ADDRESS] 
```
Again substitute [MAC ADDRESS] with the MAC address of your device

You should see:
```
< Changing XX:XX:XX:XX:XX:XX trust succeeded
```
To make your Pi discoverable so that you can pair/connect to it from the other device enter:
```
> discoverable on
```
To exit the command line Bluetooth manager enter:
```
> quit
```

## Part 3. Setup and Use a Bluetooth Speaker/Headphones

Install required pulseaudio and supporting packages
```
> sudo apt-get install pulseaudio pulseaudio-module-bluetooth pavucontrol bluez-firmware
```
Install the mplayer package:
```
> sudo apt-get install mplayer
```
Pair and connect to your Bluetooth speaker or headphones using instructions above

To play an audio file on the connected Bluetooth device enter:
```
> mplayer -ao pulse [file.mp3]
```
Substitute [file.mp3] with the path of the audio file to stream

Your audio file will begin to stream

**Make sure that you select your Bluetooth device as the current audio output device from the pulse audio GUI manager found unnder the Pi menu**

## Part 4. Test Bluetooth Serial Communication With Android Device

### A. Raspberry Pi Side
First pair your Android phone from the Pi using the paring instructions in Part 2

**Important Bug Fix:** Edit `/etc/bluetooth/main.conf` configuration file
```
 > sudo nano /etc/bluetooth/main.conf
 ```
 Go to bottom of file and add the following line of text: 
 ```
 DisablePlugins = pnat
 ```
Then save and exit (Ctrl-O Ctrl-X)

**Next setup the Serial Port:**
```
> sdptool add sp
```
Listen for connections on the serial port:
```
> sudo rfcomm listen hcio&
```
You should see the following:
```
< Waiting for connection on channel 1
```
The '&' at the end of the command allows it to run in the background. Press enter to return to the command line.

### B. Android Device Side

Install the BlueTerm app on your andriod device from the Google play store:
https://play.google.com/store/apps/details?id=es.pymasde.blueterm&hl=en

Connect to the Pi from the blueterm app on your Android phone:
  1. Open the blueterm app on your Android phone
  2. Press three dot bottom on bottom right corner of the blueterm app screen
  3. Select "Connect device"
  4. Choose the already paired __'raspberrypi'__ from the list of paired devices
 
The blueterm app should now be connected to your Pi

To echo what you type in the BlueTerm app to the Pi Terminal Screen enter:
```
> cat /dev/rfcomm0
```
Now you can type something in the bluterm app and you should see it echoed to your Pi screen

## Part 5. Custom Android Application to Control the Pi Over Bluetooth

In this section we will write a Python script on the Pi side and a custom Android application to control the Pi using Bluetooth. In this simple case we will just be turning an LED on/off from the Android device but you can imagine adding a Z-Wave adapter to your Pi and controling your Home Automation lighing and other devices from your phone.

### A. Raspberry Pi Side
First make sure that you can connect and echo text using the blueterm app (Part 4)

Install the python Bluetooth package:
```
> sudo apt-get install python-bluez
```
**Important bug fix:** Edit `/lib/systemd/system/bluetooth.service` configuration file:
```
> sudo nano /lib/systemd/system/bluetooth.service
```
Change the following line:
```
ExecStart=/usr/lib/bluetooth/bluetoothd
```
To the following:
```
ExecStart=/usr/lib/bluetooth/bluetoothd --compat
```
If you don't do this you might get a cryptic error message about a file not found when running the python script.

**Run the `pi-rfcomm.py` script**

Change directory to where the `pi-rfcomm.py` script is located 

`pi-rfcomm.py` can be found in the `/RaspberryPi` directory of this repo
```
> sudo python pi-rfcomm.py
```
Be sure to run the python script using 'sudo' so that it has the reqired permissions to run

The Python RFCOMM server side is now running.

If you attach the positive (long) leg of an LED to pin 7 and the negative (short) leg of the LED to pin 9 (ground) you can toggle it on/off from The Android App. Be sure to include a 270-300 Ohm resistor to avoid buring out the LED. These pins are for Raspberry Pi 2. If you are using a different version of the Pi the GPIO pins may differ and you will need to change the python script to refect the actual GPIO pins used.

### B. Android Device Side
Build and run the Android Application found in the `/Android` directory of this repo

This is an android studio project but you can build and run the app on your device in any way you prefer

The important files are:

`/Android/app/src/main/AndroidManifest.xml`

`/Android/app/src/main/java/net/clopresti/rpibluetooth/MainActivity.java`

`/Android/app/src/main/res/layout/activity_main.xml`

`/Android/app/src/main/res/layout/content_main.xml`

Once the App is built and running on your Android device you can click the "connect" button 

On the Pi side you should see "Connection from: (...)" and you are now connected to the Pi

Press the "on"/"off" toggle button

On the Pi side you should see "Received [on]" "Received [off]" messages as you toggle the button

If you connected an LED to your Pi (described above) you will be able to toggle the LED on/off using your Android device
