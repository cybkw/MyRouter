package com.bkw.router_compiler.utils;

/**
 * 注解配置常量类
 *
 * @author bkw
 */
public class Cons {

    /**
     * 路由注解全路径
     */
    public static final String ROUTER_ANNOTATIONS_TYPES = "com.bkw.router_annotation.core.Router";

    /**
     * 参数注解全路径
     */
    public static final String PARAMETER_ANNOTATIONS_TYPES = "com.bkw.router_annotation.core.Parameter";


    /**
     * 每个子模块的模块名
     */
    public static final String MODULE_NAME = "moduleName";

    /**
     * 模块包名：用于存储APT生成的类文件
     */
    public static final String APT_PACKAGE = "packageNameForAPT";

    public static final String BASE_PACKAGE = "com.bkw.router_api";
    /**
     * Group加载数据接口
     */
    public static final String ROUTER_GROUP_INTERFACE = BASE_PACKAGE + ".core.RouterLoadGroup";

    /**
     * Path路径加载数据接口
     */
    public static final String ROUTER_PATH_INTERFACE = BASE_PACKAGE + ".core.RouterLoadPath";

    /**
     * 参数加载数据接口
     */
    public static final String PARAMETER_INTERFACE = BASE_PACKAGE + ".core.ParameterLoad";

    /**
     * 跨模块业务，回调接口
     */
    public static final String CALL = BASE_PACKAGE + ".core.Call";

    public static final String METHOD_PARAMETER_NAME = "loadParameter";

    /**
     * Parameter获取参数，参数名
     */
    public static final String PARAMETER_NAME = "target";

    /**
     * 路由组Group,方法名
     */
    public static final String METHOD_GROUP_NAME = "loadGroup";

    /**
     * 路由组Group 参数名
     */
    public static final String PARAMETER_GROUP_NAME = "groupMap";

    /**
     * 路由组Path,方法名
     */
    public static final String METHOD_PATH_NAME = "loadPath";

    /**
     * 路由组Path 参数名
     */
    public static final String PARAMETER_PATH_NAME = "pathMap";

    /**
     * APT生成路由类名文件前缀
     */
    public static final String PRE_GROUP_SUFFIX = "Router$$Group$$";
    public static final String PRE_PATH_SUFFIX = "Router$$Path$$";

    /**
     * String 全类名
     */
    public static final String STRING = "java.lang.String";

    /**
     * Activity全类名
     */
    public static final String ACTIVITY = "android.app.Activity";

    /**
     * RouterManager类名
     */
    public static final String ROUTER_MANAGER = "RouterManager";
    public static final String PARAMETER_FILE_NAME = "$$Parameter";
}
