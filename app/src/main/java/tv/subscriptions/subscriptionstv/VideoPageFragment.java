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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindInt;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import tv.subscriptions.services.UnplayedVideosService;

import static tv.subscriptions.subscriptionstv.MainActivity.LOG_TAG;

public class VideoPageFragment extends Fragment {

    private RecyclerListAdapter adapter;
    private MainActivity mActivity;
    @BindView(R.id.swipeContainer) SwipeRefreshLayout swipeContainer;
    private View view;
    @BindString(R.string.err_not_logged_in) String errNotLoggedIn;
    @BindInt(R.integer.numberOfVideosToLoad) int numberOfVideosToLoad;
    // Store a member variable for the listener
    private EndlessRecyclerViewScrollListener scrollListener;

    public SwipeRefreshLayout getSwipeContainer() {
        return swipeContainer;
    }

    public EndlessRecyclerViewScrollListener getScrollListener() {
        return scrollListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.view = inflater.inflate(R.layout.videos_page, container, false);
        ButterKnife.bind(this, this.view);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_videos);
        this.mActivity = (MainActivity) getActivity();
        this.mActivity.setAdapterVideoPage(new RecyclerListAdapter(mActivity.getBaseContext()));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        this.adapter = this.mActivity.getAdapterVideoPage();
        recyclerView.setAdapter(this.adapter);
        // Retain an instance so that you can call `resetState()` for fresh searches
        this.scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadNextDataFromApi(page);
            }
        };
        // Adds the scroll listener to RecyclerView
        recyclerView.addOnScrollListener(scrollListener);
        //swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
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
                Snackbar.make(view, errNotLoggedIn, Snackbar.LENGTH_LONG)
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
                        if (adapter.getListVideosDisplayed().isEmpty())
                            return;
                        Video video = adapter.getListVideosDisplayed().get(viewHolder.getAdapterPosition());
                        String idRemovedVideo = video.getIdYT();
                        String thumbnailsUrl = video.getThumbnailsUrl();
                        String channelTitle = video.getChannelTitle();
                        String title = video.getTitle();
                        Log.i(LOG_TAG, "removed video untitled : "+idRemovedVideo);
                        //removing the video from the adapter list
                        adapter.getListVideosDisplayed().remove(viewHolder.getAdapterPosition());
                        //and notify the view that the data has changed
                        adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                        //TODO Insert the video at it's right place (maybe use a sortedList ?)
                        if (mActivity.getAdapterVideoWatchedPage()!=null) {
                            mActivity.getAdapterVideoWatchedPage().getListVideosDisplayed().add(0, video);
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
        //this.loadVideos();

        //We fetch the unplayed videos
        UnplayedVideosService unplayedVideosService = new UnplayedVideosService(mActivity);
        List<Video> listVideos = unplayedVideosService.fetchUnplayedVideos();
        if (listVideos.isEmpty())
            this.fetchTimelineAsync(0);

        mActivity.getAdapterVideoPage().getListVideos().addAll(listVideos);
        this.loadNextDataFromApi(0);
        this.scrollListener.resetState();
        mActivity.getAdapterVideoPage().notifyDataSetChanged();
        mActivity.fab.setOnClickListener(new CallIntentListener(mActivity, mActivity.getFullUrl()));
        return view;
    }

    // Append the next page of data into the adapter
    // This method probably sends out a network request and appends new data items to your adapter.
    public void loadNextDataFromApi(int offset) {
        // Send an API request to retrieve appropriate paginated data
        //  --> Send the request including an offset value (i.e `page`) as a query parameter.
        //  --> Deserialize and construct new model objects from the API response
        //  --> Append the new data objects to the existing set of items inside the array of items
        //  --> Notify the adapter of the new items made with `notifyItemRangeInserted()`

        ArrayList<Video> listVideos = mActivity.getAdapterVideoPage().getListVideos();
        if (listVideos.isEmpty())
            return;
        int subListSizeToDisplay = numberOfVideosToLoad;
        if (listVideos.size()<subListSizeToDisplay)
            subListSizeToDisplay = listVideos.size();

        ArrayList<Video> videoToDisplay = new ArrayList<Video>(listVideos.subList(0, subListSizeToDisplay));
        mActivity.getAdapterVideoPage().getListVideosDisplayed().addAll(videoToDisplay);
        listVideos.subList(0, subListSizeToDisplay).clear();
        // Delay before notifying the adapter since the scroll listeners
        // can be called while RecyclerView data cannot be changed.
        this.view.post(new Runnable() {
            @Override
            public void run() {
                // Notify adapter with appropriate notify methods
                //adapter.notifyItemRangeInserted(curSize, allContacts.size() - 1);
                mActivity.getAdapterVideoPage().notifyDataSetChanged();
            }
        });


    }

    public void fetchTimelineAsync(int page) {
        //we clean the unplayed video table and the adapter
        final YoutubeSubscriptionsTVOpenDatabaseHelper youtubeSubscriptionsTVOpenDatabaseHelper = OpenHelperManager.getHelper(mActivity, YoutubeSubscriptionsTVOpenDatabaseHelper.class);
        youtubeSubscriptionsTVOpenDatabaseHelper.clearUnplayedVideoTable();
        mActivity.getAdapterVideoPage().clear();
        mActivity.getAdapterVideoPage().notifyDataSetChanged();
        //Reset endless scroll listener when performing a new search
        scrollListener.resetState();
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
            Snackbar.make(view, errNotLoggedIn, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}
