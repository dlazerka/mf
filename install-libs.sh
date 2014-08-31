#!/bin/bash

# Run this before trying "android" module.


mvn install:install-file -Dfile=$ANDROID_HOME/add-ons/addon-google_apis-google-19-1/libs/maps.jar \
	-DgroupId=com.google.android.maps \
	-DartifactId=maps \
	-Dversion=19.1 \
	-Dpackaging=jar

#mvn install:install-file -Dfile=$ANDROID_HOME/platforms/android-19/android.jar \
#	-DgroupId=com.google.android \
#	-DartifactId=android \
#	-Dversion=19 \
#	-Dpackaging=jar
#
#
#mvn install:install-file -Dfile=$ANDROID_HOME/extras/google/google_play_services/libproject/google-play-services_lib/libs/google-play-services.jar \
#	-DgroupId=com.google.android.gms \
#	-DartifactId=google-play-services \
#	-Dversion=13 \
#	-Dpackaging=jar
#
