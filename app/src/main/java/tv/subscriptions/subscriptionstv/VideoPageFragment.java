package tv.subscriptions.subscriptionstv;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static tv.subscriptions.subscriptionstv.MainActivity.LOG_TAG;

public class VideoPageFragment extends Fragment {

    private RecyclerListAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.videos_page, container, false);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_videos);
        final MainActivity mActivity = (MainActivity) getActivity();
        ((MainActivity) getActivity()).setAdapter(new RecyclerListAdapter(mActivity.getBaseContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        this.adapter = ((MainActivity) getActivity()).getAdapter();
        recyclerView.setAdapter(this.adapter);

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
                        Video video = adapter.getListVideos().get(viewHolder.getAdapterPosition());
                        String idRemovedVideo = video.getIdYT();
                        String thumbnailsUrl = video.getThumbnailsUrl();
                        String channelTitle = video.getChannelTitle();
                        String title = video.getTitle();
                        Log.i(LOG_TAG, "removed video untitled : "+idRemovedVideo);
                        adapter.getListVideos().remove(viewHolder.getAdapterPosition());
                        adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                        ((MainActivity) getActivity()).getMydatabase().execSQL("INSERT INTO T_VIDEO_PLAYED VALUES('"+idRemovedVideo+"', '"+ TextUtils.htmlEncode(title)+"', '"+thumbnailsUrl+"', '"+TextUtils.htmlEncode(channelTitle)+"');");
                    }
                });
        mIth.attachToRecyclerView(recyclerView);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
