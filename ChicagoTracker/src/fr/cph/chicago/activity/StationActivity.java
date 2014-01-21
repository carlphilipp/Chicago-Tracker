package fr.cph.chicago.activity;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import fr.cph.chicago.ChicagoTracker;
import fr.cph.chicago.R;
import fr.cph.chicago.connection.CtaConnect;
import fr.cph.chicago.connection.CtaRequestType;
import fr.cph.chicago.connection.GStreetViewConnect;
import fr.cph.chicago.data.DataHolder;
import fr.cph.chicago.data.Preferences;
import fr.cph.chicago.data.TrainData;
import fr.cph.chicago.entity.Eta;
import fr.cph.chicago.entity.Position;
import fr.cph.chicago.entity.Station;
import fr.cph.chicago.entity.Stop;
import fr.cph.chicago.entity.TrainArrival;
import fr.cph.chicago.entity.enumeration.TrainDirection;
import fr.cph.chicago.entity.enumeration.TrainLine;
import fr.cph.chicago.util.Util;
import fr.cph.chicago.xml.Xml;

public class StationActivity extends Activity {

	/** Tag **/
	private static final String TAG = "StationActivity";

	private TrainData data;

	private Integer stationId;

	private ImageView streetViewImage;

	private ImageView mapImage;

	private ImageView directionImage;

	private ImageView favoritesImage;

	private boolean isFavorite;

	private TrainArrival arrival;

	private Map<String, Integer> ids;

	// private ArrayList<View> viewListToRemove = new ArrayList<View>();

	private Station station;

	private LinearLayout.LayoutParams paramsStop;

	private Menu menu;

	private boolean firstLoad = true;

	// private MenuItem menuItem;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load data
		DataHolder dataHolder = DataHolder.getInstance();
		this.data = dataHolder.getTrainData();

		ids = new HashMap<String, Integer>();

		// Load right xml
		setContentView(R.layout.activity_station);

		// Get station id from bundle extra
		stationId = getIntent().getExtras().getInt("stationId");
		// Get station from station id
		station = data.getStation(stationId);

		MultiMap<String, String> reqParams = new MultiValueMap<String, String>();
		reqParams.put("mapid", String.valueOf(station.getId()));
		new LoadData().execute(reqParams);

		// Call google street api to load image
		new DisplayGoogleStreetPicture().execute(station.getStops().get(0).getPosition());

		this.isFavorite = isFavorite();

		TextView textView = (TextView) findViewById(R.id.activity_station_station_name);
		textView.setText(station.getName().toString());

		streetViewImage = (ImageView) findViewById(R.id.activity_station_streetview_image);

		mapImage = (ImageView) findViewById(R.id.activity_station_map_image);

		directionImage = (ImageView) findViewById(R.id.activity_station_map_direction);

		// LinearLayout colorView = (LinearLayout) findViewById(R.id.activity_station_station_color);

		int width = (int) getResources().getDimension(R.dimen.activity_station_line_width);
		int height = (int) getResources().getDimension(R.dimen.activity_station_line_height);
		int line1PaddingColor = (int) getResources().getDimension(R.dimen.activity_station_stops_line1_padding_color);
		int line1PaddingTop = (int) getResources().getDimension(R.dimen.activity_station_stops_line1_padding_top);

		android.view.ViewGroup.LayoutParams params = new android.view.ViewGroup.LayoutParams(width, height);

