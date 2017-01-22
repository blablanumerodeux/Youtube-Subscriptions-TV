package tv.subscriptions.services;

import android.app.Activity;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tv.subscriptions.subscriptionstv.UnplayedVideo;
import tv.subscriptions.subscriptionstv.Video;
import tv.subscriptions.subscriptionstv.YoutubeSubscriptionsTVOpenDatabaseHelper;

//TODO MIGRATE TO SERVICES WHEN POSSIBLE !
public class UnplayedVideosService {

    private Activity mainActivity;

    public UnplayedVideosService(Activity mainActivity) {
        this.mainActivity=mainActivity;
    }

    public List<Video> fetchUnplayedVideos () {
        //We fetch the unplayed videos
        List<UnplayedVideo> listUnplayedVideos = new ArrayList<UnplayedVideo>();
        List<Video> listVideos = new ArrayList<Video>();
        YoutubeSubscriptionsTVOpenDatabaseHelper youtubeSubscriptionsTVOpenDatabaseHelper = OpenHelperManager.getHelper(this.mainActivity, YoutubeSubscriptionsTVOpenDatabaseHelper.class);
        try {
            Dao<UnplayedVideo, Long> youtubeSubscriptionsTVDao = youtubeSubscriptionsTVOpenDatabaseHelper.getUnplayedDao();
            listUnplayedVideos = youtubeSubscriptionsTVDao.queryForAll();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        //We sort the list
        Collections.sort(listUnplayedVideos, new Comparator<UnplayedVideo>(){
            public int compare(UnplayedVideo video1, UnplayedVideo video2) {
                return video2.getDatePublished().compareTo(video1.getDatePublished());
            }
        });

        for (UnplayedVideo unplayedVideos : listUnplayedVideos) {
            listVideos.add(new Video(unplayedVideos));
        }

        //We fetch and remove the played videos
        List<Video> listPlayedVideos = new ArrayList<Video>();
        try {
            Dao<Video, Long> youtubeSubscriptionsTVDao = youtubeSubscriptionsTVOpenDatabaseHelper.getDao();
            listPlayedVideos = youtubeSubscriptionsTVDao.queryForAll();
            listVideos.removeAll(listPlayedVideos);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        return listVideos;
    }

    public boolean saveUnplayedVideos(List<Video> listVideos) {
        try {
            YoutubeSubscriptionsTVOpenDatabaseHelper youtubeSubscriptionsTVOpenDatabaseHelper = OpenHelperManager.getHelper(this.mainActivity, YoutubeSubscriptionsTVOpenDatabaseHelper.class);
            Dao<UnplayedVideo, Long> youtubeSubscriptionsTVDao = youtubeSubscriptionsTVOpenDatabaseHelper.getUnplayedDao();
            for (Video video: listVideos) {
                UnplayedVideo unplayedVideo = new UnplayedVideo(video);
                youtubeSubscriptionsTVDao.create(unplayedVideo);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
