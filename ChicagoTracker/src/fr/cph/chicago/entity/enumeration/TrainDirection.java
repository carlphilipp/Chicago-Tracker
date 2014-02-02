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

package fr.cph.chicago.entity.enumeration;

public enum TrainDirection {
	NORTH("N", "North"), SOUTH("S", "South"), EAST("E", "East"), WEST("W", "West");

	private String text;
	private String formattedText;

	TrainDirection(final String text, final String formattedText) {
		this.text = text;
		this.formattedText = formattedText;
	}

	public static final TrainDirection fromString(final String text) {
		if (text != null) {
			for (TrainDirection b : TrainDirection.values()) {
				if (text.equalsIgnoreCase(b.text)) {
					return b;
				}
			}
		}
		return null;
	}

	@Override
	public final String toString() {
		return this.formattedText;
	}
}
