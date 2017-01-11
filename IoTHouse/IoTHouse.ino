/****************************************************************
* File: ServerDemo
* Project: LG Exec Ed Program
* Copyright: Copyright (c) 2013 Anthony J. Lattanze
* Versions:
* 1.0 May 2013 - initial version
* 1.5 April 2014 - added #define for port id
* 2.0 November 2015 - modified for LGE Testing Program
*
* Description:
*
* This program runs on an Arduino processor with a WIFI shield. 
* This program is a server demo. This runs in a loop communicating
* with a client process that also runs in a loop. The protocol is
* that after a client connects, this process sends two '\n' 
* terminated strings. Then this process waits for the client to 
* send a string back. This illustrates basic connection and
* two-way communication.
*
* Compilation and Execution Instructions: Must be compiled using 
* Arduino IDE VERSION 1.0.4
*
* Parameters: None
*
* Internal Methods: void printConnectionStatus()
*
****************************************************************/

#include <SPI.h>
#include <WiFi.h>
#include <dht.h>                // Note that the DHT file must be in your Arduino installation 
                                // folder, in the library foler.
#include <Servo.h>

#define DhtPin   8             // This #define defines the pin that will be used to get data 
                               // from the tempurature & humidity sensor. 
#define PORTID 5050            // IP socket port#

char ssid[] = "LGTeam4";       // The network SSID
int status = WL_IDLE_STATUS;   // Network connection status
WiFiServer server(PORTID);     // Server connection and port
IPAddress ip;                  // The IP address of the shield
IPAddress subnet;              // The IP address of the shield
long rssi;                     // Wifi shield signal strength
byte mac[6];                   // Wifi shield MAC address
char recvBuffer[1024];         // receive buffer
                           
dht DHT;                       // This sets up an equivalence between dht and DHT.

Servo myservo;
int ServoPin = 9;          // Servo Pin
int DoorSwitchPin = 2;     // Pin the door switch is connected to
int LightPin = 5;          // Pin that the corner light LED is connected to
int AlarmPin = 3;          // Pin that the alarm inidicator LED is connected to
int QtiPin = 6;            // The pin with QTI/proximity sensor

boolean alarmState = false;
boolean lightState = false;

 void setup() {
   // Initialize a serial terminal for debug messages.
   Serial.begin(9600);
   myservo.attach(ServoPin);                     
   AlarmOff();
   LightOff();
  
   // Attempt to connect to Wifi network.
   while ( status != WL_CONNECTED) 
   { 
     Serial.print("Attempting to connect to SSID: ");
     Serial.println(ssid);
     status = WiFi.begin(ssid);
   }  
   
   // Print the basic connection and network information.
   printConnectionStatus();
   
   // Start the server and print and message for the user.
   server.begin();
   Serial.println("The Server is started."); 
 
   
 } // setup

/**
 * The main loop
 */
 void loop() { 

   // Wait for the client:
   WiFiClient client = server.available();
   
   if (client)   {
     String req = "";                // make a String to hold incoming data from the client
     while (client.connected()) {    // loop while the client's connected
       if (client.available()) {  
         char c = client.read();
         
         if (c == '.') {
           req += c;
           Serial.println("Request: '" + req + "'");
           String response = ProcessRequest(req);
           client.println (response);
           client.flush();
           
           response = "";
           req  = "";
           
         } else {
           req += c;
         }
       }
     }
     client.stop();
     Serial.println("Done!");
     Serial.println(".....................");
     
   } // if we are connected
 } // loop

/*********************************************************************
* String ProcessRequest()
* Parameters: the incoming request           
*
* Description: 
* This method processes the received message to determine if it is a 
* set state request or a get state request. The response is returned as a 
* String
***********************************************************************/
String  ProcessRequest(String request) {

  int hdrPosition = request.indexOf(':');
  int endPosition = request.indexOf('.');
  String response = "";

  String hdr = request.substring(0,2);
   
  if (hdr == "SS") {
    // a set state request has a body
    String body = request.substring(hdrPosition+1,endPosition);
    response =  HandleSetState(body);
  }
  else if (hdr == "GS") {
    response = HandleGetState();
  }
  
  return response;
}

