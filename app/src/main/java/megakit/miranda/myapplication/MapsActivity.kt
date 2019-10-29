package megakit.miranda.myapplication

import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.gson.Gson
import com.google.maps.android.SphericalUtil
import kotlinx.android.synthetic.main.activity_maps.*
import kotlin.math.abs

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    val latLngs = arrayListOf<LatLng>()

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        val json =
            "{\"list\":[{\"latitude\": 47.044, \"longitude\": 37.47472},{\"latitude\": 47.046229999999994, \"longitude\": 37.47718},{\"latitude\": 47.044959999999996, \"longitude\": 37.480059999999995},{\"latitude\": 47.05085, \"longitude\": 37.48566999999999},{\"latitude\": 47.049139999999994, \"longitude\": 37.48969999999999},{\"latitude\": 47.04921999999999, \"longitude\": 37.494719999999994},{\"latitude\": 47.05742999999999, \"longitude\": 37.503659999999996},{\"latitude\": 47.06215999999999, \"longitude\": 37.505449999999996},{\"latitude\": 47.06699999999999, \"longitude\": 37.51004},{\"latitude\": 47.078509999999994, \"longitude\": 37.529399999999995},{\"latitude\": 47.08369, \"longitude\": 37.541799999999995},{\"latitude\": 47.09563, \"longitude\": 37.543029999999995},{\"latitude\": 47.09526, \"longitude\": 37.55188},{\"latitude\": 47.09673, \"longitude\": 37.550169999999994},{\"latitude\": 47.11395, \"longitude\": 37.56665999999999},{\"latitude\": 47.121930000000006, \"longitude\": 37.57126999999999},{\"latitude\": 47.125960000000006, \"longitude\": 37.57719999999999},{\"latitude\": 47.13761000000001, \"longitude\": 37.58177999999999},{\"latitude\": 47.13502000000001, \"longitude\": 37.59648999999999},{\"latitude\": 47.13549000000001, \"longitude\": 37.605369999999986},{\"latitude\": 47.143670000000014, \"longitude\": 37.617719999999984},{\"latitude\": 47.14684000000001, \"longitude\": 37.63595999999998},{\"latitude\": 47.149760000000015, \"longitude\": 37.64235999999998},{\"latitude\": 47.15910000000002, \"longitude\": 37.648929999999986},{\"latitude\": 47.16242000000002, \"longitude\": 37.649889999999985},{\"latitude\": 47.16806000000002, \"longitude\": 37.65495999999999},{\"latitude\": 47.17122000000002, \"longitude\": 37.663189999999986},{\"latitude\": 47.17126000000002, \"longitude\": 37.666229999999985},{\"latitude\": 47.17223000000002, \"longitude\": 37.66672999999999},{\"latitude\": 47.17093000000002, \"longitude\": 37.673419999999986},{\"latitude\": 47.17397000000002, \"longitude\": 37.674129999999984},{\"latitude\": 47.174520000000015, \"longitude\": 37.677669999999985}]}"
        val coordinateModel = Gson().fromJson(json, CoordinateModel::class.java)


        val list = coordinateModel.list
        for (index in list.indices) {
            latLngs.add(list[index].toLatLng())
            if (index + 1 < list.size) {
                val deltaX = (list[index + 1].latitude - list[index].latitude) / 3
                val deltaY = (list[index + 1].longitude - list[index].longitude) / 3
                for (i in 1..2) {
                    latLngs.add(
                            LatLng(
                                    list[index].latitude + deltaX * i,
                                    list[index].longitude + deltaY * i
                            )
                    )
                }
            }
        }

        val builder = LatLngBounds.Builder()
        for (latLng in latLngs) {
            builder.include(LatLng(latLng.latitude, latLng.longitude))
        }

        val latLngBounds = builder.build()


        Handler().postDelayed({
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100))
            drawPath(googleMap.projection.visibleRegion.latLngBounds, latLngs)

            mMap.setOnCameraMoveListener {
                drawPath(googleMap.projection.visibleRegion.latLngBounds, latLngs)
            }

            fakeCarMove()
        }, 300)


        mMap.uiSettings.isRotateGesturesEnabled = false
        mMap.uiSettings.isTiltGesturesEnabled = false
    }

    private fun fakeCarMove() {
        if (latLngs.size > 2) {
            Handler().postDelayed({
                if (latLngs.size > 2) {
                    latLngs.removeAt(0)
                    drawPath(mMap.projection.visibleRegion.latLngBounds, latLngs)
                    fakeCarMove()
                }
            }, 2100)
        }
    }

    private fun drawPath(bounds: LatLngBounds, latLngs: List<LatLng>) {
        val latDiff = abs(bounds.northeast.latitude - bounds.southwest.latitude)
        val lngDiff = abs(bounds.northeast.longitude - bounds.southwest.longitude)

        mapOverlayView.post {
            val points = latLngs.map {

                Pair(1 - (it.latitude - bounds.southwest.latitude) / latDiff,
                        (it.longitude - bounds.southwest.longitude) / lngDiff)
            }.map {
                PointF((mapOverlayView.width * it.second).toFloat(), (mapOverlayView.height * it.first).toFloat())
            }

            mapOverlayView.drawPath(points)
            if (latLngs.size > 2) {
                val bearing = SphericalUtil.computeHeading(latLngs[0], latLngs[1])
                mapOverlayView.drawCar(points[0], mMap.cameraPosition.zoom, bearing, latLngs[0])
            } else {
                mapOverlayView.drawCar(points[0], mMap.cameraPosition.zoom, 0.0, latLngs[0])
            }
        }
    }

    fun restart(view: View) {
        val json =
            "{\"list\":[{\"latitude\": 47.044, \"longitude\": 37.47472},{\"latitude\": 47.046229999999994, \"longitude\": 37.47718},{\"latitude\": 47.044959999999996, \"longitude\": 37.480059999999995},{\"latitude\": 47.05085, \"longitude\": 37.48566999999999},{\"latitude\": 47.049139999999994, \"longitude\": 37.48969999999999},{\"latitude\": 47.04921999999999, \"longitude\": 37.494719999999994},{\"latitude\": 47.05742999999999, \"longitude\": 37.503659999999996},{\"latitude\": 47.06215999999999, \"longitude\": 37.505449999999996},{\"latitude\": 47.06699999999999, \"longitude\": 37.51004},{\"latitude\": 47.078509999999994, \"longitude\": 37.529399999999995},{\"latitude\": 47.08369, \"longitude\": 37.541799999999995},{\"latitude\": 47.09563, \"longitude\": 37.543029999999995},{\"latitude\": 47.09526, \"longitude\": 37.55188},{\"latitude\": 47.09673, \"longitude\": 37.550169999999994},{\"latitude\": 47.11395, \"longitude\": 37.56665999999999},{\"latitude\": 47.121930000000006, \"longitude\": 37.57126999999999},{\"latitude\": 47.125960000000006, \"longitude\": 37.57719999999999},{\"latitude\": 47.13761000000001, \"longitude\": 37.58177999999999},{\"latitude\": 47.13502000000001, \"longitude\": 37.59648999999999},{\"latitude\": 47.13549000000001, \"longitude\": 37.605369999999986},{\"latitude\": 47.143670000000014, \"longitude\": 37.617719999999984},{\"latitude\": 47.14684000000001, \"longitude\": 37.63595999999998},{\"latitude\": 47.149760000000015, \"longitude\": 37.64235999999998},{\"latitude\": 47.15910000000002, \"longitude\": 37.648929999999986},{\"latitude\": 47.16242000000002, \"longitude\": 37.649889999999985},{\"latitude\": 47.16806000000002, \"longitude\": 37.65495999999999},{\"latitude\": 47.17122000000002, \"longitude\": 37.663189999999986},{\"latitude\": 47.17126000000002, \"longitude\": 37.666229999999985},{\"latitude\": 47.17223000000002, \"longitude\": 37.66672999999999},{\"latitude\": 47.17093000000002, \"longitude\": 37.673419999999986},{\"latitude\": 47.17397000000002, \"longitude\": 37.674129999999984},{\"latitude\": 47.174520000000015, \"longitude\": 37.677669999999985}]}"
        val coordinateModel = Gson().fromJson(json, CoordinateModel::class.java)


        val list = coordinateModel.list
        for (index in list.indices) {
            latLngs.add(list[index].toLatLng())
            if (index + 1 < list.size) {
                val deltaX = (list[index + 1].latitude - list[index].latitude) / 3
                val deltaY = (list[index + 1].longitude - list[index].longitude) / 3
                for (i in 1..2) {
                    latLngs.add(LatLng(list[index].latitude + deltaX * i, list[index].longitude + deltaY * i))
                }
            }
        }

        val builder = LatLngBounds.Builder()
        for (latLng in latLngs) {
            builder.include(LatLng(latLng.latitude, latLng.longitude))
        }

        val latLngBounds = builder.build()
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100))
        fakeCarMove()
    }
}
