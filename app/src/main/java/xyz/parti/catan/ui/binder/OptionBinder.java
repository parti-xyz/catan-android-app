package xyz.parti.catan.ui.binder;

import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.joanzapata.iconify.widget.IconTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import xyz.parti.catan.R;
import xyz.parti.catan.models.Option;
import xyz.parti.catan.models.Post;
import xyz.parti.catan.ui.presenter.PostFeedPresenter;

/**
 * Created by dalikim on 2017. 4. 28..
 */

class OptionBinder {
    @BindView(R.id.optionBody)
    TextView optionBodyText;
    @BindView(R.id.optionCheckBox)
    CheckBox optionCheckBox;
    @BindView(R.id.feedbacksCount)
    TextView feedbacksCountView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.optionSelected)
    IconTextView optionSelectedText;

    private final PostFeedPresenter presenter;

    OptionBinder(PostFeedPresenter presenter, ViewGroup view) {
        this.presenter = presenter;
        ButterKnife.bind(this, view);
    }

    public void bindData(final Post post, final Option option) {
        optionBodyText.setText(option.body);
        if(post.survey.is_open) {
            optionBodyText.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View view) {
                    optionCheckBox.setChecked(!optionCheckBox.isChecked());
                }
            });
        }

        if(post.survey.is_open) {
            optionCheckBox.setVisibility(android.view.View.VISIBLE);
            optionCheckBox.setChecked(option.is_my_select);
            optionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    presenter.onClickSurveyOption(post, option, b);
                }
            });
            optionSelectedText.setVisibility(android.view.View.GONE);
        } else {
            optionCheckBox.setVisibility(android.view.View.GONE);
            if(option.is_my_select) {
                optionSelectedText.setVisibility(android.view.View.VISIBLE);
            } else {
                optionSelectedText.setVisibility(android.view.View.GONE);
            }
        }

        if(post.survey.is_feedbacked_by_me || !post.survey.is_open) {
            if(option.is_mvp) {
                feedbacksCountView.setText("" + option.feedbacks_count + "표 \u2022 최다득표");
            } else {
                feedbacksCountView.setText("" + option.feedbacks_count + "표");
            }
            feedbacksCountView.setVisibility(android.view.View.VISIBLE);

            progressBar.setMax(post.survey.feedback_users_count);
            progressBar.setProgress(option.feedbacks_count);
            progressBar.setVisibility(android.view.View.VISIBLE);
        } else {
            feedbacksCountView.setVisibility(android.view.View.GONE);
            progressBar.setVisibility(android.view.View.GONE);
        }
    }
}