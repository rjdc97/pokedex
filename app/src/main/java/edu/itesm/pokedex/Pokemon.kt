package edu.itesm.pokedex

import android.graphics.Bitmap

data class Pokemon(val id: String, val nombre: String,
                   val tipo: String, val latitude: Double, val longitude: Double, val foto: String){
    constructor():this("","", "", 0.0,0.0,"")
}
