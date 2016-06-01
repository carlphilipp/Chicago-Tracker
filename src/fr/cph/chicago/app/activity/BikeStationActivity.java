/**
 * Copyright 2016 Carl-Philipp Harmant
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

package fr.cph.chicago.app.activity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.annimon.stream.Stream;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.cph.chicago.R;
import fr.cph.chicago.app.App;
import fr.cph.chicago.app.listener.GoogleMapDirectionOnClickListener;
import fr.cph.chicago.app.listener.GoogleMapOnClickListener;
import fr.cph.chicago.app.listener.GoogleStreetOnClickListener;
import fr.cph.chicago.data.Preferences;
import fr.cph.chicago.entity.BikeStation;
import fr.cph.chicago.entity.enumeration.TrainLine;
import fr.cph.chicago.rx.observable.ObservableUtil;
import fr.cph.chicago.rx.subscriber.BikeAllBikeStationsSubscriber;
import fr.cph.chicago.util.Util;

/**
 * Activity the list of train stations
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
public class BikeStationActivity extends AbstractStationActivity {

    @BindView(R.id.activity_station_swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.activity_favorite_star) ImageView favoritesImage;
    @BindView(R.id.activity_bike_station_streetview_image) ImageView streetViewImage;
    @BindView(R.id.activity_bike_station_steetview_text) TextView streetViewText;
    @BindView(R.id.activity_map_image) ImageView mapImage;
    @BindView(R.id.activity_map_direction) ImageView directionImage;
    @BindView(R.id.map_container) LinearLayout mapContainer;
    @BindView(R.id.walk_container) LinearLayout walkContainer;
    @BindView(R.id.favorites_container) LinearLayout favoritesImageContainer;
    @BindView(R.id.activity_bike_station_value) TextView bikeStationValue;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.favorites_bikes_list) LinearLayout container;

    private BikeStation bikeStation;
    private boolean isFavorite;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!this.isFinishing()) {
            setContentView(R.layout.activity_bike_station);
            ButterKnife.bind(this);
            bikeStation = getIntent().getExtras().getParcelable(getString(R.string.bundle_bike_station));
            if (bikeStation != null) {
                final double latitude = bikeStation.getLatitude();
                final double longitude = bikeStation.getLongitude();

                swipeRefreshLayout.setOnRefreshListener(
                    () -> ObservableUtil.createAllBikeStationsObservable(getApplicationContext())
                        .subscribe(new BikeAllBikeStationsSubscriber(BikeStationActivity.this, bikeStation.getId(), swipeRefreshLayout))
                );

                isFavorite = isFavorite();

                // Call google street api to load image
                createGoogleStreetObservable(latitude, longitude);
                subscribeToGoogleStreet(streetViewImage, streetViewText);

                mapImage.setColorFilter(ContextCompat.getColor(this, R.color.grey_5));
                directionImage.setColorFilter(ContextCompat.getColor(this, R.color.grey_5));

                if (isFavorite) {
                    favoritesImage.setColorFilter(ContextCompat.getColor(this, R.color.yellowLineDark));
                } else {
                    favoritesImage.setColorFilter(ContextCompat.getColor(this, R.color.grey_5));
                }

                favoritesImageContainer.setOnClickListener(view -> BikeStationActivity.this.switchFavorite());
                bikeStationValue.setText(bikeStation.getStAddress1());
                streetViewImage.setOnClickListener(new GoogleStreetOnClickListener(latitude, longitude));
                mapContainer.setOnClickListener(new GoogleMapOnClickListener(latitude, longitude));
                walkContainer.setOnClickListener(new GoogleMapDirectionOnClickListener(latitude, longitude));

                drawData();
            }
            setToolBar();
        }
    }

    private void setToolBar() {
        toolbar.inflateMenu(R.menu.main);
        toolbar.setOnMenuItemClickListener((item -> {
            swipeRefreshLayout.setRefreshing(true);
            ObservableUtil.createAllBikeStationsObservable(getApplicationContext())
                .subscribe(new BikeAllBikeStationsSubscriber(BikeStationActivity.this, bikeStation.getId(), swipeRefreshLayout));
            return false;
        }));
        Util.setWindowsColor(this, toolbar, TrainLine.NA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(4);
        }
        toolbar.setTitle(bikeStation.getName());
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setOnClickListener(v -> finish());
    }

    private void drawData() {
        final Context context = getApplicationContext();

        final LinearLayout availableLayout = new LinearLayout(context);
        final LinearLayout availableBikes = new LinearLayout(context);
        final LinearLayout availableDocks = new LinearLayout(context);

        final TextView availableBike = new TextView(context);
        final TextView availableDock = new TextView(context);
        final TextView amountBike = new TextView(context);
        final TextView amountDock = new TextView(context);

        container.removeAllViews();
        container.setOrientation(LinearLayout.HORIZONTAL);
        availableLayout.setOrientation(LinearLayout.VERTICAL);
        availableBikes.setOrientation(LinearLayout.HORIZONTAL);
        availableBike.setText(getResources().getText(R.string.bike_available_bikes));
        availableBike.setTextColor(ContextCompat.getColor(context, R.color.grey_5));
        availableBikes.addView(availableBike);
        amountBike.setText(String.valueOf(bikeStation.getAvailableBikes()));
        if (bikeStation.getAvailableBikes() == 0) {
            amountBike.setTextColor(ContextCompat.getColor(context, R.color.red));
        } else {
            amountBike.setTextColor(ContextCompat.getColor(context, R.color.green));
        }
        availableBikes.addView(amountBike);
        availableLayout.addView(availableBikes);
        availableDocks.setOrientation(LinearLayout.HORIZONTAL);
        availableDock.setText(getResources().getText(R.string.bike_available_docks));
        availableDock.setTextColor(ContextCompat.getColor(context, R.color.grey_5));
        availableDocks.addView(availableDock);
        amountDock.setText(String.valueOf(bikeStation.getAvailableDocks()));
        if (bikeStation.getAvailableDocks() == 0) {
            amountDock.setTextColor(ContextCompat.getColor(context, R.color.red));
        } else {
            amountDock.setTextColor(ContextCompat.getColor(context, R.color.green));
        }
        availableDocks.addView(amountDock);
        availableLayout.addView(availableDocks);
        container.addView(availableLayout);
    }

    @Override
    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        bikeStation = savedInstanceState.getParcelable(getString(R.string.bundle_bike_station));
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        savedInstanceState.putParcelable(getString(R.string.bundle_bike_station), bikeStation);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Is favorite or not ?
     *
     * @return if the station is favorite
     */
    @Override
    protected boolean isFavorite() {
        final List<String> favorites = Preferences.getBikeFavorites(getApplicationContext(), App.PREFERENCE_FAVORITES_BIKE);
        return Stream.of(favorites)
            .filter(favorite -> Integer.valueOf(favorite) == bikeStation.getId())
            .findFirst()
            .isPresent();
    }

    public void refreshStation(@NonNull final BikeStation station) {
        this.bikeStation = station;
        drawData();
    }

    /**
     * Add/remove favorites
     */
    private void switchFavorite() {
        if (isFavorite) {
            Util.removeFromBikeFavorites(bikeStation.getId(), swipeRefreshLayout);
            favoritesImage.setColorFilter(ContextCompat.getColor(this, R.color.grey_5));
            isFavorite = false;
        } else {
            Util.addToBikeFavorites(bikeStation.getId(), swipeRefreshLayout);
            Preferences.addBikeRouteNameMapping(getApplicationContext(), Integer.toString(bikeStation.getId()), bikeStation.getName());
            favoritesImage.setColorFilter(ContextCompat.getColor(this, R.color.yellowLineDark));
            isFavorite = true;
        }
    }
}
