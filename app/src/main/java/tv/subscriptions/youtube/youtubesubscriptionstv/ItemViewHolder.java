package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

class ItemViewHolder extends RecyclerView.ViewHolder {

    private ImageView imageView;
    private TextView textView;

    public ItemViewHolder(View itemView) {

        super(itemView);
        this.imageView = (ImageView) itemView.findViewById(R.id.thumbnail);
        this.textView = (TextView) itemView.findViewById(R.id.title);
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
}