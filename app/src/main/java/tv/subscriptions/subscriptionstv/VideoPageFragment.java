package tv.subscriptions.subscriptionstv;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;

import static tv.subscriptions.subscriptionstv.MainActivity.LOG_TAG;

public class VideoPageFragment extends Fragment {

    private RecyclerListAdapter adapter;
    private MainActivity mActivity;
    private SwipeRefreshLayout swipeContainer;

    public SwipeRefreshLayout getSwipeContainer() {
        return swipeContainer;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.videos_page, container, false);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_videos);
        this.mActivity = (MainActivity) getActivity();
        this.mActivity.setAdapterVideoPage(new RecyclerListAdapter(mActivity.getBaseContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        this.adapter = this.mActivity.getAdapterVideoPage();
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


        mActivity.fab = (FloatingActionButton) view.findViewById(R.id.launch_playlist);
        mActivity.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "You need to sign in to launch the playlist", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        ItemTouchHelper mIth = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.ANIMATION_TYPE_SWIPE_CANCEL, ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        // move item in `fromPos` to `toPos` in adapter.
                        //final int fromPos = viewHolder.getAdapterPosition();
                        //final int toPos = target.getAdapterPosition();
                        Log.i(LOG_TAG, "moving items is disabled !!! ");
                        return true;// true if moved, false otherwise
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        String directionString = (direction==ItemTouchHelper.LEFT)?"left":"right";
                        if (adapter.getListVideos().isEmpty())
                            return;
                        Video video = adapter.getListVideos().get(viewHolder.getAdapterPosition());
                        String idRemovedVideo = video.getIdYT();
                        String thumbnailsUrl = video.getThumbnailsUrl();
                        String channelTitle = video.getChannelTitle();
                        String title = video.getTitle();
                        Log.i(LOG_TAG, "removed video untitled : "+idRemovedVideo);
                        //removing the video from the adapter list
                        adapter.getListVideos().remove(viewHolder.getAdapterPosition());
                        //and notify the view that the data has changed
                        adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                        //TODO Insert the video at it's right place (maybe use a sortedList ?)
                        if (mActivity.getAdapterVideoWatchedPage()!=null) {
                            mActivity.getAdapterVideoWatchedPage().getListVideos().add(0, video);
                            mActivity.getAdapterVideoWatchedPage().notifyDataSetChanged();
                        }

                        //mActivity.getMydatabase().execSQL("INSERT INTO T_VIDEO_PLAYED VALUES('"+idRemovedVideo+"', '"+ TextUtils.htmlEncode(title)+"', '"+thumbnailsUrl+"', '"+TextUtils.htmlEncode(channelTitle)+"');");

                        YoutubeSubscriptionsTVOpenDatabaseHelper youtubeSubscriptionsTVOpenDatabaseHelper = OpenHelperManager.getHelper(mActivity, YoutubeSubscriptionsTVOpenDatabaseHelper.class);
                        try {
                            Dao<Video, Long> youtubeSubscriptionsTVDao= youtubeSubscriptionsTVOpenDatabaseHelper.getDao();
                            youtubeSubscriptionsTVDao.create(video);
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

    public void fetchTimelineAsync(int page) {
        //adapter.clear();
        mActivity.getAdapterVideoPage().clear();
        this.loadVideos();
    }

    public void loadVideos() {
        if (mActivity.mAuthState != null && mActivity.mAuthState.isAuthorized()) {
            //we load the videos
            final HandleSubs handleSubs = new HandleSubs(mActivity,swipeContainer);
            mActivity.mAuthState.performActionWithFreshTokens(mActivity.authorizationService, new AuthState.AuthStateAction() {

                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                    mActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            swipeContainer.setRefreshing(true);
                        }
                    });
                    handleSubs.execute(accessToken);
                }
            });
        }else {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    swipeContainer.setRefreshing(false);
                }
            });
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}
