package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

class ItemViewHolder extends RecyclerView.ViewHolder {

    public final TextView textView;

    public ItemViewHolder(View itemView) {

        super(itemView);
        textView = (TextView) itemView;
    }
}