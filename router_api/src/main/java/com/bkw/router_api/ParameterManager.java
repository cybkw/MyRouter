package com.bkw.router_api;

import android.app.Activity;
import android.util.LruCache;

import com.bkw.router_api.core.ParameterLoad;


/**
 * 参数Parameter加载管理器
 * @author bkw
 */
public class ParameterManager {

    private static ParameterManager instance;
    /**
     * Lru缓存，key:类名，value：参数Parameter加载接口
     */
    private LruCache<String, ParameterLoad> cache;

    /**
     * APT生成的获取参数源文件，后缀名
     */
    private static final String FILE_SUFFIX_NAME = "$$Parameter";

    private static final int MAX_SIZE = 100;

    public static ParameterManager getInstance() {
        if (null == instance) {
            synchronized (ParameterManager.class) {
                if (null == instance) {
                    instance = new ParameterManager();
                }
            }
        }
        return instance;
    }

    private ParameterManager() {
        //初始化，并赋值缓存中条目最大值
        cache = new LruCache<>(MAX_SIZE);
    }


    /**
     * 传入的Activity中所有被@Paramater注解的属性，通过加载APT生成源文件，并给属性赋值
     *
     * @param activity 需要给属性赋值的类，如：MainActivity中所有被@Parameter注解的属性
     */
    public void loadParameter(Activity activity) {
        String className = activity.getClass().getName();

        //查找缓存集合中是否有对应activity的value
        ParameterLoad parameterLoad = cache.get(className);

        //找不到，加载类后放入缓存集合
        try {
            if (parameterLoad == null) {
                Class<?> clazz = Class.forName(className + FILE_SUFFIX_NAME);
                parameterLoad = (ParameterLoad) clazz.newInstance();
                cache.put(className, parameterLoad);
            }

            //通过传入参数给生成的源文件中所有属性赋值
            parameterLoad.loadParameter(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
