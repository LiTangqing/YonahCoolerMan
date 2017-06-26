#include <SD.h>
#include <SPI.h>
#include <SoftwareSerial.h>
#include <Wire.h> // Used to establied serial communication on the I2C bus
#include "SparkFunTMP102.h" // Used to send and recieve specific information from our sensor


#define esp8266_serial_rx_pin 9 //  Connect this pin to TX on the esp8266
#define esp8266_serial_tx_pin 8 //  Connect this pin to RX on the esp8266
#define esp8266_reset_pin 5 // Connect this pin to CH_PD on the esp8266, not reset. (let reset be unconnected)

SoftwareSerial ESP8266(esp8266_serial_rx_pin, esp8266_serial_tx_pin);

TMP102 sensor0(0x48); // Initialize sensor at I2C address 0x48

File myFile;
File newFile;

void setup() {
  // put your setup code here, to run once:
  ESP8266.begin(57600);
  Serial.begin(57600);

  //Set up Temp Sensor
  sensor0.begin();  // Join I2C bus

  // Initialize sensor0 settings
  // These settings are saved in the sensor, even if it loses power

  // set the number of consecutive faults before triggering alarm.
  // 0-3: 0:1 fault, 1:2 faults, 2:4 faults, 3:6 faults.
  sensor0.setFault(0);  // Trigger alarm immediately

  // set the polarity of the Alarm. (0:Active LOW, 1:Active HIGH).
  sensor0.setAlertPolarity(1); // Active HIGH

  // set the sensor in Comparator Mode (0) or Interrupt Mode (1).
  sensor0.setAlertMode(0); // Comparator Mode.

  // set the Conversion Rate (how quickly the sensor gets a new reading)
  //0-3: 0:0.25Hz, 1:1Hz, 2:4Hz, 3:8Hz
  sensor0.setConversionRate(3);

  //set Extended Mode.
  //0:12-bit Temperature(-55C to +128C) 1:13-bit Temperature(-55C to +150C)
  sensor0.setExtendedMode(0);


  //Setup for SD card
  pinMode(10,OUTPUT);
  digitalWrite(10,HIGH);
  SD.begin();

  //Create a file on the SD card: "temperature_log.txt"
  newFile = SD.open("templogs.txt", FILE_WRITE);
  newFile.close();

//    Only for debugging 
//  Serial.print("Initializing SD card...");
//  if (!SD.begin()) {
//    Serial.println("initialization failed!");
//    return;
//  }
//  Serial.println("initialization done.");
  
}



void loop() {
  float temperature;
  String data; 
  String logdown;
  String check = "*";
  String Temp = "Temp: ";

  sensor0.wakeup();
  temperature = sensor0.readTempC();
  sensor0.sleep();

  //Send temperature value to ESP8266
  data = check + Temp + String(temperature);
  char sendthis[13];
  for(int i=0;i<13;i++){
    sendthis[i] = data.charAt(i);
  }
  sendthis[12] = '\0';
  ESP8266.write(sendthis);

//listen to esp8266
  while(ESP8266.available()>0){
      logdown = ESP8266.readString();
//      Serial.print("Received: ");
//      Serial.println(logdown);


      
      //start to log temperature value and time on SD card
      // open the file. note that only one file can be open at a time,
      // so you have to close this one before opening another.
      myFile = SD.open("templogs.txt", FILE_WRITE);
      if(myFile){
        myFile.println(logdown);
        myFile.close();
      }
//      else{
//        Serial.println("Error opening the file");
//      }
      
  }

  delay(5000);    //send temperature value once every 5 sec
  

}
 
