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

package fr.cph.chicago.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;

import android.util.Log;
import au.com.bytecode.opencsv.CSVReader;
import fr.cph.chicago.ChicagoTracker;
import fr.cph.chicago.connection.CtaConnect;
import fr.cph.chicago.connection.CtaRequestType;
import fr.cph.chicago.entity.BusRoute;
import fr.cph.chicago.entity.BusStop;
import fr.cph.chicago.entity.Position;
import fr.cph.chicago.exception.ConnectException;
import fr.cph.chicago.exception.ParserException;
import fr.cph.chicago.xml.Xml;

public class BusData {

	/** Tag **/
	private static final String TAG = "BusData";

	private List<BusRoute> routes;
	private List<BusStop> stops;

	private static BusData busData;

	public static BusData getInstance() {
		if (busData == null) {
			busData = new BusData();
		}
		return busData;
	}

	private BusData() {
		this.routes = new ArrayList<BusRoute>();
		this.stops = new ArrayList<BusStop>();
	}

	public final List<BusStop> readBusStops() {
		if (stops.size() == 0) {
			try {
				CSVReader reader = new CSVReader(new InputStreamReader(ChicagoTracker.getAppContext().getAssets().open("stops.txt")));
				reader.readNext();
				String[] row = null;
				while ((row = reader.readNext()) != null) {
					int locationType = Integer.valueOf(row[6]);// location_type
					if (locationType == 0) {

						Integer stopId = Integer.valueOf(row[0]); // stop_id
						// String stopCode = TrainDirection.fromString(row[1]); // stop_code
						String stopName = row[2]; // stop_name
						// String stopDesc = row[3]; // stop_desc
						Double latitude = Double.valueOf(row[4]);// stop_lat
						Double longitude = Double.valueOf(row[5]);// stop_lon

						BusStop busStop = new BusStop();
						busStop.setId(stopId);
						busStop.setName(stopName);
						Position positon = new Position();
						positon.setLatitude(latitude);
						positon.setLongitude(longitude);
						busStop.setPosition(positon);

						stops.add(busStop);
					}
				}
				reader.close();
				order();
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
		return stops;
	}

	private void order() {
		if (stops.size() != 0) {
			Collections.sort(stops);
		}
	}

	public final List<BusRoute> loadBusRoutes() throws ParserException, ConnectException {
		if (routes.size() == 0) {
			MultiMap<String, String> params = new MultiValueMap<String, String>();
			CtaConnect connect = CtaConnect.getInstance();
			Xml xml = new Xml();
			String xmlResult = connect.connect(CtaRequestType.BUS_ROUTES, params);
			routes = xml.parseBusRoutes(xmlResult);
		}
		return routes;
	}

	public List<BusRoute> getRoutes() {
		return routes;
	}

	public final int getRouteSize() {
		return routes.size();
	}

	public final BusRoute getRoute(final int position) {
		return routes.get(position);
	}

	public final BusRoute getRoute(final String routeId) {
		BusRoute result = null;
		for (BusRoute br : routes) {
			if (br.getId().equals(routeId)) {
				result = br;
				break;
			}
		}
		return result;
	}

	public final List<BusStop> loadBusStop(final String stopId, final String bound) throws ConnectException, ParserException {
		CtaConnect connect = CtaConnect.getInstance();
		MultiMap<String, String> param = new MultiValueMap<String, String>();
		param.put("rt", stopId);
		param.put("dir", bound);
		List<BusStop> busStops = null;
		String xmlResult = connect.connect(CtaRequestType.BUS_STOP_LIST, param);
		Xml xml = new Xml();
		busStops = xml.parseBusBounds(xmlResult);
		return busStops;
	}

	public final List<BusStop> readAllBusStops() {
		return this.stops;
	}

	public final BusStop readOneBus(int id){
		BusStop res = null;
		for (BusStop busStop : stops) {
			if(busStop.getId().intValue() == id){
				res = busStop;
				break;
			}
		}
		return res;
	}
	
	public final List<BusStop> readNearbyStops(Position position) {
		
		final double dist = 0.004472;
		
		List<BusStop> res = new ArrayList<BusStop>();
		double latitude = position.getLatitude();
		double longitude = position.getLongitude();

		double latMax = latitude + dist;
		double latMin = latitude - dist;
		double lonMax = longitude + dist;
		double lonMin = longitude - dist;

		for (BusStop busStop : stops) {
			double busLatitude = busStop.getPosition().getLatitude();
			double busLongitude = busStop.getPosition().getLongitude();
			if (busLatitude <= latMax && busLatitude >= latMin && busLongitude <= lonMax && busLongitude >= lonMin) {
				res.add(busStop);
				Log.i(TAG, busStop.toString());
			}

		}
		Log.i(TAG, "size: " + res.size() + "");
		return res;
	}
}
