package tv.subscriptions.subscriptionstv;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.api.client.util.Joiner;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import butterknife.BindInt;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tv.subscriptions.services.PlayedVideosService;
import tv.subscriptions.services.UnplayedVideosService;

import static tv.subscriptions.subscriptionstv.MainActivity.LOG_TAG;

class HandleSubs extends AsyncTask<String, Void, List<Video>> {

    private MainActivity mMainActivity;
    private SwipeRefreshLayout swipeContainer;

    public HandleSubs(MainActivity mMainActivity, SwipeRefreshLayout swipeContainer ) {
        this.mMainActivity=mMainActivity;
        this.swipeContainer=swipeContainer;
    }

    private String processPageSubscriptionsAndChannelIds(JSONObject userInfo, ArrayList<String> listSubscribesIds, Map<String, Integer> listSubscribesIdsMap){
        //Get the subs channelsIDs
        String nextPageToken = userInfo.optString("nextPageToken", null);

        //we keep the subscriptionsID list
        JSONArray listSubscribes = (JSONArray) userInfo.optJSONArray("items");
        for (int j = 0; j < listSubscribes.length(); j++){
            String channelId = listSubscribes.optJSONObject(j).optJSONObject("snippet").optJSONObject("resourceId").optString("channelId");
            //int newItemCount = listSubscribes.optJSONObject(j).optJSONObject("contentDetails").optInt("newItemCount");
            if (channelId.isEmpty()){
                channelId= "EMPTY CHANNEL ID";
                Log.w(LOG_TAG, "EMPTY CHANNEL ID !");
            }
            listSubscribesIds.add(channelId);
            //listSubscribesIdsMap.put(channelId, newItemCount);
        }
        return nextPageToken;
    }

