package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeIntents;
import com.google.api.client.util.Joiner;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.provider.MediaStore.Video.Thumbnails.VIDEO_ID;

public class ScrollingActivity extends AppCompatActivity {

    AppCompatButton mAuthorize;
    AppCompatButton mMakeApiCall;
    AppCompatButton mSignOut;
    AppCompatButton mLaunchPlaylist;
    private static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String USED_INTENT = "USED_INTENT";
    public static final String LOG_TAG = "Youtube Subs TV";
    private ArrayAdapter<String> adapter ;
    // state
    AuthState mAuthState;

    private String fullUrl;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        mAuthorize = (AppCompatButton) findViewById(R.id.authorize);
        mAuthorize.setOnClickListener(new AuthorizeListener());
        mMakeApiCall = (AppCompatButton) findViewById(R.id.makeApiCall);
        mSignOut = (AppCompatButton) findViewById(R.id.signOut);
        mLaunchPlaylist = (AppCompatButton) findViewById(R.id.launch_playlist);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        ListView mListView = (ListView) this.findViewById(R.id.list);
        mListView.setAdapter(adapter);
    }



    /**
     * Kicks off the authorization flow.
     */
    public static class AuthorizeListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {

            // code from the step 'Create the Authorization Request',
            // and the step 'Perform the Authorization Request' goes here.
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

            AuthorizationService authorizationService = new AuthorizationService(view.getContext());

            String action = "tv.subscriptions.youtube.youtubesubscriptionstv.HANDLE_AUTHORIZATION_RESPONSE";
            Intent postAuthorizationIntent = new Intent(action);
            PendingIntent pendingIntent = PendingIntent.getActivity(view.getContext(), request.hashCode(), postAuthorizationIntent, 0);
            authorizationService.performAuthorizationRequest(request, pendingIntent);

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

    @Override
    protected void onStart() {
        super.onStart();
        checkIntent(getIntent());
    }

    /**
     * Exchanges the code, for the {@link TokenResponse}.
     *
     * @param intent represents the {@link Intent} from the Custom Tabs or the System Browser.
     */
    private void handleAuthorizationResponse(@NonNull Intent intent) {

        // code from the step 'Handle the Authorization Response' goes here.
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);

        if (response != null) {
            Log.i(LOG_TAG, String.format("Handled Authorization Response %s ", authState.toJsonString()));
            AuthorizationService service = new AuthorizationService(this);
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
                .commit();
        enablePostAuthorizationFlows();
    }



    private void enablePostAuthorizationFlows() {
        mAuthState = restoreAuthState();
        if (mAuthState != null && mAuthState.isAuthorized()) {
            //Change the button
            mMakeApiCall.setVisibility(View.VISIBLE);
            mMakeApiCall.setOnClickListener(new MakeApiCallListener(this, mAuthState, new AuthorizationService(this)));
            mSignOut.setVisibility(View.VISIBLE);
            mLaunchPlaylist.setVisibility(View.VISIBLE);
            //TODO Make signOut working
            //mSignOut.setOnClickListener(new SignOutListener(this));
            mAuthorize.setVisibility(View.GONE);
        } else {
            mMakeApiCall.setVisibility(View.GONE);
            mSignOut.setVisibility(View.GONE);
            mLaunchPlaylist.setVisibility(View.GONE);
            mAuthorize.setVisibility(View.VISIBLE);
        }
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


    public static class MakeApiCallListener implements Button.OnClickListener {

        private final ScrollingActivity mMainActivity;
        private AuthState mAuthState;
        private AuthorizationService mAuthorizationService;

        public MakeApiCallListener(@NonNull ScrollingActivity mainActivity, @NonNull AuthState authState, @NonNull AuthorizationService authorizationService) {
            mMainActivity = mainActivity;
            mAuthState = authState;
            mAuthorizationService = authorizationService;
        }

        @Override
        public void onClick(View view) {

            // code from the section 'Making API Calls' goes here
            mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                    new AsyncTask<String, Void, List<String>>() {
                        @Override
                        protected List<String> doInBackground(String... tokens) {
                            OkHttpClient client = new OkHttpClient();

                            //TODO CHANGE THE API KEY
                            Request request = new Request.Builder()
                                    .url("https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&maxResults=15&mine=true&order=unread&key="+"YOUR API KEY HERE ")
                                    .addHeader("Authorization", String.format("Bearer %s", tokens[0]))
                                    .build();

                            try {
                                Response response = client.newCall(request).execute();
                                String jsonBody = response.body().string();
                                JSONObject userInfo = new JSONObject(jsonBody);
                                //Log.i(LOG_TAG, String.format("Subscriptions %s", jsonBody));
                                String message;
                                if (userInfo.has("error")) {
                                    message = String.format("%s [%s]", mMainActivity.getString(R.string.request_failed), userInfo.optString("error_description", "No description"));
                                } else {
                                    message = mMainActivity.getString(R.string.request_complete);
                                }


                                if (userInfo != null) {

                                    //Get the subs channelsIDs
                                    ArrayList<String> listSubscribesIds = new ArrayList<String>();
                                    try {
                                        //TODO manage multiple pages
                                        String nextPageToken = userInfo.optString("nextPageToken", null);
                                        JSONArray listSubscribes = (JSONArray) userInfo.getJSONArray("items");
                                        for (int i = 0; i < listSubscribes.length(); i++) {
                                            //TODO secure this if null or empty strings...
                                            listSubscribesIds.add(listSubscribes.getJSONObject(i).getJSONObject("snippet").getJSONObject("resourceId").getString("channelId"));
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    Log.i(LOG_TAG, String.format("ChannelIds list of my subscriptions : %s", listSubscribesIds));

                                    //Parse the sub rss feed
                                    final HandleXML handleXML = new HandleXML("");
                                    ExecutorService threadPool = Executors.newFixedThreadPool(10);
                                    for (String channelId : listSubscribesIds) {
                                        threadPool.submit(handleXML.fetchXML("https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId));
                                    }
                                    threadPool.shutdown();
                                    threadPool.awaitTermination(5, TimeUnit.MINUTES);

                                    //we take only the first 15 items
                                    return handleXML.getListVideos().subList(0,15);
                                }

                            } catch (Exception exception) {
                                Log.w(LOG_TAG, exception);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(List<String> listVideos) {

                            //TODO here we have to wait for all the threads to finish (use threadpool)

                            // Merge video IDs
                            Joiner stringJoiner = Joiner.on(',');
                            String url = stringJoiner.join(listVideos);
                            mMainActivity.fullUrl = "http://www.youtube.com/watch_videos?video_ids=" + url;
                            Log.i(LOG_TAG, "Ultime list of videos : " + mMainActivity.fullUrl);

                            //Display the list
                            ListView mListView = (ListView) mMainActivity.findViewById(R.id.list);
                            //obj.getListVideos().add(0,"<a>http://www.youtube.com/watch_videos?video_ids=" + url.toString()+"</a>");
                            TextView txtview = (TextView) mMainActivity.findViewById(R.id.txtview);
                            txtview.setText(mMainActivity.fullUrl);
                            mMainActivity.adapter.addAll(listVideos);
                            mMainActivity.adapter.notifyDataSetChanged();

                            //manage the intent button
                            mMainActivity.mLaunchPlaylist.setOnClickListener(new CallIntentListener(mMainActivity, mMainActivity.fullUrl));
                            /*mMainActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    ListView mListView = (ListView) mMainActivity.findViewById(R.id.list);
                                    mListView.getAdapter().notifyDataSetChanged();
                                }
                            });*/
                            //Log.i(LOG_TAG, "onPostExecute "+ Looper.myLooper().getThread().getName());
                        }
                    }.execute(accessToken);
                }
            });
        }
    }



    public static class CallIntentListener implements Button.OnClickListener {

        private final ScrollingActivity mMainActivity;
        private final String fullUrl;

        public CallIntentListener(@NonNull ScrollingActivity mainActivity,@NonNull String fullUrl) {
            this.mMainActivity = mainActivity;
            this.fullUrl = fullUrl;
        }

        @Override
        public void onClick(View view) {

            //the youtube player api doesn't work when we try to open multiple videos
            //Intent intent = YouTubeIntents.createPlayVideoIntent(mMainActivity, url);//"a4NT5iBFuZs");
            //mMainActivity.startActivity(intent);

            //so we use the browser to generate a anonymous playlist and playit
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fullUrl));
            mMainActivity.startActivity(intent);

        }
    }
}
