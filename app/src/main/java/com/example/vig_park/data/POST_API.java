package com.example.vig_park.data;

import com.example.vig_park.model.POST_PHOTO;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface POST_API {

    @FormUrlEncoded
    @POST("/docreateguest")
    Call<POST_PHOTO> Post_img64(@Field("ID") String ID, @Field("img64") String img64, @Field("name") String name);



}
