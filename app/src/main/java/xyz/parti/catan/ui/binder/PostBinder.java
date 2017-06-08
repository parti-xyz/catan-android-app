package xyz.parti.catan.ui.binder;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import xyz.parti.catan.Constants;
import xyz.parti.catan.R;
import xyz.parti.catan.data.model.FileSource;
import xyz.parti.catan.data.model.Option;
import xyz.parti.catan.data.model.Poll;
import xyz.parti.catan.data.model.Post;
import xyz.parti.catan.data.model.Survey;
import xyz.parti.catan.data.model.User;
import xyz.parti.catan.helper.ImageHelper;
import xyz.parti.catan.helper.TextHelper;
import xyz.parti.catan.ui.view.LooselyRelativeTimeTextView;

/**
 * Created by dalikim on 2017. 5. 15..
 */

public class PostBinder {
    private final Context context;
    private final PostBindablePresenter presenter;
    private final LatestCommentsBinder latestCommentsBinder;
    private final LinkSourceBinder linkSourceBinder;
    private final FileSourcesBinder fileSourcesBinder;
    private final PollBinder pollBinder;
    private final SurveyBinder surveyBinder;

    @BindView(R.id.imageview_parti_logo)
    ImageView partiLogoImageView;
    @BindView(R.id.textview_parti_title)
    TextView partiTitleTextView;
    @BindView(R.id.textview_group_title)
    TextView groupTitleTextView;
    @BindView(R.id.textview_user_nickname)
    TextView userNicknameTextView;
    @BindView(R.id.imageview_user_image)
    CircleImageView userImageImageView;
    @BindView(R.id.textview_created_at)
    LooselyRelativeTimeTextView createdAtTextView;
    @BindView(R.id.textview_body)
    TextView bodyTextView;
    @BindView(R.id.textview_title)
    TextView titleTextView;
    @BindView(R.id.textview_prefix_group_title)
    TextView prefixGroupTitleTextView;
    @BindView(R.id.layout_references)
    LinearLayout referencesLayout;
    @BindView(R.id.button_like)
    Button likeButton;
    @BindView(R.id.button_new_comment)
    Button newCommentButton;
    @BindView(R.id.button_show_likes)
    Button showLikesButton;
    @BindView(R.id.layout_comments_section)
    LinearLayout commentsSectionLayout;
    @BindView(R.id.view_references_link_source)
    View referencesLinkSourceView;
    @BindView(R.id.view_references_file_sources)
    View referencesFileSourcesView;
    @BindView(R.id.view_references_poll)
    View referencesPollView;
    @BindView(R.id.view_references_survey)
    View referencesSurveyView;

    public PostBinder(Context context, View view, PostBindablePresenter presenter) {
        this.context = context;
        this.presenter = presenter;
        ButterKnife.bind(this, view);

        latestCommentsBinder = new LatestCommentsBinder(presenter, commentsSectionLayout);
        linkSourceBinder = new LinkSourceBinder(referencesLinkSourceView);
        fileSourcesBinder = new FileSourcesBinder(presenter, referencesFileSourcesView);
        pollBinder = new PollBinder(presenter, referencesPollView);
        surveyBinder = new SurveyBinder(presenter, referencesSurveyView);
    }

    public void bindData(Post post) {
        bindBasic(post);
        bindLike(post);
        bindComments(post);
        bindReferences(post);
    }

    private void bindComments(Post post) {
        latestCommentsBinder.bindData(post);
    }

    private void bindReferences(Post post) {
        resetReferences();

        bindFileSources(post);
        bindLinkSources(post);
        bindPoll(post);
        bindSurvey(post);
    }

    private void resetReferences() {
        referencesLayout.setVisibility(ViewGroup.GONE);
    }

