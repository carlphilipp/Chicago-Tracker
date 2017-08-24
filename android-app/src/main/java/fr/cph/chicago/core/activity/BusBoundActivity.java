/**
 * Copyright 2017 Carl-Philipp Harmant
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cph.chicago.core.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.annimon.stream.Stream;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.cph.chicago.R;
import fr.cph.chicago.core.App;
import fr.cph.chicago.core.adapter.BusBoundAdapter;
import fr.cph.chicago.entity.BusPattern;
import fr.cph.chicago.entity.BusStop;
import fr.cph.chicago.entity.Position;
import fr.cph.chicago.entity.enumeration.TrainLine;
import fr.cph.chicago.rx.ObservableUtil;
import fr.cph.chicago.util.Util;

import static fr.cph.chicago.Constants.BUSES_PATTERN_URL;
import static fr.cph.chicago.Constants.BUSES_STOP_URL;

/**
 * Activity that represents the bus bound activity
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
@SuppressWarnings("WeakerAccess")
public class BusBoundActivity extends ListActivity {

    private static final String TAG = BusBoundActivity.class.getSimpleName();

    @BindView(R.id.bellow)
    LinearLayout layout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.bus_filter)
    EditText filter;

    @BindString(R.string.bundle_bus_stop_id)
    String bundleBusStopId;
    @BindString(R.string.bundle_bus_route_id)
    String bundleBusRouteId;
    @BindString(R.string.bundle_bus_bound)
    String bundleBusBound;
    @BindString(R.string.bundle_bus_bound_title)
    String bundleBusBoundTitle;
    @BindString(R.string.bundle_bus_stop_name)
    String bundleBusStopName;
    @BindString(R.string.bundle_bus_route_name)
    String bundleBusRouteName;
    @BindString(R.string.bundle_bus_latitude)
    String bundleBusLatitude;
    @BindString(R.string.bundle_bus_longitude)
    String bundleBusLongitude;

    @BindDrawable(R.drawable.ic_arrow_back_white_24dp)
    Drawable arrowBackWhite;

    private final ObservableUtil observableUtil;
    private final Util util;

    private MapFragment mapFragment;
    private String busRouteId;
    private String busRouteName;
    private String bound;
    private String boundTitle;
    private BusBoundAdapter busBoundAdapter;
    private List<BusStop> busStops;

    public BusBoundActivity() {
        observableUtil = ObservableUtil.INSTANCE;
        util = Util.INSTANCE;
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.Companion.checkBusData(this);
        if (!this.isFinishing()) {
            setContentView(R.layout.activity_bus_bound);
            ButterKnife.bind(this);

            if (busRouteId == null || busRouteName == null || bound == null || boundTitle == null) {
                final Bundle extras = getIntent().getExtras();
                busRouteId = extras.getString(bundleBusRouteId);
                busRouteName = extras.getString(bundleBusRouteName);
                bound = extras.getString(bundleBusBound);
                boundTitle = extras.getString(bundleBusBoundTitle);
            }
            busBoundAdapter = new BusBoundAdapter();
            setListAdapter(busBoundAdapter);
            getListView().setOnItemClickListener((adapterView, view, position, id) -> {
                final BusStop busStop = (BusStop) busBoundAdapter.getItem(position);
                final Intent intent = new Intent(getApplicationContext(), BusActivity.class);

                final Bundle extras = new Bundle();
                extras.putInt(bundleBusStopId, busStop.getId());
                extras.putString(bundleBusStopName, busStop.getName());
                extras.putString(bundleBusRouteId, busRouteId);
                extras.putString(bundleBusRouteName, busRouteName);
                extras.putString(bundleBusBound, bound);
                extras.putString(bundleBusBoundTitle, boundTitle);
                extras.putDouble(bundleBusLatitude, busStop.getPosition().getLatitude());
                extras.putDouble(bundleBusLongitude, busStop.getPosition().getLongitude());

                intent.putExtras(extras);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });

            filter.addTextChangedListener(new TextWatcher() {
                private List<BusStop> busStopsFiltered;

                @Override
                public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                    busStopsFiltered = new ArrayList<>();
                }

                @Override
                public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                    if (busStops != null) {
                        Stream.of(busStops)
                            .filter(busStop -> StringUtils.containsIgnoreCase(busStop.getName(), s))
                            .forEach(busStopsFiltered::add);
                    }
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    busBoundAdapter.setBusStops(busStopsFiltered);
                    busBoundAdapter.notifyDataSetChanged();
                }
            });


            util.setWindowsColor(this, toolbar, TrainLine.NA);
            toolbar.setTitle(busRouteId + " - " + boundTitle);

            toolbar.setNavigationIcon(arrowBackWhite);
            toolbar.setOnClickListener(v -> finish());

            observableUtil.createBusStopBoundObservable(getApplicationContext(), busRouteId, bound)
                .subscribe(onNext -> {
                        busStops = onNext;
                        busBoundAdapter.setBusStops(onNext);
                        busBoundAdapter.notifyDataSetChanged();
                    },
                    onError -> {
                        Log.e(TAG, onError.getMessage(), onError);
                        util.showOopsSomethingWentWrong(getListView());
                    }
                );

            util.trackAction((App) getApplication(), R.string.analytics_category_req, R.string.analytics_action_get_bus, BUSES_STOP_URL);

            // Preventing keyboard from moving background when showing up
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    @Override
    public final void onStart() {
        super.onStart();
        if (mapFragment == null) {
            final android.app.FragmentManager fm = getFragmentManager();
            final GoogleMapOptions options = new GoogleMapOptions();
            final CameraPosition camera = new CameraPosition(util.chicago(), 7, 0, 0);
            options.camera(camera);
            mapFragment = MapFragment.newInstance(options);
            mapFragment.setRetainInstance(true);
            fm.beginTransaction().replace(R.id.map, mapFragment).commit();
        }
    }

    @Override
    public final void onResume() {
        super.onResume();
        mapFragment.getMapAsync(googleMap -> {
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.getUiSettings().setZoomControlsEnabled(false);
            googleMap.getUiSettings().setMapToolbarEnabled(false);
            util.trackAction((App) getApplication(), R.string.analytics_category_req, R.string.analytics_action_get_bus, BUSES_PATTERN_URL);
            observableUtil.createBusPatternObservable(getApplicationContext(), busRouteId, bound)
                .subscribe(
                    busPattern -> {
                        if (!busPattern.getDirection().equals("error")) {
                            final int center = busPattern.getPoints().size() / 2;
                            final Position position = busPattern.getPoints().get(center).getPosition();
                            if (position.getLatitude() == 0 && position.getLongitude() == 0) {
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(util.chicago(), 10));
                            } else {
                                final LatLng latLng = new LatLng(position.getLatitude(), position.getLongitude());
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 7));
                                googleMap.animateCamera(CameraUpdateFactory.zoomTo(9), 500, null);
                            }
                            drawPattern(busPattern);
                        } else {
                            util.showMessage(this, R.string.message_error_could_not_load_path);
                        }
                    },
                    onError -> {
                        util.handleConnectOrParserException(onError, null, layout, layout);
                        Log.e(TAG, onError.getMessage(), onError);
                    }
                );
        });
    }

    private void drawPattern(@NonNull final BusPattern pattern) {
        mapFragment.getMapAsync(googleMap -> {
            final PolylineOptions poly = new PolylineOptions();
            poly.geodesic(true).color(Color.BLACK);
            poly.width(((App) getApplication()).getLineWidth());
            Stream.of(pattern.getPoints())
                .map(patternPoint -> new LatLng(patternPoint.getPosition().getLatitude(), patternPoint.getPosition().getLongitude()))
                .forEach(poly::add);
            googleMap.addPolyline(poly);
        });
    }

    @Override
    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        busRouteId = savedInstanceState.getString(bundleBusRouteId);
        busRouteName = savedInstanceState.getString(bundleBusRouteName);
        bound = savedInstanceState.getString(bundleBusBound);
        boundTitle = savedInstanceState.getString(bundleBusBoundTitle);
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        savedInstanceState.putString(bundleBusRouteId, busRouteId);
        savedInstanceState.putString(bundleBusRouteName, busRouteName);
        savedInstanceState.putString(bundleBusBound, bound);
        savedInstanceState.putString(bundleBusBoundTitle, boundTitle);
        super.onSaveInstanceState(savedInstanceState);
    }
}
