package xyz.parti.catan.data.services;

import com.google.gson.JsonNull;

import io.reactivex.Flowable;
import retrofit2.Response;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface UpvotesService {
    @FormUrlEncoded
    @POST("/api/v1/upvotes")
    Flowable<Response<JsonNull>> create(@Field("upvote[upvotable_type]") String upvotableType, @Field("upvote[upvotable_id]") long upvotableId);

    @DELETE("/api/v1/upvotes")
    Flowable<Response<JsonNull>> destroy(@Query("upvote[upvotable_type]") String upvotableType, @Query("upvote[upvotable_id]") long upvotableId);
}
