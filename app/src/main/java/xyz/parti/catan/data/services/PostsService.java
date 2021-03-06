package xyz.parti.catan.data.services;

import java.util.List;

import io.reactivex.Flowable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import xyz.parti.catan.data.model.Page;
import xyz.parti.catan.data.model.Post;


public interface PostsService {
    @GET("/api/v1/posts/dashboard")
    Flowable<Response<Page<Post>>> getDashBoardLastest();

    @GET("/api/v1/posts/dashboard")
    Flowable<Response<Page<Post>>> getDashboardAfter(
            @Query("last_id") long lastId);

    @GET("/api/v1/posts/{id}/download_file/{file_source_id}")
    @Streaming
    Call<ResponseBody> downloadFile(
            @Path(value = "id") Long id,
            @Path(value = "file_source_id") Long fileSourceId);

    @GET("/api/v1/posts/{id}")
    Flowable<Response<Post>> getPost(@Path(value = "id") Long id);

    @GET("/api/v1/posts/by_sticky_comment")
    Flowable<Response<Post>> getPostByStickyCommentId(@Query("comment_id") long commentId);

    @Multipart
    @POST("/api/v1/posts")
    Flowable<Response<Post>> create(
            @Part("post[parti_id]") RequestBody partiId,
            @Part("post[body]") RequestBody body,
            @Part("post[is_html_body]") RequestBody isHtmlBody,
            @Part List<MultipartBody.Part> file_sources_attachments);
}
