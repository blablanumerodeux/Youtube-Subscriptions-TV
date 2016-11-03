package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class RecyclerListAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private Context mContext;

    public RecyclerListAdapter(Context context) {
        this.mContext = context;
    }

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
        holder.getTextView().setText(video.getTitle()/*+"|"+video.getThumbnailsUrl()+"|"+video.getChannelTitle()*/);
        //holder.textView.setBackgroundColor(Color.CYAN);

        //Render image using Picasso library
        if (!TextUtils.isEmpty(video.getThumbnailsUrl())) {
            Picasso.with(mContext).load(video.getThumbnailsUrl())
                    .error(R.drawable.placeholder)
                    .placeholder(R.drawable.placeholder)
                    .into(holder.imageView);
        }

    }
    @Override
    public int getItemCount() {
        return this.listVideos.size();
    }
}