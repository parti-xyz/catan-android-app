package xyz.parti.catan.ui.task;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import retrofit2.Response;
import xyz.parti.catan.BuildConfig;
import xyz.parti.catan.Constants;
import xyz.parti.catan.R;
import xyz.parti.catan.data.ServiceBuilder;
import xyz.parti.catan.data.SessionManager;
import xyz.parti.catan.data.model.PartiAccessToken;
import xyz.parti.catan.data.model.User;
import xyz.parti.catan.data.services.AuthTokenService;
import xyz.parti.catan.data.services.UsersService;
import xyz.parti.catan.helper.ReportHelper;
import xyz.parti.catan.helper.RxGuardian;

/**
 * Created by dalikim on 2017. 5. 9..
 */

public class LoginTask {
    private final AuthTokenService authTokenService;
    private final Context context;
    private final After afterLogin;
    private final SessionManager session;
    private final RxGuardian rxGuardian;
    private Disposable loginDisposable;

    public LoginTask(Context context, LoginTask.After afterLogin) {
        this.context = context;
        this.afterLogin = afterLogin;
        this.session = new SessionManager(this.context.getApplicationContext());
        this.authTokenService = ServiceBuilder.createUnsignedService(AuthTokenService.class);
        this.rxGuardian = new RxGuardian();
    }

    public void cancel() {
        this.rxGuardian.unsubscribeAll();
    }

    public void loginSocial(String provider, String assertion) {
        loginSocial(provider, assertion, null);
    }

    public void loginSocial(String provider, String assertion, String secret) {
        String grantType = "assertion";
        Flowable<Response<PartiAccessToken>> flowable = authTokenService.getNewAccessTokenByAssertion(provider,
                assertion, secret, grantType, BuildConfig.PARTI_APP_ID, BuildConfig.PARTI_SECRET_KEY);
        process(flowable);
    }

    public void loginCredentials(String email, String password) {
        String grantType = "password";
        Flowable<Response<PartiAccessToken>> flowable = authTokenService.getNewAccessTokenByCredentials(email, password,
                grantType, BuildConfig.PARTI_APP_ID, BuildConfig.PARTI_SECRET_KEY);
        process(flowable);
    }

    private void process(Flowable<Response<PartiAccessToken>> flowable) {
        loginDisposable = rxGuardian.subscribe(loginDisposable,
                flowable.flatMap(response -> {
                    if(response.isSuccessful()) {
                        UsersService userService = ServiceBuilder.createNoRefreshService(UsersService.class, response.body());
                        return userService.getCurrentUser();
                    } else {
                        return Flowable.error(new NotFoundUserError());
                    }
                }, (tokenResponse, userResponse) -> new Pair<>(tokenResponse.body(), userResponse))
                , pair -> {
                    PartiAccessToken token = pair.first;
                    Response<User> response = pair.second;

                    if(! response.isSuccessful()) {
                        ReportHelper.wtf(context.getApplicationContext(), context.getResources().getString(R.string.login_fail));
                        afterLogin.onFail();
                        return;
                    }

                    User user = response.body();
                    session.createLoginSession(user, token);
                    if(BuildConfig.DEBUG) {
                        Log.d(Constants.TAG, user.nickname + "(으)로 로그인");
                    }
                    afterLogin.onSuccess();
                }, error -> {
                    if(error instanceof NotFoundUserError) {
                        ReportHelper.wtf(context.getApplicationContext(), "TODO: 가입화면으로 넘겨야함");
                    } else {
                        ReportHelper.wtf(context.getApplicationContext(), error);
                    }
                    afterLogin.onError();
                });
    }

    public static class NotFoundUserError extends Throwable {}

    public interface After {
        void onSuccess();
        void onFail();
        void onError();
    }
}