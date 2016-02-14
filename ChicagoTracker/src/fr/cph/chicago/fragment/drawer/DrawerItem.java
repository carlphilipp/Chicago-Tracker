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

package fr.cph.chicago.fragment.drawer;

/**
 * Drawer item
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
public final class DrawerItem {
	/** **/
	private String name;
	/** **/
	private int imgId;

	public DrawerItem(final String name, final int imgId) {
		this.name = name;
		this.imgId = imgId;
	}

	public final String getName() {
		return name;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	public final int getImgId() {
		return imgId;
	}

	public final void setImgId(final int imgId) {
		this.imgId = imgId;
	}
}
