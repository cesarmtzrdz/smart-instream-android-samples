package com.smartadserver.android.videoviewsample;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;
import com.smartadserver.android.instreamsdk.admanager.SVSAdManager;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRule;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRuleData;
import com.smartadserver.android.instreamsdk.model.adplacement.SVSAdPlacement;
import com.smartadserver.android.instreamsdk.model.adplayerconfig.SVSAdPlayerConfiguration;
import com.smartadserver.android.instreamsdk.model.contentdata.SVSContentData;
import com.smartadserver.android.instreamsdk.plugin.SVSVideoViewPlugin;
import com.smartadserver.android.instreamsdk.util.SVSLibraryInfo;

/**
 * Simple activity that contains one an instance of {@link VideoView} as content player
 */
public class MainActivity extends AppCompatActivity {

    // Constants

    // content video url
    static final private String CONTENT_VIDEO_URL = "https://ns.sascdn.com/mobilesdk/samples/videos/BigBuckBunnyTrailer_360p.mp4";

    // Smart Instream SDK placement parameters
    static final public int SITE_ID = 213040;
    static final public int PAGE_ID = 901271;
    static final public int FORMAT_ID = 29117;
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

    /**
     * Performs Activity initialization after creation.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set label of SDK version label
        TextView sdkVersionTextview = findViewById(R.id.sdk_version_textview);
        sdkVersionTextview.setText("Smart Instream SDK v" + SVSLibraryInfo.getSharedInstance().getVersion());

        /**
         * GDPR Consent String manual setting.
         *
         * By uncommenting the following code, you will set the GDPR consent string manually.
         * Note: the Smart Instream SDK will use retrieve the consent string from the SharedPreferences using the official IAB key "IABConsent_ConsentString".
         * If using the SmartCMP SDK, you will not have to do this because the SmartCMP already stores the consent string
         * using the official key.
         * If you are using any other CMP that do not store the consent string in the SharedPreferences using the official
         * IAB key, please store it yourself with the official key.
         */
        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // SharedPreferences.Editor editor = prefs.edit();
        // editor.putString("IABConsent_ConsentString", "YourConsentString");
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
        if (adManager != null) {
            adManager.onResume();
        }
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
        videoView = findViewById(R.id.video_view_player);
        contentPlayerContainer = findViewById(R.id.content_player_container);
    }

    /**
     * Configures the player. See https://developer.android.com/reference/android/widget/VideoView.html
     */
    private void configurePlayer() {
        mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.parse(CONTENT_VIDEO_URL));

        // add a listener on the VideoView instance to start the SVSAdManager when the video is prepared
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                mediaController.setAnchorView(videoView);
                // Once prepared, we start the SVSAdManager.
                startAdManager();
            }
        });
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
    private SVSAdPlayerConfiguration instantiateAdPlayerConfiguration() {
        /*************************************************************************************************
         * SVSAdPlayerConfiguration is responsible for modifying the look and behavior ot the Ad Player.
         * This object is optional:
         * SVSAdManager will create its own if no SVSAdPlayerConfiguration is passed upon initialization.
         *************************************************************************************************/

        // Create a new SVSAdPlayerConfiguration.
        SVSAdPlayerConfiguration adPlayerConfiguration = new SVSAdPlayerConfiguration();

        // Force skip delay of 5 seconds for any ad.
        adPlayerConfiguration.getPublisherOptions().setForceSkipDelay(true);
        adPlayerConfiguration.getPublisherOptions().setSkipDelay(5000);

        // video view to not have fullscreen button.
        adPlayerConfiguration.getDisplayOptions().setEnableFullscreen(false);

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

        SVSContentData contentData = new SVSContentData("contentID",
                "contentTitle",
                "videoContentType",
                "videoContentCategory",
                60,
                1,
                2,
                "videoContentRating",
                "contentProviderID",
                "contentProviderName",
                "videoContainerDistributorID",
                "videoContainerDistributorName",
                new String[]{"tag1", "tag2"},
                "externalContentID",
                "videoCMSID");

        return contentData;
    }

    /**
     * Creates the {@link SVSVideoViewPlugin} that connects the {@link SVSAdManager} intance to the {@link VideoView} content player.
     */
    private SVSContentPlayerPlugin instantiateContentPlayerPlugin() {
        /************************************************************************************************
         * To know when to display AdBreaks, the SVSAdManager needs to monitor your content, especially:
         * - total duration
         * - current time
         * To be able to start the SVSAdManager, you need to pass an object implementing
         * the SVSContentPlayerPlugin interface. Here, we instantiate a ready-to-use SVSExoPlayerPlugin
         * for the ExoPlayer.
         ************************************************************************************************/
        SVSVideoViewPlugin playerPlugin = new SVSVideoViewPlugin(videoView, mediaController, contentPlayerContainer, false);

        return playerPlugin;
    }

}
