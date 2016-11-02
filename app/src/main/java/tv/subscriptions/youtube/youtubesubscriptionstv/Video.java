package tv.subscriptions.youtube.youtubesubscriptionstv;

import java.util.Date;

/**
 * Created by dreaser on 01/11/16.
 */

public class Video {

    private Date datePublished;
    private String title;
    private String idYT;

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
