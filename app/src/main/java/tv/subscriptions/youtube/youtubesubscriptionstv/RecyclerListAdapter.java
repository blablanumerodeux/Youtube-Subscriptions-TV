package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


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
        Video video = this.listVideos.get(position);
        holder.textView.setText(video.getTitle()/*+"|"+video.getThumbnailsUrl()+"|"+video.getChannelTitle()*/);
        //holder.textView.setBackgroundColor(Color.CYAN);
    }
    @Override
    public int getItemCount() {
        return this.listVideos.size();
    }
}