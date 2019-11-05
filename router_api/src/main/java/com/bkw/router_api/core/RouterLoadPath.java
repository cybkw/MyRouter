package com.bkw.router_api.core;


import com.bkw.router_annotation.model.RouterBean;

import java.util.Map;

/**
 * Path对外提供的路径下信息及目标类接口
 *
 * @author bkw
 */
public interface RouterLoadPath {

    Map<String, RouterBean> loadPath();
}
