package com.example.renter.models

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class LandlordProfile (
    @DocumentId
    var id:String = ""
) : Serializable