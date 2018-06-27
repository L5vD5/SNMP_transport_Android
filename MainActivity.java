package com.example.l5vd5.myapplication;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    public SendData mSendData = null;
    public SendData2 mSendData2 = null;
    public SendData3 mSendData3 = null;
    public TextView txtView = null;
    public TextView txtView2 = null;
    String[] types = {"STRING", "INTEGER", "OID", "TIMETICKS", "GAUGE32", "COUNTER32"};
    String selectedtype = "";

    /*
        parameter : int a
        return : size of int a
    */
    public int sizeofint(int a) {
        int size = 1;

        if (a > 0x7FFFFF) {
            size = 4;
        }else if (a > 0x7FFF) {
            size = 3;
        } else if (a > 0x7F) {
            size = 2;
        }
        return size;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        /*
         SNMPGet Button
         SNMPSet Button
         SNMPWalk Button

         Upper TextView
         Down TextView
         DropDown
         */
        Button snmpgetbtn = (Button) findViewById(R.id.button);
        Button snmpsetbtn = (Button) findViewById(R.id.button2);
        Button snmpwalkbtn = (Button) findViewById(R.id.button3);
        txtView = (TextView) findViewById(R.id.textView);
        txtView2 = (TextView) findViewById(R.id.textView2);
        txtView2.setMovementMethod(new ScrollingMovementMethod());
        final Spinner dropdown = (Spinner)findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, types);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedtype = ""+dropdown.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        snmpgetbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSendData = new SendData();
                mSendData.start();
            }
        });
        snmpsetbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSendData2 = new SendData2();
                mSendData2.start();
            }
        });
        snmpwalkbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtView2.setText("");
                mSendData3 = new SendData3();
                mSendData3.start();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /*
        parameter String[] -> OID String Array
        return int[] -> OID Integer Array
     */
    private int[] processLine(String[] strings) {
        int[] intarray= new int[strings.length];
        int i=0;
        for(String str:strings){
            intarray[i]=Integer.parseInt(str.trim());//Exception in this line
            i++;
        }
        return intarray;
    }
    //Send GET REQUEST packet & receive GET RESPONSE packet
    public class SendData extends Thread {

        public void run() {
            try{
                EditText OIDtext = (EditText)findViewById(R.id.editText); //EditText -> input of OID
                String OIDString = OIDtext.getText().toString(); // OIDtext's value (String)
                String[] StringArray = OIDString.split("\\."); // split OIDString into String[]
                DatagramSocket socket = new DatagramSocket(); // Socket for UDP transport
                InetAddress serverAddr = InetAddress.getByName("kuwiden.iptime.org");
                ByteBuffer buffer = null;
                int[] OIDArray = processLine(StringArray); // convert String[] into int[]
                int OIDlength = BER.getOIDLength(OIDArray);
                int reqnum = 10; // static request number
                BEROutputStream BOS = new BEROutputStream(buffer.allocate(31+1+OIDlength));// init BEROutputStream
                /*
                    Encode data and write into BOS
                 */
                BER.encodeSequence(BOS, BER.SEQUENCE, 29+1+OIDlength);//length 2
                BER.encodeInteger(BOS, BER.ASN_INTEGER, 1);//fixed length 2+1
                BER.encodeString(BOS, BER.ASN_OCTET_STR, ("public").getBytes());//fixed length 2+6
                BER.encodeHeader(BOS, (byte)0xA0, 16+1+OIDlength);//length 2
                BER.encodeInteger(BOS, BER.ASN_INTEGER, reqnum);//length 2+?
                BER.encodeInteger(BOS, BER.ASN_INTEGER, 0);//fixed length 2+1
                BER.encodeInteger(BOS, BER.ASN_INTEGER, 0);//fixed length 2+1
                BER.encodeSequence(BOS, BER.SEQUENCE, 6+OIDlength);//length 2
                BER.encodeSequence(BOS, BER.SEQUENCE, 4+OIDlength);//length 2
                BER.encodeOID(BOS, BER.OID, OIDArray);//length 2+OIDArray-1
                BER.encodeHeader(BOS, BER.NULL, 0);//length 2

                /*
                    Get byte array of Buffer and make the GetRequest packet with it.
                    Send GetRequest packet with the socket.
                 */
                byte[] buf = BOS.getBuffer().array();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, 11161);

                socket.send(packet);
                /*
                    Receive get GetResponse packet with the socket.
                    Decode the packet's data and assign it to variable.
                 */
                byte[] msg = new byte[10000];
                DatagramPacket rpacket = new DatagramPacket(msg, msg.length, serverAddr, 11161);
                socket.receive(rpacket);
                ByteBuffer BB = ByteBuffer.wrap(msg);
                BERInputStream BIS = new BERInputStream(ByteBuffer.allocate(BB.capacity()));
                BIS.setBuffer(BB);
                BER.decodeHeader(BIS, new BER.MutableByte());
                int version = BER.decodeInteger(BIS, new BER.MutableByte());
                String community = new String(BER.decodeString(BIS, new BER.MutableByte()));
                BER.decodeHeader(BIS, new BER.MutableByte());
                BER.decodeInteger(BIS, new BER.MutableByte());
                int err1 = BER.decodeInteger(BIS, new BER.MutableByte());
                int err2 = BER.decodeInteger(BIS, new BER.MutableByte());
                BER.decodeHeader(BIS, new BER.MutableByte());
                BER.decodeHeader(BIS, new BER.MutableByte());
                BER.decodeOID(BIS, new BER.MutableByte());
                //System.out.println(OID.length);

                /*
                    Detect type of the value.
                    With "switch" and detected type, call proper decode function.
                */
                String value = "";
                int index = BIS.getBuffer().get();
                String type = "";
                BIS.getBuffer().position(BIS.getBuffer().position()-1);
                switch (index) {
                    case 0x04:
                        byte[] get = BER.decodeString(BIS, new BER.MutableByte());
                        if(get.length == 0) {
                            type="STRING";
                            value="\"\"";
                            break;
                        }
                        if(get[0] != 0x00 && get[0] != 0x07) {
                            type="STRING";
                            value = new String(get);
                        } else {
                            type="Hex_STRING";
                            for(byte a: get) {
                                value = value+String.format("%02x", a)+" ";
                            }
                        }
                        break;
                    case 0x02:
                        type="INTEGER";
                        value = Integer.toString(BER.decodeInteger(BIS, new BER.MutableByte()));
                        break;
                    case BER.OID:
                        type="OID";
                        String vstring = "";
                        for (int a : BER.decodeOID(BIS, new BER.MutableByte())) {
                            vstring = vstring+Integer.toString(a);
                            vstring = vstring+".";
                        }
                        vstring = vstring.substring(0, vstring.length()-1);
                        value = vstring;
                        break;
                    case BER.TIMETICKS:
                        type="TIMETICKS";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, new BER.MutableByte()));
                        break;
                    case BER.GAUGE32:
                        type="GAUGE32";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, new BER.MutableByte()));
                        break;
                    case BER.COUNTER32:
                        type="COUNTER32";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, new BER.MutableByte()));
                        break;
                    case BER.NULL:
                        type="NULL";
                        value = "null";
                        break;
                    case -126:
                        type = "END";
                        value = "End of MIB view";
                        break;
                    case -127:
                        type = "No Instance";
                        value = "No Such Insance";
                        break;
                    case -128:
                        type = "No Object";
                        value = "No Such Object";
                        break;
                    default:
                        type="?";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, new BER.MutableByte()));
                        break;
                }
                /*
                    Close a socket. (We can use a few socket)
                 */
                socket.close();
                BOS.close();
                BIS.close();
                /*
                    Process for Application View.
                 */
                if(err2!=0) {
                    txtView.setText("Err in packet");
                    txtView.append("\nReason: err-status : "+ err1);
                    txtView.append("\nFailed Object: "+OIDString);
                } else {
                    txtView.setText(OIDString+" = "+ type + " : " + value);
                }
            } catch (Exception e) {
                Log.d("SendData", e.getMessage());
            }
        }
    }
    //Send SET REQUEST packet & receive GET RESPONSE packet
    public class SendData2 extends Thread {
        public void run() {
            try{
                EditText OIDtext = (EditText)findViewById(R.id.editText);//EditText -> input of OID
                EditText OIDtext2 = (EditText)findViewById(R.id.editText2);// EditText -> input of Value.

                String OIDString = OIDtext.getText().toString();// OIDtext's value (String)
                String input_value = OIDtext2.getText().toString();// Value for SetRequest (String)

                String[] StringArray = OIDString.split("\\.");
                DatagramSocket socket = new DatagramSocket();
                InetAddress serverAddr = InetAddress.getByName("kuwiden.iptime.org");
                ByteBuffer buffer = null;
                int requestnum = 10;
                int[] OIDArray = processLine(StringArray);
                int OIDlength = BER.getOIDLength(OIDArray);
                /*
                    Calculate a input_size depending selected type of input value.
                 */
                int[] inputOIDArray = null;
                int input_size = 0;
                if(selectedtype.equals("INTEGER")) {
                    input_size=sizeofint(Integer.parseInt(input_value));
                }
                else if(selectedtype.equals("STRING")) {
                    input_size=input_value.length();
                }
                else if(selectedtype.equals("OID")) {
                    String[] inputStringArray = input_value.split("\\.");
                    inputOIDArray = processLine(inputStringArray);
                    input_size=BER.getOIDLength(inputOIDArray);
                }
                else if(selectedtype.equals("TIMETICKS")) {
                    input_size=sizeofint(Integer.parseInt(input_value));
                }
                else if(selectedtype.equals("GAUGE32")) {
                    input_size=sizeofint(Integer.parseInt(input_value));
                }
                else if(selectedtype.equals("COUNTER32")) {
                    input_size=sizeofint(Integer.parseInt(input_value));
                }
                /*
                    Encode data and write into BOS
                    How to encode value data depends on what type of data value has.
                 */
                BEROutputStream BOS = new BEROutputStream(buffer.allocate(30+OIDlength+input_size+sizeofint(requestnum)));
                BER.encodeSequence(BOS, BER.SEQUENCE, 28+OIDlength+input_size+sizeofint(requestnum));//2
                BER.encodeInteger(BOS, BER.ASN_INTEGER, 1);//3
                BER.encodeString(BOS, BER.ASN_OCTET_STR, ("write").getBytes());//2+5
                BER.encodeHeader(BOS, (byte)0xA3, 16+OIDlength+input_size+sizeofint(requestnum));//2
                BER.encodeInteger(BOS, BER.ASN_INTEGER, requestnum);//2+sizeofint(requestnum)
                BER.encodeInteger(BOS, BER.ASN_INTEGER, 0);//2+1
                BER.encodeInteger(BOS, BER.ASN_INTEGER, 0);//2+1
                BER.encodeSequence(BOS, BER.SEQUENCE, 6+OIDlength+input_size);//2
                BER.encodeSequence(BOS, BER.SEQUENCE, 4+OIDlength+input_size);//2
                BER.encodeOID(BOS, BER.OID, OIDArray);//2+OIDlength
                if(selectedtype.equals("INTEGER")) {
                    BER.encodeInteger(BOS, BER.INTEGER, Integer.parseInt(input_value));//2+sizeofint
                }
                else if(selectedtype.equals("STRING")){
                    BER.encodeString(BOS, BER.ASN_OCTET_STR, input_value.getBytes("utf-8"));
                }
                else if(selectedtype.equals("OID")) {
                    BER.encodeOID(BOS, BER.OID, inputOIDArray);
                }
                else if(selectedtype.equals("TIMETICKS")) {
                    BER.encodeUnsignedInteger(BOS, BER.TIMETICKS, Integer.parseInt(input_value));
                }
                else if(selectedtype.equals("GAUGE32")) {
                    BER.encodeUnsignedInteger(BOS, BER.GAUGE32, Integer.parseInt(input_value));
                }
                else if(selectedtype.equals("COUNTER32")) {
                    BER.encodeUnsignedInteger(BOS, BER.COUNTER32, Integer.parseInt(input_value));
                }

                /*
                    Get byte array of Buffer and make the SetRequest packet with it.
                    Send SetRequest packet with the socket.
                 */
                byte[] buf = BOS.getBuffer().array();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, 11161);
                socket.send(packet);

                /*
                    Receive get GetResponse packet with the socket.
                    Decode the packet's data and assign it to variable.
                 */
                byte[] msg = new byte[1000];
                DatagramPacket rpacket = new DatagramPacket(msg, msg.length, serverAddr, 11161);
                socket.receive(rpacket) ;
                ByteBuffer BB = ByteBuffer.wrap(msg);
                BERInputStream BIS = new BERInputStream(ByteBuffer.allocate(BB.capacity()));
                BIS.setBuffer(BB);

                BER.decodeHeader(BIS, new BER.MutableByte());
                BER.decodeInteger(BIS, new BER.MutableByte());
                new String(BER.decodeString(BIS, new BER.MutableByte()));
                BER.decodeHeader(BIS, new BER.MutableByte());
                BER.decodeInteger(BIS, new BER.MutableByte());
                int err1 = BER.decodeInteger(BIS, new BER.MutableByte());
                int err2 = BER.decodeInteger(BIS, new BER.MutableByte());

                BER.decodeHeader(BIS, new BER.MutableByte());
                BER.decodeHeader(BIS, new BER.MutableByte());
                BER.decodeOID(BIS, new BER.MutableByte());

                String value = null;
                int index = BIS.getBuffer().get();
                String type = null;
                BIS.getBuffer().position(BIS.getBuffer().position()-1);
                switch (index) {
                    //String
                    case 0x04: type = "STRING"; value = new String(BER.decodeString(BIS, new BER.MutableByte())); break;
                    case 0x02: type = "INTEGER"; value = Integer.toString(BER.decodeInteger(BIS, new BER.MutableByte())); break;
                    case BER.OID:
                        type="OID";
                        String vstring = "";
                        for (int a : BER.decodeOID(BIS, new BER.MutableByte())) {
                            vstring = vstring+Integer.toString(a);
                            vstring = vstring+".";
                        }
                        vstring = vstring.substring(0, vstring.length()-1);
                        value = vstring;
                        break;
                    case BER.TIMETICKS:
                        type="TIMETICKS";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, new BER.MutableByte()));
                        break;
                    case BER.GAUGE32:
                        type="GAUGE32";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, new BER.MutableByte()));
                        break;
                    case BER.COUNTER32:
                        type="COUNTER32";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, new BER.MutableByte()));
                        break;
                    default : value = Long.toString(BER.decodeUnsignedInteger(BIS, new BER.MutableByte())); break;
                }
                if(err2!=0) {
                    txtView.setText("Err in packet");
                    txtView.append("\nReason: err-status : "+ err1);
                    txtView.append("\nFailed Object: "+OIDString);
                } else {
                    txtView.setText(OIDString+" : "+ type + " : " + value);
                }

            } catch (Exception e) {
                Log.d("SendData", e.getMessage());
            }
        }
    }
    //GET NEXT REQUEST & GET RESPONSE
    public class SendData3 extends Thread {
        /*
            Init OID Array and request number outside of run.
            With recursive call, this two value get changed.
         */
        int[] OIDArray = {1, 3, 6, 1, 2, 1};
        int requestnum = 10;
        public void run() {
            try{
                DatagramSocket socket = new DatagramSocket();
                InetAddress serverAddr = InetAddress.getByName("kuwiden.iptime.org");
                ByteBuffer buffer = null;
                int OIDlength = BER.getOIDLength(OIDArray);
                BEROutputStream BOS = new BEROutputStream(buffer.allocate(31+sizeofint(requestnum)+OIDlength));
                BER.encodeSequence(BOS, BER.SEQUENCE, 29+sizeofint(requestnum)+OIDlength);//2
                BER.encodeInteger(BOS, BER.ASN_INTEGER, 1);//3
                BER.encodeString(BOS, BER.ASN_OCTET_STR, ("public").getBytes());//2+6
                BER.encodeHeader(BOS, (byte)0xA1, 16+sizeofint(requestnum)+OIDlength);//2
                BER.encodeInteger(BOS, BER.ASN_INTEGER, requestnum++);//2+requestnumsize
                BER.encodeInteger(BOS, BER.ASN_INTEGER, 0);//2+1
                BER.encodeInteger(BOS, BER.ASN_INTEGER, 0);//2+1
                BER.encodeSequence(BOS, BER.SEQUENCE, 6+OIDlength);//2
                BER.encodeSequence(BOS, BER.SEQUENCE, 4+OIDlength);//2
                BER.encodeOID(BOS, BER.OID, OIDArray);//2+OIDArray-1
                BER.encodeHeader(BOS, BER.NULL, 0);//length 2
                /*
                    Get byte array of Buffer and make the SetRequest packet with it.
                    Send SetRequest packet with the socket.
                 */
                byte[] buf = BOS.getBuffer().array();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, 11161);

                socket.send(packet);

                /*
                    Receive get GetResponse packet with the socket.
                    Decode the packet's data and assign it to variable.
                 */
                byte[] msg = new byte[1000];
                DatagramPacket rpacket = new DatagramPacket(msg, msg.length, serverAddr, 11161);
                socket.receive(rpacket);
                ByteBuffer BB = ByteBuffer.wrap(msg);
                BERInputStream BIS = new BERInputStream(ByteBuffer.allocate(BB.capacity()));
                BIS.setBuffer(BB);

                BER.MutableByte A=new BER.MutableByte();
                BER.decodeHeader(BIS, A);
                BER.decodeInteger(BIS, A);
                BER.decodeString(BIS, A);
                BER.decodeHeader(BIS, A);
                BER.decodeInteger(BIS, A);
                BER.decodeInteger(BIS, A);
                BER.decodeInteger(BIS, A);
                BER.decodeHeader(BIS, A);
                BER.decodeHeader(BIS, A);
                OIDArray = BER.decodeOID(BIS, A);
                String value="";
                int index = BIS.getBuffer().get();
                BIS.getBuffer().position(BIS.getBuffer().position() - 1);
                String type;
                boolean end = false;
                switch (index) {
                    case 0x04:
                        byte[] get = BER.decodeString(BIS, new BER.MutableByte());
                        if(get.length == 0) {
                            type="STRING";
                            value="\"\"";
                            break;
                        }
                        if(get[0] != 0x00 && get[0] != 0x07) {
                            type="STRING";
                            value = new String(get);
                        } else {
                            type="Hex_STRING";
                            for(byte a: get) {
                                value = value+String.format("%02x", a)+" ";
                            }
                        }
                        break;
                    case 0x02:
                        type="INTEGER";
                        value = Integer.toString(BER.decodeInteger(BIS, A));
                        break;
                    case BER.OID:
                        type="OID";
                        String vstring = "";
                        for (int a : BER.decodeOID(BIS, A)) {
                            vstring = vstring+Integer.toString(a);
                            vstring = vstring+".";
                        }
                        vstring = vstring.substring(0, vstring.length()-1);
                        value = vstring;
                        break;
                    case BER.TIMETICKS:
                        type="TIMETICKS";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, A));
                        break;
                    case BER.GAUGE32:
                        type="GAUGE32";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, A));
                        break;
                    case BER.COUNTER32:
                        type="COUNTER32";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, A));
                        break;
                    case BER.NULL:
                        type="NULL";
                        value = "null";
                        break;
                    case -126:
                        type = "END";
                        value = "End of MIB view";
                        end = true;
                        break;
                    default:
                        type="?";
                        value = Long.toString(BER.decodeUnsignedInteger(BIS, A));
                        break;
                }
                socket.close();
                BOS.close();
                BIS.close();
                String OIDstring= "";
                for (int a : OIDArray) {
                    OIDstring= OIDstring+Integer.toString(a);
                    OIDstring = OIDstring+".";
                }
                /*
                    Process for Application View.
                 */
                txtView2.append("\n" + OIDstring + " = " + type + " : " + value);

                System.out.println(OIDstring+" = "+type+" : "+value); // logging for SNMPWalk

                /*
                    If we get End of MIB view, recursive call is end.
                 */
                if(!end) {
                    this.run();
                }
            } catch (Exception e) {
                Log.d("Walk", e.getMessage());
            }
        }
    }

}
