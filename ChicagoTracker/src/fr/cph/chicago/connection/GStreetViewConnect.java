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

package fr.cph.chicago.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.graphics.drawable.Drawable;
import android.util.Log;
import fr.cph.chicago.entity.Position;
import fr.cph.chicago.util.Util;

/**
 * 
 * @author carl
 * 
 */
public class GStreetViewConnect {

	/** **/
	private static final String TAG = "GStreetViewConnect";
	/** **/
	private static final String BASE_URL = "http://maps.googleapis.com/maps/api/streetview";
	/** **/
	private String GOOGLE_KEY;
	/** **/
	private static final int WIDTH = 1000;
	/** **/
	private static final int HEIGTH = 300;
	/** **/
	private static GStreetViewConnect instance = null;

	/**
	 * 
	 */
	private GStreetViewConnect() {
		GOOGLE_KEY = Util.getProperty("google.streetmap.key");
	}

	/**
	 * 
	 * @return
	 */
	public static GStreetViewConnect getInstance() {
		if (instance == null) {
			instance = new GStreetViewConnect();
		}
		return instance;
	}

	/**
	 * 
	 * @param adress
	 * @return
	 * @throws IOException
	 */
	private Drawable connectUrl(final String adress) throws IOException {
		Log.v(TAG, "adress: " + adress);
		try {
			InputStream is = (InputStream) new URL(adress).getContent();
			Drawable d = Drawable.createFromStream(is, "src name");
			return d;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 
	 * @param position
	 * @return
	 * @throws IOException
	 */
	public final Drawable connect(final Position position) throws IOException {
		StringBuilder adress = new StringBuilder(BASE_URL);
		adress.append("?key=" + GOOGLE_KEY);
		adress.append("&sensor=false");
		adress.append("&size=" + WIDTH + "x" + HEIGTH);
		adress.append("&fov=120");
		adress.append("&location=" + position.getLatitude() + "," + position.getLongitude());
		return connectUrl(adress.toString());
	}
}