    private void bindLinkSources(final Post post) {
        if(post.link_source != null) {
            linkSourceBinder.bindData(post.link_source);
            referencesLinkSourceView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickLinkSource(post);
                }
            });
            referencesLinkSourceView.setVisibility(View.VISIBLE);
            referencesLayout.setVisibility(ViewGroup.VISIBLE);
        } else {
            referencesLinkSourceView.setOnClickListener(null);
            referencesLinkSourceView.setVisibility(View.GONE);
        }
    }

    private void bindFileSources(Post post) {
        if(post.file_sources != null) {
            fileSourcesBinder.bindData(post);
            referencesFileSourcesView.setVisibility(View.VISIBLE);
            referencesLayout.setVisibility(ViewGroup.VISIBLE);
        } else {
            fileSourcesBinder.unbindData();
            referencesFileSourcesView.setVisibility(View.GONE);
        }
    }

    private void bindPoll(final Post post) {
        if(post.poll != null) {
            pollBinder.bindData(post);
            referencesPollView.setVisibility(View.VISIBLE);
            referencesLayout.setVisibility(ViewGroup.VISIBLE);
        } else {
            referencesPollView.setVisibility(View.GONE);
        }
    }

    private void bindSurvey(final Post post) {
        if(post.survey != null) {
            surveyBinder.bindData(post);
            referencesSurveyView.setVisibility(View.VISIBLE);
            referencesLayout.setVisibility(ViewGroup.VISIBLE);
        } else {
            referencesSurveyView.setVisibility(View.GONE);
        }
    }

    private void bindLike(final Post post) {
        if(post.is_upvoted_by_me) {
            likeButton.setTypeface(null, Typeface.BOLD);
            likeButton.setTextColor(ContextCompat.getColor(context, R.color.style_color_accent));
        } else {
            likeButton.setTypeface(null, Typeface.NORMAL);
            likeButton.setTextColor(ContextCompat.getColor(context, R.color.post_button_text));
        }
        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onClickLike(post);
            }
        });

        if(post.upvotes_count > 0) {
            showLikesButton.setText(String.format("{fa-heart} %d", post.upvotes_count));
            showLikesButton.setVisibility(View.VISIBLE);
        } else {
            showLikesButton.setVisibility(View.GONE);
        }
    }

    private void bindBasic(final Post post) {
        new ImageHelper(partiLogoImageView).loadInto(post.parti.logo_url, ImageView.ScaleType.CENTER_CROP, ImageView.ScaleType.CENTER_CROP);
        partiTitleTextView.setText(post.parti.title);
        if(post.parti.group.isIndie()) {
            prefixGroupTitleTextView.setVisibility(android.view.View.GONE);
            groupTitleTextView.setVisibility(android.view.View.GONE);
        } else {
            prefixGroupTitleTextView.setVisibility(android.view.View.VISIBLE);
            groupTitleTextView.setVisibility(android.view.View.VISIBLE);
            groupTitleTextView.setText(post.parti.group.title);
        }
        new ImageHelper(userImageImageView).loadInto(post.user.image_url, ImageView.ScaleType.CENTER_CROP, ImageView.ScaleType.CENTER_CROP);
        userNicknameTextView.setText(post.user.nickname);
        createdAtTextView.setReferenceTime(post.created_at.getTime());
        createdAtTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onClickCreatedAt(post);
            }
        });

        if(TextUtils.isEmpty(post.parsed_title)) {
            titleTextView.setVisibility(android.view.View.GONE);
        } else {
            titleTextView.setVisibility(android.view.View.VISIBLE);
            new TextHelper(context).setTextViewHTML(titleTextView, post.parsed_title);
        }

        if(TextUtils.isEmpty(post.parsed_body)) {
            bodyTextView.setVisibility(android.view.View.GONE);
        } else {
            bodyTextView.setVisibility(android.view.View.VISIBLE);
            new TextHelper(context).setTextViewHTML(bodyTextView, post.parsed_body, post.truncated_parsed_body);
        }

        newCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onClickNewComment(post);
            }
        });
    }

    public void rebindData(Post post, Object payload) {
        if (Post.PLAYLOAD_LATEST_COMMENT.equals(payload)) {
            this.bindComments(post);
        } else if (Post.IS_UPVOTED_BY_ME.equals(payload)) {
            this.bindLike(post);
        } else if(payload instanceof Survey) {
            this.bindReferences(post);
        } else if(payload instanceof Poll) {
            this.bindReferences(post);
        } else if(payload instanceof LatestCommentsBinder.CommentDiff) {
            new LatestCommentsBinder(presenter, commentsSectionLayout).rebindComment(post, (LatestCommentsBinder.CommentDiff) payload);
        } else {
            Log.d(Constants.TAG, "PostFeedRecyclerView bind : invalid playload");
        }
    }

    public interface PostBindablePresenter extends CommentBinder.CommentLikablePresenter {
        void onClickLinkSource(Post post);
        void onClickNewComment(Post post);
        void onClickLike(Post post);
        void onClickSurveyOption(Post post, Option option, boolean b);
        void onClickPollAgree(Post post);
        void onClickPollDisgree(Post post);
        void onClickMoreComments(Post post);
        void onClickImageFileSource(Post post);
        void onClickDocFileSource(Post post, FileSource docFileSource);
        void onClickCreatedAt(Post post);
        User getCurrentUser();
    }
}
