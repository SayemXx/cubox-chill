package app.moviebox.mobile;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.media3.exoplayer.offline.*;
import androidx.media3.exoplayer.scheduler.Scheduler;
import androidx.media3.exoplayer.offline.DownloadNotificationHelper;
import java.util.List;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class OfflineService extends DownloadService {
    public OfflineService(){super(42,1000,"downloads",R.string.download_channel,R.string.download_description);}
    protected DownloadManager getDownloadManager(){Offline.initialize(this);return Offline.manager;}
    protected Scheduler getScheduler(){return null;}
    protected Notification getForegroundNotification(List<Download> downloads,int requirements){
        PendingIntent intent=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).putExtra("downloads",true),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new DownloadNotificationHelper(this,"downloads").buildProgressNotification(this,R.drawable.ic_app,intent,"cubox & chill • Offline library",downloads,requirements);
    }
}
