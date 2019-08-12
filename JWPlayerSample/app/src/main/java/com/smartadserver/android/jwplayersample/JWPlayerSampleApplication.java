package com.smartadserver.android.jwplayersample;

import android.app.Application;

import com.smartadserver.android.instreamsdk.util.SVSConfiguration;

/**
 * Use custom {@link Application} class to perform Smart instream SDK initialization
 */
public class JWPlayerSampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // the configuration must be setup before making any ad call
        SVSConfiguration.getSharedInstance().configure(this, MainActivity.SITE_ID);
    }
}
