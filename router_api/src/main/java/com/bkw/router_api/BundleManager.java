package com.bkw.router_api;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityOptionsCompat;

import com.bkw.router_api.core.Call;

import org.jetbrains.annotations.Nullable;

public class BundleManager {

    private Bundle bundle = new Bundle();
    /**
     * 底层业务接口
     */
    private Call call;

    /**
     * 是否需要setResult回调
     */
    private boolean isResult;

    /**
     * 转场动画
     */
    private int enterAnim = -1;
    private int exitAnim = -1;
    private Bundle optionsCompat;

    public Bundle getBundle() {
        return bundle;
    }

    public int getEnterAnim() {
        return enterAnim;
    }

    public int getExitAnim() {
        return exitAnim;
    }

    public Bundle getOptionsCompat() {
        return optionsCompat;
    }

    public boolean isResult() {
        return isResult;
    }

    public Call getCall() {
        return call;
    }

    public void setCall(Call call) {
        this.call = call;
    }

    public BundleManager withString(String key, String value) {
        bundle.putString(key, value);
        return this;
    }

    // 示例代码，需要拓展
    public BundleManager withResultString(String key, @Nullable String value) {
        bundle.putString(key, value);
        isResult = true;
        return this;
    }

    public BundleManager withBoolean(String key, boolean value) {
        bundle.putBoolean(key, value);
        return this;
    }

    public BundleManager withInt(String key, int value) {
        bundle.putInt(key, value);
        return this;
    }

    public BundleManager withBundle(Bundle bundle) {
        this.bundle = bundle;
        return this;
    }

    /**
     * 转场动画
     *
     * @param enterAnim
     * @param exitAnim
     * @return
     */
    public BundleManager withTransition(int enterAnim, int exitAnim) {
        this.enterAnim = enterAnim;
        this.exitAnim = exitAnim;
        return this;
    }


    /**
     * Set options compat
     *
     * @param compat compat
     * @return this
     */
    @RequiresApi(16)
    public BundleManager withOptionsCompat(ActivityOptionsCompat compat) {
        if (null != compat) {
            this.optionsCompat = compat.toBundle();
        }
        return this;
    }

    public Object navigation(Context context) {
        return navigation(context, -1);
    }


    public Object navigation(Context context, int code) {
        return RouterManager.getInstance().navigation(context, this, code);
    }

}
