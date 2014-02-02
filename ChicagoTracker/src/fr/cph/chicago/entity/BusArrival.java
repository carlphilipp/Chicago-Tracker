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

package fr.cph.chicago.entity;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fr.cph.chicago.entity.enumeration.PredictionType;

public final class BusArrival {
	private Date timeStamp;
	private String errorMessage;
	private PredictionType predictionType;
	private String stopName;
	private Integer stopId;
	private Integer busId;
	private Integer distanceToStop; // feets
	private String routeId;
	private String routeDirection;
	private String busDestination;
	private Date predictionTime;
	private Boolean isDly = false;

	public final Date getTimeStamp() {
		return timeStamp;
	}

	public final void setTimeStamp(final Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public final String getErrorMessage() {
		return errorMessage;
	}

	public final void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public final PredictionType getPredictionType() {
		return predictionType;
	}

	public final void setPredictionType(final PredictionType predictionType) {
		this.predictionType = predictionType;
	}

	public final String getStopName() {
		return stopName;
	}

	public final void setStopName(final String stopName) {
		this.stopName = stopName;
	}

	public final Integer getStopId() {
		return stopId;
	}

	public final void setStopId(final Integer stopId) {
		this.stopId = stopId;
	}

	public final Integer getBusId() {
		return busId;
	}

	public final void setBusId(final Integer busId) {
		this.busId = busId;
	}

	public final Integer getDistanceToStop() {
		return distanceToStop;
	}

	public final void setDistanceToStop(final Integer distanceToStop) {
		this.distanceToStop = distanceToStop;
	}

	public final String getRouteId() {
		return routeId;
	}

	public final void setRouteId(final String routeId) {
		this.routeId = routeId;
	}

	public final String getRouteDirection() {
		return routeDirection;
	}

	public final void setRouteDirection(final String routeDirection) {
		this.routeDirection = routeDirection;
	}

	public final String getBusDestination() {
		return busDestination;
	}

	public final void setBusDestination(final String busDestination) {
		this.busDestination = busDestination;
	}

	public final Date getPredictionTime() {
		return predictionTime;
	}

	public final void setPredictionTime(final Date predictionTime) {
		this.predictionTime = predictionTime;
	}

	public final Boolean getIsDly() {
		return isDly;
	}

	public final void setIsDly(final Boolean isDly) {
		this.isDly = isDly;
	}

	public final String getTimeLeft() {
		long time = predictionTime.getTime() - timeStamp.getTime();
		return String.format(Locale.ENGLISH, "%d min", TimeUnit.MILLISECONDS.toMinutes(time));
	}

	public final String getTimeLeftDueDelay() {
		String result;
		if (getIsDly()) {
			result = "Delay";
		} else {
			result = getTimeLeft();
		}
		return result;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((busDestination == null) ? 0 : busDestination.hashCode());
		result = prime * result + ((busId == null) ? 0 : busId.hashCode());
		result = prime * result + ((distanceToStop == null) ? 0 : distanceToStop.hashCode());
		result = prime * result + ((errorMessage == null) ? 0 : errorMessage.hashCode());
		result = prime * result + ((isDly == null) ? 0 : isDly.hashCode());
		result = prime * result + ((predictionTime == null) ? 0 : predictionTime.hashCode());
		result = prime * result + ((predictionType == null) ? 0 : predictionType.hashCode());
		result = prime * result + ((routeDirection == null) ? 0 : routeDirection.hashCode());
		result = prime * result + ((routeId == null) ? 0 : routeId.hashCode());
		result = prime * result + ((stopId == null) ? 0 : stopId.hashCode());
		result = prime * result + ((stopName == null) ? 0 : stopName.hashCode());
		result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
		return result;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BusArrival other = (BusArrival) obj;
		if (busDestination == null) {
			if (other.busDestination != null)
				return false;
		} else if (!busDestination.equals(other.busDestination))
			return false;
		if (busId == null) {
			if (other.busId != null)
				return false;
		} else if (!busId.equals(other.busId))
			return false;
		if (distanceToStop == null) {
			if (other.distanceToStop != null)
				return false;
		} else if (!distanceToStop.equals(other.distanceToStop))
			return false;
		if (errorMessage == null) {
			if (other.errorMessage != null)
				return false;
		} else if (!errorMessage.equals(other.errorMessage))
			return false;
		if (isDly == null) {
			if (other.isDly != null)
				return false;
		} else if (!isDly.equals(other.isDly))
			return false;
		if (predictionTime == null) {
			if (other.predictionTime != null)
				return false;
		} else if (!predictionTime.equals(other.predictionTime))
			return false;
		if (predictionType != other.predictionType)
			return false;
		if (routeDirection == null) {
			if (other.routeDirection != null)
				return false;
		} else if (!routeDirection.equals(other.routeDirection))
			return false;
		if (routeId == null) {
			if (other.routeId != null)
				return false;
		} else if (!routeId.equals(other.routeId))
			return false;
		if (stopId == null) {
			if (other.stopId != null)
				return false;
		} else if (!stopId.equals(other.stopId))
			return false;
		if (stopName == null) {
			if (other.stopName != null)
				return false;
		} else if (!stopName.equals(other.stopName))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		return true;
	}

}
