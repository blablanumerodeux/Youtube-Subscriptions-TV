package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.util.ArrayList;

/*
help taken from here :

https://codelabs.developers.google.com/codelabs/appauth-android-codelab/
https://github.com/openid/AppAuth-Android
https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.634ehe1z6
http://androidessence.com/swipe-to-dismiss-recyclerview-items/
https://developers.google.com/identity/protocols/OAuth2

 */
public class ScrollingActivity extends AppCompatActivity {

    AppCompatButton mAuthorize;
    AppCompatButton mMakeApiCall;
    AppCompatButton mSignOut;
    AppCompatButton mLaunchPlaylist;
    ProgressBar mProgress;
    private static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String USED_INTENT = "USED_INTENT";
    public static final String LOG_TAG = "Youtube Subs TV";
    private RecyclerListAdapter adapter;
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
        setContentView(R.layout.activity_scrolling);
        this.authorizationService = new AuthorizationService(this);
        mAuthorize = (AppCompatButton) findViewById(R.id.authorize);
        mAuthorize.setOnClickListener(new AuthorizeListener(authorizationService));
        mSignOut = (AppCompatButton) findViewById(R.id.signOut);
        mLaunchPlaylist = (AppCompatButton) findViewById(R.id.launch_playlist);
        mProgress = (ProgressBar) findViewById(R.id.progress_bar);
        apiKey = getString(R.string.api_key);

        Resources res = getResources();
        maxResultsPerPageYTAPI = res.getInteger(R.integer.maxResultsPerPageYTAPI);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(ScrollingActivity.this));

        adapter = new RecyclerListAdapter();
        recyclerView.setAdapter(adapter);

        ItemTouchHelper mIth = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(ItemTouchHelper.ANIMATION_TYPE_SWIPE_CANCEL, ItemTouchHelper.RIGHT) {

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    // move item in `fromPos` to `toPos` in adapter.
                    //final int fromPos = viewHolder.getAdapterPosition();
                    //final int toPos = target.getAdapterPosition();
                    Log.i(LOG_TAG, "moving items is normaly disabled !!! ");
                    return true;// true if moved, false otherwise
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    String directionString = (direction==ItemTouchHelper.LEFT)?"left":"right";
                    String idRemovedVideo = adapter.getListVideos().get(viewHolder.getAdapterPosition()).getIdYT();
                    Log.i(LOG_TAG, "removed video untitled : "+idRemovedVideo);
                    adapter.getListVideos().remove(viewHolder.getAdapterPosition());
                    adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                    mydatabase.execSQL("INSERT INTO T_VIDEO_PLAYED VALUES('"+idRemovedVideo+"');");
                }
            });
        mIth.attachToRecyclerView(recyclerView);

        this.mydatabase = openOrCreateDatabase("Youtube Subscriptions TV Database",MODE_PRIVATE,null);
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS T_VIDEO_PLAYED(VideoId VARCHAR);");
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
        this.clearAuthState();
        this.authorizationService.dispose();
        Log.i(LOG_TAG, "- ON DESTROY -");
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public RecyclerListAdapter getAdapter() {
        return adapter;
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

    /**
     * Kicks off the authorization flow.
     */
    public static class AuthorizeListener implements Button.OnClickListener {

        private AuthorizationService authorizationService;

        public AuthorizeListener(AuthorizationService authorizationService) {
            this.authorizationService = authorizationService;
        }

        @Override
        public void onClick(View view) {
            AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                    Uri.parse("https://accounts.google.com/o/oauth2/auth") /* auth endpoint */,
                    Uri.parse("https://accounts.google.com/o/oauth2/token") /* token endpoint */
            );

            String clientId = "99998132999-okeq5mdnjj3uv6q4kape9emq0eeg92ge.apps.googleusercontent.com";
            Uri redirectUri = Uri.parse("tv.subscriptions.youtube.youtubesubscriptionstv:/oauth2redirect");
            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    AuthorizationRequest.RESPONSE_TYPE_CODE,
                    redirectUri
            );
            builder.setScope("https://www.googleapis.com/auth/youtube.readonly");
            AuthorizationRequest request = builder.build();

            String action = "tv.subscriptions.youtube.youtubesubscriptionstv.HANDLE_AUTHORIZATION_RESPONSE";
            Intent postAuthorizationIntent = new Intent(action);
            PendingIntent pendingIntent = PendingIntent.getActivity(view.getContext(), request.hashCode(), postAuthorizationIntent, 0);
            this.authorizationService.performAuthorizationRequest(request, pendingIntent);
        }
    }

    public static class SignOutListener implements Button.OnClickListener {
        private final ScrollingActivity mMainActivity;
        public SignOutListener(@NonNull ScrollingActivity mainActivity) {
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
                case "tv.subscriptions.youtube.youtubesubscriptionstv.HANDLE_AUTHORIZATION_RESPONSE":
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

            //we load the videos
            final HandleSubs handleSubs = new HandleSubs(this);
            mAuthState.performActionWithFreshTokens(this.authorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                    handleSubs.execute(accessToken);
                }
            });

            //Change the button
            mSignOut.setVisibility(View.VISIBLE);
            mLaunchPlaylist.setVisibility(View.GONE);
            mSignOut.setOnClickListener(new SignOutListener(this));
            mAuthorize.setVisibility(View.GONE);
        } else {
            mSignOut.setVisibility(View.GONE);
            mLaunchPlaylist.setVisibility(View.GONE);
            mAuthorize.setVisibility(View.VISIBLE);
        }
    }

}
