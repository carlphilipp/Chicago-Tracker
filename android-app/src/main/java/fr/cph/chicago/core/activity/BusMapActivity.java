/**
 * Copyright 2017 Carl-Philipp Harmant
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cph.chicago.core.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindString;
import butterknife.ButterKnife;
import fr.cph.chicago.R;
import fr.cph.chicago.core.App;
import fr.cph.chicago.entity.Bus;
import fr.cph.chicago.entity.BusDirections;
import fr.cph.chicago.entity.BusPattern;
import fr.cph.chicago.entity.Position;
import fr.cph.chicago.entity.enumeration.TrainLine;
import fr.cph.chicago.marker.RefreshBusMarkers;
import fr.cph.chicago.rx.BusFollowObserver;
import fr.cph.chicago.rx.BusObserver;
import fr.cph.chicago.rx.ObservableUtil;
import fr.cph.chicago.service.BusService;
import fr.cph.chicago.util.Util;

import static fr.cph.chicago.Constants.BUSES_ARRIVAL_URL;
import static fr.cph.chicago.Constants.BUSES_DIRECTION_URL;
import static fr.cph.chicago.Constants.BUSES_PATTERN_URL;
import static fr.cph.chicago.Constants.BUSES_VEHICLES_URL;

/**
 * @author Carl-Philipp Harmant
 * @version 1
 */
@SuppressWarnings("WeakerAccess")
public class BusMapActivity extends AbstractMapActivity {

    private static final String TAG = BusMapActivity.class.getSimpleName();

    @BindString(R.string.bundle_bus_id)
    String bundleBusId;
    @BindString(R.string.bundle_bus_route_id)
    String bundleBusRouteId;
    @BindString(R.string.bundle_bus_bounds)
    String bundleBusBounds;
    @BindString(R.string.analytics_bus_map)
    String analyticsBusMap;
    @BindString(R.string.request_rt)
    String requestRt;

    private final ObservableUtil observableUtil;
    private final BusService busService;

    private List<Marker> busMarkers;
    private List<Marker> busStationMarkers;
    private Map<Marker, View> views;
    private Map<Marker, Boolean> status;

    private Integer busId;
    private String busRouteId;
    private String[] bounds;
    private RefreshBusMarkers refreshBusesBitmap;

    private boolean loadPattern = true;

    public BusMapActivity() {
        observableUtil = ObservableUtil.INSTANCE;
        busService = BusService.INSTANCE;
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.Companion.checkBusData(this);
        if (!this.isFinishing()) {
            MapsInitializer.initialize(getApplicationContext());
            setContentView(R.layout.activity_map);
            ButterKnife.bind(this);

            if (savedInstanceState != null) {
                busId = savedInstanceState.getInt(bundleBusId);
                busRouteId = savedInstanceState.getString(bundleBusRouteId);
                bounds = savedInstanceState.getStringArray(bundleBusBounds);
            } else {
                final Bundle extras = getIntent().getExtras();
                busId = extras.getInt(bundleBusId);
                busRouteId = extras.getString(bundleBusRouteId);
                bounds = extras.getStringArray(bundleBusBounds);
            }

            // Init data
            initData();

            // Init toolbar
            setToolbar();

            // Google analytics
            Util.INSTANCE.trackScreen(analyticsBusMap);
        }
    }

    @Override
    public final void onStop() {
        super.onStop();
        loadPattern = false;
    }

    @Override
    protected void initData() {
        super.initData();
        busMarkers = new ArrayList<>();
        busStationMarkers = new ArrayList<>();
        views = new HashMap<>();
        status = new HashMap<>();
        refreshBusesBitmap = new RefreshBusMarkers();
    }

    @Override
    protected void setToolbar() {
        super.setToolbar();
        getToolbar().setOnMenuItemClickListener((item -> {
            Util.INSTANCE.trackAction(R.string.analytics_category_req, R.string.analytics_action_get_bus, BUSES_VEHICLES_URL);
            observableUtil.createBusListObservable(busId, busRouteId).subscribe(new BusObserver(BusMapActivity.this, false, getLayout()));
            return false;
        }));

        Util.INSTANCE.setWindowsColor(this, getToolbar(), TrainLine.NA);
        getToolbar().setTitle(busRouteId);
    }

    public void centerMapOnBus(@NonNull final List<Bus> result) {
        final boolean sizeIsOne = result.size() == 1;
        final Position position = sizeIsOne ? result.get(0).getPosition() : Bus.Companion.getBestPosition(result);
        final int zoom = sizeIsOne ? 15 : 11; // FIXME magic numbers
        centerMapOn(position.getLatitude(), position.getLongitude(), zoom);
    }

    public void drawBuses(@NonNull final List<Bus> buses) {
        cleanAllMarkers();
        final BitmapDescriptor bitmapDescr = refreshBusesBitmap.getCurrentDescriptor();
        Stream.of(buses).forEach(bus -> {
            final LatLng point = new LatLng(bus.getPosition().getLatitude(), bus.getPosition().getLongitude());
            final Marker marker = getGoogleMap().addMarker(
                new MarkerOptions()
                    .position(point)
                    .title("To " + bus.getDestination())
                    .snippet(bus.getId() + "")
                    .icon(bitmapDescr)
                    .anchor(0.5f, 0.5f)
                    .rotation(bus.getHeading())
                    .flat(true)
            );
            busMarkers.add(marker);

            final LayoutInflater layoutInflater = (LayoutInflater) BusMapActivity.this.getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View view = layoutInflater.inflate(R.layout.marker, getViewGroup(), false);
            final TextView title = view.findViewById(R.id.title);
            title.setText(marker.getTitle());

            views.put(marker, view);
        });
    }