		favoritesImage = (ImageView) findViewById(R.id.activity_station_favorite_star);
		if (isFavorite) {
			favoritesImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_save_active));
		}
		favoritesImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				StationActivity.this.switchFavorite();
			}
		});

		LinearLayout stopsView = (LinearLayout) findViewById(R.id.activity_station_stops);

		Map<TrainLine, List<Stop>> stops = station.getStopByLines();
		CheckBox checkBox = null;
		for (Entry<TrainLine, List<Stop>> e : stops.entrySet()) {

			RelativeLayout line1 = new RelativeLayout(this);
			line1.setPadding(0, line1PaddingTop, 0, 0);

			paramsStop = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			line1.setLayoutParams(paramsStop);

			final TrainLine line = e.getKey();
			List<Stop> stopss = e.getValue();
			Collections.sort(stopss);

			TextView textView2 = new TextView(this);
			textView2.setText("T");
			textView2.setTypeface(Typeface.DEFAULT_BOLD);
			textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
			textView2.setTextColor(getResources().getColor(R.color.grey_M));
			int id = Util.generateViewId();
			textView2.setId(id);
			textView2.setPadding(0, 0, line1PaddingColor, 0);
			line1.addView(textView2);

			textView2 = new TextView(this);
			textView2.setBackgroundColor(line.getColor());
			textView2.setLayoutParams(params);
			int id2 = Util.generateViewId();
			textView2.setId(id2);
			RelativeLayout.LayoutParams derp2 = new RelativeLayout.LayoutParams(width, height);
			derp2.addRule(RelativeLayout.RIGHT_OF, id);
			textView2.setLayoutParams(derp2);
			line1.addView(textView2);

			textView2 = new TextView(this);
			textView2.setText(line.toStringWithLine());
			textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
			textView2.setPadding(line1PaddingColor, 0, 0, 0);
			textView2.setTextColor(getResources().getColor(R.color.grey));

			RelativeLayout.LayoutParams derp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			derp.addRule(RelativeLayout.ALIGN_BASELINE, id);
			derp.addRule(RelativeLayout.RIGHT_OF, id2);

			textView2.setLayoutParams(derp);
			line1.addView(textView2);

			stopsView.addView(line1);

			// LinearLayout line2 = new LinearLayout(this);
			// line2.setOrientation(LinearLayout.HORIZONTAL);
			// line2.setLayoutParams(paramsStop);

			for (final Stop stop : stopss) {

				LinearLayout line2 = new LinearLayout(this);
				line2.setOrientation(LinearLayout.HORIZONTAL);
				line2.setLayoutParams(paramsStop);

				checkBox = new CheckBox(this);
				checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						Preferences.saveFilter(stationId, line, stop.getDirection(), isChecked);
					}
				});
				checkBox.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// Update timing
						MultiMap<String, String> reqParams = new MultiValueMap<String, String>();
						reqParams.put("mapid", String.valueOf(station.getId()));
						new LoadData().execute(reqParams);
					}
				});
				checkBox.setChecked(Preferences.getFilter(stationId, line, stop.getDirection()));
				checkBox.setText(stop.getDirection().toString());
				checkBox.setTextColor(getResources().getColor(R.color.grey));

				line2.addView(checkBox);
				stopsView.addView(line2);

				LinearLayout line3 = new LinearLayout(this);
				line3.setOrientation(LinearLayout.VERTICAL);
				line3.setLayoutParams(paramsStop);
				int id3 = Util.generateViewId();
				line3.setId(id3);
				ids.put(line.toString() + "_" + stop.getDirection().toString(), id3);

				stopsView.addView(line3);
			}

		}
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.menu = menu;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		MenuItem refreshMenuItem = menu.findItem(R.id.action_refresh);
		refreshMenuItem.setActionView(R.layout.progressbar);
		refreshMenuItem.expandActionView();

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Intent returnIntent = new Intent();
			// setResult(RESULT_CANCELED, returnIntent);
			finish();
			return true;
		case R.id.action_refresh:
			MenuItem menuItem = item;
			menuItem.setActionView(R.layout.progressbar);
			menuItem.expandActionView();

			MultiMap<String, String> params = new MultiValueMap<String, String>();
			List<Integer> favorites = Preferences.getFavorites(ChicagoTracker.PREFERENCE_FAVORITES);
			for (Integer fav : favorites) {
				params.put("mapid", String.valueOf(fav));
			}
			MultiMap<String, String> reqParams = new MultiValueMap<String, String>();
			reqParams.put("mapid", String.valueOf(station.getId()));
			new LoadData().execute(reqParams);
			Toast.makeText(this, "Refresh...!", Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.onOptionsItemSelected(item);

	}

	protected void switchFavorite() {
		if (isFavorite) {
			removeFromFavorites(null);
			isFavorite = false;
		} else {
			addToFavorites(null);
			isFavorite = true;
		}
		if (isFavorite) {
			favoritesImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_save_active));
		} else {
			favoritesImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_save_disabled));
		}
	}

	public boolean isFavorite() {
		boolean isFavorite = false;
		List<Integer> favorites = Preferences.getFavorites(ChicagoTracker.PREFERENCE_FAVORITES);
		for (Integer fav : favorites) {
			if (fav.intValue() == stationId.intValue()) {
				isFavorite = true;
			}
		}
		return isFavorite;
	}

	public void addToFavorites(View view) {
		List<Integer> favorites = Preferences.getFavorites(ChicagoTracker.PREFERENCE_FAVORITES);
		if (!favorites.contains(stationId)) {
			favorites.add(stationId);
			Preferences.saveFavorites(ChicagoTracker.PREFERENCE_FAVORITES, favorites);
		}
		Toast.makeText(this, "Adding to favorites", Toast.LENGTH_SHORT).show();
	}

	public void removeFromFavorites(View view) {
		List<Integer> favorites = Preferences.getFavorites(ChicagoTracker.PREFERENCE_FAVORITES);
		favorites.remove(stationId);
		Preferences.saveFavorites(ChicagoTracker.PREFERENCE_FAVORITES, favorites);
		Toast.makeText(this, "Removing from favorites", Toast.LENGTH_SHORT).show();
	}

	private class DisplayGoogleStreetPicture extends AsyncTask<Position, Void, Drawable> {
		private Position position;

		@Override
		protected Drawable doInBackground(Position... params) {
			GStreetViewConnect connect = GStreetViewConnect.getInstance();
			try {
				this.position = params[0];
				return connect.connect(params[0]);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(Drawable result) {
			int height = (int) getResources().getDimension(R.dimen.activity_station_street_map_height);
			android.widget.RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) StationActivity.this.streetViewImage
					.getLayoutParams();
			ViewGroup.LayoutParams params2 = StationActivity.this.streetViewImage.getLayoutParams();
			params2.height = height;
			params2.width = params.width;
			StationActivity.this.streetViewImage.setLayoutParams(params2);
			StationActivity.this.streetViewImage.setImageDrawable(result);
			StationActivity.this.streetViewImage.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String uri = String.format(Locale.ENGLISH, "google.streetview:cbll=%f,%f&cbp=1,180,,0,1&mz=1", position.getLatitude(),
							position.getLongitude());
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
					intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
					try {
						startActivity(intent);

						// To get direction
						// String uri = "http://maps.google.com/?f=d&daddr="+position.getLatitude() + "," + position.getLongitude();
						// Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
						// i.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
						// startActivity(i);
					} catch (ActivityNotFoundException ex) {
						uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?q=&layer=c&cbll=%f,%f&cbp=11,0,0,0,0",
								position.getLatitude(), position.getLongitude());
						Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
						startActivity(unrestrictedIntent);
					}
				}
			});
			StationActivity.this.mapImage.setImageDrawable(ChicagoTracker.getAppContext().getResources().getDrawable(R.drawable.da_turn_arrive));
			StationActivity.this.mapImage.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String uri = "http://maps.google.com/maps?z=12&t=m&q=loc:" + position.getLatitude() + "+" + position.getLongitude();
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
					i.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
					startActivity(i);
				}
			});

			StationActivity.this.directionImage.setImageDrawable(ChicagoTracker.getAppContext().getResources()
					.getDrawable(R.drawable.ic_directions_walking));
			StationActivity.this.directionImage.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String uri = "http://maps.google.com/?f=d&daddr=" + position.getLatitude() + "," + position.getLongitude() + "&dirflg=w";
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
					i.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
					startActivity(i);
				}
			});

			MenuItem refreshMenuItem = menu.findItem(R.id.action_refresh);
			refreshMenuItem.collapseActionView();
			refreshMenuItem.setActionView(null);
			firstLoad = false;
		}
	}

	private class LoadData extends AsyncTask<MultiMap<String, String>, Void, TrainArrival> {

		@Override
		protected TrainArrival doInBackground(MultiMap<String, String>... params) {
			// Get menu item and put it to loading mod
			publishProgress((Void[]) null);
			SparseArray<TrainArrival> arrivals = new SparseArray<TrainArrival>();
			CtaConnect connect = CtaConnect.getInstance();
			try {
				Xml xml = new Xml();
				String xmlResult = connect.connect(CtaRequestType.TRAIN_ARRIVALS, params[0]);
				// String xmlResult = connectTest();
				arrivals = xml.parseArrivals(xmlResult, StationActivity.this.data);
				// Apply filters
				int index = 0;
				while (index < arrivals.size()) {
					TrainArrival arri = arrivals.valueAt(index++);
					List<Eta> etas = arri.getEtas();
					// Sort Eta by arriving time
					Collections.sort(etas);
					// Copy data into new list to be able to avoid looping on a list that we want to modify
					List<Eta> etas2 = new ArrayList<Eta>();
					etas2.addAll(etas);
					int j = 0;
					Eta eta = null;
					Station station = null;
					TrainLine line = null;
					TrainDirection direction = null;
					for (int i = 0; i < etas2.size(); i++) {
						eta = etas2.get(i);
						station = eta.getStation();
						line = eta.getRouteName();
						direction = eta.getStop().getDirection();
						boolean toRemove = Preferences.getFilter(station.getId(), line, direction);
						if (!toRemove) {
							etas.remove(i - j++);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			if (arrivals.size() == 1) {
				@SuppressWarnings("unchecked")
				String id = ((List<String>) params[0].get("mapid")).get(0);
				return arrivals.get(Integer.valueOf(id));
			} else {
				return null;
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// Get menu item and put it to loading mod
			if (menu != null) {
				MenuItem refreshMenuItem = menu.findItem(R.id.action_refresh);
				refreshMenuItem.setActionView(R.layout.progressbar);
				refreshMenuItem.expandActionView();
			}
		}

		@Override
		protected void onPostExecute(TrainArrival result) {
			arrival = result;
			List<Eta> etas;
			if (arrival != null) {
				etas = arrival.getEtas();
			} else {
				etas = new ArrayList<Eta>();
			}
			reset(StationActivity.this.station);
			for (Eta eta : etas) {
				drawLine3(eta);
			}
			if (!firstLoad) {
				MenuItem refreshMenuItem = menu.findItem(R.id.action_refresh);
				refreshMenuItem.collapseActionView();
				refreshMenuItem.setActionView(null);
			}
		}
	}

	private void reset(Station station) {
		Set<TrainLine> setTL = station.getLines();
		for (TrainLine tl : setTL) {
			for (TrainDirection d : TrainDirection.values()) {
				Integer id = ids.get(tl.toString() + "_" + d.toString());
				if (id != null) {
					LinearLayout line3View = (LinearLayout) findViewById(id);
					if (line3View != null) {
						line3View.setVisibility(View.GONE);
						if (line3View.getChildCount() > 0) {
							for (int i = 0; i < line3View.getChildCount(); i++) {
								LinearLayout view = (LinearLayout) line3View.getChildAt(i);
								TextView timing = (TextView) view.getChildAt(1);
								if (timing != null) {
									timing.setText("");
								}
							}
						}
					}
				}
			}
		}
	}

	protected void drawLine3(Eta eta) {
		TrainLine line = eta.getRouteName();
		Stop stop = eta.getStop();
		int line3Padding = (int) getResources().getDimension(R.dimen.activity_station_stops_line3);
		LinearLayout line3View = (LinearLayout) findViewById(ids.get(line.toString() + "_" + stop.getDirection().toString()));

		Integer id = ids.get(line.toString() + "_" + stop.getDirection().toString() + "_" + eta.getDestName());
		if (id == null) {
			LinearLayout insideLayout = new LinearLayout(this);
			insideLayout.setOrientation(LinearLayout.HORIZONTAL);
			insideLayout.setLayoutParams(paramsStop);
			int newId = Util.generateViewId();
			insideLayout.setId(newId);
			ids.put(line.toString() + "_" + stop.getDirection().toString() + "_" + eta.getDestName(), newId);

			TextView stopName = new TextView(this);
			stopName.setText(eta.getDestName() + ": ");
			stopName.setTextColor(getResources().getColor(R.color.grey));
			stopName.setPadding(line3Padding, 0, 0, 0);
			insideLayout.addView(stopName);

			TextView timing = new TextView(this);
			timing.setText(eta.getTimeLeftDueDelay() + " ");
			timing.setTextColor(getResources().getColor(R.color.grey));
			insideLayout.addView(timing);

			line3View.addView(insideLayout);
		} else {
			LinearLayout insideLayout = (LinearLayout) findViewById(id);

			TextView timing = (TextView) insideLayout.getChildAt(1);
			timing.setText(timing.getText() + eta.getTimeLeftDueDelay() + " ");
		}
		line3View.setVisibility(View.VISIBLE);
	}

	// To delete
	// public String connectTest() {
	// StringBuilder derp = new StringBuilder();
	// try {
	// InputStreamReader ipsr = new InputStreamReader(this.getAssets().open("test2.xml"));
	// BufferedReader reader = new BufferedReader(ipsr);
	// String line = null;
	//
	// while ((line = reader.readLine()) != null) {
	// derp.append(line);
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// return derp.toString();
	// }
}