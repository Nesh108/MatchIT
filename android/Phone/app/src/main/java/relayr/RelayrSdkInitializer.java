package relayr;

/**
 * Created by nesh on 09.05.15.
 */
import android.content.Context;

import io.relayr.RelayrSdk;

abstract class RelayrSdkInitializer {

    static void initSdk(Context context) {
        new RelayrSdk.Builder(context).inMockMode(true).build();
    }

}