/*********************************************************************
* void HandleSetState()
* Parameters: the new state           
*
* Description: 
* This method sets the new state and retuns "OK" if successful.
***********************************************************************/
String HandleSetState(String newState) {
  
  String body[20];
  int counter = 0;
  
  int i=0;
  String param = "";
  while (i < newState.length()) { 
    char c = newState.charAt(i);
    if (c == ';') { 
      body[counter] = String (param);
      counter++;
      param = "";
    }    
    else {
      param += c;
    }
    i++;
  }

  // body array now has parameters
  for (int j=0; j < counter; j++) {
    String item = body[j];
    
    // Door
    String  val = item.substring(3);
    // door state
    if (item.substring(0,2) == "DS") {      
      if (val != "1") {
        CloseDoor();
      } else {
        OpenDoor();
      }
    }
    // Light state
    else if (item.substring(0,2) == "LS") {
       if (val == "1") {
         LightOn();
      } else {
         LightOff();
       }
    }
    // alarm state
    else if (item.substring(0,2) == "AS") {
      if (val == "1") {
         AlarmOn();
      } else {
         AlarmOff();
      }
    }
    // HVAC state not yet implemented
    
    // Humdiifer state
    else if (item.substring(0,3) == "HUS") {
      // Not yet implemented 
      
    }
    // Heater state
    else if (item.substring(0,3) == "HES") {
      // not yet implemented
    }
    // Chiller state
    else if (item.substring(0,3) == "CS") {
      // Not yet implemented  
    }
  }  
  return String("OK");
}

/*********************************************************************
* void HandleSetState()
* Parameters: None           
*
* Description: 
* This method gets the current state and retuns it as a String.
***********************************************************************/
String HandleGetState() {

  Serial.println("HandleGetState");
  String response = "SU:"; // state update

  //response += "DS=";

  int ds = DoorState();
  if (ds == 0) {
    response += "DS=1";
  } else {
    response += "DS=0";
  }
  //response += String(DoorState());
  
  response += ";";

  response += "TR=";

  // Now we read the data from the sensor
  DHT.read11(DhtPin);

  // Must be read in Farenheight
  response += (int) ((DHT.temperature*1.8)+32);

  response += ";";

  response += "HR="; 
  
  response += (int) DHT.humidity;

  response += ";";
  
  if (lightState==true) {
    response += "LS=1";
  } else {
    response += "LS=0";
  }
  response += ";";

  if (alarmState==true) {
    response += "AS=1";
  } else {
    response += "AS=0";
  }
  response += ";";

  long pv = ProximityVal(QtiPin);
  if (pv == 0) {
    // home
    response += "PS=1";
  } else {
    response += "PS=0";
  }
  response += ".";

  Serial.println("HandleGetState response: " + response);
  
  return response;
}

/*********************************************************************
* void CloseDoor()
* Parameters: None           
* Global Variable: myservo
*
* Description: 
* This method uses a servo write command to set the servo to its 50%
* position - this is sufficient to close the door. The write method 
* converts the integer 90 into a pulse width modulated value to move 
* the servo to the mid-point.
*
* WARNING: do not change the servo write value... doing so could break 
* the door and/or servo.
***********************************************************************/

void CloseDoor()
{
  Serial.println( "Closing Door..." );
  myservo.write(90);  // Set servo to mid-point. This closes
                      // the door.
}  

/*********************************************************************
* void OpenDoor()
* Parameters: None           
* Global Variable: myservo
*
* Description: 
* This method uses a servo write command to set the servo to its full
* clockwise position - this is sufficient to open the door. The write 
* method converts the integer 0 into a pulse width modulated value to 
* move the servo.
*
* WARNING: do not change the servo write value... doing so could break 
* the door and/or servo.
***********************************************************************/

void OpenDoor()
{
  Serial.println( "Opening Door..." );
  myservo.write(0);  // Set servo to its full clockwise position.
                     // This opens the door
}

/*********************************************************************
* int DoorState(int Pin)
* Parameters:            
* int pin - the pin on the Arduino where the door switch is connected.
*
* Description: 
* This method reads the state of Pin to determine what the voltage level
* is. If the input is 5v it is high, and will return a 1 signifying that
* the door is closed. If the input is 0v it is low, and will return a 0
* signifying that the door is open.
***********************************************************************/

int DoorState( )
{
  int val = 0;
  val = digitalRead(DoorSwitchPin);
  return val;
}

/*********************************************************************
* void LedOn(int Pin)
* Parameters:            
* int pin - the pin on the Arduino where the LED is connected.
*
* Description: 
* This method writes a 1 to the specified pin. This places 5v on the Pin
* lighting the LED.
***********************************************************************/

