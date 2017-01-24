package tv.subscriptions.subscriptionstv;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.util.List;

public class SetLastPlaylistAsWatchedDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.mark_as_watched)
                .setPositiveButton(R.string.watched, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        List<Video> playlist = mainActivity.getPlaylist();
                        if (playlist != null && !playlist.isEmpty()){
                            YoutubeSubscriptionsTVOpenDatabaseHelper youtubeSubscriptionsTVOpenDatabaseHelper = OpenHelperManager.getHelper(mainActivity, YoutubeSubscriptionsTVOpenDatabaseHelper.class);
                            try {
                                Dao<Video, Long> youtubeSubscriptionsTVDao= youtubeSubscriptionsTVOpenDatabaseHelper.getDao();
                                for (Video video: playlist) {
                                    //if the video already exist in the DB so we do not insert it again.
                                    if (youtubeSubscriptionsTVDao.queryBuilder().where().eq("idYT", video.getIdYT()).countOf() > 0)
                                        continue;
                                    youtubeSubscriptionsTVDao.create(video);
                                    //((MainActivity) getActivity()).getMydatabase().execSQL("INSERT INTO T_VIDEO_PLAYED VALUES('"+video.getIdYT()+"', '"+ TextUtils.htmlEncode(video.getTitle())+"', '"+video.getThumbnailsUrl()+"', '"+TextUtils.htmlEncode(video.getChannelTitle())+"');");
                                }
                                mainActivity.getAdapterVideoPage().getListVideosDisplayed().removeAll(playlist);
                                mainActivity.getAdapterVideoWatchedPage().getListVideos().addAll(0, playlist);
                                mainActivity.videoPageFragment.loadNextDataFromApi(0);
                                mainActivity.getAdapterVideoPage().notifyDataSetChanged();
                                if (mainActivity.getAdapterVideoWatchedPage()!=null)
                                    mainActivity.getAdapterVideoWatchedPage().notifyDataSetChanged();
                            } catch (java.sql.SQLException e) {
                                e.printStackTrace();
                                return;
                            }
                        }else{
                            Context context = getActivity().getApplicationContext();
                            CharSequence text = "La playlist est vide !";
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }


}
