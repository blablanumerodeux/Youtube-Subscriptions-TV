package tv.subscriptions.subscriptionstv;

import android.content.Context;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import butterknife.BindInt;
import butterknife.BindString;

public class YoutubeSubscriptionsTVOpenDatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "youtubeSubscriptionsTV.db";
    private static final int DATABASE_VERSION = 4;

    /**
     * The data access object used to interact with the Sqlite database to do C.R.U.D operations.
     */
    private Dao<Video, Long> youtubeSubscriptionsTVDao;
    private Dao<UnplayedVideo, Long> youtubeSubscriptionsTVUnplayedDao;


    public YoutubeSubscriptionsTVOpenDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION,
        /**
         * R.raw.ormlite_config is a reference to the ormlite_config.txt file in the
         * /res/raw/ directory of this project
         * */
                R.raw.ormlite_config);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            /**
             * creates the Video database table
             */
            TableUtils.createTable(connectionSource, Video.class);
            TableUtils.createTable(connectionSource, UnplayedVideo.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {
        try {
/**
 * Recreates the database when onUpgrade is called by the framework
 */
            TableUtils.dropTable(connectionSource, Video.class, false);
            TableUtils.dropTable(connectionSource, UnplayedVideo.class, false);
            onCreate(database, connectionSource);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns an instance of the data access object
     * @return
     * @throws SQLException
     */
    public Dao<Video, Long> getDao() throws SQLException {
        if(youtubeSubscriptionsTVDao == null) {
            youtubeSubscriptionsTVDao = getDao(Video.class);
        }
        return youtubeSubscriptionsTVDao;
    }

    /**
     * Returns an instance of the data access object
     * @return
     * @throws SQLException
     */
    public Dao<UnplayedVideo, Long> getUnplayedDao() throws SQLException {
        if(youtubeSubscriptionsTVUnplayedDao == null) {
            youtubeSubscriptionsTVUnplayedDao = getDao(UnplayedVideo.class);
        }
        return youtubeSubscriptionsTVUnplayedDao;
    }

    /*
    Clear the table
     */
    public void clearTable() {
        try {
            /**
             * Recreates the database
             */
            TableUtils.dropTable(connectionSource, Video.class, false);
            TableUtils.createTable(connectionSource, Video.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /*
    Clear the table
     */
    public void clearUnplayedVideoTable() {
        try {
            /**
             * Recreates the database
             */
            TableUtils.dropTable(connectionSource, UnplayedVideo.class, false);
            TableUtils.createTable(connectionSource, UnplayedVideo.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
