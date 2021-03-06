package xyz.parti.catan.data.services;

import com.google.gson.JsonNull;

import io.reactivex.Flowable;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public interface VotingsService {
    @FormUrlEncoded
    @POST("/api/v1/votings")
    Flowable<Response<JsonNull>> voting(@Field("poll_id") long pollId, @Field("choice") String choice);
}
