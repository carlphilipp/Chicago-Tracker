package fr.cph.chicago.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.graphics.drawable.Drawable;
import android.util.Log;
import fr.cph.chicago.entity.Position;
import fr.cph.chicago.util.Util;

public class GStreetViewConnect {

	private static final String TAG = "GStreetViewConnect";

	private static final String BASE_URL = "http://maps.googleapis.com/maps/api/streetview";

	private String GOOGLE_KEY;

	private static final int WIDTH = 1000;

	private static final int HEIGTH = 300;

	private static GStreetViewConnect instance = null;

	private GStreetViewConnect() {
		GOOGLE_KEY = Util.getProperty("google.streetmap.key");
	}

	public static GStreetViewConnect getInstance() {
		if (instance == null) {
			instance = new GStreetViewConnect();
		}
		return instance;
	}

	private Drawable connectUrl(String adress) throws IOException {
		Log.v(TAG, "adress: " + adress);
		try {
			InputStream is = (InputStream) new URL(adress).getContent();
			Drawable d = Drawable.createFromStream(is, "src name");
			return d;
		} catch (Exception e) {
			return null;
		}
	}

	public Drawable connect(Position position) throws IOException {
		StringBuilder adress = new StringBuilder(BASE_URL);
		adress.append("?key=" + GOOGLE_KEY);
		adress.append("&sensor=false");
		adress.append("&size=" + WIDTH + "x" + HEIGTH);
		adress.append("&fov=120");
		adress.append("&location=" + position.getLatitude() + "," + position.getLongitude());
		return connectUrl(adress.toString());
	}

}