package xyz.parti.catan.ui.presenter;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.gson.JsonNull;

import java.io.File;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import retrofit2.Response;
import xyz.parti.catan.Constants;
import xyz.parti.catan.data.ServiceBuilder;
import xyz.parti.catan.data.SessionManager;
import xyz.parti.catan.data.model.Comment;
import xyz.parti.catan.data.model.Page;
import xyz.parti.catan.data.model.Post;
import xyz.parti.catan.data.services.CommentsService;
import xyz.parti.catan.data.services.UpvotesService;
import xyz.parti.catan.helper.ReportHelper;
import xyz.parti.catan.ui.adapter.CommentFeedRecyclerViewAdapter;
import xyz.parti.catan.ui.binder.CommentBinder;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by dalikim on 2017. 5. 3..
 */

public class CommentFeedPresenter extends BasePresenter<CommentFeedPresenter.View> implements CommentBinder.CommentLikablePresenter {
    private final Post post;
    private final CommentsService commentsService;
    private final UpvotesService upvotesService;
    private CommentFeedRecyclerViewAdapter feedAdapter;
    private Disposable firstCommentsPublisher;
    private Disposable moreCommentsPublisher;
    private Disposable createCommentPublisher;
    private Disposable onClickLikePublisher;

    public CommentFeedPresenter(Post post, SessionManager session) {
        this.post = post;
        commentsService = ServiceBuilder.createService(CommentsService.class, session);
        upvotesService = ServiceBuilder.createService(UpvotesService.class, session);
    }

    @Override
    public void attachView(CommentFeedPresenter.View view) {
        super.attachView(view);
    }

    @Override
    public void detachView() {
        feedAdapter = null;
        super.detachView();
    }

    private boolean isActive() {
        return getView() != null && feedAdapter != null;
    }

    public void setCommentFeedRecyclerViewAdapter(CommentFeedRecyclerViewAdapter feedAdapter) {
        this.feedAdapter = feedAdapter;
    }

    public void loadFirstComments() {
        if(feedAdapter == null) {
            Log.d(Constants.TAG, "moreCommentsPublisher : feedAdapter is null");
            return;
        }

        firstCommentsPublisher = getRxGuardian().subscribe(firstCommentsPublisher,
                commentsService.getComments(post.id),
                response -> {
                    if (!isActive()) {
                        return;
                    }

                    if (response.isSuccessful()) {
                        Page<Comment> page = response.body();
                        feedAdapter.appendModels(page.items);
                        feedAdapter.setMoreDataAvailable(page.has_more_item);
                    } else {
                        ReportHelper.wtf(getApplicationContext(), "AllComments load first error : " + response.code());
                        feedAdapter.setMoreDataAvailable(false);
                    }
                    feedAdapter.setLoadFinished();
                }, error -> {
                    if(!isActive()) {
                        return;
                    }

                    ReportHelper.wtf(getApplicationContext(), error);
                    feedAdapter.setLoadFinished();
                });
    }

    public void loadMoreComments() {
        if(feedAdapter == null) {
            Log.d(Constants.TAG, "moreCommentsPublisher : feedAdapter is null");
            return;
        }

        Comment comment = feedAdapter.getFirstModel();
        if(comment == null) {
            Log.d(Constants.TAG, "moreCommentsPublisher : first comment is null");
            return;
        }
        feedAdapter.prependLoader();
        feedAdapter.notifyItemInserted(0);

        moreCommentsPublisher = getRxGuardian().subscribe(moreCommentsPublisher,
            commentsService.getComments(post.id, comment.id),
            response -> {
                if(!isActive()) {
                    return;
                }

                if(response.isSuccessful()) {
                    //remove loading view
                    feedAdapter.removeFirstMoldelHolder();

                    Page<Comment> page = response.body();
                    List<Comment> result = page.items;
                    if(result.size() > 0){
                        //add loaded data
                        feedAdapter.prependModels(page.items);
                        feedAdapter.setMoreDataAvailable(page.has_more_item);
                    }else{
                        //result size 0 means there is no more data available at server
                        feedAdapter.setMoreDataAvailable(false);
                    }
                } else {
                    feedAdapter.setMoreDataAvailable(false);
                    ReportHelper.wtf(getApplicationContext(), "AllComments load more error : " + response.code());
                }
                feedAdapter.setLoadFinished();
            }, error -> {
                if(!isActive()) {
                    return;
                }

                feedAdapter.setMoreDataAvailable(false);
                feedAdapter.setLoadFinished();
                ReportHelper.wtf(getApplicationContext(), error);
            });
    }

    public void onClickCommentCreateButton(String body) {
        if(feedAdapter == null) {
            return;
        }

        feedAdapter.setLoadStarted();
        getView().setSendingCommentForm();

        createCommentPublisher = getRxGuardian().subscribe(createCommentPublisher,
                commentsService.createComment(post.id, body),
                response -> {
                    if(!isActive()) {
                        return;
                    }

                    if(response.isSuccessful()) {
                        Comment comment = response.body();
                        feedAdapter.appendModel(comment);
                        feedAdapter.notifyItemChanged(feedAdapter.getLastPosition() - 1);

                        if(!feedAdapter.isEmpty()) {
                            getView().showCommentList();
                        }
                        post.addComment(comment);
                    } else {
                        ReportHelper.wtf(getApplicationContext(), "Create comment error : " + response.code());
                    }
                    getView().setCompletedCommentForm();
                    feedAdapter.setLoadFinished();
                }, error -> {
                    if(!isActive()) {
                        return;
                    }

                    getView().setCompletedCommentForm();
                    feedAdapter.setLoadFinished();

                    ReportHelper.wtf(getApplicationContext(), error);
                });
    }

    public Post getPost() {
        return post;
    }

    public void preloadImage(String url) {
        if(url == null) {
            return;
        }
        Glide.with(getView().getContext())
                .load(url)
                .downloadOnly(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(File resource, GlideAnimation<? super File> glideAnimation) {
                    }
                });
    }

    @Override
    public void onClickLike(Post post, Comment comment) {
        if(!isActive()) return;

        feedAdapter.changeModel(comment, Comment.IS_UPVOTED_BY_ME);
        Flowable<Response<JsonNull>> call =  ( comment.is_upvoted_by_me ?
                upvotesService.destroy("Comment", comment.id) : upvotesService.create("Comment", comment.id)
        );
        onClickLikePublisher = getRxGuardian().subscribe(onClickLikePublisher,
                call,
                response -> {
                    if(response.isSuccessful()) {
                        post.toggleCommentUpvoting(comment);
                        changeComment(comment, Comment.IS_UPVOTED_BY_ME);
                    } else {
                        ReportHelper.wtf(getApplicationContext(), "Like error : " + response.code());
                    }
                }, error -> ReportHelper.wtf(getApplicationContext(), error)
        );
    }

    private void changeComment(Comment comment, Object payload) {
        if(!isActive()) return;
        feedAdapter.changeModel(comment, payload);
    }

    @Override
    public void onClickNewComment(Post post, Comment comment) {
        getView().showNewCommentForm(comment);
    }

    public interface View {
        void setSendingCommentForm();
        void setCompletedCommentForm();
        void showCommentList();
        void showNewCommentForm(Comment comment);
        Context getContext();
    }
}
