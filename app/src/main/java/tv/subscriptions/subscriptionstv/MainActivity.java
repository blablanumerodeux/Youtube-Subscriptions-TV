package tv.subscriptions.subscriptionstv;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.util.List;

import butterknife.BindInt;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import tv.subscriptions.services.PlayedVideosService;

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
https://guides.codepath.com/android/Fragment-Navigation-Drawer#persistent-navigation-drawer
https://github.com/codepath/android_guides/wiki/Creating-and-Using-Fragments
http://stackoverflow.com/questions/14347588/show-hide-fragment-in-android
https://guides.codepath.com/android/Implementing-Pull-to-Refresh-Guide#step-2-setup-swiperefreshlayout
https://horaceheaven.com/android-ormlite-tutorial/
http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_5.html#DAO-Methods
https://guides.codepath.com/android/Endless-Scrolling-with-AdapterViews-and-RecyclerView#resetting-the-endless-scroll-state
http://stackoverflow.com/questions/14678593/the-application-may-be-doing-too-much-work-on-its-main-thread
 */
public class MainActivity extends AppCompatActivity {

    AppCompatButton mAuthorize;
    AppCompatButton mSignOut;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawer;
    @BindView(R.id.nvView) NavigationView nvDrawer;
    ActionBarDrawerToggle drawerToggle;
    Menu mMenu;
    FloatingActionButton fab;
    private static final String SHARED_PREFERENCES_NAME = "Youtube Subs TV";
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String USED_INTENT = "USED_INTENT";
    public static final String LOG_TAG = "Youtube Subs TV";
    private RecyclerListAdapter adapterVideoPage;
    private RecyclerListAdapter adapterVideoWatchedPage;
    private MyPagerAdapter mPagerAdapter;
    private String fullUrl;
    @BindString(R.string.api_key) String apiKey;
    @BindInt(R.integer.maxResultsPerPageYTAPI) int maxResultsPerPageYTAPI;
    //private SQLiteDatabase mydatabase;
    AuthorizationService authorizationService;
    // state
    AuthState mAuthState;
    private boolean askForSetAsWatched;
    private List<Video> playlist;
    VideoPageFragment videoPageFragment;
    VideoWatchedPageFragment videoWatchedPageFragment;
    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "+ ON CREATE +");
        setContentView(R.layout.drawer_layout);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        this.authorizationService = new AuthorizationService(this);
        this.mAuthorize = (AppCompatButton) findViewById(R.id.authorize);
        this.mSignOut = (AppCompatButton) findViewById(R.id.signOut);
        this.askForSetAsWatched = false;
        this.drawerToggle = setupDrawerToggle();
        this.mDrawer.addDrawerListener(drawerToggle);
        this.setupDrawerContent(nvDrawer);

        //we restore the token that has been saved on the SharedPreferences
        this.enablePostAuthorizationFlows();

        videoPageFragment = new VideoPageFragment();
        videoWatchedPageFragment = new VideoWatchedPageFragment();
        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.flContent, videoPageFragment, "A");
        ft.add(R.id.flContent, videoWatchedPageFragment, "B");
        ft.hide(videoWatchedPageFragment);
        ft.commit();
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        // NOTE: Make sure you pass in a valid toolbar reference.  ActionBarDrawToggle() does not require it
        // and will not render the hamburger icon without it.
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open,  R.string.drawer_close);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (!videoWatchedPageFragment.isAdded()) { // if the fragment is already in container
            ft.add(R.id.flContent, videoWatchedPageFragment, "B");
        }
        switch(menuItem.getItemId()) {
            case R.id.nav_first_fragment:
                ft.hide(videoWatchedPageFragment);
                ft.show(videoPageFragment);
                break;
            case R.id.nav_third_fragment:
                ft.hide(videoPageFragment);
                ft.show(videoWatchedPageFragment);
                break;
            default:
                ft.hide(videoWatchedPageFragment);
                ft.show(videoPageFragment);
                break;
        }
        ft.commit();

        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);
        // Set action bar title
        setTitle(menuItem.getTitle());
        // Close the navigation drawer
        mDrawer.closeDrawers();
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
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }else {
            switch (item.getItemId()) {
                case R.id.authorize:
                    AuthorizeListener authListener = new AuthorizeListener(authorizationService, this);
                    authListener.onClick(this.findViewById(android.R.id.content).getRootView());
                    return true;
                case R.id.signOut:
                    SignOutListener signOutListener = new SignOutListener(this);
                    signOutListener.onClick(findViewById(R.id.signOut));
                    return true;
                case R.id.emptyDbButton:
                    adapterVideoPage.getListVideosDisplayed().addAll(adapterVideoWatchedPage.getListVideos());
                    final YoutubeSubscriptionsTVOpenDatabaseHelper youtubeSubscriptionsTVOpenDatabaseHelper = OpenHelperManager.getHelper(this, YoutubeSubscriptionsTVOpenDatabaseHelper.class);
                    youtubeSubscriptionsTVOpenDatabaseHelper.clearTable();
                    adapterVideoWatchedPage.getListVideos().clear();
                    adapterVideoWatchedPage.notifyDataSetChanged();
                    if (adapterVideoPage != null){
                        adapterVideoPage.notifyDataSetChanged();
                    }
                    return true;
                case R.id.watchedAllButton:
                    final MainActivity mainActivity = this;
                    AsyncTask<Void, Void, Boolean> thread = new AsyncTask<Void, Void, Boolean>(){
                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            if (videoPageFragment.isVisible())
                                videoPageFragment.getSwipeContainer().setRefreshing(true);
                            if (videoWatchedPageFragment.isVisible())
                                videoWatchedPageFragment.getSwipeContainer().setRefreshing(true);
                        }

                        @Override
                        protected void onPostExecute(Boolean aBoolean) {
                            super.onPostExecute(aBoolean);
                            adapterVideoPage.notifyDataSetChanged();
                            if (adapterVideoWatchedPage!=null)
                                adapterVideoWatchedPage.notifyDataSetChanged();
                            if (videoPageFragment.isVisible())
                                videoPageFragment.getSwipeContainer().setRefreshing(false);
                            if (videoWatchedPageFragment.isVisible())
                                videoWatchedPageFragment.getSwipeContainer().setRefreshing(false);
                        }

                        @Override
                        protected Boolean doInBackground(Void... voids) {
                            //TODO INJECT THIS IF POSSIBLE
                            PlayedVideosService playedVideosService = new PlayedVideosService(mainActivity);
                            //TODO MIGRATE TO SERVICES WHEN POSSIBLE (LIKE HERE) !
                            playedVideosService.markAllAsWatched(adapterVideoPage.getListVideosDisplayed());
                            playedVideosService.markAllAsWatched(adapterVideoPage.getListVideos());
                            mainActivity.getAdapterVideoWatchedPage().getListVideos().addAll(0, adapterVideoPage.getListVideos());
                            mainActivity.getAdapterVideoWatchedPage().getListVideos().addAll(0, adapterVideoPage.getListVideosDisplayed());

                            adapterVideoPage.getListVideosDisplayed().clear();
                            //Reset endless scroll listener when performing a new search
                            //videoPageFragment.getScrollListener().resetState();
                            return true;
                        }
                    };
                    thread.execute();
                    return true;
                case R.id.action_settings:
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
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

        List<Video> playlist = this.getPlaylist();
        if (askForSetAsWatched && playlist != null && !playlist.isEmpty()){
            // Create an instance of the dialog fragment and show it
            DialogFragment dialog = new SetLastPlaylistAsWatchedDialogFragment();
            dialog.show(getSupportFragmentManager(), "SetLastPlaylistAsWatchedDialogFragment");
        }else{
            askForSetAsWatched = true;
        }

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
        /*if (mydatabase.isOpen())
            mydatabase.close();*/
        Log.i(LOG_TAG, "- ON DESTROY -");
    }

    // `onPostCreate` called when activity start-up is complete after `onStart()`
    // NOTE 1: Make sure to override the method with only a single `Bundle` argument
    // Note 2: Make sure you implement the correct `onPostCreate(Bundle savedInstanceState)` method.
    // There are 2 signatures and only `onPostCreate(Bundle state)` shows the hamburger icon.
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
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

    /*public SQLiteDatabase getMydatabase() {
        return mydatabase;
    }*/

    public RecyclerListAdapter getAdapterVideoPage() {
        return adapterVideoPage;
    }

    public void setAdapterVideoPage(RecyclerListAdapter adapterVideoPage) {
        this.adapterVideoPage = adapterVideoPage;
    }

    public RecyclerListAdapter getAdapterVideoWatchedPage() {
        return adapterVideoWatchedPage;
    }

    public void setAdapterVideoWatchedPage(RecyclerListAdapter adapterVideoWatchedPage) {
        this.adapterVideoWatchedPage = adapterVideoWatchedPage;
    }

    public MyPagerAdapter getmPagerAdapter() {
        return mPagerAdapter;
    }

    public void setmPagerAdapter(MyPagerAdapter mPagerAdapter) {
        this.mPagerAdapter = mPagerAdapter;
    }

    public List<Video> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(List<Video> playlist) {
        this.playlist = playlist;
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
                            //loadVideos();
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
