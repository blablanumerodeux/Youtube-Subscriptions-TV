package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static tv.subscriptions.youtube.youtubesubscriptionstv.ScrollingActivity.LOG_TAG;


public class RecyclerListAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private ArrayList<String> listVideosPlayed = new ArrayList<String>();
    private ArrayList<Video> listVideos = new ArrayList<Video>();

    public ArrayList<Video> getListVideos() {
        return listVideos;
    }

    public ArrayList<String> getListVideosPlayed() {
        return listVideosPlayed;
    }

    public void setListVideosPlayed(ArrayList<String> listVideosPlayed) {
        this.listVideosPlayed = listVideosPlayed;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video, parent, false);
        return new ItemViewHolder(view);
    }
    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        //if the video has already been played we don't add it to the playlist
        if (listVideosPlayed.contains(this.listVideos.get(position).getIdYT()))
            Log.i(LOG_TAG, "Video already played");
        else
            holder.textView.setText(this.listVideos.get(position).getTitle());
    }
    @Override
    public int getItemCount() {
        return this.listVideos.size();
    }
}