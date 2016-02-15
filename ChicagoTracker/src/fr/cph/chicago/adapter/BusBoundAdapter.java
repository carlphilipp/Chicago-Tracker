/**
 * Copyright 2016 Carl-Philipp Harmant
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cph.chicago.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import fr.cph.chicago.ChicagoTracker;
import fr.cph.chicago.R;
import fr.cph.chicago.entity.BusStop;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that will handle buses bound
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
public class BusBoundAdapter extends BaseAdapter {
	/** List of bus stop **/
	private List<BusStop> busStops;
	/** **/
	private String stopId;

	/**
	 * Constructor
	 *
	 * @param stopId
	 *            the stop id of the bu
	 */
	public BusBoundAdapter(final String stopId) {
		this.busStops = new ArrayList<>();
		this.stopId = stopId;
	}

	@Override
	public final int getCount() {
		return busStops.size();
	}

	@Override
	public final Object getItem(final int position) {
		return busStops.get(position);
	}

	@Override
	public final long getItemId(final int position) {
		return busStops.get(position).getId();
	}

	@Override
	public final View getView(final int position, View convertView, final ViewGroup parent) {

		BusStop busStop = busStops.get(position);

		TextView routNumberView = null;
		TextView routNameView = null;

		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater) ChicagoTracker.getAppContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.list_bus_bounds, parent, false);

			ViewHolder holder = new ViewHolder();
			routNumberView = (TextView) convertView.findViewById(R.id.route_number);
			holder.routNumberView = routNumberView;

			routNameView = (TextView) convertView.findViewById(R.id.station_name);
			holder.routNameView = routNameView;

			convertView.setTag(holder);
		} else {
			ViewHolder viewHolder = (ViewHolder) convertView.getTag();
			routNumberView = viewHolder.routNumberView;
			routNameView = viewHolder.routNameView;
		}

		routNumberView.setText(stopId);
		routNameView.setText(busStop.getName());

		return convertView;
	}

	/**
	 * DP view holder
	 *
	 * @author Carl-Philipp Harmant
	 * @version 1
	 */
	static class ViewHolder {
		TextView routNumberView;
		TextView routNameView;
	}

	/**
	 * Update of the bus stops
	 *
	 * @param result
	 *            the list of bus stops
	 */
	public final void update(final List<BusStop> result) {
		this.busStops = null;
		this.busStops = result;
	}

}
