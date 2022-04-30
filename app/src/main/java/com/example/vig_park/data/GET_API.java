package com.example.vig_park.data;

import com.example.vig_park.model.GET_CODE;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GET_API {

    // ?invite_code={invite_code}&master_password=secretmasterpasswordvig
    @GET("backendofreact")
    Call<GET_CODE> Check_code(@Query("invite_code") String invite_code, @Query("master_password") String master_password);



}
