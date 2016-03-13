package fr.cph.chicago.task;

import android.os.AsyncTask;
import android.widget.Toast;

import java.util.List;

import fr.cph.chicago.ChicagoTracker;
import fr.cph.chicago.R;
import fr.cph.chicago.activity.BusBoundActivity;
import fr.cph.chicago.adapter.BusBoundAdapter;
import fr.cph.chicago.data.DataHolder;
import fr.cph.chicago.entity.BusStop;
import fr.cph.chicago.exception.ConnectException;
import fr.cph.chicago.exception.ParserException;
import fr.cph.chicago.exception.TrackerException;
import fr.cph.chicago.util.Util;

/**
 * Task that connect to API to get the bound of the selected stop
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
public class BusBoundAsyncTask extends AsyncTask<Void, Void, List<BusStop>> {

    private BusBoundActivity activity;
    private String busRouteId;
    private String bound;
    private BusBoundAdapter busBoundAdapter;
    private TrackerException trackerException;

    public BusBoundAsyncTask(final BusBoundActivity activity, final String busRouteId, final String bound, final BusBoundAdapter busBoundAdapter) {
        this.activity = activity;
        this.busRouteId = busRouteId;
        this.bound = bound;
        this.busBoundAdapter = busBoundAdapter;
    }

    @Override
    protected final List<BusStop> doInBackground(final Void... params) {
        List<BusStop> lBuses = null;
        try {
            lBuses = DataHolder.getInstance().getBusData().loadBusStop(busRouteId, bound);
        } catch (final ParserException | ConnectException e) {
            this.trackerException = e;
        }
        Util.trackAction(activity, R.string.analytics_category_req, R.string.analytics_action_get_bus, R.string.analytics_action_get_bus_stop, 0);
        return lBuses;
    }

    @Override
    protected final void onPostExecute(final List<BusStop> result) {
        activity.setBusStops(result);
        if (trackerException == null) {
            busBoundAdapter.update(result);
            busBoundAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(ChicagoTracker.getContext(), trackerException.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}