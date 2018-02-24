# Chicago Commutes

## Description

Chicago Commutes is a brand new public transit tracker making it easier to travel in Chicago. Easy and quick to use, Chicago Commutes has all levels of familiarity with the city in mind. Quick loading times and easy searchability for frequent transit users. For first time transit users Google Street View integration and internal bus and train locators ensure greater ease of travel.
* Extremely user friendly UI.
* Easy Filtering System: See only the buses and trains that you want to see and filter out the rest.
* Locate Nearby Stations and Bus Stops: Chicago Commutes locates the nearest bus and train locations so that you don’t have to, making it easier for travel throughout the entire city of Chicago.
* Integrated Google Street View: Each bus and train stop has a Google Street View picture associated with it, so you know exactly where you need to be in order to get where you want to be!
* Search any train stop or bus route and get live arrival times.
* Follow buses and trains live on the map
* Read the last alerts from CTA.
* Free! No Ads!

Download on [play store](https://play.google.com/store/apps/details?id=fr.cph.chicago).

## Build

* Duplicate `keystore.properties.template` and rename the copy to `keystore.properties`. Update the new file with correct information.
* Do the same thing for `keystore.properties.template`.
* You are ready to build it!

Build debug APK:

`./gradlew clean assembleDebug`

Build prod APK:

`./gradlew clean build`


## Issues

To fix the emulator not starting in arch linux: `ln -sf /usr/lib/libstdc++.so.6  $ANDROID_SDK/emulator/lib64/libstdc++`
