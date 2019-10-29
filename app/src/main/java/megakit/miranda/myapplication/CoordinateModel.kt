package megakit.miranda.myapplication

import com.google.android.gms.maps.model.LatLng

class CoordinateModel (val list:List<Location>)  {
}

class Location(val latitude :Double ,val longitude :Double){

   fun toLatLng() = LatLng(latitude,longitude)
}
