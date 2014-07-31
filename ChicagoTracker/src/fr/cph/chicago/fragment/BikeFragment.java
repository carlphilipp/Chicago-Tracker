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

package fr.cph.chicago.fragment;

/**
 * Created by carl on 11/15/13.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import fr.cph.chicago.ChicagoTracker;
import fr.cph.chicago.R;
import fr.cph.chicago.activity.MainActivity;
import fr.cph.chicago.adapter.BikeAdapter;
import fr.cph.chicago.connection.DivvyConnect;
import fr.cph.chicago.entity.BikeStation;
import fr.cph.chicago.exception.ConnectException;
import fr.cph.chicago.exception.ParserException;
import fr.cph.chicago.json.Json;
import fr.cph.chicago.util.Util;

/**
 * Bike Fragment
 * 
 * @author Carl-Philipp Harmant
 * @version 1
 */
public class BikeFragment extends Fragment {
	/** Tag **/
	private static final String TAG = "BikeFragment";
	/** The fragment argument representing the section number for this fragment. **/
	private static final String ARG_SECTION_NUMBER = "section_number";
	/** The main actvity **/
	private MainActivity mActivity;
	/** Adapter **/
	private BikeAdapter mAdapter;
	/** Bike data **/
	private List<BikeStation> mBikeStations;
	/** Root view **/
	private View mRootView;
	/** Loading layout **/
	private RelativeLayout mLoadingLayout;
	/** Desactivated layout **/
	private RelativeLayout mDesactivatedLayout;
	/** The list view **/
	private ListView mListView;
	/** The filter text view **/
	private TextView mFilterView;

	/**
	 * Returns a new instance of this fragment for the given section number.
	 * 
	 * @param sectionNumber
	 *            the section number
	 * @return the fragment
	 */
	public static BikeFragment newInstance(final int sectionNumber) {
		BikeFragment fragment = new BikeFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public final void onAttach(final Activity activity) {
		super.onAttach(activity);
		mActivity = (MainActivity) activity;
		mActivity.onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
	}

	@Override
	public final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			this.mBikeStations = savedInstanceState.getParcelableArrayList("bikeStations");
		} else {
			Bundle bundle = mActivity.getIntent().getExtras();
			this.mBikeStations = bundle.getParcelableArrayList("bikeStations");
		}
		if (this.mBikeStations == null) {
			this.mBikeStations = new ArrayList<BikeStation>();
		}
		setHasOptionsMenu(true);

