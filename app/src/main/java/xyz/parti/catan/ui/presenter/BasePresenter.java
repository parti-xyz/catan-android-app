package xyz.parti.catan.ui.presenter;

import xyz.parti.catan.data.model.PushMessage;
import xyz.parti.catan.helper.RxGuardian;

/**
 * Created by dalikim on 2017. 5. 7..
 */

abstract class BasePresenter<T> {
    private T view;
    private RxGuardian rxGuardian = new RxGuardian();

    public void attachView(T view) {
        this.view = view;
        this.rxGuardian.unsubscribeAll();
    }

    public void detachView() {
        this.rxGuardian.unsubscribeAll();
        this.view = null;
    }

    RxGuardian getRxGuardian() {
        return this.rxGuardian;
    }

    public T getView() {
        return view;
    }

    private boolean isViewAttached() {
        return view != null;
    }

    void checkViewAttached() {
        if (!isViewAttached()) throw new ViewNotAttachedException();
    }



    private static class ViewNotAttachedException extends RuntimeException {
        public ViewNotAttachedException() {
            super("Please call Presenter.attachView(MvpView) before" +
                    " requesting data to the Presenter");
        }
    }
}
