#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

const char* ssid = "TC";
const char* password = "oahoah2014";
const char* host_ip = "192.168.43.1";
const int host_port = 8080;

bool SEND_DATA = false;    //to indicate whether to send the temperature value to smartphone
bool NO_DATA = true;    

WiFiUDP Udp;
unsigned int localUdpPort = 4210;

#define DATA_LENGTH 13
String _data = "";
char sendthis[DATA_LENGTH];
char incomingPacket[255];  // buffer for incoming packets

void setup()
{
  Serial.begin(57600);
  WiFi.begin(ssid, password);
  Udp.begin(localUdpPort);
  SEND_DATA = false;
  NO_DATA = true;
}



char check;   //check whether the data coming from Arduino is in the correct format that starts with a '*'
void loop()
{
  while(Serial.available()>0)
  {
    check = Serial.read();
    if(check == '*'){
      _data = Serial.readString();
      for(int i=0;i<13;i++){
        sendthis[i] = _data.charAt(i);
      }
      sendthis[12] = '\0';      
      SEND_DATA = true;
      NO_DATA = false;
    }
  }  

  if(NO_DATA){
    Udp.beginPacket(host_ip, host_port);
    Udp.write("Channel is clear! No data coming...");
    Udp.endPacket();
    }

  if(SEND_DATA){
    Udp.beginPacket(host_ip, host_port);
    Udp.write(sendthis);
    Udp.endPacket();
    SEND_DATA = false;
  }

  int packetSize = Udp.parsePacket();
  if (packetSize)
  {
    // receive incoming UDP packets
    int len = Udp.read(incomingPacket, 255);
    if (len > 0)
    {
      incomingPacket[len] = '\0';
    }
    Serial.write(incomingPacket);
  }

  
// Only for connectivity test (ESP8266 to smartphone)
//  String printit = String(i) + "Channel is clear!";
//  char show[] = "";
//  for(int j=0;j<printit.length();j++){
//    show[j] =  printit.charAt(j);
//  }
//  Udp.beginPacket(host_ip, host_port);
//  Udp.write(show);
//  Udp.endPacket();
//  delay(2000);
//  i++;
}


