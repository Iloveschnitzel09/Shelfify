package de.schnitzel.shelfify.api

import de.schnitzel.shelfify.util.Products
import retrofit2.Call
import retrofit2.http.GET

interface ProductApi {
    @GET("/products")
    fun getAllProducts(): Call<List<Products>>
}