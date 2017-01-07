package tv.subscriptions.subscriptionstv;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "T_VIDEO_UNPLAYED")
public class UnplayedVideo {

    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField
    private Date datePublished;

    @DatabaseField
    private String title;

    @DatabaseField
    private String idYT;

    @DatabaseField
    private String thumbnailsUrl;

    @DatabaseField
    private String channelTitle;

    public UnplayedVideo() {
    }

    public UnplayedVideo(Date datePublished, String title, String idYT, String thumbnailsUrl, String channelTitle) {
        this.datePublished = datePublished;
        this.title = title;
        this.idYT = idYT;
        this.thumbnailsUrl = thumbnailsUrl;
        this.channelTitle = channelTitle;
    }

    public UnplayedVideo(Video video) {
        this.datePublished = video.getDatePublished();
        this.title = video.getTitle();
        this.idYT = video.getIdYT();
        this.thumbnailsUrl = video.getThumbnailsUrl();
        this.channelTitle = video.getChannelTitle();
    }

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

        UnplayedVideo video = (UnplayedVideo) o;

        return idYT.equals(video.idYT);
    }

    @Override
    public int hashCode() {
        return idYT.hashCode();
    }
}
