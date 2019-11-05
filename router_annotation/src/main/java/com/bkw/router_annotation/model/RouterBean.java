package com.bkw.router_annotation.model;


import javax.lang.model.element.Element;

/**
 * PathBean的升级版
 *
 * @author bkw
 */
public class RouterBean {

    /**
     * 定义枚举
     */
    public enum Type {
        ACTIVITY,
        CALL
    }

    /**
     * 类信息
     */
    private Element element;

    /**
     * 组名
     */
    private String group;
    /**
     * 路径
     */
    private String path;

    /**
     * 目标类
     */
    private Class<?> clazz;

    /**
     * 类型
     */
    private Type type;

    private RouterBean(Builder builder) {
        this.path = builder.path;
        this.group = builder.group;
        this.element = builder.element;
    }

    private RouterBean(Type type, Class<?> clazz, String path, String group) {
        this.type = type;
        this.clazz = clazz;
        this.path = path;
        this.group = group;

    }

    public static RouterBean create(Type type, Class<?> clazz, String path, String group) {
        return new RouterBean(type, clazz, path, group);
    }

    public Element getElement() {
        return element;
    }

    public String getGroup() {
        return group;
    }

    public String getPath() {
        return path;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Type getType() {
        return type;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * 构建者模式
     */
    public static final class Builder {

        //类节点
        private Element element;

        /**
         * 路由的组名
         */
        private String group;

        /**
         * 路径名
         */
        private String path;

        public Builder setElement(Element element) {
            this.element = element;
            return this;
        }

        public Builder setGroup(String group) {
            this.group = group;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        /**
         * @description 最后的build或者create方法，往往是做参数校验或初始化操作
         */
        public RouterBean build() {
            if (path == null || path.length() == 0) {
                throw new IllegalArgumentException("path必填项为空，如：/app/MainActivity");
            }

            return new RouterBean(this);
        }
    }
}
