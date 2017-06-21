package sg.yonah.coolerman20;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import im.delight.android.location.SimpleLocation;


public class MainActivity extends AppCompatActivity {

    private TextView tvClientMsg,tvLocation,tvTime,serverSocketPort;
    private SimpleLocation location;

    DatagramPacket incomingPacket;
    DatagramPacket outgoingPacket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvClientMsg = (TextView) findViewById(R.id.tvClientMessage);
        tvLocation = (TextView)findViewById(R.id.displayLocation);
        tvTime = (TextView) findViewById(R.id.displayTime);
        serverSocketPort = (TextView) findViewById(R.id.serverSocketPort);

        tvTime.setText(getTime());

        // instantiate location manager
        location = new SimpleLocation(this);
        if (!location.hasLocationEnabled()) {
            // ask the user to enable location access
            SimpleLocation.openSettings(this);
        }

        // Start main thread
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }


    /*
    Thread that communicates with hotspot client.
    Code referenced from here: http://android-er.blogspot.sg/2014/08/bi-directional-communication-between.html
     */
    private class SocketServerThread extends Thread {

        static final int PORT = 8080;
        DatagramSocket datagramSocket;
        DataInputStream dataInputStream = null;
        String messageFromClient;
        int colonIdx;
        Double currTemp;

        /*
        Instantiate Data Analyst to :
        1. keeps the incoming data points
        2. generate stats summary when sending sms
         */
        DataAnalyst dataAnalyst = new DataAnalyst();

        @Override
        public void run() {

            //open UDP socket
            try {datagramSocket = new DatagramSocket(PORT);
                datagramSocket.setReuseAddress(true);
            }catch (SocketException e){
                e.printStackTrace();
            }

            try {
                // Display UDP Port Number
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        serverSocketPort.setText("I'm waiting here: "
                                + datagramSocket.getLocalPort());
                    }
                });

                byte[] buffer = new byte[2048];
                //create incomingPacket to receive data into the buffer
                incomingPacket = new DatagramPacket(buffer, buffer.length);

                while (true) {
                    datagramSocket.receive(incomingPacket);
                    messageFromClient = new String(buffer,0,incomingPacket.getLength());
                    incomingPacket.setLength(buffer.length);

                    dataInputStream = new DataInputStream(new ByteArrayInputStream(incomingPacket.getData(), incomingPacket.getOffset(), incomingPacket.getLength()));

                    //construct outgoing msg:
                    //tag data entry with current time
                    String outgoingMsg = getTime() + " = " + messageFromClient;
                    InetAddress destinationIP = InetAddress.getByName("192.168.43.110");
                    outgoingPacket = new DatagramPacket(outgoingMsg.getBytes(),outgoingMsg.length(),destinationIP,4210);
                    datagramSocket.send(outgoingPacket);


                    //get Temperature double from the substring starting from colon:
                    colonIdx = messageFromClient.indexOf(":");
                    currTemp = Double.parseDouble(messageFromClient.substring(colonIdx+1));
                    //pass temp data pt to data analyst
                    dataAnalyst.addNumber(currTemp);


                    // 1. update UI - display current location and msg received from client
                    // 2. check for hubs nearby
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double currLat = location.getLatitude();
                            double currLong = location.getLongitude();
                            tvLocation.setText( Double.toString(currLat) + "," +Double.toString(currLong));
                            tvClientMsg.setText(messageFromClient);


                            // Reaching hub. Send out sms
//                            if(checkNearHub(currLat,currLong)!=null){
//                                String nearhub = checkNearHub(currLat,currLong);
//                                String smsContent = "Vaccines are approaching " + nearhub;
//                                smsContent += ". Average, Max and Min Temperature for this trip: ";
//                                for (Double stat :dataAnalyst.getStatsSummary()){
//                                    smsContent += stat.toString();
//                                    smsContent += " ";
//                                }
//                                sendSMS("+65 8430 6455", smsContent);
//                            }   // close check for hub
                        }
                    });
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                final String errMsg = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvClientMsg.setText(errMsg);
                    }
                });
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /*
     * Get current Time in String format - HH:MM:SS
     */
    public String getTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("HH:mm:ss a");
        date.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));   // specify time zone here
        return date.format(currentLocalTime);
    }
    /*
     * Method to send SMS
     */
    public void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    /*
     * check if approaching one of the destinations
     * return true or false
     * auto send out sms to inform delivery if there is hub nearby
     * tested to be working
     */
    public String checkNearHub(double currLat, double currLong){

        //hashmap that holds the geolocation of all the destinations
        HashMap<String,List<Double>> hubLatLongMap = new HashMap<String,List<Double>>();
        hubLatLongMap.put("Kompiam", new ArrayList<Double>(Arrays.asList(-5.0,144.0)));
        hubLatLongMap.put("Icube", new ArrayList<Double>(Arrays.asList(1.2921,103.776)));
        String msgContent;

        for(Map.Entry<String,List<Double>> entry:hubLatLongMap.entrySet()){
            List<Double> value = entry.getValue();
            double distance;
            // calculate distance bwt 2 geo location
            // obtained formula from here: http://andrew.hedges.name/experiments/haversine/
            distance = Math.pow(Math.sin((currLat-value.get(0))/2),2)+Math.cos(currLat)*Math.cos(value.get(1))*Math.pow(Math.sin((currLong-value.get(1))/2),2);
            distance = 6373 * 2 * Math.atan2(Math.sqrt(distance),Math.sqrt(1-distance));
            if (distance < 10){   // within 10 km
               return entry.getKey();
            }
        }
        return null;
    }
}
