package net.clopresti.rpibluetooth;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton;

import java.util.UUID;
import java.io.IOException;
import java.io.OutputStream;


public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothDevice btDevice = null;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private TextView textView = null;
    private Button connectButton = null;
    private ToggleButton toggleButton = null;

    // Arbitrary UUID - must match RPi side
    private static final UUID SID =
            UUID.fromString("133f71c6-b7b6-437e-8fd1-d2f59cc76066");

    // MAC Address of RPi BT Adapter
    // Find Using: > sudo hciconfig
    private static String address = "00:19:86:00:02:87";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);

        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectBluetooth();
            }
        });

        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleLED(isChecked);
            }
        });

        SetupBluetooth();
    }

    private void toggleLED(boolean turnOn) {
        if (outStream == null)
            return;
        String message = (turnOn ? "on" : "off") + "\n";
        byte[] msgBuffer = message.getBytes();
        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            FatalError("Failed to send message: " + e.getMessage() + ".");
        }
    }

    private void SetupBluetooth() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            FatalError("Bluetooth Not supported.");
        } else {
            if (btAdapter.isEnabled()) {
                textView.append("Bluetooth is enabled.\n");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void ConnectBluetooth() {
        try {
            btDevice = btAdapter.getRemoteDevice(address);
            btSocket = btDevice.createRfcommSocketToServiceRecord(SID);
        } catch (Exception e) {
            FatalError("Create socket failed: " + e.getMessage() + ".");
        }

        // Turn off discovery - resource intensive.
        btAdapter.cancelDiscovery();

        try {
            btSocket.connect();
            outStream = btSocket.getOutputStream();
            textView.append("Connection established.\n");
            connectButton.setEnabled(false);
            toggleButton.setEnabled(true);
        } catch (Exception e) {
            ShowMessage("Error", "Connection Failed.\n" + e.getMessage());
            try {
                btSocket.close();
            } catch (IOException e2) {
                FatalError("Unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }
    }

    private void Alert(String title, String message, final boolean fatal) {
        if (fatal)
            message += "\nPress OK to exit.";
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    if (fatal)
                        finish();
                }
            }).show();
    }

    private void ShowMessage(String title, String message) {
        Alert(title, message, false);
    }

    private void FatalError(String message) {
        Alert("Fatal Error", message, true);
    }
}
