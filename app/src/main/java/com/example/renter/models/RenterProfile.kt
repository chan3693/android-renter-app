package com.example.renter.models

import com.google.firebase.firestore.DocumentId

data class RenterProfile(
    @DocumentId
    var id:String = "",
)
