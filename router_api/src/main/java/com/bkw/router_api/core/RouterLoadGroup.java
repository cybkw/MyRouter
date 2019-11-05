package com.bkw.router_api.core;

import java.util.Map;

/**
 * group提供对外加载组名的路径信息类的接口
 *
 * @author bkw
 */
public interface RouterLoadGroup {
    /**
     * @return 对应组名下的path集合
     */
    Map<String, Class<? extends RouterLoadPath>> loadGroup();
}
