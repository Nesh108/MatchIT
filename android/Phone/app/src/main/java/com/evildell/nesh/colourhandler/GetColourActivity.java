package com.evildell.nesh.colourhandler;

import com.evildell.nesh.colourhandler.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import io.relayr.RelayrSdk;
import io.relayr.ble.BleDevice;
import io.relayr.ble.BleDeviceMode;
import io.relayr.ble.BleDeviceType;
import io.relayr.ble.service.BaseService;
import io.relayr.ble.service.DirectConnectionService;
import io.relayr.model.Reading;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class GetColourActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = false;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private Subscription mScannerSubscription = Subscriptions.empty();
    private Subscription mDeviceSubscription = Subscriptions.empty();

    private boolean mStartedScanning;
    private BleDevice mDevice;

    private View contentView;
    private TextView contentTextView;
    private Button searchBtn;

    boolean isScanOn = false;

    int colourDatasetRes;

    int PROX_THRESHOLD = 600;
    double proxVal = PROX_THRESHOLD;
    String SEARCH_URL = "https://dry-sea-4593.herokuapp.com/matching_color/";
    String foundColour = null;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new RelayrSdk.Builder(this).inMockMode(true).build();

        setContentView(R.layout.activity_get_colour);

        colourDatasetRes = R.raw.colour_dataset;

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        contentView = findViewById(R.id.fullscreen_content);
        contentTextView = (TextView) findViewById(R.id.fullscreen_content);
        searchBtn = (Button) findViewById(R.id.search_btn);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.scan_button).setOnTouchListener(mDelayHideTouchListener);
    }

    public void scanColour(View view) throws InterruptedException {
        isScanOn = !isScanOn;

        TextView btnTV = (TextView) findViewById(R.id.scan_button);
        if(isScanOn)
        {

            if (!mBluetoothAdapter.isEnabled())
                mBluetoothAdapter.enable();


            mStartedScanning = true;
            discoverColourSensor();


            btnTV.setText("Stop Scan");

            Log.d("SENSOR", "I AM ON!");
        }
        else {
            if (mBluetoothAdapter.isEnabled())
                mBluetoothAdapter.disable();


            btnTV.setText("Scan Colour");

            Log.d("SENSOR", "I AM OFF!");
        }


    }

    public void searchColour(View view) throws IOException {

        new RequestTask().execute(SEARCH_URL + foundColour);


    }

    class RequestTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... uri) {

            String responseString = "";
            Log.d("SENSOR_HTTP", "Fetching from: " + uri[0]);

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    responseString = out.toString();

                    Log.d("SENSOR_HTTP", responseString);
                    out.close();
                    //..more logic
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Do anything with response..
        }
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!RelayrSdk.isBleSupported()) {
            Toast.makeText(this, getString(R.string.bt_not_supported), Toast.LENGTH_SHORT).show();
        } else if (!RelayrSdk.isBleAvailable()) {
            RelayrSdk.promptUserToActivateBluetooth(this);
        }
    }

    @Override
    protected void onDestroy() {
        unSubscribeToUpdates();

        super.onDestroy();
    }

    public void discoverColourSensor() {
        // Search for WunderBar colour sensors and take first that is direct connection mode
        mScannerSubscription = RelayrSdk.getRelayrBleSdk()
                .scan(Arrays.asList(BleDeviceType.WunderbarLIGHT))
                .filter(new Func1<List<BleDevice>, Boolean>() {
                    @Override
                    public Boolean call(List<BleDevice> bleDevices) {
                        for (BleDevice device : bleDevices) {
                            if (device.getMode() == BleDeviceMode.DIRECT_CONNECTION) {
                                // We can stop scanning, since we've found a sensor
                                RelayrSdk.getRelayrBleSdk().stop();
                                return true;
                            }
                        }

                        mStartedScanning = false;
                        return false;
                    }
                })
                .map(new Func1<List<BleDevice>, BleDevice>() {
                    @Override
                    public BleDevice call(List<BleDevice> bleDevices) {
                        for (BleDevice device : bleDevices) {
                            if (device.getMode() == BleDeviceMode.DIRECT_CONNECTION) return device;
                        }

                        mStartedScanning = false;
                        return null; // will never happen since it's been filtered out before
                    }
                })
                .take(1)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<BleDevice>() {
                    @Override
                    public void onCompleted() {
                        mStartedScanning = false;
                    }

                    @Override
                    public void onError(Throwable e) {
                        mStartedScanning = false;
                        //tvColorError.setText(R.string.Sr_discovery_error);

                        Log.d("SENSOR_ERROR", getResources().getString(R.string.sensor_discovery_error));
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(BleDevice device) {
                        subscribeForColourUpdates(device);
                    }
                });
    }

    private void subscribeForColourUpdates(final BleDevice device) {
        mDevice = device;
        mDeviceSubscription = device.connect()
                .flatMap(new Func1<BaseService, Observable<Reading>>() {
                    @Override
                    public Observable<Reading> call(BaseService baseService) {
                        return ((DirectConnectionService) baseService).getReadings();
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        device.disconnect();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Reading>() {
                    @Override
                    public void onCompleted() {
                        mStartedScanning = false;
                    }

                    @Override
                    public void onError(Throwable e) {
                        mStartedScanning = false;
                        Log.d("SENSOR_ERROR", getResources().getString(R.string.sensor_reading_error));
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Reading reading) {

                        try {
                            TextView btnTV = (TextView) findViewById(R.id.scan_button);


                            Log.d("SENSOR", "I AM OUTSIDE THE LOOP");
                            if (isScanOn && reading.meaning.equals("proximity")) {
                                btnTV.setText("Scanning...");
                                proxVal = Double.parseDouble(reading.value.toString());
                                Log.d("SENSOR", "PROX VAL - " + proxVal);

                            } else if (isScanOn && reading.meaning.equals("color") && proxVal > PROX_THRESHOLD) {
                                Log.d("SENSOR", "I AM INSIDE THE LOOP");

                                String delims = "[=,}]";
                                String[] tokens = reading.value.toString().split(delims);
                                double dr = Double.parseDouble(tokens[5]);
                                double db = Double.parseDouble(tokens[1]);
                                double dg = Double.parseDouble(tokens[3]);

                                dr *= 2.0 / 3.0;
                                double max = Math.max(dr, Math.max(dg, db));

                                if (max > 0) {
                                    dr = dr / max * 255.0;
                                    dg = dg / max * 255.0;
                                    db = db / max * 255.0;
                                }

                                int intR = (int) dr;
                                int intG = (int) dg;
                                int intB = (int) db;

                                Log.d("SENSOR_VALUE", "R: " + intR + ",G: " + intG + ",B: " + intB);

                                intR = (intR << 16) & 0x00FF0000;
                                intG = (intG << 8) & 0x0000FF00;
                                intB = intB & 0x000000FF;

                                String hexR = Integer.toHexString(intR).substring(0,2);
                                String hexG = Integer.toHexString(intG).substring(0,2);
                                String hexB = Integer.toHexString(intB);

                                foundColour = (hexR + hexG + hexB).toUpperCase();

                                Log.d("SENSOR", "Hex found; " + foundColour);

                                contentView.setBackgroundColor(Color.rgb(intR, intG, intB));


                                String colourName = extractColour(intR, intG, intB);

                                if (!colourName.equals("")) {
                                    contentTextView.setText(colourName);

                                    if (mBluetoothAdapter.isEnabled())
                                        mBluetoothAdapter.disable();

                                    isScanOn = false;
                                    proxVal = PROX_THRESHOLD;
                                    btnTV.setText("Scan Colour");
                                    searchBtn.setVisibility(View.VISIBLE);
                                }
                            }

                        }

                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                    }

                });
    }




    private String extractColour(int R, int G, int B) {

        String colourFoundName = "";

        try {

            InputStream is = this.getResources().openRawResource(colourDatasetRes);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = br.readLine()) != null)
            {
                sb.append(line + "\n");
            }
            String result = sb.toString();

            JSONArray colourDataset = new JSONArray(result);

            int curDistance = 1000000;

            // looping through All nodes
            for (int i = 0; i < colourDataset.length(); i++) {

                JSONObject c = colourDataset.getJSONObject(i);
                int rValue = c.getInt("x");
                int gValue = c.getInt("y");
                int bValue = c.getInt("z");
                String name = c.getString("label");

                int colourValue = rValue * (256 * 256) + gValue * 256 + bValue;
                int myColourValue = R * (256 * 256) + G * 256 + B;

                if (Math.abs(colourValue - myColourValue) < curDistance)
                {
                    curDistance = Math.abs(colourValue - myColourValue);
                    colourFoundName = name;

                    Log.d("COLOR_MATCHING", "Closest colour: " + colourFoundName);
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return colourFoundName;
    }

    private void unSubscribeToUpdates() {
        mScannerSubscription.unsubscribe();
        mDeviceSubscription.unsubscribe();

        if (mDevice != null) mDevice.disconnect();
    }

}
