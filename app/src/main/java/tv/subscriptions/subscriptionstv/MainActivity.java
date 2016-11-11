package tv.subscriptions.subscriptionstv;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.util.List;
import java.util.Vector;

/*
help taken from here :

https://codelabs.developers.google.com/codelabs/appauth-android-codelab/
https://github.com/openid/AppAuth-Android
https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.634ehe1z6
http://androidessence.com/swipe-to-dismiss-recyclerview-items/
https://developers.google.com/identity/protocols/OAuth2
https://developers.google.com/youtube/v3/guides/auth/installed-apps?hl=fr#Obtaining_Access_Tokens
https://developers.google.com/youtube/android/player/reference/com/google/android/youtube/player/YouTubeIntents#createOpenPlaylistIntent%28android.content.Context,%20java.lang.String%29
https://developer.android.com/training/material/lists-cards.html#RecyclerView
http://www.tutos-android.com/fragment-slider-page-lautre
https://developer.android.com/training/basics/data-storage/shared-preferences.html
https://material.google.com/components/lists.html
http://stackoverflow.com/questions/5273436/how-to-get-activitys-content-view
http://www.materialdoc.com/linear-progress/

 */
public class MainActivity extends AppCompatActivity {

    AppCompatButton mAuthorize;
    AppCompatButton mMakeApiCall;
    AppCompatButton mSignOut;
    AppCompatButton mLaunchPlaylist;
    ProgressBar mProgress;
    ViewPager mViewPager;
    TabLayout mTabLayout;
    Menu mMenu;
    FloatingActionButton fab;
    private static final String SHARED_PREFERENCES_NAME = "Youtube Subs TV";
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String USED_INTENT = "USED_INTENT";
    public static final String LOG_TAG = "Youtube Subs TV";
    private RecyclerListAdapter adapter;
    private MyPagerAdapter mPagerAdapter;
    private String fullUrl;
    private String apiKey;
    private int maxResultsPerPageYTAPI;
    private SQLiteDatabase mydatabase;
    private AuthorizationService authorizationService;
    // state
    AuthState mAuthState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "+ ON CREATE +");

        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        this.authorizationService = new AuthorizationService(this);
        this.mAuthorize = (AppCompatButton) findViewById(R.id.authorize);
        //this.mAuthorize.setOnClickListener(new AuthorizeListener(authorizationService, this));
        this.mSignOut = (AppCompatButton) findViewById(R.id.signOut);
        //this.mLaunchPlaylist = (AppCompatButton) findViewById(R.id.launch_playlist);
        this.mProgress = (ProgressBar) findViewById(R.id.progress_bar);
        this.mViewPager = (ViewPager) findViewById(R.id.view_pager);
        this.mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        this.mTabLayout.setupWithViewPager(mViewPager);

        List<Fragment> fragments = new Vector();
        fragments.add(Fragment.instantiate(this,VideoPageFragment.class.getName()));
        fragments.add(Fragment.instantiate(this,VideoWatchedPageFragment.class.getName()));
        this.mPagerAdapter = new MyPagerAdapter(super.getSupportFragmentManager(), fragments);
        this.mViewPager.setAdapter(this.mPagerAdapter);
        this.mTabLayout.getTabAt(0).setText(R.string.tab_videos);
        this.mTabLayout.getTabAt(1).setText(R.string.tab_videos_watched);
        //this.mTabLayout.getTabAt(1).select();

        this.apiKey = getString(R.string.api_key);

        Resources res = getResources();
        this.maxResultsPerPageYTAPI = res.getInteger(R.integer.maxResultsPerPageYTAPI);

        this.mydatabase = openOrCreateDatabase("Youtube Subscriptions TV Database",MODE_PRIVATE,null);
        //this.mydatabase.execSQL("DROP TABLE T_VIDEO_PLAYED");
        this.mydatabase.execSQL("CREATE TABLE IF NOT EXISTS T_VIDEO_PLAYED(VideoId VARCHAR, Title VARCHAR, ThumbnailsUrl VARCHAR, ChannelTitle VARCHAR);");

        this.fab = (FloatingActionButton) findViewById(R.id.launch_playlist);
        this.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "You need to sign in to launch the playlist", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //we restore the token that has been saved on the SharedPreferences
        this.enablePostAuthorizationFlows();
        this.loadVideos();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        this.mMenu = menu;

        //we restore the token that has been saved on the SharedPreferences
        this.enablePostAuthorizationFlows();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.authorize:
                AuthorizeListener authListener = new AuthorizeListener(authorizationService, this);
                authListener.onClick(this.findViewById(android.R.id.content).getRootView());
                return true;
            case R.id.signOut:
                SignOutListener signOutListener = new SignOutListener(this);
                signOutListener.onClick(findViewById(R.id.signOut));
                return true;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "++ ON START ++");
        checkIntent(getIntent());
    }

    @Override
    public void onResume() {
        super.onResume();

        //we restore the token that has been saved on the SharedPreferences
        this.enablePostAuthorizationFlows();

        Log.i(LOG_TAG, "+ ON RESUME +");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.authorizationService.dispose();
        if (mydatabase.isOpen())
            mydatabase.close();
        Log.i(LOG_TAG, "- ON DESTROY -");
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getMaxResultsPerPageYTAPI() {
        return maxResultsPerPageYTAPI;
    }

    public SQLiteDatabase getMydatabase() {
        return mydatabase;
    }

    public RecyclerListAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(RecyclerListAdapter adapter) {
        this.adapter = adapter;
    }

    public MyPagerAdapter getmPagerAdapter() {
        return mPagerAdapter;
    }

    public void setmPagerAdapter(MyPagerAdapter mPagerAdapter) {
        this.mPagerAdapter = mPagerAdapter;
    }

    /**
     * Kicks off the authorization flow.
     */
    public static class AuthorizeListener implements Button.OnClickListener {

        private AuthorizationService authorizationService;
        private final MainActivity mMainActivity;

        public AuthorizeListener(AuthorizationService authorizationService, MainActivity mMainActivity) {
            this.authorizationService = authorizationService;
            this.mMainActivity = mMainActivity;
        }

        @Override
        public void onClick(View view) {
            AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                    Uri.parse("https://accounts.google.com/o/oauth2/auth") /* auth endpoint */,
                    Uri.parse("https://accounts.google.com/o/oauth2/token") /* token endpoint */
            );

            String clientId = mMainActivity.getString(R.string.clientId);
            Uri redirectUri = Uri.parse("tv.subscriptions.subscriptionstv:/oauth2redirect");
            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    AuthorizationRequest.RESPONSE_TYPE_CODE,
                    redirectUri
            );
            builder.setScope("https://www.googleapis.com/auth/youtube.readonly");
            AuthorizationRequest request = builder.build();

            String action = "tv.subscriptions.subscriptionstv.HANDLE_AUTHORIZATION_RESPONSE";
            Intent postAuthorizationIntent = new Intent(action);
            PendingIntent pendingIntent = PendingIntent.getActivity(view.getContext(), request.hashCode(), postAuthorizationIntent, 0);
            this.authorizationService.performAuthorizationRequest(request, pendingIntent);
        }
    }

    public static class SignOutListener implements Button.OnClickListener {
        private final MainActivity mMainActivity;
        public SignOutListener(@NonNull MainActivity mainActivity) {
            mMainActivity = mainActivity;
        }
        @Override
        public void onClick(View view) {
            mMainActivity.mAuthState = null;
            mMainActivity.clearAuthState();
            mMainActivity.enablePostAuthorizationFlows();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkIntent(intent);
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case "tv.subscriptions.subscriptionstv.HANDLE_AUTHORIZATION_RESPONSE":
                    if (!intent.hasExtra(USED_INTENT)) {
                        handleAuthorizationResponse(intent);
                        intent.putExtra(USED_INTENT, true);
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }

    /**
     *
     * Exchanges the code, for the {@link TokenResponse}.
     *
     * @param intent represents the {@link Intent} from the Custom Tabs or the System Browser.
     */
    private void handleAuthorizationResponse(@NonNull Intent intent) {

        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);

        if (response != null) {
            Log.i(LOG_TAG, String.format("Handled Authorization Response %s ", authState.toJsonString()));
            AuthorizationService service = this.authorizationService;
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {

                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                    if (exception != null) {
                        Log.w(LOG_TAG, "Token Exchange failed", exception);
                    } else {
                        if (tokenResponse != null) {
                            authState.update(tokenResponse, exception);
                            persistAuthState(authState);
                            loadVideos();
                            enablePostAuthorizationFlows();
                            Log.i(LOG_TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                        }
                    }
                }
            });
        }
    }

    private void persistAuthState(@NonNull AuthState authState) {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(AUTH_STATE, authState.toJsonString())
                .apply();
        enablePostAuthorizationFlows();
    }

    @Nullable
    private AuthState restoreAuthState() {
        String jsonString = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(AUTH_STATE, null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.fromJson(jsonString);
            } catch (JSONException jsonException) {
                // should never happen
            }
        }
        return null;
    }

    private void clearAuthState() {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(AUTH_STATE)
                .apply();
    }


    private void loadVideos() {
        if (mAuthState != null && mAuthState.isAuthorized()) {
            //we load the videos
            final HandleSubs handleSubs = new HandleSubs(this);
            mAuthState.performActionWithFreshTokens(this.authorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                    handleSubs.execute(accessToken);
                }
            });
        }
    }

    private void enablePostAuthorizationFlows() {
        mAuthState = restoreAuthState();
        if (mAuthState != null && mAuthState.isAuthorized()) {
            //Change the button
            if (this.mMenu != null) {
                ((MenuItem) this.mMenu.findItem(R.id.signOut)).setVisible(true);
                ((MenuItem) this.mMenu.findItem(R.id.authorize)).setVisible(false);
            }
        } else {
            if (this.mMenu != null) {
                ((MenuItem) this.mMenu.findItem(R.id.signOut)).setVisible(false);
                ((MenuItem) this.mMenu.findItem(R.id.authorize)).setVisible(true);
            }
        }
    }

}
