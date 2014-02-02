/**
 * Copyright 2014 Carl-Philipp Harmant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cph.chicago.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.SparseArray;
import fr.cph.chicago.ChicagoTracker;
import fr.cph.chicago.data.BusData;
import fr.cph.chicago.data.DataHolder;
import fr.cph.chicago.data.Preferences;
import fr.cph.chicago.data.TrainData;
import fr.cph.chicago.util.Util;

public class VehiculeArrival {

	/** Tag **/
	private static final String TAG = "VehiculeArrival";

	private SparseArray<TrainArrival> trainArrivals;
	private List<BusArrival> busArrivals;

	private List<Integer> trainFavorites;
	private List<String> busFavorites;
	private List<String> fakeBusFavorites;

	private TrainData trainData;
	private BusData busData;

	public VehiculeArrival() {
		this.trainArrivals = new SparseArray<TrainArrival>();
		this.busArrivals = new ArrayList<BusArrival>();

		this.trainFavorites = new ArrayList<Integer>();
		this.busFavorites = new ArrayList<String>();
		this.fakeBusFavorites = new ArrayList<String>();

		this.trainData = DataHolder.getInstance().getTrainData();
		this.busData = DataHolder.getInstance().getBusData();
	}

	public final int size() {
		return trainFavorites.size() + fakeBusFavorites.size();
	}

	public final Object getObject(final int position) {
		Object result = null;
		if (position < trainFavorites.size()) {
			Integer stationId = trainFavorites.get(position);
			result = trainData.getStation(stationId);
		} else {
			int indice = position - trainFavorites.size();
			if (indice < fakeBusFavorites.size()) {
				String res[] = Util.decodeBusFavorite(fakeBusFavorites.get(indice));
				return busData.getRoute(res[0]);
			}
		}
		return result;
	}

	public final TrainArrival getTrainArrival(final Integer stationId) {
		return trainArrivals.get(stationId);
	}

	public final List<BusArrival> getBusArrivals(final String routeId) {
		List<BusArrival> res = new ArrayList<BusArrival>();
		for (BusArrival busArrival : busArrivals) {
			if (busArrival.getRouteId().equals(routeId)) {
				res.add(busArrival);
			}
		}
		return res;
	}

	public final BusArrival getOneBusArrival(final String routeId) {
		BusArrival bus = null;
		for (BusArrival busArrival : busArrivals) {
			if (busArrival.getRouteId().equals(routeId)) {
				bus = busArrival;
				break;
			}
		}
		return bus;
	}

	public final Map<String, Map<String, List<BusArrival>>> getBusArrivalsMapped(final String routeId) {
		Map<String, Map<String, List<BusArrival>>> res = new HashMap<String, Map<String, List<BusArrival>>>();
		for (BusArrival busArrival : busArrivals) {
			String bound = busArrival.getRouteDirection();
			if (isInFavorites(routeId, bound)) {
				if (busArrival.getRouteId().equals(routeId)) {
					if (res.containsKey(busArrival.getStopName())) {
						Map<String, List<BusArrival>> tempMap = res.get(busArrival.getStopName());
						if (tempMap.containsKey(bound)) {
							List<BusArrival> arrivals = tempMap.get(busArrival.getRouteDirection());
							arrivals.add(busArrival);
						} else {
							List<BusArrival> arrivals = new ArrayList<BusArrival>();
							arrivals.add(busArrival);
							tempMap.put(bound, arrivals);
						}
					} else {
						Map<String, List<BusArrival>> tempMap = new HashMap<String, List<BusArrival>>();
						List<BusArrival> arrivals = new ArrayList<BusArrival>();
						arrivals.add(busArrival);
						tempMap.put(bound, arrivals);
						res.put(busArrival.getStopName(), tempMap);
					}
				}
			}
		}
		return res;
	}

	private final boolean isInFavorites(final String routeId, final String bound) {
		boolean res = false;
		for (String fav : busFavorites) {
			String decoded[] = Util.decodeBusFavorite(fav);
			if (routeId.equals(decoded[0]) && bound.equals(decoded[2])) {
				res = true;
				break;
			}
		}
		return res;
	}

	public final void setTrainArrival(final SparseArray<TrainArrival> trainArrival) {
		this.trainArrivals = trainArrival;
	}

	public final void setBusArrivals(final List<BusArrival> busArrivals) {
		this.busArrivals = busArrivals;
	}

	public final void setFavorites() {
		this.trainFavorites = Preferences.getTrainFavorites(ChicagoTracker.PREFERENCE_FAVORITES_TRAIN);
		this.busFavorites = Preferences.getBusFavorites(ChicagoTracker.PREFERENCE_FAVORITES_BUS);
		this.fakeBusFavorites = calculateaActualRouteNumberBusFavorites();
	}

	public final void setArrivals(final SparseArray<TrainArrival> trainArrivals, final List<BusArrival> busArrivals) {
		this.trainArrivals.clear();
		this.trainArrivals = trainArrivals;
		removeDuplicates(busArrivals);
		this.busArrivals.clear();
		this.busArrivals = busArrivals;
		setFavorites();
	}

	private final void removeDuplicates(final List<BusArrival> busArrivals) {
		Set<BusArrival> stBusArrivals = new LinkedHashSet<BusArrival>(busArrivals);
		busArrivals.clear();
		busArrivals.addAll(stBusArrivals);
	}

	public final List<String> calculateaActualRouteNumberBusFavorites() {
		List<String> found = new ArrayList<String>();
		List<String> favs = new ArrayList<String>();
		for (String fav : busFavorites) {
			String[] decoded = Util.decodeBusFavorite(fav);
			if (!found.contains(decoded[0])) {
				found.add(decoded[0]);
				favs.add(fav);
			}
		}
		return favs;
	}
}
