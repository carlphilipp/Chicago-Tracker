package fr.cph.chicago.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import fr.cph.chicago.ChicagoTracker;
import fr.cph.chicago.R;
import fr.cph.chicago.activity.MainActivity;
import fr.cph.chicago.adapter.NearbyAdapter;
import fr.cph.chicago.connection.CtaConnect;
import fr.cph.chicago.connection.CtaRequestType;
import fr.cph.chicago.data.BusData;
import fr.cph.chicago.data.DataHolder;
import fr.cph.chicago.data.TrainData;
import fr.cph.chicago.entity.BusArrival;
import fr.cph.chicago.entity.BusStop;
import fr.cph.chicago.entity.Position;
import fr.cph.chicago.entity.Station;
import fr.cph.chicago.entity.TrainArrival;
import fr.cph.chicago.exception.ConnectException;
import fr.cph.chicago.exception.ParserException;
import fr.cph.chicago.exception.TrackerException;
import fr.cph.chicago.xml.Xml;

public class NearbyFragment extends Fragment {

	private static final String TAG = "NearbyFragment";

	private MainActivity mActivity;

	private MapFragment mapFragment;
	private View loadLayout;
	private GoogleMap map;
	private NearbyAdapter ada;
	private ListView listView;

	private static final LatLng CHICAGO = new LatLng(41.8819, -87.6278);

	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static NearbyFragment newInstance(final int sectionNumber) {
		NearbyFragment fragment = new NearbyFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public final void onAttach(final Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity) activity;
		((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
	}

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);
		ada = new NearbyAdapter(mActivity);
		listView = (ListView) rootView.findViewById(R.id.fragment_nearby_list);
		listView.setAdapter(ada);
		setHasOptionsMenu(true);
		loadLayout = rootView.findViewById(R.id.loading_layout);
		showProgress(true, null);
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		FragmentManager fm = getFragmentManager();
		mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
		mapFragment = MapFragment.newInstance();
		mapFragment.setRetainInstance(true);
		fm.beginTransaction().replace(R.id.map, mapFragment).commit();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (map == null) {
			map = mapFragment.getMap();
		}
		new LoadNearby().execute();
	}

	/**
	 * Load error
	 * 
	 */
	public final void displayError(final TrackerException exceptionToBeThrown) {
		DataHolder.getInstance().setTrainData(null);
		DataHolder.getInstance().setBusData(null);
		ChicagoTracker.displayError(mActivity, exceptionToBeThrown);
		mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}

	private final class LoadArrivals extends AsyncTask<List<?>, Void, Void> {

		private SparseArray<Map<String, List<BusArrival>>> busArrivalsMap;
		private SparseArray<TrainArrival> trainArrivals;
		private List<BusStop> busStops;
		private List<Station> stations;

