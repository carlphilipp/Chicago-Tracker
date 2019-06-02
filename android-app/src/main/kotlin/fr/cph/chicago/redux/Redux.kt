package fr.cph.chicago.redux

import fr.cph.chicago.Constants
import org.apache.commons.lang3.StringUtils
import org.rekotlin.Action
import org.rekotlin.Store
import timber.log.Timber
import java.util.Date

val store = Store(
    reducer = ::reducer,
    state = null,
    middleware = listOf(
        baseMiddleware,
        busRoutesAndBikeStationMiddleware,
        favoritesMiddleware,
        trainStationMiddleware,
        busStopArrivalsMiddleware,
        bikeStationMiddleware,
        busRoutesMiddleware,
        alertMiddleware,
        addTrainFavorites,
        removeTrainFavorites,
        addBusFavorites,
        removeBusFavorites,
        addBikeFavorites,
        removeBikeFavorites
    )
)

private fun reducer(action: Action, oldState: State?): State {
    // if no state has been provided, create the default state
    var state = oldState ?: State()

    when (action) {
        is ResetStateAction -> {
            state = state.copy(status = Status.UNKNOWN)
        }
        is DefaultSettingsAction -> {
            val trainBaseUrl = if (action.trainUrl == StringUtils.EMPTY) Constants.TRAINS_BASE else action.trainUrl
            val divvyUrl = if (action.divvyUrl == StringUtils.EMPTY) Constants.DIVYY_URL else action.divvyUrl

            state = state.copy(
                lastStateChange = Date(),
                ctaTrainKey = action.ctaTrainKey,
                ctaBusKey = action.ctaBusKey,
                googleStreetKey = action.googleStreetKey,
                trainArrivalsUrl = trainBaseUrl + "ttarrivals.aspx",
                trainFollowUrl = trainBaseUrl + "ttfollow.aspx",
                trainLocationUrl = trainBaseUrl + "ttpositions.aspx",
                divvyUrl = divvyUrl)
        }
        is BaseAction -> {
            val status = when {
                action.localError -> Status.FULL_FAILURE
                action.trainArrivalsDTO.error || action.busArrivalsDTO.error -> Status.FAILURE
                else -> Status.SUCCESS
            }
            state = state.copy(
                lastStateChange = Date(),
                status = status,
                lastFavoritesUpdate = Date(),
                trainArrivalsDTO = action.trainArrivalsDTO,
                busArrivalsDTO = action.busArrivalsDTO,
                trainFavorites = action.trainFavorites,
                busFavorites = action.busFavorites,
                busRouteFavorites = action.busRouteFavorites,
                bikeFavorites = action.bikeFavorites
            )
        }
        is BusRoutesAndBikeStationAction -> {
            val status = when {
                state.trainArrivalsDTO.error && state.busArrivalsDTO.error && action.bikeStationsError -> Status.FULL_FAILURE
                state.trainArrivalsDTO.error || state.busArrivalsDTO.error || action.bikeStationsError -> Status.FAILURE
                else -> Status.SUCCESS
            }
            val busRoutes = if (action.busRoutesError) state.busRoutes else action.busRoutes
            val busRoutesStatus = when {
                action.busRoutesError && busRoutes.isEmpty() -> Status.FULL_FAILURE
                action.busRoutesError && busRoutes.isNotEmpty() -> Status.FAILURE
                else -> Status.SUCCESS
            }
            val bikeStations = if (action.bikeStationsError) state.bikeStations else action.bikeStations
            val bikeStationsStatus = when {
                action.bikeStationsError && bikeStations.isEmpty() -> Status.FULL_FAILURE
                action.bikeStationsError && bikeStations.isNotEmpty() -> Status.FAILURE
                else -> Status.SUCCESS
            }
            state = state.copy(
                lastStateChange = Date(),
                status = status,
                busRoutesStatus = busRoutesStatus,
                bikeStationsStatus = bikeStationsStatus,
                busRoutes = busRoutes,
                bikeStations = bikeStations
            )
        }
        is FavoritesAction -> {
            val status = when {
                action.favoritesDTO.trainArrivalDTO.error && action.favoritesDTO.busArrivalDTO.error && action.favoritesDTO.bikeError -> {
                    when {
                        action.favoritesDTO.trainArrivalDTO.trainsArrivals.size() == 0
                            && action.favoritesDTO.busArrivalDTO.busArrivals.isEmpty()
                            && action.favoritesDTO.bikeStations.isEmpty() -> Status.FULL_FAILURE
                        else -> Status.FAILURE
                    }
                }
                action.favoritesDTO.trainArrivalDTO.error || action.favoritesDTO.busArrivalDTO.error || action.favoritesDTO.bikeError -> Status.FAILURE
                else -> Status.SUCCESS
            }
            val bikeStations = if (action.favoritesDTO.bikeError) state.bikeStations else action.favoritesDTO.bikeStations
            val bikeStationsStatus = when {
                action.favoritesDTO.bikeError && bikeStations.isEmpty() -> Status.FULL_FAILURE
                action.favoritesDTO.bikeError && bikeStations.isNotEmpty() -> Status.FAILURE
                else -> Status.SUCCESS
            }
            state = state.copy(
                lastStateChange = Date(),
                status = status,
                lastFavoritesUpdate = Date(),
                trainArrivalsDTO = action.favoritesDTO.trainArrivalDTO,
                busArrivalsDTO = action.favoritesDTO.busArrivalDTO,
                bikeStations = bikeStations,
                bikeStationsStatus = bikeStationsStatus
            )
        }
        is TrainStationAction -> {
            val trainStationStatus = when {
                action.error -> Status.FAILURE
                else -> Status.SUCCESS
            }
            if (trainStationStatus == Status.SUCCESS) {
                state.trainArrivalsDTO.trainsArrivals.remove(action.trainStationId)
                state.trainArrivalsDTO.trainsArrivals.put(action.trainStationId, action.trainArrival)
            }

            state = state.copy(
                lastStateChange = Date(),
                trainArrivalsDTO = state.trainArrivalsDTO,
                trainStationStatus = trainStationStatus,
                trainStationArrival = action.trainArrival
            )
        }
        is BusStopArrivalsAction -> {
            val busStopStatus = when {
                action.error -> Status.FAILURE
                else -> Status.SUCCESS
            }
            state = state.copy(
                lastStateChange = Date(),
                busStopStatus = busStopStatus,
                busStopErrorMessage = action.errorMessage,
                busArrivalStopDTO = action.busArrivalStopDTO
            )
        }
        is BikeStationAction -> {
            val bikeStations = if (action.error) state.bikeStations else action.bikeStations
            val bikeStationsStatus = when {
                action.error && bikeStations.isEmpty() -> Status.FULL_FAILURE
                action.error && bikeStations.isNotEmpty() -> Status.FAILURE
                else -> Status.SUCCESS
            }
            state = state.copy(
                lastStateChange = Date(),
                bikeStationsStatus = bikeStationsStatus,
                bikeStationsErrorMessage = action.errorMessage,
                bikeStations = bikeStations
            )
        }
        is BusRoutesAction -> {
            val busRoutes = if (action.error) state.busRoutes else action.busRoutes
            val status = when {
                action.error && busRoutes.isEmpty() -> Status.FULL_FAILURE
                action.error && busRoutes.isNotEmpty() -> Status.FAILURE
                else -> Status.SUCCESS
            }
            state = state.copy(
                lastStateChange = Date(),
                busRoutes = if (action.error) state.busRoutes else action.busRoutes,
                busRoutesStatus = status,
                busRoutesErrorMessage = action.errorMessage
            )
        }
        is AlertAction -> {
            val alertsDTO = if (action.error) state.alertsDTO else action.routesAlertsDTO
            val status = when {
                action.error && alertsDTO.isEmpty() -> Status.FULL_FAILURE
                action.error && alertsDTO.isNotEmpty() -> Status.FAILURE
                else -> Status.SUCCESS
            }
            state = state.copy(
                lastStateChange = Date(),
                alertStatus = status,
                alertErrorMessage = action.errorMessage,
                alertsDTO = alertsDTO
            )
        }
        is AddTrainFavoriteAction -> {
            state = state.copy(
                lastStateChange = Date(),
                trainFavorites = action.trainFavorites,
                trainStationStatus = Status.ADD_FAVORITES
            )
        }
        is RemoveTrainFavoriteAction -> {
            state = state.copy(
                lastStateChange = Date(),
                trainFavorites = action.trainFavorites,
                trainStationStatus = Status.REMOVE_FAVORITES
            )
        }
        is AddBusFavoriteAction -> {
            state = state.copy(
                lastStateChange = Date(),
                busFavorites = action.busFavorites,
                busRouteFavorites = action.busRouteFavorites,
                busStopStatus = Status.ADD_FAVORITES
            )
        }
        is RemoveBusFavoriteAction -> {
            state = state.copy(
                lastStateChange = Date(),
                busFavorites = action.busFavorites,
                busRouteFavorites = action.busRouteFavorites,
                busStopStatus = Status.REMOVE_FAVORITES
            )
        }
        is AddBikeFavoriteAction -> {
            state = state.copy(
                lastStateChange = Date(),
                bikeFavorites = action.bikeFavorites,
                bikeStationsStatus = Status.ADD_FAVORITES
            )
        }
        is RemoveBikeFavoriteAction -> {
            state = state.copy(
                lastStateChange = Date(),
                bikeFavorites = action.bikeFavorites,
                bikeStationsStatus = Status.REMOVE_FAVORITES
            )
        }
        else -> Timber.w("Action %s unknown", action)
    }
    return state
}
