#!/usr/bin/env bash

# https://android.googleapis.com/gcm/send

if [ -z "$1" ]
  then
    echo "Usage: send-gcm.sh <token>"
    exit 1
fi

API_KEY="AIzaSyB9onbgaDOcYC-4-r36D3p_zdiQsOVsE70"

MESSAGE='{"to":"'"$1"'",
        "notification":{
            "body":"Test body",
            "title":"Test",
            "icon":"ic_launcher"
        },
        "data":{
            "type": "LocationRequest"
            "payload": "test"
        }
    }'

echo "Sending $MESSAGE"

curl --header "Authorization: key=$API_KEY" \
     --header "Content-Type: application/json" \
     https://fcm.googleapis.com/fcm/send -i \
     -d "$MESSAGE"

echo
