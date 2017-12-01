package tv.subscriptions.subscriptionstv;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static tv.subscriptions.subscriptionstv.MainActivity.LOG_TAG;

public class VideoWatchedPageFragment extends Fragment {

    private RecyclerListAdapter adapter;
    private SwipeRefreshLayout swipeContainer;
    private MainActivity mMainActivity;

    public SwipeRefreshLayout getSwipeContainer() {
        return swipeContainer;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.mMainActivity = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.videos_watched_page, container, false);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_videos_watched);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        this.adapter = new RecyclerListAdapter(mMainActivity.getBaseContext(), true);
        this.mMainActivity.setAdapterVideoWatchedPage(this.adapter);
        recyclerView.setAdapter(this.adapter);
        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                fetchTimelineAsync(0);
            }
        });

        ItemTouchHelper mIth = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.ANIMATION_TYPE_SWIPE_CANCEL, ItemTouchHelper.LEFT) {

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        Log.i(LOG_TAG, "moving items is normaly disabled !!! ");
                        return true;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        String directionString = (direction==ItemTouchHelper.LEFT)?"left":"right";
                        Video videoToRemove = adapter.getListVideos().get(viewHolder.getAdapterPosition());
                        String idRemovedVideo = videoToRemove.getIdYT();
                        Log.i(LOG_TAG, "removed video untitled : "+idRemovedVideo);
                        adapter.getListVideos().remove(viewHolder.getAdapterPosition());
                        adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                        //TODO Insert the video at it's right place (maybe use a sortedList ?)
                        if (mMainActivity.getAdapterVideoPage()!=null) {
                            mMainActivity.getAdapterVideoPage().getListVideosDisplayed().add(0, videoToRemove);
                            mMainActivity.getAdapterVideoPage().notifyDataSetChanged();
                        }

                        YoutubeSubscriptionsTVOpenDatabaseHelper youtubeSubscriptionsTVOpenDatabaseHelper = OpenHelperManager.getHelper(mMainActivity, YoutubeSubscriptionsTVOpenDatabaseHelper.class);
                        try {
                            Dao<Video, Long> youtubeSubscriptionsTVDao = youtubeSubscriptionsTVOpenDatabaseHelper.getDao();
                            youtubeSubscriptionsTVDao.delete(videoToRemove);
                        } catch (java.sql.SQLException e) {
                            e.printStackTrace();
                            return;
                        }

                    }
                });
        mIth.attachToRecyclerView(recyclerView);
        this.loadVideos();
        return view;
    }

    public void loadVideos() {
        mMainActivity.runOnUiThread(new Runnable() {
            public void run() {
                swipeContainer.setRefreshing(true);
            }
        });

        //We fetch the videos already played
        List<Video> listPlayedVideos = new ArrayList<Video>();
        YoutubeSubscriptionsTVOpenDatabaseHelper youtubeSubscriptionsTVOpenDatabaseHelper = OpenHelperManager.getHelper(mMainActivity, YoutubeSubscriptionsTVOpenDatabaseHelper.class);
        try {
            Dao<Video, Long> youtubeSubscriptionsTVDao = youtubeSubscriptionsTVOpenDatabaseHelper.getDao();
            listPlayedVideos = youtubeSubscriptionsTVDao.queryForAll();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return;
        }

        //We sort the list
        Collections.sort(listPlayedVideos, new Comparator<Video>(){
            public int compare(Video video1, Video video2) {
                return video2.getDatePublished().compareTo(video1.getDatePublished());
            }
        });

        this.adapter.getListVideos().addAll(listPlayedVideos);
        this.adapter.notifyDataSetChanged();
        mMainActivity.runOnUiThread(new Runnable() {
            public void run() {
                swipeContainer.setRefreshing(false);
            }
        });
    }

    public void fetchTimelineAsync(int page) {
        this.adapter.clear();
        this.adapter.notifyDataSetChanged();
        this.loadVideos();
    }

}
