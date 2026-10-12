package pw.thedrhax.mosmetro.services;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import pw.thedrhax.mosmetro.R;

@RequiresApi(24)
public class AutoConnectTileService extends TileService {
    @Override
    public void onStartListening() {
        refresh();
    }

    @Override
    public void onClick() {
        AutoConnect.setEnabled(this, !AutoConnect.isEnabled(this));
        refresh();
    }

    private void refresh() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean on = AutoConnect.isEnabled(this);
        tile.setLabel(getString(R.string.tile_autoconnect));
        tile.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= 29) {
            tile.setSubtitle(on
                    ? getString(R.string.pref_autoconnect)
                    : getString(R.string.force_off));
        }
        tile.setIcon(Icon.createWithResource(this,
                on ? R.drawable.ic_notification_success : R.drawable.ic_notification_error));
        tile.updateTile();
    }
}