		@SuppressWarnings("unchecked")
		@Override
		protected Void doInBackground(List<?>... params) {
			busStops = (List<BusStop>) params[0];
			stations = (List<Station>) params[1];

			busArrivalsMap = new SparseArray<Map<String, List<BusArrival>>>();
			trainArrivals = new SparseArray<TrainArrival>();

			CtaConnect cta = CtaConnect.getInstance();

			// Loop over bus stops around user
			for (BusStop busStop : busStops) {
				Map<String, List<BusArrival>> tempMap;

				// Create
				tempMap = busArrivalsMap.get(busStop.getId(), null);
				if (tempMap == null) {
					tempMap = new HashMap<String, List<BusArrival>>();
					busArrivalsMap.put(busStop.getId(), tempMap);
				}

				// Buses
				try {

					MultiMap<String, String> reqParams = new MultiValueMap<String, String>();
					reqParams.put("stpid", busStop.getId().toString());

					String xmlRes = cta.connect(CtaRequestType.BUS_ARRIVALS, reqParams);
					Xml xml = new Xml();
					List<BusArrival> busArrivals = xml.parseBusArrivals(xmlRes);
					for (BusArrival busArrival : busArrivals) {
						String direction = busArrival.getRouteDirection();
						if (tempMap.containsKey(direction)) {
							List<BusArrival> temp = tempMap.get(direction);
							temp.add(busArrival);
						} else {
							List<BusArrival> temp = new ArrayList<BusArrival>();
							temp.add(busArrival);
							tempMap.put(direction, temp);
						}
					}
				} catch (ConnectException e) {
					Log.e(TAG, e.getMessage(), e);
				} catch (ParserException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}

			// Train
			for (Station station : stations) {
				try {
					MultiMap<String, String> reqParams = new MultiValueMap<String, String>();
					reqParams.put("mapid", String.valueOf(station.getId()));
					String xmlRes = cta.connect(CtaRequestType.TRAIN_ARRIVALS, reqParams);
					Xml xml = new Xml();
					SparseArray<TrainArrival> temp = xml.parseArrivals(xmlRes, DataHolder.getInstance().getTrainData());
					for (int j = 0; j < temp.size(); j++) {
						trainArrivals.put(temp.keyAt(j), temp.valueAt(j));
					}
				} catch (ConnectException e) {
					Log.e(TAG, e.getMessage(), e);
				} catch (ParserException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
			return null;
		}

		@Override
		protected final void onPostExecute(Void result) {
			load(busStops, busArrivalsMap, stations, trainArrivals);
		}
	}

	/**
	 * Load nearby data
	 * 
	 * @author Carl-Philipp Harmant
	 * 
	 */
	private final class LoadNearby extends AsyncTask<Void, Void, Void> implements LocationListener {

		private static final String TAG = "LoadNearby";

		// The minimum distance to change Updates in meters
		private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

		// The minimum time between updates in milliseconds
		private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

		// flag for GPS status
		boolean isGPSEnabled = false;
		// flag for network status
		boolean isNetworkEnabled = false;

		boolean canGetLocation = false;

		private Location location;
		private Position position;
		private double latitude; // latitude
		private double longitude; // longitude

		private List<BusStop> busStops;
		private List<Station> trainStations;

		@Override
		protected final Void doInBackground(final Void... params) {
			busStops = new ArrayList<BusStop>();
			trainStations = new ArrayList<Station>();

			DataHolder dataHolder = DataHolder.getInstance();
			BusData busData = dataHolder.getBusData();
			TrainData trainData = dataHolder.getTrainData();

			LocationManager locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

			// getting GPS status
			isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// getting network status
			isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (!isGPSEnabled && !isNetworkEnabled) {
				// no network provider is enabled
				showSettingsAlert();
			} else {
				this.canGetLocation = true;
				if (isNetworkEnabled) {
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES,
							this, Looper.getMainLooper());
					if (locationManager != null) {
						location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						if (location != null) {
							latitude = location.getLatitude();
							longitude = location.getLongitude();
						}
					}
				}
				// if GPS Enabled get lat/long using GPS Services
				if (isGPSEnabled) {
					if (location == null) {
						locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES,
								this, Looper.getMainLooper());
						if (locationManager != null) {
							location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							if (location != null) {
								latitude = location.getLatitude();
								longitude = location.getLongitude();
							}
						}
					}
				}
				position = new Position();
				position.setLatitude(latitude);
				position.setLongitude(longitude);

				busStops = busData.readNearbyStops(position);

				trainStations = trainData.readNearbyStation(position);
			}
			return null;
		}

		@Override
		protected final void onPostExecute(Void result) {
			new LoadArrivals().execute(busStops, trainStations);
			centerMap(position);
		}

		@Override
		public final void onLocationChanged(final Location location) {
		}

		@Override
		public final void onProviderDisabled(final String provider) {
		}

		@Override
		public final void onProviderEnabled(final String provider) {
		}

		@Override
		public final void onStatusChanged(final String provider, final int status, final Bundle extras) {
		}

		/**
		 * Function to show settings alert dialog On pressing Settings button will lauch Settings Options
		 * */
		public void showSettingsAlert() {
			new Thread() {
				public void run() {
					NearbyFragment.this.mActivity.runOnUiThread(new Runnable() {
						public void run() {
							AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(NearbyFragment.this.getActivity());
							alertDialogBuilder.setTitle("GPS is settings");
							alertDialogBuilder.setMessage("GPS is not enabled. Do you want to go to settings menu?");
							alertDialogBuilder.setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									NearbyFragment.this.getActivity().startActivity(intent);
								}
							}).setNegativeButton("No", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.cancel();
								}
							});
							AlertDialog alertDialog = alertDialogBuilder.create();
							alertDialog.show();
						}
					});
				}
			}.start();
		}
	}

	public final void centerMap(final Position positon) {
		while (mapFragment.getMap() == null) {
		}
		map = mapFragment.getMap();
		map.setMyLocationEnabled(true);
		LatLng latLng = null;
		if (positon != null) {
			latLng = new LatLng(positon.getLatitude(), positon.getLongitude());
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
		} else {
			latLng = CHICAGO;
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
		}
		// map.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
	}

	private final void load(final List<BusStop> buses, final SparseArray<Map<String, List<BusArrival>>> busArrivals, final List<Station> stations,
			final SparseArray<TrainArrival> trainArrivals) {
		List<Marker> markers = new ArrayList<Marker>();
		for (BusStop busStop : buses) {
			LatLng point = new LatLng(busStop.getPosition().getLatitude(), busStop.getPosition().getLongitude());
			Marker marker = map.addMarker(new MarkerOptions().position(point).title(busStop.getName()).snippet(busStop.getId().toString())
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
			markers.add(marker);
		}
		for (Station station : stations) {
			for (Position position : station.getStopsPosition()) {
				LatLng point = new LatLng(position.getLatitude(), position.getLongitude());
				Marker marker = map.addMarker(new MarkerOptions().position(point).title(station.getName()).snippet(station.getId().toString())
						.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
				markers.add(marker);
			}
		}
		addClickEventsToMarkers(buses, stations);
		ada.updateData(buses, busArrivals, stations, trainArrivals, map, markers);
		ada.notifyDataSetChanged();
		showProgress(false, null);
		listView.setVisibility(View.VISIBLE);

	}

	private void addClickEventsToMarkers(final List<BusStop> busStops, final List<Station> stations) {
		map.setOnMarkerClickListener(new OnMarkerClickListener() {

			@Override
			public boolean onMarkerClick(Marker marker) {
				boolean found = false;
				for (int i = 0; i < stations.size(); i++) {
					if (marker.getSnippet().equals(stations.get(i).getId().toString())) {
						listView.smoothScrollToPosition(i);
						found = true;
						break;
					}
				}
				if (!found) {
					for (int i = 0; i < busStops.size(); i++) {
						int indice = i + stations.size();
						if (marker.getSnippet().equals(busStops.get(i).getId().toString())) {
							listView.smoothScrollToPosition(indice);
							break;
						}
					}
				}
				return false;
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private final void showProgress(final boolean show, final String errorMessage) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
				int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
				loadLayout.setVisibility(View.VISIBLE);
				loadLayout.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						loadLayout.setVisibility(show ? View.VISIBLE : View.GONE);
					}
				});
			} else {
				loadLayout.setVisibility(show ? View.VISIBLE : View.GONE);
			}
			mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		} catch (IllegalStateException e) {
			Log.w(TAG, e.getMessage(), e);
		}
	}

	public final void reloadData() {
		map.clear();
		showProgress(true, null);
		listView.setVisibility(View.GONE);
		new LoadNearby().execute();
	}

}
