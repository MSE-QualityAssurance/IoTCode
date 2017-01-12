/**************************************************************************************
* File: TestAllDevices
* Project: LG Exec Ed Program
* Copyright: Copyright (c) 2015 Anthony J. Lattanze
* Versions:
* 1.0 May 2015 - Initial version
*
* Description:
*
* This program demonstrates how to use the devices connected to a sensor actuator (SA) 
* node. The SA node is an Arduino processor with a WIFI shield. Note, this program does 
* not test the WIFI shield. The sensors actuators include the follow:
*
* Compilation and Execution Instructions: Compiled using Arduino IDE VERSION 1.6.4
*
* Parameters: None
*
* Internal Methods: 
* long ProximityVal(int sensorIn) - Determines if the proximity sensor is covered 
* or not. If covered, this method will return a 0. If not, it will return 1.
*
* void CloseDoor() - Operates server to close door.
*
* void OpenDoor() - Operates server to open door.
*
* int DoorState( int Pin ) - Reads the pin connected to the door switch. If the door
* is open the pin will read 0, otherwise, the door will read 1.
*
* Each of these methods are explained in detail where they appear below.
**************************************************************************************/

#include <dht.h>           // Note that the DHT file must be in your Arduino installation 
                           // folder, in the library foler.
#include <Servo.h> 
#define DhtPin   8         // This #define defines the pin that will be used to get data 
                           // from the tempurature & humidity sensor. 
                           
dht DHT;                   // This sets up an equivalence between dht and DHT.

Servo myservo;
int ServoPin = 9;          // Servo Pin
int DoorSwitchPin = 2;     // Pin the door switch is connected to
int LightPin = 5;          // Pin that the corner light LED is connected to
int AlarmPin = 3;          // Pin that the alarm inidicator LED is connected to
int QtiPin = 6;            // The pin with QTI/proximity sensor

void setup() 
{ 
  Serial.begin(9600);                           // Set up a debug window
  myservo.attach(ServoPin);                     // Attach to servo

} 

void loop() 
{
  CloseDoor();                                  // Close the door and wait for 
  delay(2500);                                  // 2.5 seconds

  Serial.print("Door state = ");                // Get the state of the door and
  Serial.println( DoorState(DoorSwitchPin) );   // print it to the debug window
  delay(2500);                                  // then we wait for 2.5 seconds

  Serial.println("Alarm set.");                 // Now we set the alarm LED to on
  LedOn( AlarmPin );                            // and then we wait for 2.5 seconds
  delay(2500);  
  
  Serial.println("Alarm disabled.");            // Now we set the alarm LED to off
  LedOff( AlarmPin );                           // then we wait for 2.5 seconds
  delay(2500);  
  
  OpenDoor();                                   // Open the door and wait for
  delay(2500);                                  // 2.5 seconds

  Serial.print("Door state = ");                // Get the state of the door and
  Serial.println( DoorState(DoorSwitchPin) );   // print it to the debug window
  delay(2500);                                  // then we wait for 2.5 seconds
  
  LedOn( LightPin );                            // Now we set the Lamp LED to on
  Serial.println( "Light is on..." );           // then we wait for 2.5 seconds
  delay(2500);
  
  LedOff( LightPin);                            // Now we set the Lamp LED to off
  Serial.println( "Light is off..." );          // then we wait for 2.5 seconds
  delay(2500);                                  
  
  // Now we read the data from the sensor
  DHT.read11(DhtPin);                           // Now we read the temperature and 
  Serial.print("Current humidity = ");          // humidity sensor. Note that this 
  Serial.print(DHT.humidity);                   // method is provided by the sensor
  Serial.print("%  ");                          // manufacturer and is located in the
  Serial.print("temp = ");                      // DHT folder. You must place this 
  Serial.print( ((DHT.temperature*1.8)+32) );   // folder in your Arduino/libraries 
  Serial.print("F  ");                          // folder in order to compile this 
  Serial.print("temp = ");                      // program. Of course we pring the 
  Serial.print(DHT.temperature);                // humidity and temperature to the 
  Serial.println("C  ");                        // debug window. Then wait 2.5 seconds.
  delay( 2500 );
  
  Serial.print("Proximity Value = ");           // Determine the value of the QTI
  Serial.println( ProximityVal(QtiPin) );       // proximity sensor and wait for 
  delay( 2500 );                                // 2.5 seconds.
  
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

int DoorState( int Pin )
{
  int val = 0;
  val = digitalRead( Pin );
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

void LedOn( int Pin )
{
   pinMode( Pin, OUTPUT);      // Set the specified pin to output mode.
   digitalWrite( Pin, HIGH);   // Set the pin to 5v.
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

void LedOff( int Pin )
{
   pinMode( Pin, OUTPUT);     // Set the pin to output mode.
   digitalWrite( Pin, LOW);   // Set the pin to 0 volts.
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
