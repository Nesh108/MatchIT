package com.evildell.nesh.matchit;

import android.annotation.TargetApi;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

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

public class MatchITActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    private Subscription mScannerSubscription = Subscriptions.empty();
    private Subscription mDeviceSubscription = Subscriptions.empty();

    private boolean mStartedScanning;
    private BleDevice mDevice;

    private View searchView;
    private View scanView;
    private View productView;

    private TextView productData;
    private TextView productId;

    private ImageView productImage;

    boolean isScanOn = false;


    int PROX_THRESHOLD = 500;
    double proxVal = PROX_THRESHOLD;
    String SEARCH_URL = "https://dry-sea-4593.herokuapp.com/matching_color/";
    String foundColour = "";

    String currPrice = "";
    String currCurrency = "";
    String currName = "";
    String currProdId = "";

    int color_counter = 3;

    private static final String TAG = "SENSOR";

    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler;


    private static final String MOBILE_PATH = "/mobile";
    private static final long CONNECTION_TIME_OUT_MS = 30;

    private String nodeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new RelayrSdk.Builder(this).inMockMode(true).build();
        mHandler = new Handler();
        Log.d(TAG, "onCreate");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        setContentView(R.layout.activity_match_it);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                searchView = findViewById(R.id.search_view);
                scanView = findViewById(R.id.scan_view);
                productView = findViewById(R.id.product_view);
                productData = (TextView) findViewById(R.id.product_data);
                productId = (TextView) findViewById(R.id.product_id);
                productImage = (ImageView) findViewById(R.id.product_image);

            }
        });
    }


    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.d(TAG, "onMessageReceived: " + event);
        generateEvent("Message", event.toString());
    }



    public void scanColour(View view) throws InterruptedException {
        isScanOn = !isScanOn;

        TextView btnTV = (TextView) findViewById(R.id.scan_button);
        if(isScanOn)
        {
            productView.setVisibility(View.INVISIBLE);

           // if (!mBluetoothAdapter.isEnabled())
           //     mBluetoothAdapter.enable();


            mStartedScanning = true;
            discoverColourSensor();


            btnTV.setText("Getting ready...");

            Log.d("SENSOR", "I AM ON!");
        }
        else {

            productView.setVisibility(View.INVISIBLE);

           // if (mBluetoothAdapter.isEnabled())
           //     mBluetoothAdapter.disable();


            btnTV.setText("Scan");

            Log.d("SENSOR", "I AM OFF!");
        }


    }

    public void buyProduct(View view)   {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Buy product");
        builder.setMessage("Do you want to buy this product for " + currPrice + " " + currCurrency + "?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                Toast.makeText(getBaseContext(), "You are going to buy '" + currName + "'.", Toast.LENGTH_LONG).show();

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

        //new RequestTask().execute(SEARCH_URL + foundColour);

        Toast.makeText(this, "Sending message to phone...", Toast.LENGTH_LONG).show();


    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    private void generateEvent(final String title, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Title: " + title + " - Text: " + text);
            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(): " + dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (DataLayerListenerService.IMAGE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photo = dataMapItem.getDataMap()
                            .getAsset(DataLayerListenerService.IMAGE_KEY);
                    final Bitmap bitmap = loadBitmapFromAsset(mGoogleApiClient, photo);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Setting background image..");
                        }
                    });

                } else if (DataLayerListenerService.COUNT_PATH.equals(path)) {
                    Log.d(TAG, "Data Changed for COUNT_PATH");
                    generateEvent("DataItem Changed", event.getDataItem().toString());
                } else {
                    Log.d(TAG, "Unrecognized path: " + path);
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                generateEvent("DataItem Deleted", event.getDataItem().toString());
            } else {
                generateEvent("Unknown data event type", "Type = " + event.getType());
            }
        }
    }

    /**
     * Extracts {@link android.graphics.Bitmap} data from the
     * {@link com.google.android.gms.wearable.Asset}
     */
    private Bitmap loadBitmapFromAsset(GoogleApiClient apiClient, Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                apiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    public void onPeerConnected(Node node) {
        generateEvent("Node Connected", node.getId());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        generateEvent("Node Disconnected", node.getId());
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

            //searchBtn.setVisibility(View.INVISIBLE);

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


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!RelayrSdk.isBleSupported()) {
            Toast.makeText(this, getString(R.string.bt_not_supported), Toast.LENGTH_SHORT).show();
        } else if (!RelayrSdk.isBleAvailable()) {
            RelayrSdk.promptUserToActivateBluetooth(this);
        }


        mGoogleApiClient.connect();

    }

    @Override
    protected void onDestroy() {
        unSubscribeToUpdates();

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
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
                                proxVal = Double.parseDouble(reading.value.toString());
                                Log.d("SENSOR", "PROX VAL - " + proxVal);

                            } else if (isScanOn && reading.meaning.equals("color") && proxVal > PROX_THRESHOLD) {

                                color_counter--;

                                btnTV.setText("Scan - " + color_counter);

                                Log.d("SENSOR", "Counter: " + color_counter);
                                if(color_counter == 0) {
                                    Log.d("SENSOR", "COLOUR VAL - " + reading.value.toString());

                                    String delims = "[=,}]";
                                    String[] tokens = reading.value.toString().split(delims);
                                    double dr = Double.parseDouble(tokens[5]);
                                    double db = Double.parseDouble(tokens[1]);
                                    double dg = Double.parseDouble(tokens[3]);

                                    Log.d("SENSOR", "R: " + dr + " - G: " + db + " - B: " + dg);

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

                                    String hexR = Integer.toHexString(intR).substring(0, 2);
                                    String hexG = Integer.toHexString(intG).substring(0, 2);
                                    String hexB = Integer.toHexString(intB);

                                    foundColour = (hexR + hexG + hexB).toUpperCase();

                                    Log.d("SENSOR", "Hex found; " + foundColour);

                                    searchView.setVisibility(View.VISIBLE);
                                    searchView.setBackgroundColor(Color.rgb(intR, intG, intB));

                                    //searchBtn.bringToFront();
                                    // String colourName = extractColour(intR, intG, intB);

                                    //if (mBluetoothAdapter.isEnabled())
                                   //     mBluetoothAdapter.disable();

                                    isScanOn = false;
                                    proxVal = PROX_THRESHOLD;
                                    btnTV.setText("Scan!");
                                    color_counter = 3;
                                }

                            }
                            else if (isScanOn && reading.meaning.equals("color") && proxVal <= PROX_THRESHOLD)
                            {
                                color_counter = 3;
                                btnTV.setText("Scan!");
                            }

                        }

                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }

                });
    }


    private void unSubscribeToUpdates() {
        mScannerSubscription.unsubscribe();
        mDeviceSubscription.unsubscribe();

        if (mDevice != null) mDevice.disconnect();
    }

}
