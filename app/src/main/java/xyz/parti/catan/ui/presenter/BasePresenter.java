package xyz.parti.catan.ui.presenter;

import xyz.parti.catan.helper.RxGuardian;


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
}
