Yonah CoolerMan 
===============
**What Is This For**
--------------------
An android app as part of the vaccine temperature logging system. Main functionalities:
- Communicate with arduino (with ESP8266 wifi module connected) through UDP 
- Fetch temeperature data from arduino 
- Tag incoming data point with current time stamp and send back to arduino 
- Check geolacation and auto send SMS to receiver when system approaching destiantion 
- generate statistics summary(for temperature data) upon arrival(send through sms)
- upload data entries to remote server(currently working in progress)

**How To Use it**
-----------------
Clone the entire repo and import into Android Studio

**More**
--------
This project is created as part of [Yonah](www.yonah.sg)'s Service. 
