package xyz.parti.catan.data.services;

import com.google.gson.JsonElement;

import io.reactivex.Flowable;
import retrofit2.Response;
import retrofit2.http.GET;

/**
 * Created by dalikim on 2017. 5. 6..
 */

public interface SettingsService {
    @GET("/api/v1/settings")
    Flowable<Response<JsonElement>> getAll();
}
