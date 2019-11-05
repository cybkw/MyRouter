package com.bkw.router_api;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.bkw.router_annotation.model.RouterBean;
import com.bkw.router_api.core.RouterLoadGroup;
import com.bkw.router_api.core.RouterLoadPath;

/**
 * 路由路径跳转管理器
 *
 * @author bkw
 */
public class RouterManager {
    private final int MAX_SIZE = 100;
    /**
     * 路由组名
     */
    private String group;
    /**
     * 路由详细路径
     */
    private String path;


    /**
     * Lru缓存，key:类名，value:路由组group加载接口
     */
    private static LruCache<String, RouterLoadGroup> groupLruCache;


    /**
     * Lru缓存，key:类名，value:路由组对应的详细Path加载接口
     */
    private static LruCache<String, RouterLoadPath> pathLruCache;

    /**
     * APT生成的路由组Group源文件前缀名 （包名拼接）
     */
    private static final String GROUP_FILE_PRE_SUFFIX = ".Router$$Group$$";

    private static RouterManager instance;

    public static RouterManager getInstance() {
        if (instance == null) {
            synchronized (RouterManager.class) {
                if (instance == null) {
                    instance = new RouterManager();
                }
            }
        }
        return instance;
    }

    private RouterManager() {
        //初始化，赋值缓存中条目的最大值，最多100组
        groupLruCache = new LruCache<>(MAX_SIZE);
        //每组最多100条路径值
        pathLruCache = new LruCache<>(MAX_SIZE);
    }

    /**
     * @param path 路由地址，如：（"/personal/Personal_MainActivity"）
     * @return
     */
    public BundleManager build(String path) {
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new IllegalArgumentException("未按照规范传递，如：/personal/Personal_MainActivity");
        }

        group = subFromPath2Group(path);

        this.path = path;
        return new BundleManager();
    }

    /**
     * 截取组名
     *
     * @param path path 路由地址，如：（"/personal/Personal_MainActivity"）
     * @return 最终组名，如：app
     */
    private String subFromPath2Group(String path) {
        //如果开发者写成："/MainActivity"
        if (path.lastIndexOf("/") == 0) {
            throw new IllegalArgumentException("@ARouter未按照规范传递，如：/personal/Personal_MainActivity");
        }

        String finalGroup = path.substring(1, path.indexOf("/", 1));

        if (TextUtils.isEmpty(finalGroup)) {
            //架构师定义规范，让开发者遵循
            throw new IllegalArgumentException("@ARouter未按照规范传递，如：/personal/Personal_MainActivity");
        }

        return finalGroup;
    }

    /**
     * 跳转操作
     *
     * @param context       上下文
     * @param bundleManager 参数管理对象
     * @param code          requestCode或resultCode,取决于isResult
     * @return 用于跨模块接口Call，普通跳转可以忽略。
     */
    public Object navigation(Context context, BundleManager bundleManager, int code) {
        // 精华：阿里的路由path随意写，导致无法找到随意拼接APT生成的源文件，如：ARouter$$Group$$abc
        // 找不到，就加载私有目录下apk中的所有dex并遍历，获得所有包名为xxx的类。并开启了线程池工作
        // 这里的优化是：代码规范写法，准确定位ARouter$$Group$$app
        //如果context=orderMainActivity.this, com.bkw.module.apt.ARouter$$Group$$+order
        String groupClassName = context.getPackageName() + ".apt" + GROUP_FILE_PRE_SUFFIX + group;
        Log.e("TAG", "groupClassName>>>" + groupClassName);

        try {
            RouterLoadGroup loadGroup = groupLruCache.get(group);
            if (loadGroup == null) {
                Class<?> clazz = Class.forName(groupClassName);
                loadGroup = (RouterLoadGroup) clazz.newInstance();
                groupLruCache.put(group, loadGroup);
            }

            // 获取路由路径类ARouter$$Path$$app
            if (loadGroup.loadGroup().isEmpty()) {
                throw new RuntimeException("路由Group加载失败");
            }


            //读取路由Path类文件的缓存
            RouterLoadPath pathLoad = getPathLoad(loadGroup);
            if (pathLoad != null) {
                // tempMap赋值
                pathLoad.loadPath();

                if (pathLoad.loadPath().isEmpty()) {
                    throw new RuntimeException("路由Path加载失败");
                }

                //通过pathLoad得到RouterBean对象，取出对应的目标class
                RouterBean routerBean = pathLoad.loadPath().get(path);
                if (routerBean != null) {
                    switch (routerBean.getType()) {
                        case ACTIVITY:

                            Intent intent = new Intent(context, routerBean.getClazz());
                            Log.e("RouterManager", " >> >>>>>case ACTIVITY" + routerBean.getClazz().getName());
                            if (bundleManager.getBundle() != null) {
                                intent.putExtras(bundleManager.getBundle());
                            }

                            // startActivityForResult -> setResult
                            if (bundleManager.isResult()) {
                                ((Activity) context).setResult(code, intent);
                                ((Activity) context).finish();
                            }

//                            if (code > 0) {
//                                // 跳转时是否回调
//                                ((Activity) context).startActivityForResult(intent, code, bundleManager.getBundle());
//                                Log.e("RouterManager", ">>>>>>>>context.startActivityForResult");
//                            } else {
//                                context.startActivity(intent, bundleManager.getBundle());
//                                Log.e("RouterManager", ">>>>>>>>>context.startActivity");
//                            }

                            //如果使用Context的startActivity，则需要开启一个新的task.
                            if (context instanceof Application) {
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }

                            if (code > 0) {
                                ActivityCompat.startActivityForResult((Activity) context, intent, code, bundleManager.getBundle());
                            } else {
                                ActivityCompat.startActivity(context, intent, bundleManager.getBundle());

                            }

                            //是否设置了转场动画
                            if (bundleManager.getEnterAnim() != -1 && bundleManager.getExitAnim() != -1 && context instanceof Activity) {
                                ((Activity) context).overridePendingTransition(bundleManager.getEnterAnim(), bundleManager.getExitAnim());
                            }


                            break;

                        case CALL:
                            //这里返回的就是Call接口的实现类
                            return routerBean.getClazz().newInstance();
                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private RouterLoadPath getPathLoad(RouterLoadGroup groupLoad) throws Exception {
        RouterLoadPath pathLoad = pathLruCache.get(path);
        if (pathLoad == null) {
            Class<? extends RouterLoadPath> clazz = groupLoad.loadGroup().get(group);
            if (clazz != null) {
                pathLoad = clazz.newInstance();
            }
            if (pathLoad != null) {
                pathLruCache.put(path, pathLoad);
            }
        }
        return pathLoad;
    }
}
