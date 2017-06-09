package xyz.parti.catan.ui.presenter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;
import xyz.parti.catan.BuildConfig;
import xyz.parti.catan.Constants;
import xyz.parti.catan.R;
import xyz.parti.catan.data.ServiceBuilder;
import xyz.parti.catan.data.SessionManager;
import xyz.parti.catan.data.activerecord.ReadPostFeed;
import xyz.parti.catan.data.model.Group;
import xyz.parti.catan.data.model.Page;
import xyz.parti.catan.data.model.Parti;
import xyz.parti.catan.data.model.Post;
import xyz.parti.catan.data.model.PushMessage;
import xyz.parti.catan.data.model.Update;
import xyz.parti.catan.data.model.User;
import xyz.parti.catan.data.preference.LastPostFeedPreference;
import xyz.parti.catan.data.preference.NotificationsPreference;
import xyz.parti.catan.data.services.PartiesService;
import xyz.parti.catan.data.services.PostsService;
import xyz.parti.catan.helper.AppVersionHelper;
import xyz.parti.catan.ui.adapter.InfinitableModelHolder;
import xyz.parti.catan.ui.adapter.PostFeedRecyclerViewAdapter;
import xyz.parti.catan.ui.binder.PostBinder;
import xyz.parti.catan.ui.task.AppVersionCheckTask;
import xyz.parti.catan.ui.task.ReceivablePushMessageCheckTask;

/**
 * Created by dalikim on 2017. 5. 3..
 */

public class PostFeedPresenter extends BasePostBindablePresenter<PostFeedPresenter.View> implements PostBinder.PostBindablePresenter {
    private final DatabaseReference partiesFirebaseRoot;
    private final List<DatabaseReference> listenPartiFireBases = new ArrayList<>();

    private SessionManager session;
    private final PostsService postsService;
    private final PartiesService partiesService;
    private PostFeedRecyclerViewAdapter feedAdapter;
    private Date lastStrockedAtOfNewPostCheck = null;
    private long lastLoadFirstPostsAtMillis = -1;

    private Disposable loadFirstPostsPublisher;
    private Disposable loadMorePostsPublisher;
    private Disposable checkNewPostsPublisher;
    private Disposable receivePushMessageForPostPublisher;
    private Disposable savePost;
    private Disposable loadDrawer;
    private Disposable loadParti;
    private AppVersionCheckTask appVersionCheckTask;
    private ReceivablePushMessageCheckTask receivablePushMessageCheckTask;
    private List<Parti> joindedParties = new ArrayList<>();
    private long currentPostFeedId = Constants.POST_FEED_DASHBOARD;
    private Parti currentParti;
    private ValueEventListener newPostListener;
    private LastPostFeedPreference lastPostFeedPreference;

    public PostFeedPresenter(SessionManager session) {
        super(session);
        this.session = session;
        postsService = ServiceBuilder.createService(PostsService.class, session);
        partiesService = ServiceBuilder.createService(PartiesService.class, session);
        partiesFirebaseRoot = FirebaseDatabase.getInstance().getReference(BuildConfig.FIREBASE_DATABASE_PARTIES);
    }

    @Override
    public void attachView(PostFeedPresenter.View view) {
        super.attachView(view);
        appVersionCheckTask = new AppVersionCheckTask(new AppVersionHelper(getView().getContext()).getCurrentVerion(), getView().getContext());
        receivablePushMessageCheckTask = new ReceivablePushMessageCheckTask(getView().getContext(), session);
        lastPostFeedPreference = new LastPostFeedPreference(getView().getContext());

        currentPostFeedId = lastPostFeedPreference.fetch();
        playWithPostFeed(new OnLoadPostFeed() {
            @Override
            public void onDashbard() {
                getView().changeDashboardToolbar();
            }


            @Override
            public void onParti(Parti parti) {
                getView().changePartiPostFeedToolbar(parti);
            }
        });
    }

