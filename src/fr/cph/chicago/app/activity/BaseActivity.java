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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import fr.cph.chicago.app.App;
import fr.cph.chicago.R;
import fr.cph.chicago.data.BusData;
import fr.cph.chicago.data.DataHolder;
import fr.cph.chicago.data.TrainData;
import fr.cph.chicago.entity.BusArrival;
import fr.cph.chicago.entity.TrainArrival;
import fr.cph.chicago.rx.observable.ObservableUtil;
import fr.cph.chicago.service.BusService;
import fr.cph.chicago.service.TrainService;
import fr.cph.chicago.service.impl.BusServiceImpl;
import fr.cph.chicago.service.impl.TrainServiceImpl;
import fr.cph.chicago.util.Util;
import fr.cph.chicago.entity.dto.FavoritesDTO;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * This class represents the base activity of the application It will load the loading screen and/or the main
 * activity
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
public class BaseActivity extends Activity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private final TrainService trainService;
    private final BusService busService;

    public BaseActivity() {
        trainService = new TrainServiceImpl();
        busService = new BusServiceImpl();
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading);
        loadLocalAndFavoritesData();
        trackWithGoogleAnalytics();
    }

    private void loadLocalAndFavoritesData() {

        // Train local data
        final Observable<TrainData> trainDataObservable = Observable.create(
            (Subscriber<? super TrainData> subscriber) -> {
                subscriber.onNext(trainService.loadLocalTrainData(getApplicationContext()));
                subscriber.onCompleted();
            })
            .doOnNext(onNextTrainData -> DataHolder.getInstance().setTrainData(onNextTrainData))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());

        // Bus local data
        final Observable<BusData> busDataObservable = Observable.create(
            (Subscriber<? super BusData> subscriber) -> {
                subscriber.onNext(busService.loadLocalBusData(getApplicationContext()));
                subscriber.onCompleted();
            })
            .doOnNext(onNextBusData -> DataHolder.getInstance().setBusData(onNextBusData))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());

        // Train online favorites
        final Observable<SparseArray<TrainArrival>> trainArrivalsObservable = ObservableUtil.createTrainArrivals(getApplicationContext());

        // Bus online favorites
        final Observable<List<BusArrival>> busArrivalsObservable = ObservableUtil.createBusArrivals(getApplicationContext());

        // Run local first and then online: Ensure that local data is loaded first
        Observable.zip(trainDataObservable, busDataObservable, (trainData, busData) -> true)
            .doOnCompleted(() -> Observable.zip(trainArrivalsObservable, busArrivalsObservable, (trainArrivals, busArrivals) -> {
                    App.modifyLastUpdate(Calendar.getInstance().getTime());
                    final FavoritesDTO favoritesDTO = new FavoritesDTO();
                    favoritesDTO.setTrainArrivals(trainArrivals);
                    favoritesDTO.setBusArrivals(busArrivals);
                    return favoritesDTO;
                }
            ).subscribe(
                BaseActivity.this::startMainActivity,
                onError -> {
                    Log.e(TAG, onError.getMessage(), onError);
                    displayError("Oops, something went wrong!");
                }
            )).subscribe();
    }

    private void trackWithGoogleAnalytics() {
        Util.trackAction(this, R.string.analytics_category_req, R.string.analytics_action_get_train, R.string.url_train_arrivals, 0);
        Util.trackAction(this, R.string.analytics_category_req, R.string.analytics_action_get_bus, R.string.url_bus_arrival, 0);
    }

    /**
     * Finish current activity and start main activity with custom transition
     *
     * @param result the trains and buses arrivals
     */
    private void startMainActivity(@NonNull final FavoritesDTO result) {
        final Intent intent = new Intent(this, MainActivity.class);
        final Bundle bundle = new Bundle();
        final SparseArray<TrainArrival> trainArrival = result.getTrainArrivals() != null ? result.getTrainArrivals() : new SparseArray<>();
        final List<BusArrival> busArrivals = result.getBusArrivals() != null ? result.getBusArrivals() : new ArrayList<>();
        bundle.putParcelableArrayList(getString(R.string.bundle_bus_arrivals), (ArrayList<BusArrival>) busArrivals);
        bundle.putSparseParcelableArray(getString(R.string.bundle_train_arrivals), trainArrival);
        intent.putExtras(bundle);
        // TODO add her some stuff in bundle to handle errors from observable

        finish();
        startActivity(intent);
    }

    private void displayError(@NonNull final String message) {
        DataHolder.getInstance().setTrainData(null);
        DataHolder.getInstance().setBusData(null);
        App.startErrorActivity(this, message);
    }
}