package com.github.sdnwiselab.edgeiban;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    static int UdpServerPort;
    static int destUdpServerPort;
    static String destUdpServerAddress;
    static BluetoothAdapter bluetoothAdapter;
    static String IBAN_MAC = "7C:D1:C3:E2:B0:66";
    static Set<BluetoothDevice> btDevices;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "Found device: "+ device.getAddress());
                btDevices.add(device);
            }
        }
    };
    TextView textViewState, textViewPrompt, textViewInfoTx, textViewInfoRx;
    ScrollView scrollView;
    UdpServerThread udpServerThread;
    RFCommThread rfCommThread;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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
        UdpServerPort = Integer.parseInt(prefs.getString("server_port", "4444"));

        scrollView = findViewById(R.id.scroller);
        textViewInfoRx = findViewById(R.id.info_rx);
        textViewInfoTx = findViewById(R.id.info_tx);
        textViewState = findViewById(R.id.state);
        textViewPrompt = findViewById(R.id.prompt);
        textViewInfoRx.setText(getIpAddressAndPortRx());
        textViewInfoTx.setText(getIpAddressAndPortTx());

        setPreferences(Destination.EDGE);

        if (udpServerThread == null) {
            udpServerThread = new UdpServerThread(UdpServerPort);
            udpServerThread.start();
            Log.i(TAG, "UDP Server Started");
        }

        if (rfCommThread == null) {
            rfCommThread = new RFCommThread(IBAN_MAC);
            rfCommThread.start();
            Log.i(TAG, "RFCOMM Thread Started");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        textViewInfoRx.setText(getIpAddressAndPortRx());
        textViewInfoTx.setText(getIpAddressAndPortTx());

        if (udpServerThread != null) {
            udpServerThread.setRunning(true);
        } else {
            udpServerThread = new UdpServerThread(UdpServerPort);
            udpServerThread.start();
            Log.i(TAG, "UDP Server Starting");
        }

        if (rfCommThread == null) {
            rfCommThread = new RFCommThread(IBAN_MAC);
            rfCommThread.start();
            Log.i(TAG, "RFCOMM Thread Started");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "On Pause...");
        if (udpServerThread != null) {
            udpServerThread.setRunning(false);
            udpServerThread.interrupt();
            udpServerThread.close();
            udpServerThread = null;
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
        if (udpServerThread != null) {
            udpServerThread.setRunning(false);
            udpServerThread.interrupt();
            udpServerThread.socket.close();
            udpServerThread = null;
        }
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

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.radio_edge:
                if (checked)
                    setPreferences(Destination.EDGE);
                break;
            case R.id.radio_cloud:
                if (checked)
                    setPreferences(Destination.CLOUD);
                break;
        }

    }

    private void setPreferences(Destination dest) {
        switch (dest) {
            case EDGE:
                destUdpServerAddress = prefs.getString("edge_address", "localhost");
                destUdpServerPort = Integer.parseInt(prefs.getString("edge_port", "4445"));
                Toast.makeText(this, "Sending to the Edge", Toast.LENGTH_SHORT).show();
                break;
            default:
                destUdpServerAddress = prefs.getString("cloud_address", "localhost");
                destUdpServerPort = Integer.parseInt(prefs.getString("cloud_port", "4445"));
                Toast.makeText(this, "Sending to the Cloud", Toast.LENGTH_SHORT).show();
                break;
        }
        textViewInfoTx.setText(getIpAddressAndPortTx());
    }

    private String getIpAddressAndPortTx() {
        return "Sending to: " + destUdpServerAddress + ":" + destUdpServerPort;
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
                        ip += "\nListening at: " + inetAddress.getHostAddress() + ":" + UdpServerPort;
                    }

                }

            }

        } catch (SocketException e) {
           Log.e(TAG,"Error: ", e);
        }

        return ip;
    }

    private BluetoothDevice getDevice(String mac) {
        if (btDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
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

        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    devs.add(device);
                }
            }
        }
        return devs;
    }


    enum Destination {
        CLOUD,
        EDGE
    }

    private class UdpServerThread extends Thread {

        int serverPort;
        DatagramSocket socket;
        boolean running;

        public UdpServerThread(int serverPort) {
            super();
            this.serverPort = serverPort;
        }

        public void setRunning(boolean running) {
            this.running = running;
            Log.e(TAG, "Running " + running);
        }

        @Override
        public void run() {

            running = true;

            try {
                updateState("Starting UDP Server");
                socket = new DatagramSocket(serverPort);

                updateState("UDP Server is running");
                Log.e(TAG, "UDP Server is running");

                while (running) {
                    byte[] buf = new byte[3000];

                    // receive request
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);     //this code block the program flow

                    // send the response to the client at "address" and "port"
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    updatePrompt("[" + Calendar.getInstance().getTimeInMillis() + "] Got " + packet.getLength() + "Bytes from: " + address + ":" + port + "\n");
                    packet = new DatagramPacket(packet.getData(), packet.getLength(),
                            InetAddress.getByName(destUdpServerAddress), destUdpServerPort);
                    rfCommThread.write(packet.getData(), packet.getLength());
                    socket.send(packet);
                }

                Log.e(TAG, "UDP Server ended");

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                    Log.e(TAG, "socket.close()");
                }
            }
        }

        public void close(){
            socket.close();
        }
    }


    private class RFCommThread extends Thread {
        BluetoothSocket socket;

        public RFCommThread(String mac) {
            BluetoothDevice dev = getDevice(mac);
            if (dev != null) {
                try {
                    socket = dev.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                Log.i(TAG, "Connecting...");
                socket.connect();
                Log.i(TAG, "Connected...");
            } catch (IOException connectException) {
                try {
                    socket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }
        }

        public void write(byte[] data, int len){
            try {
                socket.getOutputStream().write(data,0,len);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}