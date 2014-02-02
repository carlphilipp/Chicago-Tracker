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

import java.util.List;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import fr.cph.chicago.ChicagoTracker;
import fr.cph.chicago.R;
import fr.cph.chicago.activity.ErrorActivity;
import fr.cph.chicago.activity.MainActivity;
import fr.cph.chicago.adapter.FavoritesAdapter;
import fr.cph.chicago.connection.CtaRequestType;
import fr.cph.chicago.data.Preferences;
import fr.cph.chicago.entity.BusArrival;
import fr.cph.chicago.entity.TrainArrival;
import fr.cph.chicago.exception.ParserException;
import fr.cph.chicago.task.CtaConnectTask;
import fr.cph.chicago.util.Util;

/**
 * A placeholder fragment containing a simple view.
 */
public class FavoritesFragment extends Fragment {
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/** Tag **/
	private static final String TAG = "FavoritesFragment";

	private static Activity mActivity;

	private static FavoritesAdapter ada;

	private Menu menu;

	private RefreshTask refreshTimingTask;

	private boolean firstLoad = true;

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static FavoritesFragment newInstance(int sectionNumber) {
		FavoritesFragment fragment = new FavoritesFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		if (ada == null) {
			ada = new FavoritesAdapter(mActivity);
		}
		ListView listView = (ListView) rootView.findViewById(R.id.favorites_list);
		listView.setAdapter(ada);

		// Force onCreateOptionsMenu being called
		setHasOptionsMenu(true);

		refreshTimingTask = (RefreshTask) new RefreshTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.i(TAG, "onCreateOptionsMenu");
		this.menu = menu;
		try {
			loadData();
		} catch (ParserException e) {
			Intent intent = new Intent(ChicagoTracker.getAppContext(), ErrorActivity.class);
			startActivity(intent);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	public void loadData() throws ParserException {
		if (firstLoad && menu.size() > 1) {
			MenuItem menuItem = menu.getItem(1);
			menuItem.setActionView(R.layout.progressbar);
			menuItem.expandActionView();

			MultiMap<String, String> params = new MultiValueMap<String, String>();
			List<Integer> favorites = Preferences.getTrainFavorites(ChicagoTracker.PREFERENCE_FAVORITES_TRAIN);
			for (Integer fav : favorites) {
				params.put("mapid", String.valueOf(fav));
			}

			MultiMap<String, String> params2 = new MultiValueMap<String, String>();
			List<String> busFavorites = Preferences.getBusFavorites(ChicagoTracker.PREFERENCE_FAVORITES_BUS);
			for (String str : busFavorites) {
				String[] fav = Util.decodeBusFavorite(str);
				params2.put("rt", fav[0]);
				params2.put("stpid", fav[1]);
			}

			CtaConnectTask task = new CtaConnectTask(FavoritesFragment.class, CtaRequestType.TRAIN_ARRIVALS, params, CtaRequestType.BUS_ARRIVALS,
					params2);
			task.execute((Void) null);
			firstLoad = false;
		}
	}

	private class RefreshTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
			ada.refreshUpdatedView();
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (!this.isCancelled()) {
				Log.v(TAG, "Updated of time " + Thread.currentThread().getId());
				try {
					publishProgress();
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					Log.v(TAG, "Stopping thread. Normal Behavior");
				}
			}
			return null;
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		refreshTimingTask.cancel(true);
	}

	@Override
	public void onResume() {
		Log.i(TAG, "on resume");
		super.onResume();
		ada.setFavorites();
		ada.notifyDataSetChanged();
		if (refreshTimingTask.getStatus() == Status.FINISHED) {
			refreshTimingTask = (RefreshTask) new RefreshTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
		((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
	}

	public static void reloadData(SparseArray<TrainArrival> trainArrivals, List<BusArrival> busArrivals) {
		ada.setArrivals(trainArrivals, busArrivals);
		ada.refreshUpdated();
		ada.notifyDataSetChanged();
		((MainActivity) mActivity).stopRefreshAnimation();
	}
	
	public static void displayError(){
		Intent intent = new Intent(ChicagoTracker.getAppContext(), ErrorActivity.class);
		mActivity.finish();
		mActivity.startActivity(intent);
	}

	public static void updateFavorites() {
		ada.setFavorites();
	}
}