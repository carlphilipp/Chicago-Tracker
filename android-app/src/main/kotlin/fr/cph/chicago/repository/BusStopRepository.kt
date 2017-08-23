package fr.cph.chicago.repository

import fr.cph.chicago.entity.BusStop
import fr.cph.chicago.entity.Position
import io.realm.Realm

object BusStopRepository {

    fun isEmpty(): Boolean {
        val realm = Realm.getDefaultInstance()
        return realm.use {
            it.where(BusStop::class.java).findFirst() == null
        }
    }

    fun saveBuses(busStops: List<BusStop>) {
        val realm = Realm.getDefaultInstance()
        realm.use {
            it.executeTransaction { busStops.forEach { busStop -> it.copyToRealm(busStop) } }
        }
    }

    fun getStopsAround(position: Position, range: Double): List<BusStop> {
        val realm = Realm.getDefaultInstance()

        val latitude = position.latitude
        val longitude = position.longitude

        val latMax = latitude + range
        val latMin = latitude - range
        val lonMax = longitude + range
        val lonMin = longitude - range

        return realm.use {
            it.where(BusStop::class.java)
                // TODO use between when child object is supported by Realm
                .greaterThan("position.latitude", latMin)
                .lessThan("position.latitude", latMax)
                .greaterThan("position.longitude", lonMin)
                .lessThan("position.longitude", lonMax)
                .findAllSorted("name")
                .map { currentBusStop ->
                    val busStop = BusStop()
                    busStop.name = currentBusStop.name
                    busStop.description = currentBusStop.description
                    val pos = Position()
                    pos.latitude = currentBusStop.position.latitude
                    pos.longitude = currentBusStop.position.longitude
                    busStop.position = pos
                    busStop.id = currentBusStop.id
                    busStop
                }
                .toList()
        }
    }
}
