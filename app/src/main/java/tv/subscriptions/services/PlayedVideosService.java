package tv.subscriptions.services;

import android.app.Activity;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

import tv.subscriptions.subscriptionstv.Video;
import tv.subscriptions.subscriptionstv.YoutubeSubscriptionsTVOpenDatabaseHelper;

//TODO MIGRATE TO SERVICES WHEN POSSIBLE !
public class PlayedVideosService {

    private Activity mainActivity;

    public PlayedVideosService(Activity mainActivity) {
        this.mainActivity=mainActivity;
    }

    public Boolean markAllAsWatched (List<Video> listVideos) {
        try {
            final YoutubeSubscriptionsTVOpenDatabaseHelper youtubeSubscriptionsTVOpenDatabaseHelper = OpenHelperManager.getHelper(mainActivity, YoutubeSubscriptionsTVOpenDatabaseHelper.class);
            Dao<Video, Long> youtubeSubscriptionsTVDao = youtubeSubscriptionsTVOpenDatabaseHelper.getDao();
            for(Video video : listVideos){
                youtubeSubscriptionsTVDao.create(video);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
