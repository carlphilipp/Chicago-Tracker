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

/**
 * 
 * @author carl
 * 
 */
public final class BusRoute {
	/** **/
	private String id;
	/** **/
	private String name;

	/**
	 * 
	 */
	public BusRoute() {
	}

	/**
	 * 
	 * @return
	 */
	public final String getId() {
		return id;
	}

	/**
	 * 
	 * @param id
	 */
	public final void setId(final String id) {
		this.id = id;
	}

	/**
	 * 
	 * @return
	 */
	public final String getName() {
		return name;
	}

	/**
	 * 
	 * @param name
	 */
	public final void setName(final String name) {
		this.name = name;
	}

}