    private void cleanAllMarkers() {
        Stream.of(busMarkers).forEach(Marker::remove);
        busMarkers.clear();
    }

    private void drawPattern(@NonNull final List<BusPattern> patterns) {
        final int[] index = new int[]{0};
        final BitmapDescriptor red = BitmapDescriptorFactory.defaultMarker();
        final BitmapDescriptor blue = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
        Stream.of(patterns).forEach(pattern -> {
            final PolylineOptions poly = new PolylineOptions()
                .color(index[0] == 0 ? Color.RED : (index[0] == 1 ? Color.BLUE : Color.YELLOW))
                .width(((App) getApplication()).getLineWidth()).geodesic(true);
            Stream.of(pattern.getPoints())
                .map(patternPoint -> {
                    final LatLng point = new LatLng(patternPoint.getPosition().getLatitude(), patternPoint.getPosition().getLongitude());
                    poly.add(point);
                    Marker marker = null;
                    if ("S".equals(patternPoint.getType())) {
                        marker = getGoogleMap().addMarker(new MarkerOptions()
                            .position(point)
                            .title(patternPoint.getStopName())
                            .snippet(pattern.getDirection())
                            .icon(index[0] == 0 ? red : blue)
                        );
                        marker.setVisible(false);
                    }
                    // Potential null sent, if stream api change, it could fail
                    return marker;
                })
                .filter(marker -> marker != null)
                .forEach(busStationMarkers::add);
            getGoogleMap().addPolyline(poly);
            index[0]++;
        });
    }

    @Override
    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        busId = savedInstanceState.getInt(bundleBusId);
        busRouteId = savedInstanceState.getString(bundleBusRouteId);
        bounds = savedInstanceState.getStringArray(bundleBusBounds);
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        savedInstanceState.putInt(bundleBusId, busId);
        savedInstanceState.putString(bundleBusRouteId, busRouteId);
        savedInstanceState.putStringArray(bundleBusBounds, bounds);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCameraIdle() {
        refreshBusesBitmap.refreshBusAndStation(getGoogleMap().getCameraPosition(), busMarkers, busStationMarkers);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        super.onMapReady(googleMap);

        getGoogleMap().setInfoWindowAdapter(new InfoWindowAdapter() {
            @Override
            public View getInfoWindow(final Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(final Marker marker) {
                if (marker.getTitle().startsWith("To ")) {
                    final View view = views.get(marker);
                    if (!getRefreshingInfoWindow()) {
                        setSelectedMarker(marker);
                        final String busId = marker.getSnippet();
                        Util.INSTANCE.trackAction(R.string.analytics_category_req, R.string.analytics_action_get_bus, BUSES_ARRIVAL_URL);
                        observableUtil.createFollowBusObservable(busId)
                            .subscribe(new BusFollowObserver(BusMapActivity.this, getLayout(), view, false));
                        status.put(marker, false);
                    }
                    return view;
                } else {
                    return null;
                }
            }
        });

        getGoogleMap().setOnInfoWindowClickListener(marker -> {
            if (marker.getTitle().startsWith("To ")) {
                final View view = views.get(marker);
                if (!getRefreshingInfoWindow()) {
                    setSelectedMarker(marker);
                    final String runNumber = marker.getSnippet();
                    final boolean current = status.get(marker);
                    Util.INSTANCE.trackAction(R.string.analytics_category_req, R.string.analytics_action_get_bus, BUSES_ARRIVAL_URL);
                    observableUtil.createFollowBusObservable(runNumber)
                        .subscribe(new BusFollowObserver(BusMapActivity.this, getLayout(), view, !current));
                    status.put(marker, !current);
                }
            }
        });
        loadActivityData();
    }

    private void loadActivityData() {
        if (Util.INSTANCE.isNetworkAvailable()) {
            Util.INSTANCE.trackAction(R.string.analytics_category_req, R.string.analytics_action_get_bus, BUSES_VEHICLES_URL);
            observableUtil.createBusListObservable(busId, busRouteId).subscribe(new BusObserver(BusMapActivity.this, true, getLayout()));
            if (loadPattern) {
                new LoadPattern().execute();
            }
        } else {
            Util.INSTANCE.showNetworkErrorMessage(getLayout());
        }
    }

    private class LoadPattern extends AsyncTask<Void, Void, List<BusPattern>> {
        /**
         * List of bus pattern
         **/
        private List<BusPattern> patterns;

        @Override
        protected final List<BusPattern> doInBackground(final Void... params) {
            this.patterns = new ArrayList<>();
            if (busId == 0) {
                // Search for directions
                final BusDirections busDirections = busService.loadBusDirections(busRouteId);
                bounds = new String[busDirections.getBusDirections().size()];
                for (int i = 0; i < busDirections.getBusDirections().size(); i++) {
                    bounds[i] = busDirections.getBusDirections().get(i).getText();
                }
                Util.INSTANCE.trackAction(R.string.analytics_category_req, R.string.analytics_action_get_bus, BUSES_DIRECTION_URL);
            }
            Stream.of(busService.loadBusPattern(busRouteId, bounds)).forEach(this.patterns::add);
            Util.INSTANCE.trackAction(R.string.analytics_category_req, R.string.analytics_action_get_bus, BUSES_PATTERN_URL);
            return this.patterns;
        }

        @Override
        protected final void onPostExecute(final List<BusPattern> result) {
            if (result != null) {
                drawPattern(result);
            } else {
                Util.INSTANCE.showNetworkErrorMessage(getLayout());
            }
        }
    }
}
