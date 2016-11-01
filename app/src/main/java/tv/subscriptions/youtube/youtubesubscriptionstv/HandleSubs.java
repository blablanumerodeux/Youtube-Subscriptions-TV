package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.api.client.util.Joiner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static tv.subscriptions.youtube.youtubesubscriptionstv.ScrollingActivity.LOG_TAG;

public class HandleSubs extends AsyncTask<String, Void, List<String>> {

    private ScrollingActivity mMainActivity;

    public HandleSubs(ScrollingActivity mMainActivity) {
        this.mMainActivity=mMainActivity;
    }

    @Override
    protected List<String> doInBackground(String... tokens) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&maxResults=50&mine=true&order=unread&key="+mMainActivity.getApiKey())
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
                final HandleXML handleXML = new HandleXML();
                ExecutorService threadPool = Executors.newFixedThreadPool(50);
                for (String channelId : listSubscribesIds) {
                    threadPool.submit(handleXML.fetchXML("https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId));
                }
                threadPool.shutdown();
                threadPool.awaitTermination(5, TimeUnit.MINUTES);

                return handleXML.getListVideos();
            }
        } catch (Exception exception) {
            //TODO change that
            Log.w(LOG_TAG, exception);
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<String> listVideos) {

        // Merge video IDs
        Joiner stringJoiner = Joiner.on(',');
        String url = stringJoiner.join(listVideos);
        mMainActivity.setFullUrl("http://www.youtube.com/watch_videos?video_ids=" + url);
        Log.i(LOG_TAG, "Ultime list of videos : " + mMainActivity.getFullUrl());

        //Display the list
        ListView mListView = (ListView) mMainActivity.findViewById(R.id.list);
        mMainActivity.mMakeApiCall.setVisibility(View.GONE);
        mMainActivity.mLaunchPlaylist.setVisibility(View.VISIBLE);
        mMainActivity.getAdapter().addAll(listVideos);
        mMainActivity.getAdapter().notifyDataSetChanged();

        //manage the intent button
        mMainActivity.mLaunchPlaylist.setOnClickListener(new CallIntentListener(mMainActivity, mMainActivity.getFullUrl()));
        /*mMainActivity.runOnUiThread(new Runnable() {
            public void run() {
                ListView mListView = (ListView) mMainActivity.findViewById(R.id.list);
                mListView.getAdapter().notifyDataSetChanged();
            }
        });*/
        //Log.i(LOG_TAG, "onPostExecute "+ Looper.myLooper().getThread().getName());
    }
}

class CallIntentListener implements Button.OnClickListener {

    private final ScrollingActivity mMainActivity;
    private final String fullUrl;

    public CallIntentListener(@NonNull ScrollingActivity mainActivity, @NonNull String fullUrl) {
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