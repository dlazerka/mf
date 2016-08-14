#!/usr/bin/env bash

# https://android.googleapis.com/gcm/send

if [ -z "$1" ]
  then
    echo "Usage: send-gcm.sh <token>"
    exit 1
fi

NOTIFICATION='"notification":{
            "body":"Test body",
            "title":"Test",
            "icon":"ic_launcher"
        }'

API_KEY="AIzaSyB9onbgaDOcYC-4-r36D3p_zdiQsOVsE70"

# MessageType can be either "gcm", "deleted_messages", "send_error", "send_event"

MESSAGE='{
        "to":"'"$1"'",
        "message_type": "asd",
        "data":{
            "type": "LocationRequest"
            "payload": "{\"requestId\": \"123\",\"requesterEmail\": \"test03365@gmail.com\",\"duration\": 150000}"
        }
    }'

echo "Sending $MESSAGE"

curl --header "Authorization: key=$API_KEY" \
     --header "Content-Type: application/json" \
     https://fcm.googleapis.com/fcm/send -i \
     -d "$MESSAGE"

echo
