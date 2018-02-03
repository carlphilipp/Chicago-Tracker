/**
 * Copyright 2017 Carl-Philipp Harmant
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cph.chicago.core.activity

import android.app.ListActivity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.widget.Toolbar

import butterknife.BindDrawable
import butterknife.BindString
import butterknife.BindView
import butterknife.ButterKnife
import fr.cph.chicago.R
import fr.cph.chicago.core.adapter.TrainAdapter
import fr.cph.chicago.entity.enumeration.TrainLine
import fr.cph.chicago.util.Util

/**
 * Activity the list of train stations
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
class TrainListStationActivity : ListActivity() {

    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar
    @BindString(R.string.bundle_train_line)
    lateinit var bundleTrainLine: String
    @BindDrawable(R.drawable.ic_arrow_back_white_24dp)
    lateinit var arrowBackWhite: Drawable

    private var trainLine: TrainLine? = null
    private var lineParam: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!this.isFinishing) {
            setContentView(R.layout.activity_train_station)
            ButterKnife.bind(this)

            // Load data
            lineParam = if (savedInstanceState != null) savedInstanceState.getString(bundleTrainLine) else intent.extras!!.getString(bundleTrainLine)

            trainLine = TrainLine.fromString(lineParam!!)
            title = trainLine!!.toStringWithLine()

            Util.setWindowsColor(this, toolbar, trainLine!!)
            toolbar.title = trainLine!!.toStringWithLine()

            toolbar.navigationIcon = arrowBackWhite
            toolbar.setOnClickListener { v -> finish() }

            val ada = TrainAdapter(trainLine!!, this)
            listAdapter = ada
        }
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lineParam = savedInstanceState.getString(bundleTrainLine)
        trainLine = TrainLine.fromString(lineParam!!)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putString(bundleTrainLine, lineParam)
        super.onSaveInstanceState(savedInstanceState)
    }
}