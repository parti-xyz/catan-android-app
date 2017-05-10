package xyz.parti.catan.data.services;

import java.util.Date;

import io.reactivex.Flowable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import xyz.parti.catan.data.model.Page;
import xyz.parti.catan.data.model.Post;
import xyz.parti.catan.data.model.Update;

/**
 * Created by dalikim on 2017. 3. 28..
 */

public interface PostsService {
    @GET("/api/v1/posts/dashboard_latest")
    Flowable<Response<Page<Post>>> getDashBoardLastest();

    @GET("/api/v1/posts/dashboard_after")
    Flowable<Response<Page<Post>>> getDashboardAfter(
            @Query("last_id") long lastId);

    @GET("/api/v1/posts/has_updated")
    Flowable<Response<Update>> hasUpdated(
            @Query("last_stroked_at") Date lastStrokedAt);

    @GET("/api/v1/posts/{id}/download_file/{file_source_id}")
    @Streaming
    Call<ResponseBody> downloadFile(
            @Path(value = "id") Long id,
            @Path(value = "file_source_id") Long fileSourceId);

    @GET("/api/v1/posts/{id}")
    Flowable<Response<Post>> getPost(@Path(value = "id") Long id);
}