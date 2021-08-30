package com.smartadserver.android.instreamsdk.plugin;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;
import com.smartadserver.android.instreamsdk.adplayer.SVSMediaURIProvider;

import java.io.File;

public class CreativeMediaCache implements SVSMediaURIProvider {

    private static final String TAG = "CreativeMediaCache";

    // The HttpProxyCacheServer instance
    private static HttpProxyCacheServer proxy;

    // a cache listener to display debug info about caching progress
    private static CacheListener cacheListener;

    /**
     * Contructor
     */
    public CreativeMediaCache(Context context) {
        // ensure that there is only one instance of the HttpProxyCacheServer class.
        if (proxy == null) {
            proxy = new HttpProxyCacheServer.Builder(context)
                    .maxCacheSize(1024 * 1024 * 100)       // 100 MB for cache
                    .build();

            cacheListener = new CacheListener() {
                @Override
                public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {

                    if (percentsAvailable % 10 == 0) {
                        Log.d(TAG, "onCacheAvailable for url: " + url + " %:" + percentsAvailable + " (cache file: " + cacheFile + ")");
                    }
                    // unregister cache listener for the url that is fully cached
                    if (percentsAvailable == 100) {
                        proxy.unregisterCacheListener(this, url);
                    }
                }
            };
        }
    }

    @Nullable
    @Override
    public Uri getURIForMediaUrl(@NonNull String mediaUrl) {
        Uri returnUri = null;

        if (!proxy.isCached(mediaUrl)) {
            // debug traces
            proxy.registerCacheListener(cacheListener , mediaUrl);
        }

        // get proxyied url from the HttpProxyCacheServer instance.
        String proxyUrl = proxy.getProxyUrl(mediaUrl);

        Log.d(TAG,"proxyUrl:" + proxyUrl + "  for media url:" + mediaUrl);

        returnUri =   Uri.parse(proxyUrl);

        return returnUri;
    }
}