    @Override
    public void detachView() {
        super.detachView();
        session = null;
        feedAdapter = null;
        if(appVersionCheckTask != null) {
            appVersionCheckTask.cancel();
        }
        if(receivablePushMessageCheckTask != null) {
            receivablePushMessageCheckTask.cancel();
        }
    }

    private boolean isActive() {
        return getView() != null && session != null && feedAdapter != null;
    }

    public void setPostFeedRecyclerViewAdapter(PostFeedRecyclerViewAdapter feedAdapter) {
        this.feedAdapter = feedAdapter;
    }

    public void loadFirstPosts() {
        if(!isActive()) return;

        Flowable<Response<Page<Post>>> boardLastest =
            this.currentPostFeedId == Constants.POST_FEED_DASHBOARD ? postsService.getDashBoardLastest() : partiesService.getPostsLastest(currentPostFeedId);
        loadFirstPostsPublisher = getRxGuardian().subscribe(loadFirstPostsPublisher,
                boardLastest,
                new Consumer<Response<Page<Post>>>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Response<Page<Post>> response) throws Exception {
                        /* SUCCESS */
                        if (!isActive()) {
                            return;
                        }

                        lastLoadFirstPostsAtMillis = System.currentTimeMillis();
                        getView().readyToShowPostFeed();
                        if (response.isSuccessful()) {
                            Page<Post> page = response.body();
                            feedAdapter.clearAndAppendModels(page.items, 1);
                            feedAdapter.setMoreDataAvailable(page.has_more_item);

                            ReadPostFeed readPostFeed = ReadPostFeed.forPartiOrDashboard(currentPostFeedId);
                            if (feedAdapter.getFirstModel() != null) {
                                readPostFeed.lastReadAt = feedAdapter.getFirstModel().last_stroked_at;
                            }
                            readPostFeed.save();
                            markUnreadOrNotParti(readPostFeed);
                        } else if (response.code() == 403) {
                            feedAdapter.setMoreDataAvailable(false);
                            feedAdapter.clear();
                        } else {
                            feedAdapter.setMoreDataAvailable(false);
                            getView().reportError("Load first post error : " + response.code());
                        }
                        feedAdapter.setLoadFinished();
                        getView().ensureExpendedAppBar();
                        getView().stopAndEnableSwipeRefreshing();

                        if (feedAdapter.getModelItemCount() <= 0) {
                            if(response.code() == 403) {
                                getView().showBlocked();
                            } else {
                                getView().showEmpty(!response.isSuccessful());
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable error) throws Exception {
                        /* ERROR **/
                        if (!isActive()) {
                            return;
                        }
                        getView().reportError(error);

                        feedAdapter.setLoadFinished();
                        getView().ensureExpendedAppBar();
                        getView().stopAndEnableSwipeRefreshing();
                        getView().showEmpty(true);
                    }
                });
    }

