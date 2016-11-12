package tonyg.example.com.exampleblescan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import tonyg.example.com.exampleblescan.ble.BleCommManager;
import tonyg.example.com.exampleblescan.ble.callbacks.BleScanCallbackv18;
import tonyg.example.com.exampleblescan.ble.callbacks.BleScanCallbackv21;
import tonyg.example.com.exampleblescan.models.BlePeripheralListItem;
import tonyg.example.com.exampleblescan.adapters.BlePeripheralsListAdapter;


/**
 * Scan for bluetooth low energy devices
 */
public class MainActivity extends AppCompatActivity {
    /** Constants **/
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    /** Bluetooth Stuff **/
    private BleCommManager mBleCommManager;

    /** Activity State **/
    private boolean mScanningActive = false;

    /** UI Stuff **/
    private MenuItem mScanProgressSpinner;
    private MenuItem mStartScanItem, mStopScanItem;
    private ListView mBlePeripheralsListView;
    private TextView mPeripheralsListEmptyTV;
    private BlePeripheralsListAdapter mBlePeripheralsListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadUI();
    }

    public void loadUI() {
        // load UI components, set up the Peripheral list
        mPeripheralsListEmptyTV = (TextView) findViewById(R.id.peripheral_list_empty);
        mBlePeripheralsListView = (ListView) findViewById(R.id.peripherals_list);
        mBlePeripheralsListAdapter = new BlePeripheralsListAdapter();
        mBlePeripheralsListView.setAdapter(mBlePeripheralsListAdapter);
        mBlePeripheralsListView.setEmptyView(mPeripheralsListEmptyTV);
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeBluetooth();
    }

    @Override
    public void onPause() {
        super.onPause();
        // stop scanning when the activity pauses
        mBleCommManager.stopScanning(mBleScanCallbackv18, mScanCallbackv21);
    }


    /**
     * Create a menu
     * @param menu The menu
     * @return <b>true</b> if processed successfully
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mStartScanItem = menu.findItem(R.id.action_start_scan);
        mStopScanItem =  menu.findItem(R.id.action_stop_scan);
        mScanProgressSpinner = menu.findItem(R.id.scan_progress_item);

        return true;
    }

    /**
     * Handle a menu item click
     *
     * @param item the Menuitem
     * @return <b>true</b> if processed successfully
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Start a BLE scan when a user clicks the "start scanning" menu button
        // and stop a BLE scan when a user clicks the "stop scanning" menu button
        switch (item.getItemId()) {
            case R.id.action_start_scan:
                // User chose the "Scan" item
                startScan();
                return true;

            case R.id.action_stop_scan:
                // User chose the "Stop" item
                stopScan();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    /**
     * Initialize the Bluetooth Radio
     */
    public void initializeBluetooth() {

        try {
            mBleCommManager = new BleCommManager(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }

        // should prompt user to open settings if Bluetooth is not enabled.
        if (!mBleCommManager.getBluetoothAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }


    /**
     * Start scanning for Peripherals
     *
     * New in this chapter
     */
    public void startScan() {
        // update UI components
        mStartScanItem.setVisible(false);
        mStopScanItem.setVisible(true);
        mScanProgressSpinner.setVisible(true);

        // clear the list of Peripherals and start scanning
        mBlePeripheralsListAdapter.clear();
        try {
            mScanningActive = true;
            mBleCommManager.scanForPeripherals(mBleScanCallbackv18, mScanCallbackv21);
        } catch (Exception e) {
            Log.e(TAG, "Could not open Ble Device Scanner");
        }

    }

    /**
     * Stop scanning for Peripherals
     *
     * New in this chapter
     */
    public void stopScan() {
        mBleCommManager.stopScanning(mBleScanCallbackv18, mScanCallbackv21);
    }


    /**
     * Event trigger when BLE Scanning has stopped
     *
     * New in this chapter
     */
    public void onBleScanStopped() {
        // update UI compenents to reflect that a BLE scan has stopped
        // it's possible that this method will be called before the menu has been instantiated
        // Check to see if menu items are initialized, or Activity will crash
        mScanningActive = false;
        if (mStopScanItem != null) mStopScanItem.setVisible(false);
        if (mScanProgressSpinner != null) mScanProgressSpinner.setVisible(false);
        if (mStartScanItem != null) mStartScanItem.setVisible(true);
    }

    /**
     * Event trigger when new Peripheral is discovered
     *
     * New in this chapter
     */
    public void onBlePeripheralDiscovered(BluetoothDevice bluetoothDevice, int rssi) {
        Log.v(TAG, "Found "+bluetoothDevice.getName()+", "+bluetoothDevice.getAddress());
        // only add the peripheral if
        // - it has a name, on
        // - doesn't already exist in our list, or
        // - is transmitting at a higher power (is closer) than an existing peripheral
        boolean addPeripheral = true;
        if (bluetoothDevice.getName() == null) {
            addPeripheral = false;
        }
        for(BlePeripheralListItem listItem : mBlePeripheralsListAdapter.getItems()) {
            if ( listItem.getBroadcastName().equals(bluetoothDevice.getName()) ) {
                addPeripheral = false;
            }
        }

        if (addPeripheral) {
            mBlePeripheralsListAdapter.addBluetoothPeripheral(bluetoothDevice, rssi);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBlePeripheralsListAdapter.notifyDataSetChanged();
                }
            });
        }
    }


    /**
     * Use this callback for Android API 21 (Lollipop) or greater
     *
     * New in this chapter
     */
    private final BleScanCallbackv21 mScanCallbackv21 = new BleScanCallbackv21() {
        /**
         * New Peripheral discovered
         *
         * @param callbackType int: Determines how this callback was triggered. Could be one of CALLBACK_TYPE_ALL_MATCHES, CALLBACK_TYPE_FIRST_MATCH or CALLBACK_TYPE_MATCH_LOST
         * @param result a Bluetooth Low Energy Scan Result, containing the Bluetooth Device, RSSI, and other information
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice bluetoothDevice = result.getDevice();
            int rssi = result.getRssi();

            onBlePeripheralDiscovered(bluetoothDevice, rssi);
        }

        /**
         * Several peripherals discovered when scanning in low power mode
         *
         * @param results List: List of scan results that are previously scanned.
         */
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                BluetoothDevice bluetoothDevice = result.getDevice();
                int rssi = result.getRssi();

                onBlePeripheralDiscovered(bluetoothDevice, rssi);
            }
        }

        /**
         * Scan failed to initialize
         *
         * @param errorCode	int: Error code (one of SCAN_FAILED_*) for scan failure.
         */
        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "Fails to start scan as BLE scan with the same settings is already started by the app.");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e(TAG, "Fails to start scan as app cannot be registered.");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "Fails to start power optimized scan as this feature is not supported.");
                    break;
                default: // SCAN_FAILED_INTERNAL_ERROR
                    Log.e(TAG, "Fails to start scan due an internal error");

            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });
        }

        /**
         * Scan completed
         */
        public void onScanComplete() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });

        }
    };

    /**
     * Use this callback for Android API 18, 19, and 20 (before Lollipop)
     *
     * New in this chapter
     */
    public final BleScanCallbackv18 mBleScanCallbackv18 = new BleScanCallbackv18() {
        /**
         * New Peripheral discovered
         * @param bluetoothDevice The Peripheral Device
         * @param rssi The Peripheral's RSSI indicating how strong the radio signal is
         * @param scanRecord Other information about the scan result
         */
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            onBlePeripheralDiscovered(bluetoothDevice, rssi);
        }

        /**
         * Scan completed
         */
        @Override
        public void onScanComplete() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleScanStopped();
                }
            });

        }
    };



}
