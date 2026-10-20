package cl.bolsillo.app;
import android.app.job.*;import android.content.*;
/** Completes obsolete remote-sync jobs without network access. */
public class SyncJob extends JobService {
 static void schedule(Context c){c.getSystemService(JobScheduler.class).cancel(27101);}
 @Override public boolean onStartJob(JobParameters p){NativeSync.run(this,(ok,msg)->jobFinished(p,false));return true;}
 @Override public boolean onStopJob(JobParameters p){return false;}
}
