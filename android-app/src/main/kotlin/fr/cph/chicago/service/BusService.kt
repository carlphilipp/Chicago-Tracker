/**
 * Copyright 2019 Carl-Philipp Harmant
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

package fr.cph.chicago.service

import fr.cph.chicago.client.CtaClient
import fr.cph.chicago.client.CtaRequestType.BUS_ARRIVALS
import fr.cph.chicago.client.CtaRequestType.BUS_DIRECTION
import fr.cph.chicago.client.CtaRequestType.BUS_PATTERN
import fr.cph.chicago.client.CtaRequestType.BUS_ROUTES
import fr.cph.chicago.client.CtaRequestType.BUS_STOP_LIST
import fr.cph.chicago.client.CtaRequestType.BUS_VEHICLES
import fr.cph.chicago.client.REQUEST_ROUTE
import fr.cph.chicago.client.REQUEST_STOP_ID
import fr.cph.chicago.client.allStopsParams
import fr.cph.chicago.client.busArrivalsParams
import fr.cph.chicago.client.busArrivalsStopIdParams
import fr.cph.chicago.client.busDirectionParams
import fr.cph.chicago.client.busFollowParams
import fr.cph.chicago.client.busPatternParams
import fr.cph.chicago.client.busVehiclesParams
import fr.cph.chicago.client.emptyParams
import fr.cph.chicago.core.model.Bus
import fr.cph.chicago.core.model.BusArrival
import fr.cph.chicago.core.model.BusDirections
import fr.cph.chicago.core.model.BusPattern
import fr.cph.chicago.core.model.BusRoute
import fr.cph.chicago.core.model.BusStop
import fr.cph.chicago.core.model.BusStopPattern
import fr.cph.chicago.core.model.Position
import fr.cph.chicago.core.model.dto.BusArrivalDTO
import fr.cph.chicago.core.model.dto.BusArrivalStopDTO
import fr.cph.chicago.core.model.enumeration.BusDirection
import fr.cph.chicago.entity.BusArrivalResponse
import fr.cph.chicago.entity.BusDirectionResponse
import fr.cph.chicago.entity.BusPatternResponse
import fr.cph.chicago.entity.BusPositionResponse
import fr.cph.chicago.entity.BusRoutesResponse
import fr.cph.chicago.entity.BusStopsResponse
import fr.cph.chicago.exception.CtaException
import fr.cph.chicago.parseNotNull
import fr.cph.chicago.parser.BusStopCsvParser
import fr.cph.chicago.redux.store
import fr.cph.chicago.repository.BusRepository
import fr.cph.chicago.rx.RxUtil.handleListError
import fr.cph.chicago.rx.RxUtil.singleFromCallable
import fr.cph.chicago.util.Util
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.apache.commons.collections4.MultiValuedMap
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.containsIgnoreCase
import org.apache.commons.text.WordUtils
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Callable

object BusService {

    private val busStopCsvParser = BusStopCsvParser
    private val preferenceService = PreferenceService
    private val busRepository = BusRepository
    private val ctaClient = CtaClient
    private val util = Util
    private val simpleDateFormatBus: SimpleDateFormat = SimpleDateFormat("yyyyMMdd HH:mm", Locale.US)

    fun loadFavoritesBuses(): Single<BusArrivalDTO> {
        return Single.fromCallable { preferenceService.getFavoritesBusParams() }
            .flatMap { favoritesBusParams ->
                if (favoritesBusParams.isEmpty) {
                    Single.just(listOf())
                } else {
                    val params = ArrayListValuedHashMap<String, String>(2, 1)
                    params.put(REQUEST_ROUTE, favoritesBusParams.get(REQUEST_ROUTE).joinToString(separator = ","))
                    params.put(REQUEST_STOP_ID, favoritesBusParams.get(REQUEST_STOP_ID).joinToString(separator = ","))
                    getBusArrivals(params)
                }
            }
            .subscribeOn(Schedulers.computation())
            .map { favoriteBuses -> BusArrivalDTO(favoriteBuses, false) }
    }

    fun loadAllBusStopsForRouteBound(route: String, bound: String): Single<List<BusStop>> {
        return ctaClient.getRx(BUS_STOP_LIST, allStopsParams(route, bound), BusStopsResponse::class.java)
            .map { busStopsResponse ->
                if (busStopsResponse.bustimeResponse.stops == null) {
                    throw CtaException(busStopsResponse)
                }
                busStopsResponse.bustimeResponse.stops!!.map { stop ->
                    BusStop(
                        id = stop.stpid.toInt(),
                        name = WordUtils.capitalizeFully(stop.stpnm),
                        description = stop.stpnm,
                        position = Position(stop.lat, stop.lon))
                }
            }
    }

    fun loadLocalBusData(): Single<Boolean> {
        return Single
            .fromCallable {
                if (busRepository.hasBusStopsEmpty()) {
                    Timber.d("Load bus stop from CSV")
                    busStopCsvParser.parse()
                }
            }
            .map { false }
            .subscribeOn(Schedulers.computation())
            .onErrorReturn { throwable ->
                Timber.e(throwable, "Could not create local bus data")
                true
            }
    }

    fun loadBusDirectionsSingle(busRouteId: String): Single<BusDirections> {
        return singleFromCallable(Callable { loadBusDirections(busRouteId) })
    }

    fun busRoutes(): Single<List<BusRoute>> {
        return ctaClient.getRx(BUS_ROUTES, emptyParams(), BusRoutesResponse::class.java)
            .map { bustimeResponse: BusRoutesResponse ->
                bustimeResponse.bustimeResponse
                    .routes
                    .map { route -> BusRoute(route.routeId, route.routeName) }
            }
    }

    fun loadFollowBus(busId: String): Single<List<BusArrival>> {
        return getBusArrivals(busFollowParams(busId))
            .onErrorReturn(handleListError())
            .subscribeOn(Schedulers.computation())
    }

    fun loadBusPattern(busRouteId: String, bound: String): Single<BusPattern> {
        return singleFromCallable(Callable { loadBusPattern(busRouteId, arrayOf(bound))[0] })
    }

    fun loadBusPattern(busRouteId: String, bounds: Array<String>): List<BusPattern> {
        val boundIgnoreCase = bounds.map { bound -> bound.toLowerCase(Locale.US) }
        val result = ctaClient.get(BUS_PATTERN, busPatternParams(busRouteId), BusPatternResponse::class.java)
        if (result.bustimeResponse.ptr == null) throw CtaException(result)
        return result
            .bustimeResponse
            .ptr!!
            .map { ptr ->
                BusPattern(
                    direction = ptr.rtdir,
                    busStopsPatterns = ptr.pt
                        .map { pt ->
                            BusStopPattern(Position(pt.lat, pt.lon), pt.typ, pt.stpnm
                                ?: StringUtils.EMPTY)
                        }
                        .toMutableList()
                )
            }
            .filter { pattern ->
                val directionIgnoreCase = pattern.direction.toLowerCase(Locale.US)
                boundIgnoreCase.contains(directionIgnoreCase)
            }
    }

    fun busForRouteId(busRouteId: String): Single<List<Bus>> {
        return ctaClient.getRx(BUS_VEHICLES, busVehiclesParams(busRouteId), BusPositionResponse::class.java)
            .observeOn(Schedulers.computation())
            .map { result ->
                if (result.bustimeResponse.vehicle == null) throw CtaException(result)
                result.bustimeResponse.vehicle!!
                    .map { vehicle ->
                        val position = Position(vehicle.lat.toDouble(), vehicle.lon.toDouble())
                        Bus(vehicle.vid.toInt(), position, vehicle.hdg.toInt(), vehicle.des)
                    }
            }
            .subscribeOn(Schedulers.computation())
    }

    fun loadBusArrivals(busStop: BusStop): Single<List<BusArrival>> {
        return getBusArrivals(busArrivalsStopIdParams(busStop.id))
            .onErrorReturn(handleListError())
            .subscribeOn(Schedulers.computation())
    }

    fun busStopsAround(position: Position): Single<List<BusStop>> {
        return singleFromCallable(
            Callable { busRepository.getBusStopsAround(position) })
            .onErrorReturn(handleListError())
    }

    fun saveBusStops(busStops: List<BusStop>) {
        return busRepository.saveBusStops(busStops)
    }

    /**
     *  We can't guaranty that the repo will be populated when we call that method
     */
    fun getBusRoute(routeId: String): BusRoute {
        return if (store.state.busRoutes.isEmpty()) {
            getBusRouteFromFavorites(routeId)
        } else {
            return store.state.busRoutes
                .filter { (id) -> id == routeId }
                .getOrNull(0) ?: getBusRouteFromFavorites(routeId)
        }
    }

    private fun getBusRouteFromFavorites(routeId: String): BusRoute {
        val routeName = preferenceService.getBusRouteNameMapping(routeId)
        return BusRoute(routeId, routeName)
    }

    fun searchBusRoutes(query: String): Single<List<BusRoute>> {
        return Single
            .fromCallable {
                store.state.busRoutes
                    .filter { (id, name) -> containsIgnoreCase(id, query) || containsIgnoreCase(name, query) }
                    .distinct()
                    .sortedWith(util.busStopComparatorByName)
            }
            .subscribeOn(Schedulers.computation())
    }

    fun loadBusArrivals(busRouteId: String, busStopId: Int, bound: String, boundTitle: String): Single<BusArrivalStopDTO> {
        return getBusArrivals(busArrivalsParams(busRouteId, busStopId))
            .map { busArrivals ->
                busArrivals
                    .filter { (_, _, _, _, _, _, routeDirection) -> routeDirection == bound || routeDirection == boundTitle }
                    .fold(BusArrivalStopDTO()) { accumulator, busArrival ->
                        accumulator.getOrPut(busArrival.busDestination, { mutableListOf() }).add(busArrival)
                        accumulator
                    }
            }
            .subscribeOn(Schedulers.computation())
    }

    fun extractBusRouteFavorites(busFavorites: List<String>): List<String> {
        return busFavorites
            .map { util.decodeBusFavorite(it) }
            .map { it.routeId }
            .distinct()
    }

    private fun getBusArrivals(params: MultiValuedMap<String, String>): Single<List<BusArrival>> {
        return ctaClient.getRx(BUS_ARRIVALS, params, BusArrivalResponse::class.java)
            .map { result ->
                when {
                    result.bustimeResponse.prd == null -> {
                        var res: List<BusArrival>? = null
                        if (result.bustimeResponse.error != null && result.bustimeResponse.error!!.isNotEmpty()) {
                            if (result.bustimeResponse.error!![0].noServiceScheduled()) {
                                res = listOf()
                            }
                        }
                        res ?: throw CtaException(result)
                    }
                    else -> {
                        val buses = result.bustimeResponse
                            .prd!!
                            .map { prd ->
                                BusArrival(
                                    timeStamp = simpleDateFormatBus.parseNotNull(prd.tmstmp),
                                    stopName = WordUtils.capitalizeFully(prd.stpnm),
                                    stopId = prd.stpid.toInt(),
                                    busId = prd.vid.toInt(),
                                    routeId = prd.rt,
                                    routeDirection = BusDirection.fromString(prd.rtdir).text,
                                    busDestination = prd.des,
                                    predictionTime = simpleDateFormatBus.parseNotNull(prd.prdtm),
                                    isDelay = prd.dly)
                            }
                        // limiting the number of bus arrival returned so it's not too ugly on the map
                        if (buses.size >= 20) buses.subList(0, 19) else buses
                    }
                }
            }
    }

    private fun loadBusDirections(busRouteId: String): BusDirections {
        val result = ctaClient.get(BUS_DIRECTION, busDirectionParams(busRouteId), BusDirectionResponse::class.java)
        if (result.bustimeResponse.directions == null) {
            throw CtaException(result)
        }
        val busDirections = BusDirections(busRouteId)
        result
            .bustimeResponse
            .directions
            .map { direction -> BusDirection.fromString(direction.dir) }
            .forEach { busDirections.addBusDirection(it) }
        return busDirections
    }
}
