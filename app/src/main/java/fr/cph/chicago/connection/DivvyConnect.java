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

import android.support.annotation.NonNull;

import java.io.InputStream;

import fr.cph.chicago.exception.ConnectException;

import static fr.cph.chicago.Constants.DIVYY_URL;

/**
 * Class that build url and connect to Divvy API.
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
public enum DivvyConnect {
    INSTANCE;

    @NonNull
    public final InputStream connect() throws ConnectException {
        return Connect.INSTANCE.connect(DIVYY_URL);
    }
}
