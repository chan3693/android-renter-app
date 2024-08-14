package com.example.renter.models

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Property(
    @DocumentId
    var id:String = "",
    var address:String = "",
    var latitude:Double = 0.0,
    var longitude:Double = 0.0,
    var imageUrl:String = "",
    var monthlyRentalPrice:Double = 0.0,
    var numberOfBedrooms:Int = 0,
    var rentalType:String = "",

    @JvmField
    var isAvailable:Boolean = true
) : Serializable