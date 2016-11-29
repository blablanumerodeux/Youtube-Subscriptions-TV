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

import java.util.ArrayList;

import static tv.subscriptions.subscriptionstv.MainActivity.LOG_TAG;

public class VideoWatchedPageFragment extends Fragment {

    private RecyclerListAdapter adapter;
    private SwipeRefreshLayout swipeContainer;
    private MainActivity mMainActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.mMainActivity = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.videos_watched_page, container, false);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_videos_watched);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        this.adapter = new RecyclerListAdapter(mMainActivity.getBaseContext());
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
                        String idRemovedVideo = adapter.getListVideos().get(viewHolder.getAdapterPosition()).getIdYT();
                        Log.i(LOG_TAG, "removed video untitled : "+idRemovedVideo);
                        adapter.getListVideos().remove(viewHolder.getAdapterPosition());
                        adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                        mMainActivity.getMydatabase().execSQL("DELETE FROM T_VIDEO_PLAYED WHERE VideoId='"+idRemovedVideo+"';");
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
        Cursor resultSet = mMainActivity.getMydatabase().rawQuery("Select * from T_VIDEO_PLAYED",null);
        ArrayList<Video> listPlayedVideos = new ArrayList<Video>();
        for(resultSet.moveToFirst(); !resultSet.isAfterLast(); resultSet.moveToNext()) {
            Video v = new Video();
            try {
                v.setIdYT(resultSet.getString(0));
                v.setTitle(resultSet.getString(1));
                v.setThumbnailsUrl(resultSet.getString(2));
                v.setChannelTitle(resultSet.getString(3));
                listPlayedVideos.add(v);
            }catch (Exception e){
                Log.e(LOG_TAG, "An error occured while fetching the data.");
            }
        }
        this.adapter.getListVideos().addAll(listPlayedVideos);
        this.adapter.notifyDataSetChanged();
        mMainActivity.runOnUiThread(new Runnable() {
            public void run() {
                swipeContainer.setRefreshing(false);
            }
        });
    }

    public void fetchTimelineAsync(int page) {
        //adapter.clear();
        this.adapter.clear();
        this.adapter.notifyDataSetChanged();
        this.loadVideos();
    }

}
