package fr.cph.chicago.rx.observer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;

import com.annimon.stream.Stream;

import java.util.List;

import fr.cph.chicago.R;
import fr.cph.chicago.core.activity.BikeStationActivity;
import fr.cph.chicago.entity.BikeStation;
import fr.cph.chicago.util.Util;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class BikeAllBikeStationsObserver implements Observer<List<BikeStation>> {

    private final BikeStationActivity activity;
    private final int bikeStationId;
    private final SwipeRefreshLayout swipeRefreshLayout;

    public BikeAllBikeStationsObserver(@NonNull final BikeStationActivity activity, final int bikeStationId, @NonNull final SwipeRefreshLayout swipeRefreshLayout) {
        this.activity = activity;
        this.bikeStationId = bikeStationId;
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    @Override
    public void onSubscribe(Disposable d) {
    }

    @Override
    public void onNext(final List<BikeStation> bikeStations) {
        if (bikeStations != null) {
            Stream.of(bikeStations)
                .filter(station -> bikeStationId == station.getId())
                .findFirst()
                .ifPresent(station -> {
                    activity.refreshStation(station);
                    final Bundle bundle = activity.getIntent().getExtras();
                    bundle.putParcelable(activity.getString(R.string.bundle_bike_station), station);
                });
        } else {
            Util.INSTANCE.showOopsSomethingWentWrong(swipeRefreshLayout);
        }
    }

    @Override
    public void onError(final Throwable throwable) {
        Util.INSTANCE.showOopsSomethingWentWrong(swipeRefreshLayout);
    }

    @Override
    public void onComplete() {
        swipeRefreshLayout.setRefreshing(false);
    }
}
