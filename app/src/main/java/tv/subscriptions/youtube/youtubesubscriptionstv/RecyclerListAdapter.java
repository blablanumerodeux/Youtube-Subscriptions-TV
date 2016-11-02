package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RecyclerListAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private ArrayList<String> listVideos = new ArrayList<String>();

    public ArrayList<String> getListVideos() {
        return listVideos;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video, parent, false);
        return new ItemViewHolder(view);
    }
    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        holder.textView.setText(this.listVideos.get(position));
    }
    @Override
    public int getItemCount() {
        return this.listVideos.size();
    }
}