    public void loadMorePosts() {
        if(!isActive()) return;

        Post post = feedAdapter.getLastModel();
        if(post == null) {
            return;
        }
        feedAdapter.appendLoader();

        Flowable<Response<Page<Post>>> boardLastest =
                this.currentPostFeedId == Constants.POST_FEED_DASHBOARD ? postsService.getDashboardAfter(post.id) : partiesService.getPostsAfter(currentPostFeedId, post.id);

        loadMorePostsPublisher = getRxGuardian().subscribe(loadMorePostsPublisher,
                boardLastest,
                new Consumer<Response<Page<Post>>>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Response<Page<Post>> response) throws Exception {
                        /* SUCCESS **/
                        if (!isActive()) {
                            return;
                        }

                        if (response.isSuccessful()) {
                            //remove loading view
                            feedAdapter.removeLastMoldelHolder();

                            Page<Post> page = response.body();
                            List<Post> result = page.items;
                            if (result.size() > 0) {
                                //add loaded data
                                feedAdapter.appendModels(page.items);
                                feedAdapter.setMoreDataAvailable(page.has_more_item);
                            } else {
                                //result size 0 means there is no more data available at server
                                feedAdapter.setMoreDataAvailable(false);
                                //telling adapter to stop calling loadFirstPosts more as no more server data available
                            }
                        } else {
                            feedAdapter.setMoreDataAvailable(false);
                            getView().reportError("Load more post error : " + response.code());
                        }

                        feedAdapter.setLoadFinished();
                        if (feedAdapter.getModelItemCount() <= 0) {
                            if(response.code() == 403) {
                                getView().showBlocked();
                            } else {
                                getView().showEmpty(!response.isSuccessful());
                            }
                        }

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable error) throws Exception {
                        /* ERROR **/
                        if (!isActive()) {
                            return;
                        }
                        feedAdapter.removeLastMoldelHolder();
                        feedAdapter.setLoadFinished();
                        feedAdapter.setMoreDataAvailable(false);
                        getView().reportError(error);
                    }
                });
    }

    public void checkNewPosts() {
        if(!isActive()) return;

        if(getView().isVisibleNewPostsSign()) {
            return;
        }

        final Date lastStrockedAt = getLastStrockedAtForNewPostCheck();
        if (lastStrockedAt == null) return;

        checkNewPostsPublisher = getRxGuardian().subscribe(checkNewPostsPublisher,
                postsService.hasUpdated(lastStrockedAt),
                new Consumer<Response<Update>>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Response<Update> response) throws Exception {
                        if (!isActive()) {
                            return;
                        }

                        if (response.isSuccessful()) {
                            if (getView().isVisibleNewPostsSign()) return;
                            if (!response.body().has_updated) return;
                            if (!lastStrockedAt.equals(getLastStrockedAtForNewPostCheck())) return;

                            PostFeedPresenter.this.lastStrockedAtOfNewPostCheck = response.body().last_stroked_at;
                            getView().showNewPostsSign();
                        } else {
                            getView().reportError("Check new post error : " + response.code());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable error) throws Exception {
                        if (!isActive()) {
                            return;
                        }

                        getView().reportError(error);
                    }
                });
    }

    @Nullable
    private Date getLastStrockedAtForNewPostCheck() {
        if(this.lastStrockedAtOfNewPostCheck != null) {
            return lastStrockedAtOfNewPostCheck;
        }

        Date lastStrockedAtOfFirstInPostList = null;
        if(!feedAdapter.isEmpty()) {
            InfinitableModelHolder<Post> firstPostHolder = feedAdapter.getHolder(1);
            if(!firstPostHolder.isLoader() && firstPostHolder.getModel() != null) {
                lastStrockedAtOfFirstInPostList = firstPostHolder.getModel().last_stroked_at;
            }
        }

        if(lastStrockedAtOfNewPostCheck == null) {
            return lastStrockedAtOfFirstInPostList;
        } else if(lastStrockedAtOfFirstInPostList == null) {
            return lastStrockedAtOfNewPostCheck;
        } else {
            return (lastStrockedAtOfNewPostCheck.getTime() > lastStrockedAtOfFirstInPostList.getTime() ? lastStrockedAtOfNewPostCheck : lastStrockedAtOfFirstInPostList);
        }
    }

    @Override
    public void changePost(Post post, Object playload) {
        if(!isActive()) return;

        feedAdapter.changeModel(post, playload);
    }

    @Override
    public void onClickCreatedAt(Post post) {
        getView().showPost(post);
    }

    public void onResume() {
        if(lastLoadFirstPostsAtMillis <= 0 || feedAdapter == null) {
            return;
        }

        if(feedAdapter.getModelItemCount() <= 0) {
            this.retryLoadingPost();
            return;
        }

        Post model = feedAdapter.getFirstModel();
        if(model == null) {
            return;
        }

        long reload_gap_mills = 10 * 60 * 1000;
        if(System.currentTimeMillis() - lastLoadFirstPostsAtMillis > reload_gap_mills) {
            refreshPosts();
        }
    }

    public void refreshPosts() {
        feedAdapter.clearAndAppendPostLineForm();
        feedAdapter.notifyDataSetChanged();
        getView().showPostListDemo();
        loadFirstPosts();
    }

    public void showSettings() {
        getView().showSettings();
    }

    public void checkAppVersion() {
        if(this.appVersionCheckTask == null) {
            return;
        }

        this.appVersionCheckTask.check(new AppVersionCheckTask.NewVersionAction() {
            @Override
            public void run(String newVersion) {
                if (isActive()) getView().showNewVersionMessage(newVersion);
            }
        });
    }

    public void retryLoadingPost() {
        if(!isActive()) return;

        getView().readyToRetry();
        loadFirstPosts();
    }

    public void goToParties() {
        if(!isActive()) return;

        getView().showUrl(Uri.parse("https://parti.xyz/parties"));
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
                        //OK
                    }
                });
    }

    public void receivePushMessage(int notificatiionId, PushMessage pushMessage) {
        if(!isActive()) {
            Log.d(Constants.TAG_TEST, "NOT active");
            return;
        }

        if(notificatiionId != -1) {
            Log.d(Constants.TAG_TEST, "MAIN notificatiionId" + notificatiionId);
            NotificationsPreference repository = new NotificationsPreference(getView().getContext());
            repository.destroy(notificatiionId);
        }

        if(pushMessage == null) {
            Log.d(Constants.TAG_TEST, "NULL pushMessage");
            return;
        }
        if(pushMessage.user_id != session.getCurrentUser().id) {
            Log.d(Constants.TAG_TEST, "ANOTHER USER");
            return;
        }

        if("post".equals(pushMessage.type) && pushMessage.param != null) {
            long postId = Long.parseLong(pushMessage.param);
            if(postId <= 0) {
                getView().showMessage(getView().getContext().getResources().getString(R.string.not_found_post));
                return;
            }
            receivePushMessageForPostPublisher = getRxGuardian().subscribe(receivePushMessageForPostPublisher, postsService.getPost(postId),
                    new Consumer<Response<Post>>() {
                        @Override
                        public void accept(@io.reactivex.annotations.NonNull Response<Post> response) throws Exception {
                            if (response.isSuccessful()) {
                                getView().showPost(response.body());
                            } else {
                                getView().showMessage(getView().getContext().getResources().getString(R.string.not_found_post));
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@io.reactivex.annotations.NonNull Throwable error) throws Exception {
                            getView().reportError(error);
                        }
                    });
        } else if (pushMessage.url != null && !TextUtils.isEmpty(pushMessage.url)) {
            getView().showUrl(Uri.parse(pushMessage.url));
        }
    }

    public void checkReceivablePushMessage() {
        if(!isActive()) return;
        if(receivablePushMessageCheckTask == null) return;
        receivablePushMessageCheckTask.check();
    }

    public void showProfile() {
        if(!isActive()) return;

        getView().showProfile(getCurrentUser());
    }

    interface OnLoadPostFeed {
        void onDashbard();
        void onParti(Parti parti);
    }
    private void playWithPostFeed(final OnLoadPostFeed callback) {
        if(currentPostFeedId == Constants.POST_FEED_DASHBOARD) {
            callback.onDashbard();
            return;
        }

        if(currentParti != null && currentParti.id == currentPostFeedId) {
            callback.onParti(currentParti);
            return;
        }

        loadParti = getRxGuardian().subscribe(loadParti, partiesService.getParti(currentPostFeedId),
                new Consumer<Response<Parti>>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Response<Parti> response) throws Exception {
                        if(!isActive()) return;
                        if(response.isSuccessful()) {
                            Parti parti = response.body();
                            callback.onParti(parti);
                            currentParti = parti;
                        } else {
                            getView().reportError("fetch parti error : " + response.code());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable error) throws Exception {
                        getView().reportError(error);
                    }
                });
    }

    public void showPostForm() {
        if(!isActive()) return;

        playWithPostFeed(new OnLoadPostFeed() {
            @Override
            public void onDashbard() {
                getView().showPostForm();
            }

            @Override
            public void onParti(Parti parti) {
                getView().showPostForm(parti);
            }
        });
    }

    public void savePost(final Parti parti, final String body, List<SelectedImage> fileSourcesAttachmentImages) {
        if(!isActive()) return;

        getView().scrollToTop();
        feedAdapter.prependLoader();

        RequestBody partiIdReq = RequestBody.create(okhttp3.MultipartBody.FORM, parti.id.toString());
        RequestBody bodyReq = RequestBody.create(okhttp3.MultipartBody.FORM, body);

        List<MultipartBody.Part> filesReq = new ArrayList<>();
        for(SelectedImage image : fileSourcesAttachmentImages) {
            MultipartBody.Part request = buildFileSourceAttachmentRequest(image);
            if(request == null) continue;
            filesReq.add(request);
        }

        savePost = getRxGuardian().subscribe(savePost,
                postsService.createPost(partiIdReq, bodyReq, filesReq),
                new Consumer<Response<Post>>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Response<Post> response) throws Exception {
                        if (!isActive()) return;
                        feedAdapter.removeFirstMoldelHolder();
                        if (response.isSuccessful()) {
                            feedAdapter.prependModel(response.body());
                            getView().scrollToTop();
                        } else {
                            getView().reportError("savePost error : " + response.code());
                            getView().showPostForm(parti, body);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable error) throws Exception {
                        if (!isActive()) return;
                        feedAdapter.removeFirstMoldelHolder();
                        getView().reportError(error);
                        getView().showPostForm(parti, body);
                    }
                });
    }

    private MultipartBody.Part buildFileSourceAttachmentRequest(SelectedImage image) {
        if(image == null) return null;

        File file = new File(image.path);
        if(!file.exists()) {
            Log.d(Constants.TAG, "Not found file : " + image.path);
            return null;
        }
        RequestBody requestFile = RequestBody.create(MediaType.parse(getView().getContext().getContentResolver().getType(image.uri)), file);
        return MultipartBody.Part.createFormData("post[file_sources_attributes][][attachment]", file.getName(), requestFile);
    }

    public void loadDrawer() {
        if(!isActive()) return;

        if(lastPostFeedPreference.isNewbie()) {
            getView().openDrawer();
            lastPostFeedPreference.save(currentPostFeedId);
        }

        loadDrawer = getRxGuardian().subscribe(loadDrawer,
            partiesService.getMyJoined(),
            new Consumer<Response<Parti[]>>() {
                @Override
                public void accept(@io.reactivex.annotations.NonNull Response<Parti[]> response) throws Exception {
                    if (!isActive()) return;
                    if (!getView().canRefreshDrawer()) return;
                    if (response.isSuccessful()) {
                        joindedParties.clear();
                        joindedParties.addAll(Arrays.asList(response.body()));
                        for (Parti parti : joindedParties) {
                            preloadImage(parti.logo_url);
                        }
                        getView().setUpDrawerItems(session.getCurrentUser(), getGroupList(joindedParties), PostFeedPresenter.this.currentPostFeedId);
                    }
                    getView().ensureToHideDrawerDemo();

                    watchNewPosts();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@io.reactivex.annotations.NonNull Throwable error) throws Exception {
                    getView().reportError(error);
                    getView().ensureToHideDrawerDemo();
                }
            });
    }

    public void watchNewPosts() {
        if(newPostListener != null) {
            for(DatabaseReference partiFirebase : listenPartiFireBases) {
                partiFirebase.removeEventListener(newPostListener);
            }
        }

        newPostListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getKey() == null) return;
                long partiId = Long.parseLong(dataSnapshot.getKey());
                if(!isJoinedParti(partiId)) {
                    ReadPostFeed.destroyIfExist(partiId);
                    return;
                }

                ReadPostFeed readPostFeed = ReadPostFeed.forPartiOrDashboard(partiId);

                Long lastStrokedSecondTime = dataSnapshot.child("last_stroked_at").getValue(Long.class);
                if(lastStrokedSecondTime == null || lastStrokedSecondTime < 0) {
                    readPostFeed.lastStrokedAt = null;
                } else {
                    readPostFeed.lastStrokedAt = new Date(lastStrokedSecondTime * 1000);

                    ReadPostFeed readDashboard = ReadPostFeed.forDashboard();
                    if(readDashboard.lastStrokedAt == null || readPostFeed.lastStrokedAt.getTime() > readDashboard.lastStrokedAt.getTime()) {
                        readDashboard.lastStrokedAt = readPostFeed.lastStrokedAt;
                        readDashboard.save();
                    }
                }
                readPostFeed.save();
                markUnreadOrNotParti(readPostFeed);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(Constants.TAG, databaseError.getMessage(), databaseError.toException());
            }
        };

        for(Parti parti : this.joindedParties) {
            DatabaseReference partiFirebase = partiesFirebaseRoot.child(String.valueOf(parti.id));
            partiFirebase.addValueEventListener(newPostListener);
            listenPartiFireBases.add(partiFirebase);
        }
    }

    private boolean isJoinedParti(long currentId) {
        for(Parti parti : this.joindedParties) {
            if(parti.id.equals(currentId)) {
                return true;
            }
        }
        return false;
    }

    private void markUnreadOrNotParti(ReadPostFeed readPostFeed) {
        if(!isActive()) return;

        getView().markUnreadOrNotDashboard(ReadPostFeed.forDashboard().isUnread());
        if(!readPostFeed.isDashboard()) {
            getView().markUnreadOrNotParti(readPostFeed.partiId, readPostFeed.isUnread());
        }
    }

    public void unwatchNewPosts() {
        if(newPostListener != null) {
            for(DatabaseReference partiFirebase : listenPartiFireBases) {
                partiFirebase.removeEventListener(newPostListener);
            }
        }
//
//        this.watchedPartiesFirebase.clear();
    }

    @NonNull
    private TreeMap<Group, List<Parti>> getGroupList(List<Parti> parties) {
        TreeMap<Group, List<Parti>> result = new TreeMap<>();
        for(Parti parti : parties) {
            List<Parti> items = result.get(parti.group);
            if(items == null) {
                items = new ArrayList<>();
            }
            items.add(parti);
            result.put(parti.group, items);
        }
        return result;
    }

    public void showDashboardPostFeed() {
        if(!isActive()) return;
        if(currentPostFeedId == Constants.POST_FEED_DASHBOARD) return;

        currentPostFeedId = Constants.POST_FEED_DASHBOARD;
        currentParti = null;
        lastPostFeedPreference.save(currentPostFeedId);
        getView().changeDashboardToolbar();
        refreshPosts();
    }

    public void showPartiPostFeed(Parti parti) {
        if(!isActive()) return;
        if(currentPostFeedId != Constants.POST_FEED_DASHBOARD && parti.id.equals(currentPostFeedId)) return;

        currentPostFeedId = parti.id;
        currentParti = parti;
        lastPostFeedPreference.save(currentPostFeedId);
        getView().changePartiPostFeedToolbar(parti);
        refreshPosts();
    }

    public interface View extends BasePostBindablePresenter.View {
        void stopAndEnableSwipeRefreshing();
        boolean isVisibleNewPostsSign();
        void showNewPostsSign();
        void readyToShowPostFeed();
        void showPostListDemo();
        void ensureExpendedAppBar();
        void showSettings();

        Context getContext();
        void showNewVersionMessage(String newVersion);
        void showMessage(String message);
        void showEmpty(boolean isError);
        void showBlocked();
        void readyToRetry();
        void showPostForm();
        void showPostForm(Parti parti);
        void showPostForm(Parti parti, String body);
        void scrollToTop();

        void setUpDrawerItems(User currentUser, TreeMap<Group, List<Parti>> joindedParties, long currentPartiId);
        void ensureToHideDrawerDemo();

        void markUnreadOrNotParti(long partiId, boolean unread);
        void markUnreadOrNotDashboard(boolean unread);

        void changeDashboardToolbar();
        void changePartiPostFeedToolbar(Parti parti);

        boolean canRefreshDrawer();
        void openDrawer();
        void showProfile(User user);
    }
}
