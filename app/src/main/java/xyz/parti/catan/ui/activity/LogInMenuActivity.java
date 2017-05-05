package xyz.parti.catan.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.io.IOException;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import xyz.parti.catan.BuildConfig;
import xyz.parti.catan.Constants;
import xyz.parti.catan.R;
import xyz.parti.catan.models.User;
import xyz.parti.catan.services.AuthTokenService;
import xyz.parti.catan.models.PartiAccessToken;
import xyz.parti.catan.api.ServiceGenerator;
import xyz.parti.catan.services.UsersService;
import xyz.parti.catan.sessions.SessionManager;

public class LogInMenuActivity extends BaseActivity {
    private UserLogInTask authTask = null;

    @BindView(R.id.button_login_by_email)
    View loginByEmailButton;
    @BindView(R.id.button_login_by_facebook)
    View loginByFacebookButton;
    @BindView(R.id.button_sign_up)
    View signUpButton;
    @BindView(R.id.progressbar_status)
    View statusProgressBar;
    @BindView(R.id.layout_panel)
    View panelLayout;

    private CallbackManager callbackManager;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callbackManager = initFacebook();
        setContentView(R.layout.activity_login_menu);

        session = new SessionManager(getApplicationContext());
        
        ButterKnife.bind(LogInMenuActivity.this);

        loginByEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(LogInMenuActivity.this, EmailLoginActivity.class);
                LogInMenuActivity.this.startActivity(i);
            }
        });
        loginByFacebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginManager.getInstance().logInWithReadPermissions(LogInMenuActivity.this, Arrays.asList("email"));
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://parti.xyz/users/pre_sign_up"));
                startActivity(i);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private CallbackManager initFacebook() {
        CallbackManager callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if(BuildConfig.DEBUG) {
                    Log.d(Constants.TAG, "페이스북 로그인 성공");
                }
                AccessToken accessToken = loginResult.getAccessToken();
                attemptLogin(accessToken);
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
        return callbackManager;
    }

    private void attemptLogin(AccessToken accessToken) {
        if (authTask != null) {
            return;
        }

        showProgress(true);
        authTask = new UserLogInTask("facebook", accessToken.getCurrentAccessToken().getToken(), LogInMenuActivity.this);
        authTask.execute((Void) null);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            panelLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            panelLayout.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    panelLayout.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            statusProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            statusProgressBar.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    statusProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            statusProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            panelLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLogInTask extends AsyncTask<Void, Void, Boolean> {

        Activity mActivity;

        private final String provider;
        private final String accessToken;

        UserLogInTask(String provider, String accessToken, Activity activity) {
            this.provider = provider;
            this.accessToken = accessToken;
            this.mActivity = activity;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String assertion = accessToken;
                String grantType = "assertion";

                AuthTokenService authTokenService = ServiceGenerator.createUnsignedService(AuthTokenService.class);
                Call<PartiAccessToken> call = authTokenService.getNewAccessToken(provider,
                        assertion, grantType, BuildConfig.PARTI_APP_ID, BuildConfig.PARTI_SECRET_KEY);
                retrofit2.Response<PartiAccessToken> tokenResponse = call.execute();
                if (tokenResponse.code() != 200) {
                    return false;
                }
                PartiAccessToken token = tokenResponse.body();

                UsersService userService = ServiceGenerator.createNoRefreshService(UsersService.class, token);
                Call<User> userCall = userService.getCurrentUser();
                retrofit2.Response<User> userResponse = userCall.execute();
                if (userResponse.code() == 200) {
                    User user = userResponse.body();
                    session.createLoginSession(user, token);
                    if(BuildConfig.DEBUG) {
                        Log.d(Constants.TAG, user.nickname + "(으)로 로그인");
                    }
                    return true;
                }

                return false;
            } catch(IOException e) {
                Log.e(Constants.TAG, "로그인 오류",  e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            authTask = null;
            showProgress(false);

            if (success) {
                finish();

                // Staring MainActivity
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
                this.mActivity.finish();
            } else {
                Toast.makeText(getApplicationContext(), R.string.error_login, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            authTask = null;
            showProgress(false);
        }
    }

    @Override
    public boolean willFinishIfLogOut() {
        return false;
    }
}