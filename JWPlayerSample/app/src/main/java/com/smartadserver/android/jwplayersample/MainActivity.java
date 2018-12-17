package com.smartadserver.android.jwplayersample;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.longtailvideo.jwplayer.JWPlayerFragment;
import com.longtailvideo.jwplayer.JWPlayerView;
import com.longtailvideo.jwplayer.fullscreen.FullscreenHandler;
import com.longtailvideo.jwplayer.media.playlists.PlaylistItem;
import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;
import com.smartadserver.android.instreamsdk.admanager.SVSAdManager;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRule;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRuleData;
import com.smartadserver.android.instreamsdk.model.adplacement.SVSAdPlacement;
import com.smartadserver.android.instreamsdk.model.adplayerconfig.SVSAdPlayerConfiguration;
import com.smartadserver.android.instreamsdk.model.contentdata.SVSContentData;
import com.smartadserver.android.instreamsdk.plugin.SVSJWPlayerPlugin;
import com.smartadserver.android.instreamsdk.util.SVSLibraryInfo;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Simple activity that contains one an instance of {@link JWPlayerView} as content player
 */
public class MainActivity extends AppCompatActivity implements SVSAdManager.UIInteractionListener {

    // Constants

    // content video url
    static final private String CONTENT_VIDEO_URL = "http://ns.sascdn.com/mobilesdk/samples/videos/BigBuckBunnyTrailer_360p.mp4";

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

    // JWPlayer player view
    private JWPlayerView jwPlayerView;

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
        configurePlayer();
        createAdManager();

