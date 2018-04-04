package com.github.sdnwiselab.edgeiban;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "EdgeIban";
    private static int destPort;
    private static String destAddress;
    private static BluetoothAdapter btAdapter;
    private static String ibanMac;
    private final static String ibanMacDefault = "00:00:00:00:00:00";
    private static Set<BluetoothDevice> btDevices;
    private final static UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "Found device: "+ dev.getAddress());
                if (!btDevices.contains(dev)) {
                    btDevices.add(dev);
                    if (dev.getAddress().equals(ibanMac)) {
                        rfCommThread = new RFCommThread(dev);
                        rfCommThread.start();
                        Log.i(TAG, "RFCOMM Thread Started");
                    }
                }
            }
        }
    };

    private TextView textViewState, textViewPrompt, textViewInfoTx, textViewInfoRx;
    private ScrollView scrollView;

    private RFCommThread rfCommThread;
    private TcpThread tcpThread;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        btDevices = getBtDevices();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "On Start...");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        scrollView = findViewById(R.id.scroller);
        textViewInfoRx = findViewById(R.id.info_rx);
        textViewInfoTx = findViewById(R.id.info_tx);
        textViewState = findViewById(R.id.state);
        textViewPrompt = findViewById(R.id.prompt);
        textViewInfoRx.setText(getIpAddressAndPortRx());
        textViewInfoTx.setText("Select a destination");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "On Resume...");
        textViewInfoTx.setText(getIpAddressAndPortTx());
        textViewInfoRx.setText(getIpAddressAndPortRx());

        if (tcpThread == null && destAddress != null && destPort != 0) {
            tcpThread = new TcpThread(destAddress,destPort);
            tcpThread.start();
        }

        if (rfCommThread == null) {
            BluetoothDevice dev = getDevice(ibanMac);
            if (dev == null) {
                btAdapter.startDiscovery();
            } else {
                rfCommThread = new RFCommThread(dev);
                rfCommThread.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "On Pause...");

        if (tcpThread != null) {
            tcpThread.interrupt();
            tcpThread.close();
            tcpThread = null;
        }


        if (rfCommThread != null) {
            rfCommThread.interrupt();
            rfCommThread.close();
            rfCommThread = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "On Stop...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void updateState(final String state) {
        runOnUiThread(() -> textViewState.setText(state));
    }

    private void updatePrompt(final String prompt) {
        runOnUiThread(() -> {
            textViewPrompt.append(prompt);
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private String getIpAddressAndPortRx() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "My IP: " + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            Log.e(TAG, "Error: ", e);
        }
        return ip;
    }

    /** Called when the user touches the button */
    public void connectToCloud(View view) {
        destAddress = prefs.getString("cloud_address", "localhost");
        destPort = Integer.parseInt(prefs.getString("cloud_port", "4445"));
        textViewInfoTx.setText(getIpAddressAndPortTx());

        if (tcpThread != null) {
            tcpThread.interrupt();
            tcpThread.close();
            tcpThread = null;
        }

        tcpThread = new TcpThread(destAddress,destPort);
        tcpThread.start();

        updateState("Connected to Cloud");
    }

    /** Called when the user touches the button */
    public void connectToEdge(View view) {
        destAddress = prefs.getString("edge_address", "localhost");
        destPort = Integer.parseInt(prefs.getString("edge_port", "4445"));
        textViewInfoTx.setText(getIpAddressAndPortTx());


        if (tcpThread != null) {
            tcpThread.interrupt();
            tcpThread.close();
            tcpThread = null;
        }

        tcpThread = new TcpThread(destAddress,destPort);
        tcpThread.start();

        updateState("Connected to Edge");
    }

    /** Called when the user touches the button */
    public void connectToBt(View view) {
        ibanMac = prefs.getString("iban_mac",ibanMacDefault).toUpperCase();
        if (rfCommThread == null) {
            BluetoothDevice dev = getDevice(ibanMac);
            if (dev == null) {
                btAdapter.startDiscovery();
            } else {
                rfCommThread = new RFCommThread(dev);
                rfCommThread.start();
            }
        }
    }

    /** Called when the user touches the button */
    public void sendToIban(View view) {
        String data = Long.toString(System.nanoTime());
        if (rfCommThread != null) {
            rfCommThread.write(data.getBytes(),data.length());
        }
    }

    /** Called when the user touches the button */
    public void sendToNetwork(View view) {
        String data = Long.toString(System.nanoTime());
        if (tcpThread != null){
            tcpThread.write(data.getBytes(),data.length());
        }
    }


    private String getIpAddressAndPortTx() {
        return "Sending to: " + destAddress + ":" + destPort;
    }


    private BluetoothDevice getDevice(String mac) {
        if (btDevices.size() > 0) {
            for (BluetoothDevice device : btDevices) {
                if (device.getAddress().equals(mac)) {
                    return device;
                }
            }
        }
        return null;
    }

    private Set<BluetoothDevice> getBtDevices() {
        Set<BluetoothDevice> devs = new HashSet<>();
        int REQUEST_ENABLE_BT = 1;

        if (btAdapter != null) {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    devs.add(device);
                }
            }
        }
        return devs;
    }


    private abstract class comThread extends Thread{
        InputStream is;
        OutputStream os;
        byte[] mmBuffer;
        Closeable socket;

        boolean running;

        public void setRunning(boolean running) {
            this.running = running;
            Log.e(TAG, "Running " + running);
        }

        public void write(byte[] data, int len){
            try {
                if (os != null) {
                    os.write(data, 0, len);
                }
            } catch (IOException e) {
                Log.e(TAG,"Error: ", e);
            }

        }

        public void close() {
            try {
                if (socket != null) {
                    socket.close();
                }
                is = null;
                os = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    private class RFCommThread extends comThread {

        public RFCommThread(BluetoothDevice dev) {
            BluetoothSocket tmp = null;
            if (dev != null) {
                try {
                    tmp = dev.createRfcommSocketToServiceRecord(uuid);
                } catch (IOException e) {
                    Log.e(TAG,"Null device");
                }
            }
            socket = tmp;
        }

        @Override
        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            running = true;
            btAdapter.cancelDiscovery();

            try {
                ((BluetoothSocket)socket).connect();
                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connected to " + ibanMac, Toast.LENGTH_SHORT).show());
                os = ((BluetoothSocket)socket).getOutputStream();
                is = ((BluetoothSocket)socket).getInputStream();

                while (running){
                    numBytes = is.read(mmBuffer);
                    long started =  Long.parseLong(new String(mmBuffer,0,numBytes));
                    long arrived = System.nanoTime();
                    updatePrompt("[BT]: " + ((arrived - started)/1000000000.0) + "s \n");
                }
            } catch (Exception e) {
                    Log.e(TAG, "Error:", e);
                    close();
            }
        }
    }




    private class TcpThread extends comThread {
        private String address;
        private int port;

        public TcpThread(String address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public void run() {
            mmBuffer = new byte[1024];
            int numBytes;

            running = true;
            try {
                InetAddress serverAddr = InetAddress.getByName(address);
                socket = new Socket(serverAddr, port);
                MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connected to " + destAddress, Toast.LENGTH_SHORT).show());
                os = ((Socket)socket).getOutputStream();
                is = ((Socket)socket).getInputStream();

                while (running){
                    numBytes = is.read(mmBuffer);
                    long started =  Long.parseLong(new String(mmBuffer,0,numBytes));
                    long arrived = System.nanoTime();
                    updatePrompt("[IP]: " + ((arrived - started)/1000000000.0) + "s \n");
                }
            } catch (Exception e) {
                try {
                    Log.e(TAG, "Error", e);
                    close();
                } catch (Exception ex) {
                    Log.e(TAG, "Could not close the client socket", ex);
                }

            }
        }
    }
}