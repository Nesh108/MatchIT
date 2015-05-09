package release.java.io.relayr.wearable;

/**
 * Created by nesh on 09.05.15.
 */

import android.content.Context;

import io.relayr.RelayrSdk;

public class RelayrSdkInitializer {

    public static void initSdk(Context context) {
        new RelayrSdk.Builder(context).inMockMode(false).build();
    }

}