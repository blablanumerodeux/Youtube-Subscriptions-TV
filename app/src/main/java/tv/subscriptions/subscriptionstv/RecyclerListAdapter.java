package tv.subscriptions.subscriptionstv;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class RecyclerListAdapter extends RecyclerView.Adapter<ItemViewHolder> {

    private Context mContext;
    private ArrayList<Video> listVideos = new ArrayList<Video>();
    private ArrayList<Video> listVideosDisplayed = new ArrayList<Video>();

    public RecyclerListAdapter(Context context) {
        this.mContext = context;
    }

    public RecyclerListAdapter(Context context, boolean noScrollListener) {
        this.mContext = context;
        if (noScrollListener)
            this.listVideosDisplayed = this.listVideos;
    }


    public ArrayList<Video> getListVideos() {
        return listVideos;
    }

    public ArrayList<Video> getListVideosDisplayed() {
        return listVideosDisplayed;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        if(position>=this.listVideosDisplayed.size())
            return;
        Video video = this.listVideosDisplayed.get(position);
        holder.getTextView().setText(video.getTitle());
        holder.getChannelTitle().setText(video.getChannelTitle());
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
        return this.listVideosDisplayed.size();
    }

    public void clear() {
        this.listVideos.clear();
        this.listVideosDisplayed.clear();
        notifyDataSetChanged();
    }
}