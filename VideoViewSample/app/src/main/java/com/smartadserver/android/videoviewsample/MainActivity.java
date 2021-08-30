package com.smartadserver.android.videoviewsample;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.smartadserver.android.coresdk.components.openmeasurement.SCSOpenMeasurementManager;
import com.smartadserver.android.coresdk.components.remotelogger.SCSRemoteLogUtils;
import com.smartadserver.android.coresdk.components.viewabilitymanager.SCSViewabilityStatus;
import com.smartadserver.android.coresdk.util.SCSConstants;
import com.smartadserver.android.coresdk.util.SCSUtil;
import com.smartadserver.android.coresdk.vast.SCSVastConstants;
import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;
import com.smartadserver.android.instreamsdk.admanager.SVSAdManager;
import com.smartadserver.android.instreamsdk.admanager.SVSCuePoint;
import com.smartadserver.android.instreamsdk.adplayer.SVSAdPlaybackEvent;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRule;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRuleData;
import com.smartadserver.android.instreamsdk.components.remotelogger.SVSRemoteLogger;
import com.smartadserver.android.instreamsdk.model.adbreak.SVSAdBreakType;
import com.smartadserver.android.instreamsdk.model.adbreak.event.SVSAdBreakEvent;
import com.smartadserver.android.instreamsdk.model.adobjects.SVSAdObject;
import com.smartadserver.android.instreamsdk.model.adplacement.SVSAdPlacement;
import com.smartadserver.android.instreamsdk.model.adplayerconfig.SVSAdPlayerConfiguration;
import com.smartadserver.android.instreamsdk.model.contentdata.SVSContentData;
import com.smartadserver.android.instreamsdk.plugin.CreativeMediaCache;
import com.smartadserver.android.instreamsdk.plugin.SVSVideoViewPlugin;
import com.smartadserver.android.instreamsdk.util.SVSLibraryInfo;
import com.smartadserver.android.instreamsdk.util.logging.SVSLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Simple activity that contains one an instance of {@link VideoView} as content player
 */
@SuppressWarnings("DanglingJavadoc")
public class MainActivity extends AppCompatActivity implements SVSVideoViewPlugin.HideControlButtons {

    // Constants

    // content video url
    static final private String CONTENT_VIDEO_URL = "https://ns.sascdn.com/mobilesdk/samples/videos/BigBuckBunnyTrailer_360p.mp4";

    //DUMMY list of contents
    private final List<String> CONTENTS =  new ArrayList<String>(){{
        add("format1.html");
        add("video1");
        add("format2.html");
        add("video2");
        add("format3.html");
        add("video3");
        add("video4");
    }};

    private int contentsIndex = 0;
    private static final String TAG = "MainActivity";


    // Smart Instream SDK placement parameters
    static final public int SITE_ID = 0;
    static final public int PAGE_ID = 0;
    static final public int FORMAT_ID = 0;
    static final public String TARGET = "";

    // Smart Instream SDK main ad manager class
    private SVSAdManager adManager;

    // ViewGroup that contains the content player
    private ViewGroup contentPlayerContainer;

    // flag to mark that SVSAdManager was started
    private boolean adManagerStarted;

    // VideoView related properties
    private VideoView videoView;
    private MediaController mediaController;
    private Button nextBtn, prevBtn;

    private WebView webView;

    /**
     * Performs Activity initialization after creation.
     */
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSystemUIVisibility();
        setContentView(R.layout.activity_main);

        nextBtn = findViewById(R.id.next_btn);
        nextBtn.setOnClickListener(v -> nextContent());

        prevBtn = findViewById(R.id.prev_btn);
        prevBtn.setOnClickListener(v -> previousContent());


