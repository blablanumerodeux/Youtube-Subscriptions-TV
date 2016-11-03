package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.api.client.util.Joiner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static tv.subscriptions.youtube.youtubesubscriptionstv.MainActivity.LOG_TAG;

class HandleSubs extends AsyncTask<String, Void, List<Video>> {

    private MainActivity mMainActivity;

    public HandleSubs(MainActivity mMainActivity) {
        this.mMainActivity=mMainActivity;
    }

    @Override
    protected List<Video> doInBackground(String... tokens) {
        OkHttpClient client = new OkHttpClient();

        mMainActivity.runOnUiThread(new Runnable() {
            public void run() {
                mMainActivity.mProgress.setVisibility(View.VISIBLE);
            }
        });

        Request request = new Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&maxResults="+mMainActivity.getMaxResultsPerPageYTAPI()+"&mine=true&order=alphabetical&fields=etag%2Citems(id%2Csnippet(channelId%2CresourceId%2FchannelId))%2CnextPageToken%2CpageInfo&key="+mMainActivity.getApiKey())
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
                final Double numberOfpages = Math.ceil(userInfo.optJSONObject("pageInfo").optDouble("totalResults")/mMainActivity.getMaxResultsPerPageYTAPI());
                String nextPageToken = userInfo.optString("nextPageToken", null);
                Log.i(LOG_TAG, "Number of subs : "+userInfo.optJSONObject("pageInfo").optDouble("totalResults"));

                //we keep the subscriptionsID list
                JSONArray listSubscribes = (JSONArray) userInfo.optJSONArray("items");
                for (int j = 0; j < listSubscribes.length(); j++){
                    String channelId = listSubscribes.optJSONObject(j).optJSONObject("snippet").optJSONObject("resourceId").optString("channelId");
                    if (channelId.isEmpty()){
                        channelId= "EMPTY CHANNEL ID";
                        Log.w(LOG_TAG, "EMPTY CHANNEL ID !");
                    }
                    listSubscribesIds.add(channelId);
                }
                Log.i(LOG_TAG, "page : 1/"+numberOfpages+"; nextPageToken : "+nextPageToken);
                //Log.i(LOG_TAG, "taille de la liste recu : "+listSubscribes.length());
                //Log.i(LOG_TAG, "taille de la liste : "+listSubscribesIds.size());

                for (int i = 1; i < numberOfpages; i++) {
                    //We request the next page
                    request = new Request.Builder()
                            .url("https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&maxResults="+mMainActivity.getMaxResultsPerPageYTAPI()+"&mine=true&order=alphabetical&pageToken="+nextPageToken+"&fields=etag%2Citems(id%2Csnippet(channelId%2CresourceId%2FchannelId))%2CnextPageToken%2CpageInfo"+"&key="+mMainActivity.getApiKey())
                            .addHeader("Authorization", String.format("Bearer %s", tokens[0]))
                            .build();
                    response = client.newCall(request).execute();
                    jsonBody = response.body().string();
                    userInfo = new JSONObject(jsonBody);

                    nextPageToken = userInfo.optString("nextPageToken", null);
                    listSubscribes = (JSONArray) userInfo.optJSONArray("items");
                    for (int j = 0; j < listSubscribes.length(); j++){
                        //TODO Secure this if opt... return null
                        String channelId = listSubscribes.optJSONObject(j).optJSONObject("snippet").optJSONObject("resourceId").optString("channelId");
                        if (channelId.isEmpty()){
                            channelId= "EMPTY CHANNEL ID";
                            Log.w(LOG_TAG, "EMPTY CHANNEL ID !");
                        }
                        listSubscribesIds.add(channelId);
                    }

                    final int progressStatus = (int) ((100*(i+1))/numberOfpages);
                    mMainActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            mMainActivity.mProgress.setProgress(progressStatus);
                        }
                    });

                    Log.i(LOG_TAG, "page : "+ (i+1) + "/"+numberOfpages+"; nextPageToken : "+nextPageToken);
                    //Log.i(LOG_TAG, "taille de la liste recu : "+listSubscribes.length());
                    //Log.i(LOG_TAG, "taille de la liste : "+listSubscribesIds.size());
                }

                mMainActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        mMainActivity.mProgress.setVisibility(View.GONE);
                    }
                });

                //Parse the sub rss feed
                final HandleXML handleXML = new HandleXML();
                ExecutorService threadPool = Executors.newFixedThreadPool(mMainActivity.getMaxResultsPerPageYTAPI());
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
    protected void onPostExecute(List<Video> listVideos) {

        //We sort the list
        Collections.sort(listVideos, new Comparator<Video>(){
            public int compare(Video video1, Video video2) {
                return video2.getDatePublished().compareTo(video1.getDatePublished());
            }
        });

        //We fetch and remove the videos already played
        if (!mMainActivity.getMydatabase().isOpen())
            return;
        Cursor resultSet = mMainActivity.getMydatabase().rawQuery("Select * from T_VIDEO_PLAYED",null);
        ArrayList<Video> listPlayedVideos = new ArrayList<Video>();
        for(resultSet.moveToFirst(); !resultSet.isAfterLast(); resultSet.moveToNext()) {
            Video v = new Video();
            v.setIdYT(resultSet.getString(0));
            listPlayedVideos.add(v);
        }
        listVideos.removeAll(listPlayedVideos);

        //Display the list
        mMainActivity.fab.setVisibility(View.VISIBLE);
        mMainActivity.getAdapter().getListVideos().addAll(listVideos);
        mMainActivity.getAdapter().notifyDataSetChanged();

        //manage the intent button
        //mMainActivity.mLaunchPlaylist.setOnClickListener(new CallIntentListener(mMainActivity, mMainActivity.getFullUrl()));
        mMainActivity.fab.setOnClickListener(new CallIntentListener(mMainActivity, mMainActivity.getFullUrl()));

        //Log.i(LOG_TAG, "onPostExecute "+ Looper.myLooper().getThread().getName());
    }
}

class CallIntentListener implements Button.OnClickListener {

    private final MainActivity mMainActivity;
    private final String fullUrl;

    public CallIntentListener(@NonNull MainActivity mainActivity, @NonNull String fullUrl) {
        this.mMainActivity = mainActivity;
        this.fullUrl = fullUrl;
    }

    @Override
    public void onClick(View view) {

        //We generate the titles list
        ArrayList<String> listAllTitlesOfVideos = new ArrayList<String>();
        for (Video v: mMainActivity.getAdapter().getListVideos())
            listAllTitlesOfVideos.add(v.getIdYT());

        // Merge video IDs
        Joiner stringJoiner = Joiner.on(',');
        String url = stringJoiner.join(listAllTitlesOfVideos);
        Log.i(LOG_TAG, "Ultime list of videos : " + mMainActivity.getFullUrl());

        //the youtube player api doesn't work when we try to open multiple videos
        //Intent intent = YouTubeIntents.createPlayVideoIntent(mMainActivity, url);//"a4NT5iBFuZs");
        //mMainActivity.startActivity(intent);

        //so we use the browser to generate a anonymous playlist and playit
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.youtube.com/watch_videos?video_ids=" + url));
        mMainActivity.startActivity(intent);

    }
}