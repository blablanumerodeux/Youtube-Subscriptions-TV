package tv.subscriptions.subscriptionstv;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

class ItemViewHolder extends RecyclerView.ViewHolder {

    protected ImageView imageView;
    protected TextView textView;
    protected TextView channelTitleView;
    protected TextView dateView;

    public ItemViewHolder(View itemView) {

        super(itemView);
        this.imageView = (ImageView) itemView.findViewById(R.id.thumbnail);
        this.textView = (TextView) itemView.findViewById(R.id.title);
        this.channelTitleView = (TextView) itemView.findViewById(R.id.channelTitle);
        this.dateView = (TextView) itemView.findViewById(R.id.date);
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public TextView getTextView() {
        return textView;
    }

    public void setTextView(TextView textView) {
        this.textView = textView;
    }

    public TextView getChannelTitle() {
        return channelTitleView;
    }

    public void setChannelTitle(TextView channelTitle) {
        this.channelTitleView = channelTitle;
    }

    public TextView getDateView() {
        return dateView;
    }

    public void setDateView(TextView dateView) {
        this.dateView = dateView;
    }
}