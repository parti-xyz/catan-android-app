package xyz.parti.catan.data.services;

import com.google.gson.JsonNull;

import io.reactivex.Flowable;
import retrofit2.Response;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import xyz.parti.catan.data.model.Comment;
import xyz.parti.catan.data.model.Page;


public interface CommentsService {
    @GET("/api/v1/posts/{post_id}/comments")
    Flowable<Response<Page<Comment>>> getComments(@Path(value = "post_id") Long postId);

    @GET("/api/v1/posts/{post_id}/comments")
    Flowable<Response<Page<Comment>>> getComments(@Path(value = "post_id") Long postId, @Query("last_comment_id") long lastCommentId);

    @FormUrlEncoded
    @POST("/api/v1/comments")
    Flowable<Response<Comment>> createComment(@Field(value= "comment[post_id]") Long postId, @Field(value="comment[body]") String body);

    @DELETE("/api/v1/comments/{id}")
    Flowable<Response<JsonNull>> destroyComment(@Path(value = "id") Long id);
}
