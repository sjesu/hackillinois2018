# Text-Navigation

## Description
- This project was developed to provide a direction service without the Internet access

## Requirement
1. Install the latest [Android Studio](https://developer.android.com/studio/index.html)
2. Clone this projcet
3. Build and run on Android Studio

## Usage
1. Install the application, build, and run
2. At the beginning, enable the permissions for GPS use and SMS read and write access.
3. Put down the destination and choose a type of transportation  
  - If you would like to start from a place other than your current location, you could overwrite the top textView
4. Hit "GO" button
5. It would show a list of instructions with distance information and graphical arrows, just like Google Maps

# What it does:
  This project has a client server architecture where user will send a source and destination to get a direction using android    application. This application is exceptional because it gives a direction without using internet i.e. it completely offline no need to have internet connection. Here, user enters source and destination in adroid application and under the hood application uses entered data and draft an SMS send to the number.
  
  Next, the server will read the message sent by android application and find out the route using google direction api and send backs a response to user and in application user will see the directions.
  
# Technology Used

  * Following are the technology we have used in our project:
    * AWS Lambda:
      * Our main server written in Python and hosted on AWS Lambda which is continuously listening to incoming messages. As soon as it receives any message server reads the the direction i.e. the source and destionation given in message. As it find outs the direction using google api, it creates a message and forward its to AWS Simple Notification Service (SNS) service which sends a message to users andrioid application.
    
    * Google API:
      * This API is used to find a direction using source and direction value.
      
    * Twilio Messaging System:
      * This messaging system is used to receive user message and forward it to server. Then server takes of further processing.
      
    * Android SDK:
      * Android SDK is used to build an android application and this application uses uses inbuilt mobile SMS sending and receiving functionality.
      
