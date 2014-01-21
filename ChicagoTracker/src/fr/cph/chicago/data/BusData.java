package fr.cph.chicago.data;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.xmlpull.v1.XmlPullParserException;

import fr.cph.chicago.connection.CtaConnect;
import fr.cph.chicago.connection.CtaRequestType;
import fr.cph.chicago.entity.BusRoute;
import fr.cph.chicago.entity.BusStop;
import fr.cph.chicago.xml.Xml;

public class BusData {

	private List<BusRoute> routes;
	
	private static BusData busData;
	
	public static BusData getInstance() {
		if (busData == null) {
			busData = new BusData();
		} 
		return busData;
	}

	private BusData() {
		routes = new ArrayList<BusRoute>();
	}

	public List<BusRoute> read() {
		if (routes.size() == 0) {
			MultiMap<String, String> params = new MultiValueMap<String, String>();
			CtaConnect connect = CtaConnect.getInstance();
			try {
				Xml xml = new Xml();
				String xmlResult = connect.connect(CtaRequestType.BUS_ROUTES, params);
				routes = xml.parseBusRoutes(xmlResult);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return routes;
	}
	
	public int getRouteSize(){
		return routes.size();
	}
	
	public BusRoute getRoute(int position){
		return routes.get(position);
	}
	
	public List<BusStop> readBusStop(String stopId, String bound){
		CtaConnect connect = CtaConnect.getInstance();
		MultiMap<String, String> params2 = new MultiValueMap<String, String>();
		params2.put("rt", stopId);
		params2.put("dir", bound);
		List<BusStop> busStops = null;
		try {
			String xmlResult = connect.connect(CtaRequestType.BUS_STOP_LIST, params2);
			Xml xml = new Xml();
			busStops = xml.parseBusBounds(xmlResult);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		return busStops;
	}
}
