package xyz.parti.catan.data.services;

import java.util.Date;

import io.reactivex.Flowable;
import io.realm.Realm;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import xyz.parti.catan.data.model.Page;
import xyz.parti.catan.data.model.Parti;
import xyz.parti.catan.data.model.Post;


public interface PartiesService {
    @GET("/api/v1/parties/my_joined")
    Flowable<Response<Parti[]>> getMyJoined();

    @GET("/api/v1/parties/{id}/posts")
    Flowable<Response<Page<Post>>> getPostsLastest(@Path("id") long id);

    @GET("/api/v1/parties/{id}/posts")
    Flowable<Response<Page<Post>>> getPostsAfter(
            @Path("id") long id,
            @Query("last_id") long lastId);

    @GET("/api/v1/parties/{id}")
    Flowable<Response<Parti>> getParti(@Path("id") long id);

    @GET("/api/v1/parties/my_joined_parties_changed_at")
    Flowable<Response<Date>> getMyJoinedPartiesChangedAt();
}