    private JSONObject requestAPI(String urlRequest, String token){
        OkHttpClient client = new OkHttpClient();
        //We request the next page
        Request request = new Request.Builder()
                .url(urlRequest)
                .addHeader("Authorization", String.format("Bearer %s", token))
                .build();
        try {
            Response response = client.newCall(request).execute();
            String jsonBody = response.body().string();
            JSONObject userInfo = new JSONObject(jsonBody);
            return userInfo;
        } catch (IOException e) {
            e.printStackTrace();
            mMainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Snackbar.make(mMainActivity.nvDrawer, "Verify your internet connection", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void requestSubList(ArrayList<String> listSubscribesIds, Map<String, Integer> listSubscribesIdsMapNewActivityCount, String... tokens){

        String urlRequest = "https://www.googleapis.com/youtube/v3/subscriptions?part=contentDetails%2Csnippet&maxResults="+mMainActivity.getMaxResultsPerPageYTAPI()+"&mine=true&order=unread&fields=etag%2Citems(contentDetails%2FnewItemCount%2Cid%2Csnippet(channelId%2CresourceId%2FchannelId))%2CnextPageToken%2CpageInfo&key="+mMainActivity.getApiKey();
        JSONObject userInfo = this.requestAPI(urlRequest, tokens[0]);

        if (userInfo != null) {

            Double numberOfpages = Math.ceil(userInfo.optJSONObject("pageInfo").optDouble("totalResults")/mMainActivity.getMaxResultsPerPageYTAPI());
            String nextPageToken = this.processPageSubscriptionsAndChannelIds(userInfo, listSubscribesIds, listSubscribesIdsMapNewActivityCount);
            Log.i(LOG_TAG, "Number of subs : "+userInfo.optJSONObject("pageInfo").optDouble("totalResults"));

            Resources res = mMainActivity.getResources();
            if(res.getBoolean(R.bool.debug))
                numberOfpages = (double) res.getInteger(R.integer.numberOfPagesFetchedInDebugMode);

            for (int i = 1; i < numberOfpages; i++) {

                urlRequest = "https://www.googleapis.com/youtube/v3/subscriptions?part=contentDetails%2Csnippet&maxResults="+mMainActivity.getMaxResultsPerPageYTAPI()+"&mine=true&order=unread&pageToken="+nextPageToken+"&fields=etag%2Citems(contentDetails%2FnewItemCount%2Cid%2Csnippet(channelId%2CresourceId%2FchannelId))%2CnextPageToken%2CpageInfo"+"&key="+mMainActivity.getApiKey();
                userInfo = requestAPI(urlRequest, tokens[0]);
                nextPageToken = this.processPageSubscriptionsAndChannelIds(userInfo, listSubscribesIds, listSubscribesIdsMapNewActivityCount);

                final int progressStatus = (int) (((100*(i+1))/numberOfpages)-10);
                /*mMainActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        mMainActivity.mProgress.setProgress(progressStatus);
                    }
                });*/
                Log.i(LOG_TAG, "page : "+ (i+1) + "/"+numberOfpages+"; nextPageToken : "+nextPageToken);
                //Log.i(LOG_TAG, "taille de la liste recu : "+listSubscribes.length());
                //Log.i(LOG_TAG, "taille de la liste : "+listSubscribesIds.size());
                //Youtube do not return always the exact numbers of subs asked. Don't know why...
            }
        }
    }

    @Override
    protected List<Video> doInBackground(String... tokens) {

        ArrayList<String> listSubscribesIds = new ArrayList<String>();
        Map<String, Integer> listSubscribesIdsMapNewActivityCount = new HashMap<String, Integer>();
        this.requestSubList(listSubscribesIds, listSubscribesIdsMapNewActivityCount, tokens);

        try {
            //Parse the sub rss feed
            final HandleXML handleXML = new HandleXML(listSubscribesIdsMapNewActivityCount);
            ExecutorService threadPool = Executors.newFixedThreadPool(mMainActivity.getMaxResultsPerPageYTAPI());
            Resources res = mMainActivity.getResources();
            String urlChannelRssFeed = res.getString(R.string.url_channel_rss_feed);
            for (String channelId : listSubscribesIds) {
                //if (listSubscribesIdsMapNewActivityCount.get(channelId)>0){
                    threadPool.submit(handleXML.fetchXML(urlChannelRssFeed + channelId, false));
                //}else{
                    //threadPool.submit(handleXML.fetchXML("https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId, true));
                //}
            }
            threadPool.shutdown();
            threadPool.awaitTermination(5, TimeUnit.MINUTES);

            return handleXML.getListVideos();
        } catch (InterruptedException e) {
            e.printStackTrace();
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

        PlayedVideosService playedVideosService = new PlayedVideosService(mMainActivity);
        List<Video> listPlayedVideos = playedVideosService.getAll();
        listVideos.removeAll(listPlayedVideos);

        //Display the list
        //mMainActivity.mProgress.setProgress(100);
        //mMainActivity.mProgress.setVisibility(View.GONE);
        //mMainActivity.fab.setVisibility(View.VISIBLE);
        mMainActivity.getAdapterVideoPage().getListVideos().addAll(listVideos);

        UnplayedVideosService unplayedVideosService = new UnplayedVideosService(mMainActivity);
        unplayedVideosService.saveUnplayedVideos(listVideos);

        //reload the ui
        mMainActivity.videoPageFragment.loadNextDataFromApi(0);
        mMainActivity.getAdapterVideoPage().notifyDataSetChanged();

        /*mMainActivity.runOnUiThread(new Runnable() {
            public void run() {
                swipeContainer.setRefreshing(false);
            }
        });
*/
        //manage the intent button
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
        ArrayList<String> listAllIdsOfVideos = new ArrayList<String>();
        ArrayList<Video> listVideosInAdapter = mMainActivity.getAdapterVideoPage().getListVideosDisplayed();
        for (Video v: listVideosInAdapter)
            listAllIdsOfVideos.add(v.getIdYT());

        List<Video> list50FirstTitlesOfVideos = new ArrayList<Video>();
        Resources res = mMainActivity.getResources();
        int maxResultsPerPageYTAPI = res.getInteger(R.integer.maxResultsPerPageYTAPI);

        if (listVideosInAdapter == null || listVideosInAdapter.size()==0) {
            mMainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Snackbar.make(mMainActivity.nvDrawer, mMainActivity.getString(R.string.err_empty_playlist), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
            return;
        }

        if (listVideosInAdapter.size()<maxResultsPerPageYTAPI)
            list50FirstTitlesOfVideos = new ArrayList<Video>(listVideosInAdapter.subList(0,listVideosInAdapter.size()));
        else
            list50FirstTitlesOfVideos = new ArrayList<Video>(listVideosInAdapter.subList(0,maxResultsPerPageYTAPI));

        // Merge video IDs
        Joiner stringJoiner = Joiner.on(',');
        String url = stringJoiner.join(list50FirstTitlesOfVideos );
        mMainActivity.setPlaylist(list50FirstTitlesOfVideos);

        //the youtube player api doesn't work when we try to open multiple videos
        //Intent intent = YouTubeIntents.createPlayVideoIntent(mMainActivity, url);//"a4NT5iBFuZs");
        //mMainActivity.startActivity(intent);

        //so we use the browser to generate a anonymous playlist and play it (youtube can create a 50 videos playlist max)
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(mMainActivity.getString(R.string.url_intent)+ url));// +",a4NT5iBFuZs"
        Log.i(LOG_TAG, "URL Intent "+ intent.getDataString());
        mMainActivity.startActivity(intent);

    }
}