        /**
         * TCF Consent String v2 manual setting.
         *
         * By uncommenting the following code, you will set the TCF consent string v2 manually.
         * Note: the Smart Instream SDK will retrieve the TCF consent string from the SharedPreferences using the official IAB key "IABTCF_TCString".
         *
         * If you are using a CMP that does not store the consent string in the SharedPreferences using the official
         * IAB key, please store it yourself with the official key.
         */
        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // SharedPreferences.Editor editor = prefs.edit();
        // editor.putString("IABTCF_TCString", "YourTCFConsentString");
        // editor.apply();

        /**
         * CCPA Consent String manual setting.
         *
         * By uncommenting the following code, you will set the CCPA consent string manually.
         * Note: The Smart Instream SDK will retrieve the CCPA consent string from the SharedPreferences using the official IAB key "IABUSPrivacy_String".
         *
         * If you are using a CMP that does not store the consent string in the SharedPreferences using the official
         * IAB key, please store it yourself with the official key.
         */
        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // SharedPreferences.Editor editor = prefs.edit();
        // editor.putString("IABUSPrivacy_String", "YourCCPAConsentString");
        // editor.apply();

        /******************************************
         * now perform Player related code here.
         ******************************************/
        bindViews();
        createAdManager();
        configurePlayer();
    }

    /**
     * Overriden to resume the {@link SVSAdManager} instance along with the Activity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        setSystemUIVisibility();
        if (adManager != null) {
            adManager.onResume();
        }
        playContent();
    }

    /**
     * Overriden to pause the {@link SVSAdManager} instance along with the Activity.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (adManager != null) {
            adManager.onPause();
        }
    }

    /**
     * Overriden to cleanup the {@link SVSAdManager} instance.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adManager != null) {
            adManager.onDestroy();
        }
    }

    /**
     * Bind all views to their related attributes.
     */
    private void bindViews() {
        mediaController = new MediaController(this);
        videoView = findViewById(R.id.video_view_player);
        contentPlayerContainer = findViewById(R.id.content_player_container);

        webView = findViewById(R.id.webview);

    }

    /**
     * Configures the player. See https://developer.android.com/reference/android/widget/VideoView.html
     */
    @SuppressWarnings("Convert2Lambda")
    private void configurePlayer() {

        // add a listener on the VideoView instance to start the SVSAdManager when the video is prepared
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(@NonNull MediaPlayer mediaPlayer) {
                // Once prepared, we start the SVSAdManager.
                startAdManager();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                adManager.notifyContentHasCompleted();
                nextContent();
                Log.d(TAG, "onCompletion of VIDEO");
            }
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "configurePlayer - ERROR: " + what);
            nextContent();
            return true;
        });
    }

    private void nextContent(){
        contentsIndex++;
        if (contentsIndex >= CONTENTS.size()) {
            contentsIndex = 0;
        }
        if(videoView.isPlaying()){
            videoView.seekTo(videoView.getDuration());
        }
        playContent();
    }

    private void previousContent(){
        contentsIndex--;
        if (contentsIndex < 0) {
            contentsIndex = CONTENTS.size()-1;
        }
        if(videoView.isPlaying()){
            videoView.seekTo(videoView.getDuration());
        }
        playContent();
    }


    /**
     * Decides what to play, based on the CONTENTS array. If the string contains 'video', then will play the video
     */
    private void playContent(){
        setSystemUIVisibility();

        webView.setVisibility(View.INVISIBLE);
        videoView.setVisibility(View.INVISIBLE);

        String contentToPlay = CONTENTS.get(contentsIndex);
        if(contentToPlay.contains("video")){
            webView.setVisibility(View.INVISIBLE);
            videoView.setVisibility(View.VISIBLE);
            String path = "android.resource://" + getPackageName() + "/" + getResources().getIdentifier(CONTENTS.get(contentsIndex), "raw", this.getPackageName());
            Log.d(TAG, "playVideo: " + path);

            videoView.setVideoPath(path);
            if(adManager != null){
                //is this necessary?
                adManager.replay();
            }
            videoView.start();



        }else{
            //WebView, hide VideoView show HTML
            webView.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.INVISIBLE);
            webView.loadUrl("file:///android_asset/" + contentToPlay);
        }


    }

    /**
     * Create the {@link SVSAdManager}.
     */
    private void createAdManager() {
        /******************************************************************************************************************************
         * The SVSAdManager is the class responsible for performing AdCalls and displaying ads.
         * To initialize this object you will need:
         * - a SVSAdPlacement instance, identifying this content video as ad inventory
         * - a listener implementing the SVSAdManager.UIInteractionListener interface to react to fullscreen enter/exit events
         *
         * Optional objects can also be passed during initialization:
         * - an array of SVSAdRule instances, to define advertising policy depending on the content duration. If null, the SVSAdManager
         *   will use a default one from the SVSConfiguration singleton class.
         * - SVSAdPlayerConfiguration, to modify the Ad Player look and behavior. If null, the SVSAdManager will use a default one from
         *   the SVSConfiguration singleton class.
         * - SVSContentData, describing your content. If null, the SVSAdManager will note use the data for targeting.
         *
         * Please refer to each initialization method for more information about these objects.
         ******************************************************************************************************************************/

        // Ad Placement, must be non null
        SVSAdPlacement adPlacement = instantiateAdPlacement();

        // Ad Rules, OPTIONAL
        SVSAdRule[] adRules = instantiateAdRules();

        //Ad Player Configuration, OPTIONAL
        SVSAdPlayerConfiguration adPlayerConfiguration = instantiateAdPlayerConfiguration();

        // Content Data, OPTIONAL
        SVSContentData contentData = instantiateContentData();

        // Create the SVSAdManager instance.
        adManager = new SVSAdManager(this, adPlacement, adRules, adPlayerConfiguration, contentData);

        adManager.setCreativeMediaURIProvider(new CreativeMediaCache(this));

    }

    /**
     * Starts the {@link SVSAdManager} instance.
     */
    private void startAdManager() {
        if (adManagerStarted) {
            return;
        }

        adManagerStarted = true;
        SVSContentPlayerPlugin plugin = instantiateContentPlayerPlugin();
        adManager.start(plugin);
    }

    /**
     * Creates a {@link SVSAdPlacement} instance
     */
    @NonNull
    private SVSAdPlacement instantiateAdPlacement() {
        /***************************************************************
         * SVSAdPlacement is mandatory to perform ad calls.
         * You cannot create ad SVSadManager without an SVSAdPlacement.
         ***************************************************************/

        // Create an SVSAdPlacement instance from your SiteID, PageID and FormatID.
        SVSAdPlacement adPlacement = new SVSAdPlacement(SITE_ID, PAGE_ID, FORMAT_ID);

        // Optional: you can setup the custom targeting for your placement.
        adPlacement.setGlobalTargetingString(TARGET); // Default targeting
        adPlacement.setPrerollTargetingString(null); // Preroll targeting
        adPlacement.setMidrollTargetingString(null); // Midroll targeting
        adPlacement.setPostrollTargetingString(null); // Postroll targeting

        return adPlacement;
    }

    /**
     * Creates an array of {@link SVSAdRule} instances
     */
    @NonNull
    private SVSAdRule[] instantiateAdRules() {
        /***********************************************************************************
         * SVSAdRule objects allow an advanced management of your advertising policy.
         * Please refer to the documentation for more information about these objects.
         * This object is optional:
         * SVSAdManager will create its own if no SVSAdRule are passed upon initialization.
         ***********************************************************************************/

        // Instantiate 3 SVSadruleData for Preroll, Midroll and Postroll.
        SVSAdRuleData prerollData = SVSAdRuleData.createPrerollAdRuleData(1, 1200); // Preroll with 1 ad.
        SVSAdRuleData midrollData = SVSAdRuleData.createMidrollAdRuleData(1, 1200, new double[]{50}); // Midroll with 1 ad when 50% of the content's duration is reached.
        SVSAdRuleData postrollData = SVSAdRuleData.createPostrollAdRuleData(1, 1200); // Postroll with 1 ad.

        // Instantiate a SVSAdRule with preroll, midroll and postroll SVSAdRuleData
        // this SVSAdRule will cover any duration.
        SVSAdRule adRule = new SVSAdRule(0, -1, new SVSAdRuleData[]{prerollData, midrollData, postrollData}, 0);

        // Return an array of SVSAdRule
        return new SVSAdRule[]{adRule};
    }

    /**
     * Creates a {@link SVSAdPlayerConfiguration} instance
     */
    @NonNull
    private SVSAdPlayerConfiguration instantiateAdPlayerConfiguration() {
        /*************************************************************************************************
         * SVSAdPlayerConfiguration is responsible for modifying the look and behavior ot the Ad Player.
         * This object is optional:
         * SVSAdManager will create its own if no SVSAdPlayerConfiguration is passed upon initialization.
         *************************************************************************************************/

        SVSAdPlayerConfiguration adPlayerConfiguration = new SVSAdPlayerConfiguration();

        // Force skip delay of 5 seconds for any ad. See API for more options...
        adPlayerConfiguration.getPublisherOptions().setForceSkipDelay(false);
        //disable full screen
        adPlayerConfiguration.getDisplayOptions().setEnableFullscreen(false);
        //disable count on ads
        adPlayerConfiguration.getDisplayOptions().setEnableCountdownSkip(false);
        adPlayerConfiguration.getDisplayOptions().setEnableCountdownVideo(false);

        //Enable Remote Rules Config
        adPlayerConfiguration.getPublisherOptions().setEnableSSAR(true);
        adPlayerConfiguration.getPublisherOptions().setReplayAds(true);


        // See API for more options...
        return adPlayerConfiguration;
    }

    /**
     * Creates a {@link SVSContentData} instance
     */
    private SVSContentData instantiateContentData() {
        /****************************************************************
         * SVSContentData provides information about your video content.
         * This object is optional.
         ****************************************************************/
        // Instantiate the builder.
        SVSContentData.Builder builder = new SVSContentData.Builder();

        // Sets your parameters.
        builder.setContentID("contentID");
        builder.setContentTitle("contentTitle");
        builder.setVideoContentType("videoContentType");
        builder.setVideoContentCategory("videoContentCategory");
        builder.setVideoContentDuration(60);
        builder.setVideoSeasonNumber(1);
        builder.setVideoEpisodeNumber(2);
        builder.setVideoContentRating("videoContentRating");
        builder.setContentProviderID("contentProviderID");
        builder.setContentProviderName("contentProviderName");
        builder.setVideoContentDistributorID("videoContainerDistributorID");
        builder.setVideoContentDistributorName("videoContainerDistributerName");
        builder.setVideoContentTags(new String[]{"tag1", "tag2"});
        builder.setExternalContentID("externalContentID");
        builder.setVideoCMSID("videoCMSID");

        // Then build your instance of SVSContentData
        return builder.build();
    }

    /**
     * Creates the {@link SVSVideoViewPlugin} that connects the {@link SVSAdManager} intance to the {@link VideoView} content player.
     */
    @NonNull
    private SVSContentPlayerPlugin instantiateContentPlayerPlugin() {
        /************************************************************************************************
         * To know when to display AdBreaks, the SVSAdManager needs to monitor your content, especially:
         * - total duration
         * - current time
         * To be able to start the SVSAdManager, you need to pass an object implementing
         * the SVSContentPlayerPlugin interface. Here, we instantiate a ready-to-use SVSExoPlayerPlugin
         * for the ExoPlayer.
         ************************************************************************************************/
        return new SVSVideoViewPlugin(videoView, mediaController, contentPlayerContainer, false, this);
    }

    @Override
    public void showButtons() {
        nextBtn.setVisibility(View.VISIBLE);
        prevBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideButtons() {
        nextBtn.setVisibility(View.GONE);
        prevBtn.setVisibility(View.GONE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setSystemUIVisibility();
    }

    private void setSystemUIVisibility() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