		// Google analytics
		Tracker t = ((ChicagoTracker) mActivity.getApplication()).getTracker();
		t.setScreenName("Bike fragment");
		t.send(new HitBuilders.AppViewBuilder().build());
	}

	@Override
	public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_bike, container, false);
		if (!mActivity.isFinishing()) {
			mLoadingLayout = (RelativeLayout) mRootView.findViewById(R.id.loading_relativeLayout);
			mDesactivatedLayout = (RelativeLayout) mRootView.findViewById(R.id.desactivated_layout);
			mListView = (ListView) mRootView.findViewById(R.id.bike_list);
			mFilterView = (TextView) mRootView.findViewById(R.id.bike_filter);
			if (Util.isNetworkAvailable()) {
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mActivity);
				boolean loadBike = sharedPref.getBoolean("divvy_bike", true);
				if (loadBike) {
					if (mBikeStations == null || mBikeStations.size() != 0) {
						loadList();
					} else {
						mLoadingLayout.setVisibility(RelativeLayout.VISIBLE);
						mListView.setVisibility(ListView.INVISIBLE);
						mFilterView.setVisibility(TextView.INVISIBLE);
						new WaitForRefreshData().execute();
					}
				} else {
					mDesactivatedLayout.setVisibility(RelativeLayout.VISIBLE);
					mFilterView.setVisibility(TextView.INVISIBLE);
				}
			} else {
				Toast.makeText(ChicagoTracker.getAppContext(), "No network connection detected!", Toast.LENGTH_SHORT).show();
			}
		}
		return mRootView;
	}

	private final void loadList() {
		EditText filter = (EditText) mRootView.findViewById(R.id.bike_filter);
		if (mAdapter == null) {
			mAdapter = new BikeAdapter(mActivity);
		}
		mListView.setAdapter(mAdapter);
		filter.addTextChangedListener(new TextWatcher() {

			private List<BikeStation> bikeStations = null;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				this.bikeStations = new ArrayList<BikeStation>();
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				for (BikeStation bikeStation : BikeFragment.this.mBikeStations) {
					if (StringUtils.containsIgnoreCase(bikeStation.getName(), s.toString().trim())) {
						this.bikeStations.add(bikeStation);
					}
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
				mAdapter.setBikeStations(this.bikeStations);
				mAdapter.notifyDataSetChanged();
			}
		});
		mListView.setVisibility(ListView.VISIBLE);
		mFilterView.setVisibility(ListView.VISIBLE);
		mLoadingLayout.setVisibility(RelativeLayout.INVISIBLE);
		RelativeLayout errorLayout = (RelativeLayout) mRootView.findViewById(R.id.error_layout);
		errorLayout.setVisibility(RelativeLayout.INVISIBLE);
	}

	@Override
	public final void onSaveInstanceState(final Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mActivity);
			boolean loadBike = sharedPref.getBoolean("divvy_bike", true);
			if (loadBike) {
				MenuItem menuItem = item;
				menuItem.setActionView(R.layout.progressbar);
				menuItem.expandActionView();

				new DivvyAsyncTask().execute();

				Bundle bundle = mActivity.getIntent().getExtras();
				List<BikeStation> bikeStations = bundle.getParcelableArrayList("bikeStations");

				if (bikeStations == null) {
					mActivity.startRefreshAnimation();
					mActivity.new LoadData().execute();
				}
			}
			return false;
		}
		return super.onOptionsItemSelected(item);
	}

	private final class WaitForRefreshData extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... args) {
			Bundle bundle = BikeFragment.this.mActivity.getIntent().getExtras();
			List<BikeStation> bikeStations = bundle.getParcelableArrayList("bikeStations");
			int i = 0;
			while ((bikeStations == null || bikeStations.size() == 0) && i < 10) {
				try {
					Thread.sleep(100);
					bundle = BikeFragment.this.mActivity.getIntent().getExtras();
					bikeStations = bundle.getParcelableArrayList("bikeStations");
					i++;
				} catch (InterruptedException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
			List<BikeStation> bikeStationsBundle = bundle.getParcelableArrayList("bikeStations");
			if (bikeStationsBundle == null) {
				return false;
			} else {
				return bikeStationsBundle.size() != 0;
			}

		}

		@Override
		protected final void onPostExecute(final Boolean result) {
			if (!result) {
				loadError();
			} else {
				loadList();
			}
		}
	}

	private final void loadError() {
		mLoadingLayout.setVisibility(RelativeLayout.INVISIBLE);
		RelativeLayout errorLayout = (RelativeLayout) mRootView.findViewById(R.id.error_layout);
		errorLayout.setVisibility(RelativeLayout.VISIBLE);
	}

	private final class DivvyAsyncTask extends AsyncTask<Void, Void, List<BikeStation>> {

		@Override
		protected List<BikeStation> doInBackground(Void... params) {
			List<BikeStation> bikeStations = new ArrayList<BikeStation>();
			try {
				Json json = new Json();
				DivvyConnect divvyConnect = DivvyConnect.getInstance();
				String bikeContent = divvyConnect.connect();
				bikeStations = json.parseStations(bikeContent);
				Collections.sort(bikeStations, Util.BIKE_COMPARATOR_NAME);
			} catch (ConnectException e) {
				BikeFragment.this.mActivity.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(ChicagoTracker.getAppContext(), "Error, try again later!", Toast.LENGTH_SHORT).show();
					}
				});
				Log.e(TAG, "Connect error", e);
			} catch (ParserException e) {
				BikeFragment.this.mActivity.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(ChicagoTracker.getAppContext(), "Error, try again later!", Toast.LENGTH_SHORT).show();
					}
				});
				Log.e(TAG, "Parser error", e);
			}
			return bikeStations;
		}

		@Override
		protected final void onPostExecute(final List<BikeStation> result) {
			if (result.size() != 0) {
				BikeFragment.this.mBikeStations = result;
				if (BikeFragment.this.mAdapter == null) {
					BikeFragment.this.loadList();
				}
				BikeFragment.this.mAdapter.setBikeStations(result);
				BikeFragment.this.mAdapter.notifyDataSetChanged();
				// Put in main activity the new list of bikes
				BikeFragment.this.mActivity.getIntent().putParcelableArrayListExtra("bikeStations", (ArrayList<BikeStation>) result);
				BikeFragment.this.mActivity.onNewIntent(mActivity.getIntent());
			}
			BikeFragment.this.mActivity.stopRefreshAnimation();
		}
	}
}
