/**
 * Copyright 2017 Carl-Philipp Harmant
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

package fr.cph.chicago.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Bike station entity
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
@AllArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BikeStation implements Parcelable, AStation {

    @JsonProperty("id")
    private int id;
    @JsonProperty("stationName")
    private String name;
    @JsonProperty("availableDocks")
    private Integer availableDocks;
    @JsonProperty("totalDocks")
    private Integer totalDocks;
    @JsonProperty("latitude")
    private double latitude;
    @JsonProperty("longitude")
    private double longitude;
    @JsonProperty("statusValue")
    private String statusValue;
    @JsonProperty("statusKey")
    private String statusKey;
    @JsonProperty("availableBikes")
    private Integer availableBikes;
    @JsonProperty("stAddress1")
    private String stAddress1;
    @JsonProperty("stAddress2")
    private String stAddress2;
    @JsonProperty("city")
    private String city;
    @JsonProperty("postalCode")
    private String postalCode;
    @JsonProperty("location")
    private String location;
    @JsonProperty("altitude")
    private String altitude;
    @JsonProperty("testStation")
    private boolean testStation;
    @JsonProperty("lastCommunicationTime")
    private String lastCommunicationTime;
    @JsonProperty("landMark")
    private String landMark;

    public BikeStation() {
    }

    private BikeStation(@NonNull final Parcel in) {
        readFromParcel(in);
    }

    @Override
    public final String toString() {
        return "[" + id + " " + name + " " + availableBikes + "/" + totalDocks + "]";
    }

    @Override
    public final int describeContents() {
        return 0;
    }

    @Override
    public final void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeInt(availableDocks);
        dest.writeInt(totalDocks);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(statusValue);
        dest.writeString(statusKey);
        dest.writeInt(availableBikes);
        dest.writeString(stAddress1);
        dest.writeString(stAddress2);
        dest.writeString(city);
        dest.writeString(postalCode);
        dest.writeString(location);
        dest.writeString(altitude);
        dest.writeString(String.valueOf(testStation));
        dest.writeString(lastCommunicationTime);
        dest.writeString(landMark);
    }

    private void readFromParcel(@NonNull final Parcel in) {
        id = in.readInt();
        name = in.readString();
        availableDocks = in.readInt();
        totalDocks = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        statusValue = in.readString();
        statusKey = in.readString();
        availableBikes = in.readInt();
        stAddress1 = in.readString();
        stAddress2 = in.readString();
        city = in.readString();
        postalCode = in.readString();
        location = in.readString();
        altitude = in.readString();
        testStation = Boolean.valueOf(in.readString());
        lastCommunicationTime = in.readString();
        landMark = in.readString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BikeStation other = (BikeStation) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + availableDocks;
        result = 31 * result + totalDocks;
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (statusValue != null ? statusValue.hashCode() : 0);
        result = 31 * result + (statusKey != null ? statusKey.hashCode() : 0);
        result = 31 * result + availableBikes;
        result = 31 * result + (stAddress1 != null ? stAddress1.hashCode() : 0);
        result = 31 * result + (stAddress2 != null ? stAddress2.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (postalCode != null ? postalCode.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (altitude != null ? altitude.hashCode() : 0);
        result = 31 * result + (testStation ? 1 : 0);
        result = 31 * result + (lastCommunicationTime != null ? lastCommunicationTime.hashCode() : 0);
        result = 31 * result + (landMark != null ? landMark.hashCode() : 0);
        return result;
    }

    public static final Parcelable.Creator<BikeStation> CREATOR = new Parcelable.Creator<BikeStation>() {
        public BikeStation createFromParcel(final Parcel in) {
            return new BikeStation(in);
        }

        public BikeStation[] newArray(final int size) {
            return new BikeStation[size];
        }
    };

    public static List<BikeStation> readNearbyStation(@NonNull final List<BikeStation> bikeStations, @NonNull final Position position, final double range) {
        final double latitude = position.getLatitude();
        final double longitude = position.getLongitude();

        final double latMax = latitude + range;
        final double latMin = latitude - range;
        final double lonMax = longitude + range;
        final double lonMin = longitude - range;

        return Stream.of(bikeStations)
            .filter(station -> station.getLatitude() <= latMax)
            .filter(station -> station.getLatitude() >= latMin)
            .filter(station -> station.getLongitude() <= lonMax)
            .filter(station -> station.getLongitude() >= lonMin)
            .collect(Collectors.toList());
    }
}
