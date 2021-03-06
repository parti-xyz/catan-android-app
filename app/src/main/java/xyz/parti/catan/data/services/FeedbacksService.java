package xyz.parti.catan.data.services;

import com.google.gson.JsonNull;

import io.reactivex.Flowable;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public interface FeedbacksService {
    @FormUrlEncoded
    @POST("/api/v1/feedbacks")
    Flowable<Response<JsonNull>> feedback(@Field("option_id") long optionId, @Field("selected") boolean selected);
}
