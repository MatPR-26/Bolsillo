package cl.bolsillo.app;
import android.service.quicksettings.*;import android.app.Dialog;import android.os.Build;
public class BolsilloTileService extends TileService {
 Dialog active;
 @Override public void onStartListening(){super.onStartListening();Tile t=getQsTile();if(t!=null){t.setLabel("Bolsillo");if(Build.VERSION.SDK_INT>=29)t.setSubtitle("Registrar");t.setState(Tile.STATE_INACTIVE);t.updateTile();}}
 @Override public void onClick(){super.onClick();Runnable launch=()->{if(active!=null)active.dismiss();active=QuickEntryDialog.create(this);showDialog(active);};if(isLocked())unlockAndRun(launch);else launch.run();}
 @Override public void onDestroy(){if(active!=null)active.dismiss();super.onDestroy();}
}
