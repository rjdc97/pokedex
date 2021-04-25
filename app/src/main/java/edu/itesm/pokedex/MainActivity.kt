package edu.itesm.pokedex

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

import edu.itesm.pokedex.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var bind:ActivityMainBinding

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var bundle: Bundle

    private val RICapture = 10007
    //ultimacoordenada
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    //foto
    private lateinit var foto:Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = FirebaseDatabase.getInstance()
        reference = database.getReference("pokemons")


        analytics = FirebaseAnalytics.getInstance(this)

        bundle = Bundle()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        activaReferencia()

        
    }

    private fun activaReferencia(){
        if(ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                    10007
            )
        }
    }

    public fun logout(view: View){
        Firebase.auth.signOut()
        startActivity(Intent(this,LoginActivity::class.java))
        finish()
    }

    public fun fotoPokemon(view: View){
        val tomafoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(tomafoto, RICapture)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RICapture && resultCode == RESULT_OK){
            foto = data?.extras?.get("data") as Bitmap


        }
    }


    public fun addPokemon(view: View){
        val nombre = findViewById<EditText>(R.id.nombre).text
        val tipo = findViewById<EditText>(R.id.tipo).text
        if(nombre.isNotEmpty() && nombre.isNotBlank() && tipo.isNotEmpty() && tipo.isNotBlank()){

            var latitude = 0.0
            var longitude = 0.0
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                latitude = location.latitude
                                longitude = location.longitude
                            }
                        }
            }

            if(foto != null){

                // convertir a bytes
                val baos = ByteArrayOutputStream()
                foto.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()


                val fileName = UUID.randomUUID().toString()


                val storage_reference = FirebaseStorage.getInstance().getReference("/pokefotos/$fileName")
                val uploadTask = storage_reference.putBytes(data)


                uploadTask.addOnSuccessListener {

                    storage_reference.downloadUrl.addOnSuccessListener {

                        val id = reference.push().key
                        val pokemon = Pokemon(
                                id.toString(),
                                nombre.toString(),
                                tipo.toString(),
                                latitude,
                                longitude,
                                it.toString()
                        )
                        reference.child(id!!).setValue(pokemon)
                        nombre.clear()
                        tipo.clear()
                        Toast.makeText(this, "PoKEMON CAPTURADO", Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener{
                    Toast.makeText(this, "Error al subir un pokemon", Toast.LENGTH_LONG).show()
                }
            }

            bundle.putString("edu_itesm_pokedex_main", "added_pokemon")
            analytics.logEvent("main", bundle)

        }else{
            Toast.makeText(applicationContext, "error en nombre o tipo!", Toast.LENGTH_LONG).show()
        }

    }


    public fun getPokemons(view: View){
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var listaPokemons = ArrayList<Pokemon>()
                for (pokemon in snapshot.children) {
                    var objeto = pokemon.getValue(Pokemon::class.java)
                    listaPokemons.add(objeto as Pokemon)

                }
                Log.i("pokemons", listaPokemons.toString())

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

}