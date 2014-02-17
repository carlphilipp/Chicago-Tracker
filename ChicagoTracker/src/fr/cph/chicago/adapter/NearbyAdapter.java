package fr.cph.chicago.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils.TruncateAt;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import fr.cph.chicago.ChicagoTracker;
import fr.cph.chicago.R;
import fr.cph.chicago.activity.MainActivity;
import fr.cph.chicago.data.BusData;
import fr.cph.chicago.data.DataHolder;
import fr.cph.chicago.entity.BusArrival;
import fr.cph.chicago.entity.BusStop;
import fr.cph.chicago.entity.Eta;
import fr.cph.chicago.entity.Station;
import fr.cph.chicago.entity.Stop;
import fr.cph.chicago.entity.TrainArrival;
import fr.cph.chicago.entity.enumeration.TrainLine;
import fr.cph.chicago.util.Util;

public final class NearbyAdapter extends BaseAdapter {

	private static final String TAG = "NearbyAdapter";

	private Context context;
	private MainActivity activity;
	private BusData busData;

	private List<BusStop> busStops;
	private SparseArray<Map<String, List<BusArrival>>> busArrivals;
	private SparseArray<TrainArrival> trainArrivals;
	private List<Station> stations;

	/** Google map **/
	private GoogleMap map;
	private List<Marker> markers;

	/** Layouts **/
	private Map<String, Integer> ids;
	private Map<Integer, LinearLayout> layouts;
	private Map<Integer, View> views;

	@SuppressLint("UseSparseArrays")
	public NearbyAdapter(final MainActivity activity) {
		this.context = ChicagoTracker.getAppContext();
		this.activity = activity;
		this.busStops = new ArrayList<BusStop>();
		this.busArrivals = new SparseArray<Map<String, List<BusArrival>>>();
		this.stations = new ArrayList<Station>();
		this.trainArrivals = new SparseArray<TrainArrival>();
		this.busData = DataHolder.getInstance().getBusData();

		this.ids = new HashMap<String, Integer>();
		this.layouts = new HashMap<Integer, LinearLayout>();
		this.views = new HashMap<Integer, View>();
	}

	@Override
	public final int getCount() {
		return busStops.size() + stations.size();
	}

	@Override
	public final Object getItem(int position) {
		// return busStops.get(position);
		return null;
	}

	@Override
	public final long getItemId(int position) {
		// return busStops.get(position).getId();
		return 0;
	}

