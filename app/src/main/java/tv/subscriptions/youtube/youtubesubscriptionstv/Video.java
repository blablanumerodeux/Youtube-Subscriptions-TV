package tv.subscriptions.youtube.youtubesubscriptionstv;

import java.util.Date;

public class Video {

    private Date datePublished;
    private String title;
    private String idYT;
    private String thumbnailsUrl;
    private String channelTitle;

    public Date getDatePublished() {
        return datePublished;
    }

    public void setDatePublished(Date datePublished) {
        this.datePublished = datePublished;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIdYT() {
        return idYT;
    }

    public void setIdYT(String idYT) {
        this.idYT = idYT;
    }

    public String getThumbnailsUrl() {
        return thumbnailsUrl;
    }

    public void setThumbnailsUrl(String thumbnailsUrl) {
        this.thumbnailsUrl = thumbnailsUrl;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    @Override
    public String toString() {
        return  idYT ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Video video = (Video) o;

        return idYT.equals(video.idYT);
    }

    @Override
    public int hashCode() {
        return idYT.hashCode();
    }
}
