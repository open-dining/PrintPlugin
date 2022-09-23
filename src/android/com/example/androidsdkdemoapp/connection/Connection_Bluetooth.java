package com.example.androidsdkdemoapp.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import honeywell.Monitor;
import honeywell.connection.ConnectionBase;

/**
 * This is the class for Android Bluetooth type connections.
 * It extends Oneil's ConnectionBase class, and accesses the android.bluetooth package.
 *
 * @author Datamax-O'Neil by Honeywell
 * @version 2.4.0 (20 July 2015)
 */
public class Connection_Bluetooth extends ConnectionBase {
    private static final String TAG = "Connection_Bluetooth";
    /**
     * The UUID for ssp (serial) bluetooth devices
     * http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
     **/
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Bluetooth Server Object
     */
    private BluetoothServerSocket m_BtListener = null;

    /**
     * Bluetooth Device
     **/
    private BluetoothDevice device;

    /**
     * Bluetooth Socket Object
     */
    private BluetoothSocket m_BtClient = null;

    /**
     * Stream to read from the Bluetooth device
     */
    private DataInputStream m_StreamRead = null;

    /**
     * Stream to read from the Bluetooth device
     */
    private DataOutputStream m_StreamWrite = null;

    /**
     * Target Bluetooth Address
     */
    private String m_Address = "00:00:00:00:00:00";


    /**
     * This will return the bluetooth address of the other end.
     *
     * @return address at the other end of the connection.
     */
    public String getRemoteEnd() {
        return (m_BtClient == null) ? "-none-" : m_Address;
    }

    /**
     * This will return the firendly bluetooth name of the remote device
     *
     * @return friendly name as string
     */
    public String getFriendlyName() {
        return device.getName();
    }

    /**
     * We only want connections to be able to be created from our static
     * methods. This will create a bluetooth connection object from the provided
     * parameters.
     *
     * @param isServer     Will this be in server mode.
     * @param isAsync      Will this Connection run asynchronously (multi-threaded)
     * @param targetDevice Bluetooth address of target device.
     * @throws Exception Error caused by invalid Bluetooth address or bluetooth is disable/not supported
     */
    protected Connection_Bluetooth(boolean isServer, boolean isAsync, String targetDevice) throws Exception {
        super(isServer);

        // Setup Connection
        m_Address = targetDevice;
        m_IsSynchronous = !isAsync;
        BluetoothManager bluetoothManager;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            throw new Exception("Bluetooth not supported on device.");
        }

        if (!adapter.isEnabled()) {
            throw new Exception("Bluetooth on device not enabled.");
        }


        //validate if its a MAC address
        Pattern pattern = Pattern.compile("[0-9A-Fa-f]{12}");
        Matcher matcher = pattern.matcher(targetDevice);
        if (matcher.matches()) {
            targetDevice = formatBluetoothAddress(targetDevice);
        }
        if (!BluetoothAdapter.checkBluetoothAddress(targetDevice)) {
            throw new Exception("Invalid Bluetooth Address format");
        }