        /******************************************
         * now perform Player related code here.
         ******************************************/
        bindViews();
        configurePlayer();
        createAdManager();
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
        JWPlayerFragment jwFragment = (JWPlayerFragment) getFragmentManager().findFragmentById(R.id.jw_video_player);
        jwPlayerView = jwFragment.getPlayer();
        jwFragment.onResume();
        jwFragment.setFullscreenOnDeviceRotate(true);
        contentPlayerContainer = findViewById(R.id.content_player_container);
    }

    /**
     * Configures the player. See https://developer.jwplayer.com/sdk/android/docs/developer-guide/
     */
    private void configurePlayer() {
        PlaylistItem playlistItem = new PlaylistItem(CONTENT_VIDEO_URL);

        // here is the issue : jwPlayerView returns 0 duration until after "some time" play was requested.
        // so the trick is to
        // 1 - hide JW Player
        //jwPlayerView.setVisibility(View.INVISIBLE);
        // 2 - load the video
        jwPlayerView.load(playlistItem);
        // 3 - call jwPlayerView.play()
        jwPlayerView.play();

        // 4 - start a timer that polls the duration until it is > 0
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long duration = jwPlayerView.getDuration();
                if (duration > 0) {
                    // 5 - call jwPlayerView.pause(), cancel the timer and call configureAds() that eventually starts the SVSAdManager
                    jwPlayerView.pause();
                    timer.cancel();
                    startAdManager();
                }
            }
        }, 0, 250);


        // add a full screen handler object on the JWPLayer player view
        jwPlayerView.setFullscreenHandler(new FullscreenHandler() {
            /**
             * Called when JWPlayer requests to enter fullscreen.
             */
            @Override
            public void onFullscreenRequested() {
                MainActivity.this.requestFullscreen(true, true);
            }

            /**
             * Called when JWPlayer requests to exit fullscreen.
             */
            @Override
            public void onFullscreenExitRequested() {
                MainActivity.this.requestFullscreen(false, true);
            }

            @Override
            public void onResume() {}

            @Override
            public void onPause() {}

            @Override
            public void onDestroy() {}

            @Override
            public void onAllowRotationChanged(boolean b) {}

            @Override
            public void updateLayoutParams(ViewGroup.LayoutParams layoutParams) {}

            @Override
            public void setUseFullscreenLayoutFlags(boolean b) {}
        });

        // start loading video
        jwPlayerView.load(playlistItem);
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
        adManager.addUIInteractionListener(this);
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
     * Creates the {@link SVSJWPlayerPlugin} that connects the {@link SVSAdManager} intance to the {@link JWPlayerView} content player.
     */
    private SVSContentPlayerPlugin instantiateContentPlayerPlugin() {
        /************************************************************************************************
         * To know when to display AdBreaks, the SVSAdManager needs to monitor your content, especially:
         * - total duration
         * - current time
         * To be able to start the SVSAdManager, you need to create a content player plugin,
         * conforming to the SVSContentPlayerPlugin interface.
         ************************************************************************************************/
        SVSJWPlayerPlugin playerPlugin = new SVSJWPlayerPlugin(jwPlayerView, contentPlayerContainer, false);

        return playerPlugin;
    }

    /**
     * Implementation of SVSAdManager.UIInteractionListener.
     */
    @Override
    public void onFullscreenStateChangeRequest(boolean isFullscreen) {
        // Called when the enter (or exit) fullscreen button of an Ad is clicked by the user.
        // Adapt your UI to properly react to this user action: you should resize your container view.
        requestFullscreen(isFullscreen, false);

        /************************************************************************************************************
         * NOTE ABOUT FULLSCREEN / EXIT FULLSCREEN
         *
         * For obvious reasons, SVSAdManager, will never force your application or your content player into
         * fullscreen. It is your application that decides what to do with it.
         * If you allow the fullscreen / exit fullscreen buttons on the Ad Player Interface (in SVSAdPlayerConfiguration),
         * the SVSAdManager instance will request to enter fullscreen through the onFullscreenStateChangeRequest method of
         * SVSAdManager.UIInteractionListener.
         * You are responsible for responding to this event, and change the layout of your application accordingly.
         * In return, if you allow your content player to enter/exit fullscreen you must let the SVSAdManager know about it by
         * setting the new state of the content player through the onFullscreenStateChange(boolean isFullscreen) method of SVSAdManager
         ************************************************************************************************************/
    }

    /**
     * Whether JWPlayer or AdPlayer change their fullscreen status, we must let the SVSAdManager
     * know about it so it can adjust the UI of the ad player view.
     * Adapt your UI to properly react to the fullscreen status change.
     */
    private void requestFullscreen(boolean fullscreen, boolean requestFromPlayer) {
        if (requestFromPlayer) {
            //If its from JWPlayer we make it fullScreen immediately.
            disableShowHideAnimation(getSupportActionBar());

            // Update SystemUIVisibility to hide/show the StatusBar, the ActionBar and the NavigationBar.
            int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
            if (fullscreen) {
                uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
                uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                getSupportActionBar().hide();
            } else {
                uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
                uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
                uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                getSupportActionBar().show();
            }
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);

            // Tell the adManager that we are going in ou out fullscreen.
            adManager.onFullscreenStateChange(fullscreen);

            // Update visibility of several components depending on isFullscreen value.
            makePlayerMatchParent(fullscreen);
        } else {
            // If not, we notified the JWPlayer and it will call requestFullscreen itself.
            jwPlayerView.setFullscreen(fullscreen, false);
        }
    }

    /**
     * Updates contentPlayerContainer's layoutParams to either match or not its parent.
     * @param matchParent
     */
    private void makePlayerMatchParent(boolean matchParent) {
        if (matchParent) {
            contentPlayerContainer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            // we set an hard value for the height cause if we don't, JWplayer will take all the screen.
            float playerLayoutHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, getResources().getDisplayMetrics());
            ViewGroup.LayoutParams lp = contentPlayerContainer.getLayoutParams();
            lp.height = (int)playerLayoutHeight;
            contentPlayerContainer.setLayoutParams(lp);
        }
    }

    /**
     * Workaround method to disable the show/hide animation and avoid making the ActionBar flicker.
     */
    public static void disableShowHideAnimation(ActionBar actionBar) {
        try
        {
            actionBar.getClass().getDeclaredMethod("setShowHideAnimationEnabled", boolean.class).invoke(actionBar, false);
        }
        catch (Exception exception)
        {
            try {
                Field mActionBarField = actionBar.getClass().getSuperclass().getDeclaredField("mActionBar");
                mActionBarField.setAccessible(true);
                Object icsActionBar = mActionBarField.get(actionBar);
                Field mShowHideAnimationEnabledField = icsActionBar.getClass().getDeclaredField("mShowHideAnimationEnabled");
                mShowHideAnimationEnabledField.setAccessible(true);
                mShowHideAnimationEnabledField.set(icsActionBar,false);
                Field mCurrentShowAnimField = icsActionBar.getClass().getDeclaredField("mCurrentShowAnim");
                mCurrentShowAnimField.setAccessible(true);
                mCurrentShowAnimField.set(icsActionBar,null);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
