package com.evildell.nesh.colourhandler;

import com.evildell.nesh.colourhandler.R;
import com.evildell.nesh.colourhandler.util.SystemUiHider;
import com.google.android.gms.wearable.MessageApi;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.graphics.drawable.DrawableWrapper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
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
    private View productView;
    private TextView productData;
    private TextView productId;
    private Button searchBtn;
    ImageButton productImage;

    boolean isScanOn = false;

    int colourDatasetRes;

    int PROX_THRESHOLD = 500;
    double proxVal = PROX_THRESHOLD;
    String SEARCH_URL = "https://dry-sea-4593.herokuapp.com/matching_color/";
    String PURCHASE_URL = "https://dry-sea-4593.herokuapp.com/make_payment/";
    String foundColour = "";

    String currPrice = "";
    String currCurrency = "";
    String currName = "";
    String currProdId = "";

    private static final int REQUEST_RESOLVE_ERROR = 1000;


    private static final String TAG = "SENSOR";

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String COUNT_PATH = "/count";
    private static final String IMAGE_PATH = "/image";
    private static final String IMAGE_KEY = "photo";
    private static final String COUNT_KEY = "count";

    private boolean mResolvingError = false;
    private boolean mCameraSupported = false;

    private Handler mHandler;
    int color_counter = 0;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    private static final double RELATIVE_START_POS = 320.0 / 1110.0;
    private static final double RELATIVE_STOP_POS = 885.0 / 1110.0;

    private View dotView;
    private int startY = -1;
    private int segmentLength = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new RelayrSdk.Builder(this).inMockMode(true).build();

        setContentView(R.layout.activity_get_colour);

        colourDatasetRes = R.raw.colour_dataset;

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        contentView = findViewById(R.id.fullscreen_content);
        productView = findViewById(R.id.product_view);
        productData = (TextView) findViewById(R.id.product_data);
        productId = (TextView) findViewById(R.id.product_id);
        searchBtn = (Button) findViewById(R.id.search_btn);
        productImage = (ImageButton) findViewById(R.id.product_image);

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
            productView.setVisibility(View.INVISIBLE);

            if (!mBluetoothAdapter.isEnabled())
                mBluetoothAdapter.enable();


            mStartedScanning = true;
            discoverColourSensor();

            btnTV.setText("Preparing Scanner...");

            Log.d("SENSOR", "I AM ON!");
        }
        else {

            productView.setVisibility(View.INVISIBLE);

            if (mBluetoothAdapter.isEnabled())
                mBluetoothAdapter.disable();


            btnTV.setText("Scan Colour");

            Log.d("SENSOR", "I AM OFF!");
        }


    }

    public void buyProduct(View v)   {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Buy product");
        builder.setMessage("Do you want to buy this product for " + currPrice + " " + currCurrency + "?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                Toast.makeText( getBaseContext(), "You are going to buy '" + currName + "'.", Toast.LENGTH_SHORT).show();

                new PurchaseItemTask().execute(PURCHASE_URL + currProdId);

                dialog.dismiss();

            }

        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

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
            } catch (Exception e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            searchBtn.setVisibility(View.INVISIBLE);

            // remove any previous image
            productImage.setImageDrawable(null);
            currPrice = "";
            currCurrency = "";
            currName = "";

            try {
                JSONObject c = new JSONObject(result);

                currProdId = c.getString("id");
                String prodImg = c.getString("image");
                currName = c.getJSONObject("name").getString("en");
                currPrice = "" + (((float) c.getJSONObject("price").getInt("centAmount")) / 100);
                currCurrency = c.getJSONObject("price").getString("currencyCode");


                productView.setVisibility(View.VISIBLE);
                productData.setText(currName + "\nPrice: " + currPrice + " " + currCurrency);
                productId.setText(currProdId);

                new GetImageTask().execute(prodImg);



            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    class GetImageTask extends AsyncTask<String, String, Drawable> {

        @Override
        protected Drawable doInBackground(String... uri) {

            InputStream is = null;
            try {
                is = (InputStream) new URL(uri[0]).getContent();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return Drawable.createFromStream(is, null);
        }

        @Override
        protected void onPostExecute(Drawable prodDrw) {

            super.onPostExecute(prodDrw);

            productImage.setImageDrawable(prodDrw);


        }
    }

    class PurchaseItemTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... uri) {

            String responseString = "";
            Log.d("SENSOR_HTTP2", "Fetching from: " + uri[0]);

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(uri[0]);

                // Add your data
                List<NameValuePair> productInfo = new ArrayList<NameValuePair>(2);
                productInfo.add(new BasicNameValuePair("name", currName));
                productInfo.add(new BasicNameValuePair("price", currPrice));
                productInfo.add(new BasicNameValuePair("currency", currCurrency));
                productInfo.add(new BasicNameValuePair("quantity", "1"));
                httppost.setEntity(new UrlEncodedFormEntity(productInfo));

                HttpResponse response = httpclient.execute(httppost);
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
                    responseString = "ERROR_PAYMENT";
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d("SENSOR_PURCHASE", "Got: " + responseString);
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);

            if(!result.equals("ERROR_PAYMENT"))
                Toast.makeText(getBaseContext(), "You have successfully purchased '" + currName + "'.", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getBaseContext(), "Error during payment of '" + currName + "'.", Toast.LENGTH_LONG).show();

        }
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

                        Log.d("SENSOR", "LIGHT DATA: " + reading.value.toString());
                        try {
                            TextView btnTV = (TextView) findViewById(R.id.scan_button);


                            if (isScanOn && reading.meaning.equals("proximity")) {
                                btnTV.setText("Scan now. Press to stop scanning.");
                                proxVal = Double.parseDouble(reading.value.toString());
                                Log.d("SENSOR", "PROX VAL - " + proxVal);

                            } else if (isScanOn && reading.meaning.equals("color") && proxVal > PROX_THRESHOLD) {

                                color_counter++;

                                Log.d("SENSOR", "Counter: " + color_counter);
                                if(color_counter > 2) {
                                    Log.d("SENSOR", "COLOUR VAL - " + reading.value.toString());

                                    String delims = "[=,}]";
                                    String[] tokens = reading.value.toString().split(delims);
                                    double dr = Double.parseDouble(tokens[5]);
                                    double db = Double.parseDouble(tokens[1]);
                                    double dg = Double.parseDouble(tokens[3]);

                                    Log.d("SENSOR", "R: " + dr + " - G: " + db + " - B: " + dg);
                                    /*
                                    int intR = (int) (dr * (255.0/1000.0));
                                    int intG = (int) (dg * (255.0/1000.0));
                                    int intB = (int) (db * (255.0/1000.0));

                                    */


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

                                    /*intR = (intR << 16) & 0x00FF0000;
                                    intG = (intG << 8) & 0x0000FF00;
                                    intB = intB & 0x000000FF;
                                    */

                                    String hexR = Integer.toHexString(intR).substring(0, 2);
                                    String hexG = Integer.toHexString(intG).substring(0, 2);
                                    String hexB = Integer.toHexString(intB);

                                    foundColour = (hexR + hexG + hexB).toUpperCase();

                                    Log.d("SENSOR", "Hex found; " + foundColour);

                                    searchBtn.setVisibility(View.VISIBLE);
                                    contentView.setBackgroundColor(Color.rgb(intR, intG, intB));

                                    searchBtn.bringToFront();
                                    // String colourName = extractColour(intR, intG, intB);

                                    if (mBluetoothAdapter.isEnabled())
                                        mBluetoothAdapter.disable();

                                    isScanOn = false;
                                    proxVal = PROX_THRESHOLD;
                                    btnTV.setText("Scan Colour");
                                    color_counter = 0;
                                }

                            }
                            else if (isScanOn && reading.meaning.equals("color") && proxVal <= PROX_THRESHOLD)
                                color_counter = 0;

                        }

                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }

                });
    }




    /*private String extractColour(int R, int G, int B) {

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
    }*/

    private void unSubscribeToUpdates() {
        mScannerSubscription.unsubscribe();
        mDeviceSubscription.unsubscribe();

        if (mDevice != null) mDevice.disconnect();
    }


}
