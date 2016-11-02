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

package fr.cph.chicago.core.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.apache.commons.lang3.text.WordUtils;

import java.util.List;

import fr.cph.chicago.R;
import fr.cph.chicago.entity.dto.BusDetailsDTO;

/**
 * @author Carl-Philipp Harmant
 * @version 1
 */
public class PopupBusDetailsFavoritesAdapter extends ArrayAdapter<BusDetailsDTO> {

    private final List<BusDetailsDTO> values;

    public PopupBusDetailsFavoritesAdapter(@NonNull final Context context, @NonNull final List<BusDetailsDTO> values) {
        super(context, R.layout.popup_bus_cell, values);
        this.values = values;
    }

    @NonNull
    @Override
    public final View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        final LayoutInflater vi = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View rowView = vi.inflate(R.layout.popup_bus_cell_0, parent, false);
        final TextView textView = (TextView) rowView.findViewById(R.id.label);
        final String toDisplay = values.get(position).getStopName() + " (" + WordUtils.capitalize(values.get(position).getBound().toLowerCase()) + ")";
        textView.setText(toDisplay);
        return rowView;
    }
}