        device = adapter.getRemoteDevice(m_Address);

    }

    /**
     * Create a bluetooth client connection. This function is used to create a
     * connection to a bluetooth printer/server.
     *
     * @param targetDevice Bluetooth address of target device.
     * @return Connection object that can be used to talk to the device.
     * @throws Exception Error caused by invalid Bluetooth address or bluetooth is disable/not supported
     */
    static public Connection_Bluetooth createClient(String targetDevice) throws Exception {
        return new Connection_Bluetooth(false, true, targetDevice);
    }

    /**
     * Create a bluetooth client connection. This function is used to create a
     * connection to a bluetooth printer/server.
     *
     * @param targetDevice Bluetooth address of target device.
     * @param isAsync      Will this Connection run asynchronously (multi-threaded)
     * @return Connection object that can be used to talk to the device.
     * @throws Exception Error caused by invalid Bluetooth address or bluetooth is disable/not supported
     */
    static public Connection_Bluetooth createClient(String targetDevice, boolean isAsync) throws Exception {
        return new Connection_Bluetooth(false, isAsync, targetDevice);
    }

    /**
     * Create a bluetooth server connection. This function is used to create a
     * connection which will listen for incoming bluetooth clients.
     *
     * @param port Port number to connect on.
     * @return Connection object that can be used to talk to the device.
     * @throws Exception Error caused by invalid Bluetooth address or bluetooth is disable/not supported
     */
    static public Connection_Bluetooth createServer(int port) throws Exception {
        return new Connection_Bluetooth(true, true, "");
    }

    /**
     * This will return true if there is data available to be read. This
     * indicates if the device itself, however we are talking to it, has data to
     * read.
     */
    @Override
    protected boolean getHasData() {
        boolean hasData = false;

        try {
            hasData = (m_BtClient.getInputStream().available() > 0);
        } catch (IOException ignored) {
            Log.e(TAG, "Bluetooth socket InputStream is not available");
        }

        return hasData;
    }

    /**
     * This will open the current connection if not open. The base class
     * function handles the starting of the threads used to do the asynchronous
     * communication with the device. The derived class will do the device
     * specific routines.
     *
     * @throws IOException exception
     */
    @Override
    protected boolean innerOpen() throws IOException {

        // Try to open
        if (getIsServerMode()) {
            // Server
            if (!m_Reconnecting) {
                // If reconnecting already have a listener
                m_BtClient = m_BtListener.accept();
            }
            // Mark as open
            m_IsOpen = true;
        } else {
            try {
                m_BtClient = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG,"create RFcomm socket failed");
            }

            //Connect to client socket
            if (m_BtClient != null) {
                m_BtClient.connect();            // connect client socket
                m_StreamRead = new DataInputStream(m_BtClient.getInputStream());
                m_StreamWrite = new DataOutputStream(m_BtClient.getOutputStream());
            }
            m_IsOpen = (m_StreamRead != null) && (m_StreamWrite != null);
            m_IsActive = m_IsOpen;
        }

        return m_IsOpen;
    }

    /**
     * This will close the current connection, if open, after all existing items
     * have finished printing. This is the internally called function that is
     * called with the parameter as true if it is used within this framework. If
     * the user calls Close, then that Close will pass a false. This will allow
     * is to wait for the thread to finish when the user initiates it.
     */
    @Override
    protected void close(boolean isInternalCall) {
        int timeout = (isInternalCall) ? 0 : Integer.MAX_VALUE;

        do {
            if (Monitor.tryEnter(m_LockGeneral, 0)) {
                try {
                    // We acquired a lock
                    timeout = 0;

                    if (getIsOpen()) {
                        // Base Class
                        closeBase(isInternalCall);

                        // Close
                        try {
                            if (m_StreamRead != null)
                                m_StreamRead.close();
                        } catch (Exception ignored) {
                            Log.e(TAG,"reading stream close error");
                        }
                        try {
                            if (m_StreamWrite != null)
                                m_StreamWrite.close();
                        } catch (Exception ignored) {
                            Log.e(TAG,"writing stream close error");
                        }
                        try {
                            if (m_BtClient != null)
                                m_BtClient.close();
                        } catch (Exception ignored) {
                            Log.e(TAG,"connection close error");
                        }
                        try {
                            if ((!m_Reconnecting) && (m_BtListener != null))
                                m_BtListener.close();
                        } catch (Exception ignored) {
                            Log.e(TAG,"BT listener close error");
                        }

                        // Clear
                        m_StreamRead = null;
                        m_StreamWrite = null;
                        m_BtClient = null;
                        if (!m_Reconnecting)
                            m_BtListener = null;

                        // Adjust state values
                        m_IsOpen = false;
                        m_IsActive = false;

                        //Sleep for a bit
                        Thread.sleep(1000);
                    }
                } catch (Exception ignored) {
                    Log.e(TAG,"exception");
                } finally {
                    Monitor.exit(m_LockGeneral);
                }
            } else {
                // Decrement Timeout
                timeout -= 100;
                if (timeout > 0) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception ignored) {
                        Log.e(TAG,"thead exception");
                    }
                }
            }
        }
        while (timeout > 0);
    }

    /**
     * This will read the existing data from the device creating the byte array
     * object and returning the number of bytes in the array.
     */
    @Override
    protected int innerRead(byte[] buffer) throws IOException {
        return m_StreamRead.read(buffer);
    }

    /**
     * This will write the provided buffer to the device.
     */
    @Override
    protected void innerWrite(byte[] buffer) throws IOException {
        m_StreamWrite.write(buffer);
        m_StreamWrite.flush();
    }

    /**
     * This will return true if we have connected to a new client.
     */
    @Override
    protected boolean innerListen() {
        boolean hasConnection = false;

        // Check for pending connections
        try {
            m_BtClient = m_BtListener.accept();
            m_StreamRead = new DataInputStream(m_BtClient.getInputStream());
            m_StreamWrite = new DataOutputStream(m_BtClient.getOutputStream());
            hasConnection = true;
        } catch (Exception ignored) {
            Log.e(TAG,"datastream exception");
        }

        return hasConnection;
    }

    /**
     * A summary of the configuration.
     */
    @Override
    protected String configSummary() {
        String results;

        // Build
        results = "Bluetooth " + ((getIsServerMode()) ? "(Server)" : "(Client)");

        return results;
    }

    /**
     * A single line description of the configuration.
     */
    @Override
    protected String configCompact() {
        return m_Address;
    }

    /**
     * A detailed description of the configuration.
     */
    @Override
    protected String configDetail() {
        String results = "";

        // Build
        if (getIsServerMode()) {
            // Ethernet Server
            results += "Bluetooth Server Mode\r\n";
        } else if (getIsClientMode()) {
            // Ethernet Client
            results += "" + "Bluetooth Client Settings\r\n" + "Target Address: "
                    + m_Address;
        }

        return results;
    }

    @Override
    public void clearWriteBuffer() {
        super.clearWriteBuffer();

        if (m_IsSynchronous) {
            try {
                m_StreamWrite.flush();
            } catch (IOException e) {
                Log.e(TAG,"IO exception");
            }
        }
    }

    /**
     * Converts Bluetooth Address string from 00ABCDEF0102 format => 00:AB:CD:EF:01:02 format
     *
     * @param bluetoothAddr - Bluetooth Address string to convert
     */
    private String formatBluetoothAddress(String bluetoothAddr) {
        //Format MAC address string
        StringBuilder formattedBTAddress = new StringBuilder(bluetoothAddr);
        for (int bluetoothAddrPosition = 2; bluetoothAddrPosition <= formattedBTAddress.length() - 2; bluetoothAddrPosition += 3)
            formattedBTAddress.insert(bluetoothAddrPosition, ":");
        return formattedBTAddress.toString();
    }
}
