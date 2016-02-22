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

package fr.cph.chicago.connection;

import android.util.Log;
import fr.cph.chicago.exception.ConnectException;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DivvyConnect {

	/**
	 * Tag
	 **/
	private static final String TAG = "DivvyConnect";
	/**
	 * Singleton
	 **/
	private static DivvyConnect instance = null;
	/**
	 * Divvy URL
	 **/
	private static final String URL = "http://www.divvybikes.com/stations/json";

	private DivvyConnect() {
	}

	public static DivvyConnect getInstance() {
		if (instance == null) {
			instance = new DivvyConnect();
		}
		return instance;
	}

	public final String connect() throws ConnectException {
		String toReturn = null;
		HttpURLConnection urlConnection = null;
		InputStream inputStream = null;
		try {
			Log.v(TAG, "Address: " + URL);
			final URL url = new URL(URL);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(10000);
			urlConnection.setReadTimeout(10000);
			inputStream = new BufferedInputStream(urlConnection.getInputStream());
			toReturn = IOUtils.toString(inputStream);
		} catch (final IOException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new ConnectException(ConnectException.ERROR, e);
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			IOUtils.closeQuietly(inputStream);
		}
		Log.v(TAG, "Divvy: " + toReturn);
		return toReturn;
	}
}