	@SuppressLint("NewApi")
	@Override
	public final View getView(final int position, View convertView, final ViewGroup parent) {
		LinearLayout.LayoutParams paramsLayout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		LinearLayout.LayoutParams paramsTextView = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		int line1PaddingColor = (int) context.getResources().getDimension(R.dimen.activity_station_stops_line1_padding_color);
		int stopsPaddingTop = (int) context.getResources().getDimension(R.dimen.activity_station_stops_padding_top);

		LayoutInflater vi = (LayoutInflater) ChicagoTracker.getAppContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		convertView = vi.inflate(R.layout.list_nearby, null);

		if (position < stations.size()) {
			final Station station = stations.get(position);

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (map != null) {
						LatLng latLng = new LatLng(station.getStopsPosition().get(0).getLatitude(), station.getStopsPosition().get(0).getLongitude());
						CameraPosition current = new CameraPosition.Builder().target(latLng).zoom(15.5f).bearing(0).tilt(0).build();
						map.animateCamera(CameraUpdateFactory.newCameraPosition(current), Math.max(1000, 1), null);
						for (Marker marker : markers) {
							if (marker.getSnippet().equals(station.getId().toString())) {
								marker.showInfoWindow();
								break;
							}
						}
					}
				}
			});

			final LinearLayout resultLayout;

			if (layouts.containsKey(station.getId())) {
				resultLayout = layouts.get(station.getId());
				convertView = views.get(station.getId());
			} else {
				resultLayout = (LinearLayout) convertView.findViewById(R.id.nearby_results);
				layouts.put(station.getId(), resultLayout);
				views.put(station.getId(), convertView);

				TrainViewHolder holder = new TrainViewHolder();

				TextView routeView = (TextView) convertView.findViewById(R.id.route_name_value);
				routeView.setText(station.getName());
				holder.stationNameView = routeView;

				TextView typeView = (TextView) convertView.findViewById(R.id.train_bus_type);
				typeView.setText("T");
				holder.type = typeView;

				convertView.setTag(holder);
			}

			LinearLayout.LayoutParams paramsArrival = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

			Set<TrainLine> setTL = station.getLines();
			
			// Reset ETAs
			for (int i = 0; i < resultLayout.getChildCount(); i++) {
				LinearLayout layout = (LinearLayout) resultLayout.getChildAt(i);
				LinearLayout layoutChild = (LinearLayout) layout.getChildAt(1);
				for (int j = 0; j < layoutChild.getChildCount(); j++) {
					LinearLayout layoutChildV = (LinearLayout) layoutChild.getChildAt(j);
					TextView timing = (TextView) layoutChildV.getChildAt(1);
					// to delete ?
					if (timing != null) {
						timing.setText("");
					}
				}
			}

			for (TrainLine tl : setTL) {
				if (trainArrivals.get(station.getId()) != null) {
					List<Eta> etas = trainArrivals.get(station.getId()).getEtas(tl);
					if (etas.size() != 0) {
						String key = station.getName() + "_" + tl.toString() + "_h";
						String key2 = station.getName() + "_" + tl.toString() + "_v";
						Integer idLayout = ids.get(key);
						Integer idLayout2 = ids.get(key2);

						LinearLayout llh, llv;
						if (idLayout == null) {
							llh = new LinearLayout(context);
							// llh.setBackgroundResource(R.drawable.border);
							llh.setLayoutParams(paramsLayout);
							llh.setOrientation(LinearLayout.HORIZONTAL);
							llh.setPadding(line1PaddingColor, stopsPaddingTop, 0, 0);
							int id = Util.generateViewId();
							llh.setId(id);
							ids.put(key, id);

							TextView tlView = new TextView(context);
							tlView.setBackgroundColor(tl.getColor());
							tlView.setText("   ");
							tlView.setLayoutParams(paramsTextView);
							llh.addView(tlView);

							llv = new LinearLayout(context);
							llv.setLayoutParams(paramsLayout);
							llv.setOrientation(LinearLayout.VERTICAL);
							llv.setPadding(line1PaddingColor, 0, 0, 0);
							int id2 = Util.generateViewId();
							llv.setId(id2);
							ids.put(key2, id2);

							llh.addView(llv);
							resultLayout.addView(llh);

						} else {
							llh = (LinearLayout) resultLayout.findViewById(idLayout);
							llv = (LinearLayout) resultLayout.findViewById(idLayout2);
						}
						for (Eta eta : etas) {
							Stop stop = eta.getStop();
							String key3 = (station.getName() + "_" + tl.toString() + "_" + stop.getDirection().toString() + "_" + eta.getDestName());
							Integer idLayout3 = ids.get(key3);
							if (idLayout3 == null) {
								LinearLayout insideLayout = new LinearLayout(context);
								insideLayout.setOrientation(LinearLayout.HORIZONTAL);
								insideLayout.setLayoutParams(paramsArrival);
								int newId = Util.generateViewId();
								insideLayout.setId(newId);
								ids.put(key3, newId);

								TextView stopName = new TextView(context);
								stopName.setText(eta.getDestName() + ": ");
								stopName.setTextColor(context.getResources().getColor(R.color.grey_5));
								insideLayout.addView(stopName);

								TextView timing = new TextView(context);
								timing.setText(eta.getTimeLeftDueDelay() + " ");
								timing.setTextColor(context.getResources().getColor(R.color.grey));
								timing.setLines(1);
								timing.setEllipsize(TruncateAt.END);
								insideLayout.addView(timing);

								llv.addView(insideLayout);
							} else {
								// llv can be null sometimes (after a remove from favorites for example)
								if (llv != null) {
									LinearLayout insideLayout = (LinearLayout) llv.findViewById(idLayout3);
									// InsideLayout can be null too if removed before
									TextView timing = (TextView) insideLayout.getChildAt(1);
									timing.setText(timing.getText() + eta.getTimeLeftDueDelay() + " ");
								}
							}
						}
					}
				}
			}
		} else {
			int indice = position - stations.size();
			final BusStop busStop = busStops.get(indice);

			TextView typeView = (TextView) convertView.findViewById(R.id.train_bus_type);
			typeView.setText("B");

			TextView routeView = (TextView) convertView.findViewById(R.id.route_name_value);
			routeView.setText(busStop.getName());

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (map != null) {
						LatLng latLng = new LatLng(busStop.getPosition().getLatitude(), busStop.getPosition().getLongitude());
						CameraPosition current = new CameraPosition.Builder().target(latLng).zoom(15.5f).bearing(0).tilt(0).build();
						map.animateCamera(CameraUpdateFactory.newCameraPosition(current), Math.max(1000, 1), null);
						for (Marker marker : markers) {
							if (marker.getSnippet().equals(busStop.getId().toString())) {
								marker.showInfoWindow();
								break;
							}
						}
					}
				}
			});

			LinearLayout resultLayout = (LinearLayout) convertView.findViewById(R.id.nearby_results);

			if (busArrivals.size() > 0) {
				for (Entry<String, List<BusArrival>> entry : busArrivals.get(busStop.getId()).entrySet()) {
					LinearLayout llh = new LinearLayout(context);
					llh.setLayoutParams(paramsLayout);
					llh.setOrientation(LinearLayout.HORIZONTAL);
					llh.setPadding(line1PaddingColor, stopsPaddingTop, 0, 0);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						llh.setBackground(context.getResources().getDrawable(R.drawable.any_selector));
					}

					TextView tlView = new TextView(context);
					tlView.setBackgroundColor(context.getResources().getColor(R.color.black));
					tlView.setText("   ");
					tlView.setLayoutParams(paramsTextView);
					llh.addView(tlView);

					final String key = entry.getKey();
					final List<BusArrival> value = entry.getValue();

					LinearLayout stopLayout = new LinearLayout(context);
					stopLayout.setOrientation(LinearLayout.VERTICAL);
					stopLayout.setPadding(line1PaddingColor, 0, 0, 0);

					String key2 = key;
					List<BusArrival> buses = value;

					LinearLayout boundLayout = new LinearLayout(context);
					boundLayout.setOrientation(LinearLayout.HORIZONTAL);

					TextView bound = new TextView(context);
					String routeId = busData.getRoute(buses.get(0).getRouteId()).getId();
					bound.setText(routeId + " (" + key2 + "): ");
					bound.setTextColor(context.getResources().getColor(R.color.grey_5));
					boundLayout.addView(bound);

					for (BusArrival arri : buses) {
						TextView timeView = new TextView(context);
						timeView.setText(arri.getTimeLeftDueDelay() + " ");
						timeView.setTextColor(context.getResources().getColor(R.color.grey));
						timeView.setLines(1);
						timeView.setEllipsize(TruncateAt.END);
						boundLayout.addView(timeView);
					}
					stopLayout.addView(boundLayout);
					llh.addView(stopLayout);
					resultLayout.addView(llh);
				}
			}
		}

		return convertView;
	}

	static class TrainViewHolder {
		TextView stationNameView;
		TextView type;
	}

	public final void updateData(final List<BusStop> busStops, final SparseArray<Map<String, List<BusArrival>>> busArrivals,
			final List<Station> stations, final SparseArray<TrainArrival> trainArrivals, final GoogleMap map, final List<Marker> markers) {
		this.busStops = busStops;
		this.busArrivals = busArrivals;
		this.stations = stations;
		this.trainArrivals = trainArrivals;
		this.map = map;
		this.markers = markers;
	}

}