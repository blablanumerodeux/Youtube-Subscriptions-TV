package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class ScrollingActivity extends AppCompatActivity {

    AppCompatButton mAuthorize;
    AppCompatButton mMakeApiCall;
    AppCompatButton mSignOut;
    private static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String USED_INTENT = "USED_INTENT";
    public static final String LOG_TAG = "AppAuthSample";

    // state
    AuthState mAuthState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mAuthorize = (AppCompatButton) findViewById(R.id.authorize);
        mAuthorize.setOnClickListener(new AuthorizeListener());
        mMakeApiCall = (AppCompatButton) findViewById(R.id.makeApiCall);
        mSignOut = (AppCompatButton) findViewById(R.id.signOut);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();


            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            if (mMakeApiCall.getVisibility() == View.GONE) {
                mMakeApiCall.setVisibility(View.VISIBLE);
                mMakeApiCall.setOnClickListener(new MakeApiCallListener(this, mAuthState, new AuthorizationService(this)));
            }
            /*if (mSignOut.getVisibility() == View.GONE) {
                mSignOut.setVisibility(View.VISIBLE);
                //TODO Make signOut working
                //mSignOut.setOnClickListener(new SignOutListener(this));
            }*/
        } else {
            mMakeApiCall.setVisibility(View.GONE);
            mSignOut.setVisibility(View.GONE);
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
                    new AsyncTask<String, Void, JSONObject>() {
                        @Override
                        protected JSONObject doInBackground(String... tokens) {
                            OkHttpClient client = new OkHttpClient();
                            //Request request = new Request.Builder()
                            //        .url("https://www.googleapis.com/oauth2/v3/userinfo")
                            //        .addHeader("Authorization", String.format("Bearer %s", tokens[0]))
                            //        .build();

                            //TODO CHANGE THE API KEY
                            Request request = new Request.Builder()
                                    .url("https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&maxResults=50&mine=true&order=unread&key="+"YOUR API")
                                    .addHeader("Authorization", String.format("Bearer %s", tokens[0]))
                                    .build();

                            try {
                                Response response = client.newCall(request).execute();
                                String jsonBody = response.body().string();
                                Log.i(LOG_TAG, String.format("User Info Response %s", jsonBody));
                                return new JSONObject(jsonBody);
                            } catch (Exception exception) {
                                Log.w(LOG_TAG, exception);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(JSONObject userInfo) {
                            if (userInfo != null) {
                                ArrayList<String> listSubscribesIds = new ArrayList<String>();
                                try {
                                    String fullName = userInfo.optString("nextPageToken", null);
                                    JSONArray listSubscribes = (JSONArray) userInfo.getJSONArray("items");
                                    for (int i = 0; i<listSubscribes.length(); i++) {
                                        listSubscribesIds.add(listSubscribes.getJSONObject(i).getJSONObject("snippet").getJSONObject("resourceId").getString("channelId"));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Log.i(LOG_TAG, String.format("ChannelIds list of my subscriptions : %s", listSubscribesIds));

                                HandleXML obj = new HandleXML("");
                                //https://www.youtube.com/feeds/videos.xml?channel_id=UCNu6L-xcmBsN3vNFOxYvB2Q
                                for (String channelId : listSubscribesIds) {
                                    obj.setUrlString("https://www.youtube.com/feeds/videos.xml?channel_id="+channelId);
                                    obj.fetchXML();
                                    //while(obj.parsingComplete);
                                }

                                StringBuilder url = new StringBuilder();
                                for (String s : obj.listAllVideosInMySubs) {
                                    url.append(",");
                                    url.append(s);
                                }

                                Log.i("APP", "La liste ultime avec toutes les videos : http://www.youtube.com/watch_videos?video_ids=" + url.toString());
                                /*if (!TextUtils.isEmpty(imageUrl)) {
                                    Picasso.with(mMainActivity)
                                            .load(imageUrl)
                                            .placeholder(R.drawable.ic_account_circle_black_48dp)
                                            .into(mMainActivity.mProfileView);
                                }*/
                                /*if (!TextUtils.isEmpty(fullName)) {
                                    mMainActivity.mFullName.setText(fullName);
                                }
                                if (!TextUtils.isEmpty(givenName)) {
                                    mMainActivity.mGivenName.setText(givenName);
                                }
                                if (!TextUtils.isEmpty(familyName)) {
                                    mMainActivity.mFamilyName.setText(familyName);
                                }*/

                                String message;
                                if (userInfo.has("error")) {
                                    message = String.format("%s [%s]", mMainActivity.getString(R.string.request_failed), userInfo.optString("error_description", "No description"));
                                } else {
                                    message = mMainActivity.getString(R.string.request_complete);
                                }
                                //Snackbar.make(mMainActivity.mProfileView, message, Snackbar.LENGTH_SHORT)
                                //        .show();
                            }
                        }
                    }.execute(accessToken);
                }
            });

            //https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&mine=true&key=
        }
    }



}