void AlarmOn()
{
   pinMode( AlarmPin, OUTPUT);      // Set the specified pin to output mode.
   digitalWrite( AlarmPin, HIGH);   // Set the pin to 5v.
   alarmState = true;
}

/*********************************************************************
* void LedOff(int Pin)
* Parameters:            
* int pin - the pin on the Arduino where the LED is connected.
*
* Description: 
* This method writes a 0 to the specified pin. This places 0v on the Pin
* turning off the LED.
***********************************************************************/

void AlarmOff( )
{
   pinMode( AlarmPin, OUTPUT);     // Set the pin to output mode.
   digitalWrite( AlarmPin, LOW);   // Set the pin to 0 volts.
   alarmState = false;
   
}


/*********************************************************************
* void LightOn()
* Parameters: None            
*
* Description: 
* This method turns on the light 
***********************************************************************/

void LightOn()
{
   pinMode( LightPin, OUTPUT);      // Set the specified pin to output mode.
   digitalWrite( LightPin, HIGH);   // Set the pin to 5v.
   lightState = true;
}

/*********************************************************************
* void LightOff()
* Parameters:  None          
*
* Description: 
* This method writes turns off the light 
***********************************************************************/

void LightOff( )
{
   pinMode( LightPin, OUTPUT);     // Set the pin to output mode.
   digitalWrite( LightPin, LOW);   // Set the pin to 0 volts.
   lightState = false;
}

/*********************************************************************
* void GetCurrentTemperature()
* Parameters:  None          
*
* Description: 
* This method gets the current temperature
***********************************************************************/

int GetCurrentTemperature()
{
  return DHT.temperature;
}

/*********************************************************************
* void GetCurrentHumidity()
* Parameters:  None          
*
* Description: 
* This method gets the current humidity
***********************************************************************/
int GetCurrentHumidity()
{
  return DHT.humidity;
}

/*********************************************************************
* long ProximityVal(int Pin)
* Parameters:            
* int pin - the pin on the Arduino where the QTI sensor is connected.
*
* Description:
* QTI schematics and specs: http://www.parallax.com/product/555-27401
* This method initalizes the QTI sensor pin as output and charges the
* capacitor on the QTI. The QTI emits IR light which is reflected off 
* of any surface in front of the sensor. The amount of IR light 
* reflected back is detected by the IR resistor on the QTI. This is 
* the resistor that the capacitor discharges through. The amount of 
* time it takes to discharge determines how much light, and therefore 
* the lightness or darkness of the material in front of the QTI sensor.
* Given the closeness of the object in this application you will get
* 0 if the sensor is covered
***********************************************************************/
long ProximityVal(int Pin)
{
    long duration = 0;
    pinMode(Pin, OUTPUT);         // Sets pin as OUTPUT
    digitalWrite(Pin, HIGH);      // Pin HIGH
    pinMode(Pin, INPUT);          // Sets pin as INPUT
    digitalWrite(Pin, LOW);       // Pin LOW
    while(digitalRead(Pin) != 0)  // Count until the pin goes
       duration++;                // LOW (cap discharges)
       
    return duration;              // Returns the duration of the pulse
}

/***************************************************************
* The following method prints out the connection information
****************************************************************/

 void printConnectionStatus() 
 {
     // Print the basic connection and network information: 
     // Network, IP, and Subnet mask
     ip = WiFi.localIP();
     Serial.print("Connected to ");
     Serial.print(ssid);
     Serial.print(" IP Address:: ");
     Serial.println(ip);
     subnet = WiFi.subnetMask();
     Serial.print("Netmask: ");
     Serial.println(subnet);
   
     // Print our MAC address.
     WiFi.macAddress(mac);
     Serial.print("WiFi Shield MAC address: ");
     Serial.print(mac[5],HEX);
     Serial.print(":");
     Serial.print(mac[4],HEX);
     Serial.print(":");
     Serial.print(mac[3],HEX);
     Serial.print(":");
     Serial.print(mac[2],HEX);
     Serial.print(":");
     Serial.print(mac[1],HEX);
     Serial.print(":");
     Serial.println(mac[0],HEX);
   
     // Print the wireless signal strength:
     rssi = WiFi.RSSI();
     Serial.print("Signal strength (RSSI): ");
     Serial.print(rssi);
     Serial.println(" dBm");

 } // printConnectionStatus
