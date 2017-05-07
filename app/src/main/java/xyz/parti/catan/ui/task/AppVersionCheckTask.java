package xyz.parti.catan.ui.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.parti.catan.Constants;
import xyz.parti.catan.api.ServiceGenerator;
import xyz.parti.catan.services.AppVersionService;

/**
 * Created by dalikim on 2017. 5. 6..
 */

public class AppVersionCheckTask {
    private static final String PREF_NAME = "VERSION_CHECKER";
    private static final String KEY_LAST_CHECKED_AT_MILLS = "last_checked_at_mills";
    private static final long CHECK_INTERVAL_MILLS = 48 * 60 * 60 * 1000;

    private SharedPreferences pref;
    private String currentVersion;

    public AppVersionCheckTask(String currentVersion, Context context) {
        this.currentVersion = currentVersion;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public interface NewVersionAction {
        void run(String newVersion);
    }

    public void check(final @NonNull NewVersionAction action) {
        long lastCheckedAtMills = this.pref.getLong(KEY_LAST_CHECKED_AT_MILLS, -1);

        if(lastCheckedAtMills > 0 && (System.currentTimeMillis() - lastCheckedAtMills < CHECK_INTERVAL_MILLS)) {
            return;
        }
        AppVersionService appVersionService = ServiceGenerator.createUnsignedService(AppVersionService.class);
        Call<JsonObject> call = appVersionService.getLastVersion();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if(response.isSuccessful()) {
                    pref.edit().putLong(KEY_LAST_CHECKED_AT_MILLS, System.currentTimeMillis()).apply();

                    String lastVersion = response.body().get("last_version").getAsString();
                    if (lastVersion == null || lastVersion.equals(currentVersion)) {
                    }
                    action.run(lastVersion);
                } else {
                    Log.d(Constants.TAG, "실패 " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(Constants.TAG, "getNewVersionIfAvailable 오류 ", t);

            }
        });
    }
}