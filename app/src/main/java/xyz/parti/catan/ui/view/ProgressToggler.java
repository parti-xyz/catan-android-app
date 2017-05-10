package xyz.parti.catan.ui.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.widget.ProgressBar;

/**
 * Created by dalikim on 2017. 5. 9..
 */

public class ProgressToggler {
    private final View view;
    private final ProgressBar progressBar;

    public ProgressToggler(View view, ProgressBar progressBar) {
        this.view = view;
        this.progressBar = progressBar;
    }

    public void toggle(boolean show) {
        int shortAnimTime = view.getResources().getInteger(android.R.integer.config_shortAnimTime);

        view.setVisibility(show ? View.GONE : View.VISIBLE);
        view.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}