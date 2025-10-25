package com.example.capstone_map.feature;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface UploadApi {
    @Headers("Content-Type: application/json")
    @POST("analyze")
    Call<JsonObject> sendImage(@Body JsonObject body);
}
