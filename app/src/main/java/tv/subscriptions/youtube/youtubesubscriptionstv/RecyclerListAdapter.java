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

    private ArrayList<Video> listVideos = new ArrayList<Video>();

    public ArrayList<Video> getListVideos() {
        return listVideos;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video, parent, false);
        return new ItemViewHolder(view);
    }
    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        holder.textView.setText(this.listVideos.get(position).getTitle());
    }
    @Override
    public int getItemCount() {
        return this.listVideos.size();
    }
}