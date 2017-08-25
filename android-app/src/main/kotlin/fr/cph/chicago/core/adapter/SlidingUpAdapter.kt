package fr.cph.chicago.core.adapter

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import fr.cph.chicago.R
import fr.cph.chicago.core.fragment.NearbyFragment
import fr.cph.chicago.entity.BikeStation
import fr.cph.chicago.entity.BusArrival
import fr.cph.chicago.entity.TrainArrival
import fr.cph.chicago.entity.dto.BusArrivalRouteDTO
import fr.cph.chicago.entity.enumeration.BusDirection
import fr.cph.chicago.entity.enumeration.TrainLine
import fr.cph.chicago.util.LayoutUtil
import fr.cph.chicago.util.Util

class SlidingUpAdapter(private val nearbyFragment: NearbyFragment) {

    private var nbOfLine = intArrayOf(0)

    fun updateTitleTrain(title: String) {
        createStationHeaderView(title, R.drawable.ic_train_white_24dp)
    }

    fun updateTitleBus(title: String) {
        createStationHeaderView(title, R.drawable.ic_directions_bus_white_24dp)
    }

    fun updateTitleBike(title: String) {
        createStationHeaderView(title, R.drawable.ic_directions_bike_white_24dp)
    }

    private fun createStationHeaderView(title: String, @DrawableRes drawable: Int) {
        val vi = nearbyFragment.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val convertView = vi.inflate(R.layout.nearby_station_main, nearbyFragment.slidingUpPanelLayout, false)

        val stationNameView: TextView = convertView.findViewById(R.id.station_name)
        val imageView: ImageView = convertView.findViewById(R.id.icon)

        stationNameView.text = title
        stationNameView.maxLines = 1
        stationNameView.ellipsize = TextUtils.TruncateAt.END
        imageView.setImageDrawable(ContextCompat.getDrawable(nearbyFragment.context, drawable))

        nearbyFragment.layoutContainer.addView(convertView)
    }

    fun addTrainStation(trainArrival: TrainArrival) {
        val linearLayout = nearbyResultsView

        /*
         * Handle the case where a user clicks quickly from one marker to another. Will not update anything if a child view is already present,
         * it just mean that the view has been updated already with a faster request.
         */
        if (linearLayout.childCount == 0) {
            var nbOfLine = 0

            // FIXME removed optional to test what it looks like in the sliding panel. Not sure if it breaks anything
            //if (trainArrivalOptional.isPresent()) {
            for (trainLine in TrainLine.values()) {
                val etaResult = trainArrival.getEtas(trainLine)
                val etas = etaResult.fold(mutableMapOf<String, String>(), { accumulator, eta ->
                    val stopNameData = eta.destName
                    val timingData = eta.timeLeftDueDelay
                    val value = if (accumulator.containsKey(stopNameData))
                        accumulator[stopNameData] + " " + timingData
                    else
                        timingData
                    accumulator.put(stopNameData, value)
                    accumulator
                })

                var newLine = true
                for ((i, entry) in etas.entries.withIndex()) {
                    val containParams = layoutUtil.getInsideParams(nearbyFragment.context, newLine, i == etas.size - 1)
                    val container = layoutUtil.createTrainArrivalsLayout(nearbyFragment.context, containParams, entry, trainLine)

                    linearLayout.addView(container)
                    newLine = false
                }
                nbOfLine += etas.size
            }
            //} else {
            //    handleNoResults(linearLayout);
            //    nbOfLine++;
            //}
            nearbyFragment.slidingUpPanelLayout.panelHeight = getSlidingPanelHeight(nbOfLine)
            updatePanelState()
        }
    }

    fun addBusArrival(busArrivalRouteDTO: BusArrivalRouteDTO) {
        val linearLayout = nearbyResultsView

        /*
         * Handle the case where a user clicks quickly from one marker to another. Will not update anything if a child view is already present,
         * it just mean that the view has been updated already with a faster request.
         */
        if (linearLayout.childCount == 0) {
            nbOfLine = intArrayOf(0)

            busArrivalRouteDTO.entries.forEach { entry ->
                val stopNameTrimmed = util.trimBusStopNameIfNeeded(entry.key)
                val boundMap = entry.value

                var newLine = true

                for ((i, entry2) in boundMap.entries.withIndex()) {
                    val containParams = layoutUtil.getInsideParams(nearbyFragment.context, newLine, i == boundMap.size - 1)
                    val container = layoutUtil.createBusArrivalsLayout(nearbyFragment.context, containParams, stopNameTrimmed, BusDirection.fromString(entry2.key), entry2.value as MutableList<out BusArrival>)

                    linearLayout.addView(container)
                    newLine = false
                }
                nbOfLine[0] = nbOfLine[0] + boundMap.size
            }

            // Handle the case when we have no bus returned.
            if (busArrivalRouteDTO.size == 0) {
                handleNoResults(linearLayout)
                nbOfLine[0]++
            }
            nearbyFragment.slidingUpPanelLayout.panelHeight = getSlidingPanelHeight(nbOfLine[0])
            updatePanelState()
        }
    }

    fun addBike(bikeStation: BikeStation) {
        val linearLayout = nearbyResultsView
        /*
         * Handle the case where a user clicks quickly from one marker to another. Will not update anything if a child view is already present,
         * it just mean that the view has been updated already with a faster request.
         */
        if (linearLayout.childCount == 0 || "error" == bikeStation.name) {
            val bikeResultLayout = layoutUtil.createBikeLayout(nearbyFragment.context, bikeStation)
            linearLayout.addView(bikeResultLayout)
            nearbyFragment.slidingUpPanelLayout.panelHeight = getSlidingPanelHeight(2)
            updatePanelState()
        }
    }

    private val nearbyResultsView: LinearLayout
        get() {
            val relativeLayout = nearbyFragment.layoutContainer.getChildAt(0) as RelativeLayout
            return relativeLayout.findViewById(R.id.nearby_results)
        }

    private fun handleNoResults(linearLayout: LinearLayout) {
        val containParams = layoutUtil.getInsideParams(nearbyFragment.context, true, true)
        val container = layoutUtil.createBusArrivalsNoResult(nearbyFragment.context, containParams)
        linearLayout.addView(container)
    }

    private fun getSlidingPanelHeight(nbLine: Int): Int {
        val line = util.convertDpToPixel(nearbyFragment.context, LINE_HEIGHT)
        val header = util.convertDpToPixel(nearbyFragment.context, HEADER_HEIGHT)
        return line * nbLine + header
    }

    private fun updatePanelState() {
        if (nearbyFragment.slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.HIDDEN) {
            nearbyFragment.slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        }
        nearbyFragment.showProgress(false)
    }

    companion object {
        private val LINE_HEIGHT = 27
        private val HEADER_HEIGHT = 40
        private val util = Util
        private val layoutUtil = LayoutUtil
    }
}
