package com.bkw.router_compiler;

import com.bkw.router_annotation.core.Router;
import com.bkw.router_annotation.model.RouterBean;
import com.bkw.router_compiler.utils.Cons;
import com.bkw.router_compiler.utils.EmptyUtils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * 自定义路由注解处理器
 *
 * @author bkw
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes(Cons.ROUTER_ANNOTATIONS_TYPES) //注解类型
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({Cons.MODULE_NAME, Cons.APT_PACKAGE}) //注解传递参数
public class RouterAnnotationProcessor extends AbstractProcessor {

    /**
     * 类节点工具类
     */
    private Elements elementUtils;

    /**
     * 日志信息类
     */
    private Messager messager;

    /**
     * 文件生成器
     */
    private Filer filer;

    /**
     * 类信息工具类
     */
    private Types typeUtils;


    private static Map<String, List<RouterBean>> tempPathMap = new HashMap<>();
    private static Map<String, String> tempGroupMap = new HashMap<>();

    /**
     * 模块名
     */
    private String moduleName;

    private String packageNameForAPT;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();

        // 通过ProcessingEnvironment去获取对应的参数
        Map<String, String> map = processingEnv.getOptions();
        if (!EmptyUtils.isEmpty(map)) {
            moduleName = map.get(Cons.MODULE_NAME);
            packageNameForAPT = map.get(Cons.APT_PACKAGE);
        }

        // 必传参数判空
        //判断是否有处理APT参数
        if (EmptyUtils.isEmpty(moduleName) || EmptyUtils.isEmpty(packageNameForAPT)) {
            messager.printMessage(Diagnostic.Kind.NOTE, "APT参数moduleName或packageName未配置");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        //判断是否有被@Router注解的节点
        Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(Router.class);
        if (elementsAnnotatedWith.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.NOTE, "当前程序未存在被@Router注解的节点");
            return false;
        }

        //解析类节点
        parserElement(elementsAnnotatedWith);


        return true;
    }

    private void parserElement(Set<? extends Element> elementsAnnotatedWith) {
        // 通过Element工具类，获取Activity、Callback类型
        TypeElement activityType = elementUtils.getTypeElement(Cons.ACTIVITY);
        TypeElement callType = elementUtils.getTypeElement(Cons.CALL);

        // 显示类信息（获取被注解节点，类节点）这里也叫自描述 Mirror
        TypeMirror activityMirror = activityType.asType();
        TypeMirror callMirror = callType.asType();

        // 遍历节点
        for (Element element : elementsAnnotatedWith) {
            // 获取每个元素类信息，用于比较
            TypeMirror typeMirror = element.asType();
            // 获取每个类上的@ARouter注解中的注解值
            Router router = element.getAnnotation(Router.class);

            // 路由详细信息，最终实体封装类
            RouterBean bean = new RouterBean.Builder()
                    .setElement(element)
                    .setGroup(router.group())
                    .setPath(router.path())
                    .build();

            // 高级判断：ARouter注解仅能用在类之上，并且是规定的类型，Activity或业务接口Call
            // 类型工具类方法isSubtype，相当于instance一样
            if (typeUtils.isSubtype(typeMirror, activityMirror)) {
                bean.setType(RouterBean.Type.ACTIVITY);
            } else if (typeUtils.isSubtype(typeMirror, callMirror)) {
                bean.setType(RouterBean.Type.CALL);
            } else {
                //不匹配类型，抛出异常
                throw new RuntimeException("@Router目前只能作用于Activity之上");
            }

            // 赋值临时map存储，用来存放路由组Group对应的详细Path类对象
            valuesOfMap(bean);
        }

        // routerMap遍历后，用来生成类文件
        // 获取ARouterLoadGroup、ARouterLoadPath类型（生成类文件需要实现的接口）
        TypeElement groupType = elementUtils.getTypeElement(Cons.ROUTER_GROUP_INTERFACE);
        TypeElement pathType = elementUtils.getTypeElement(Cons.ROUTER_PATH_INTERFACE);

        //创建Path路径类
        createPathFile(pathType);

        //创建Group类文件
        createGroupFile(groupType, pathType);
    }

    /**
     * 生成路由组Group文件，如：ARouter$$Group$$app
     *
     * @param groupType ARouterLoadGroup接口信息
     * @param pathType  ARouterLoadPath接口信息
     */
    private void createGroupFile(TypeElement groupType, TypeElement pathType) {
        //判断是否有需要生成的类文件
        if (EmptyUtils.isEmpty(tempPathMap) || EmptyUtils.isEmpty(tempGroupMap)) {
            return;
        }

        //创建方法返回值
        TypeName methodReturns = ParameterizedTypeName.get(
                ClassName.get(Map.class), // Map
                ClassName.get(String.class), // Map<String,
                // 第二个参数：Class<? extends ARouterLoadPath>
                // 某某Class是否属于ARouterLoadPath接口的实现类
                ParameterizedTypeName.get(ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(pathType)))
        );


        // 方法配置：public Map<String, Class<? extends ARouterLoadPath>> loadGroup() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Cons.METHOD_GROUP_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(methodReturns);

        // 遍历之前：Map<String, Class<? extends ARouterLoadPath>> groupMap = new HashMap<>();
        methodBuilder.addStatement("$T<$T,$T> $N=new $T<>()",
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(pathType))),
                Cons.PARAMETER_GROUP_NAME,
                HashMap.class);

        // 方法内容配置
        for (Map.Entry<String, String> entry : tempGroupMap.entrySet()) {
            // 类似String.format("hello %s net163 %d", "net", 163)通配符
            // groupMap.put("main", ARouter$$Path$$app.class);
            methodBuilder.addStatement("$N.put($S, $T.class)",
                    // groupMap.put
                    Cons.PARAMETER_GROUP_NAME,
                    entry.getKey(),
                    // 类文件在指定包名下
                    ClassName.get(packageNameForAPT, entry.getValue()));
        }

        methodBuilder.addStatement("return $N", Cons.PARAMETER_GROUP_NAME);

        //最终生成的类文件名
        String finalClassName = Cons.PRE_GROUP_SUFFIX + moduleName;
        messager.printMessage(Diagnostic.Kind.NOTE, "APT生成路由组Group类文件：" +
                packageNameForAPT + "." + finalClassName);

        try {
            JavaFile.builder(packageNameForAPT,
                    TypeSpec.classBuilder(finalClassName)
                            .addMethod(methodBuilder.build())
                            .addModifiers(Modifier.PUBLIC)
                            .addSuperinterface(ClassName.get(groupType))
                            .build()).build()
                    .writeTo(filer);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成路由组Group对应详细Path，如：ARouter$$Path$$app
     *
     * @param pathType ARouterLoadPath接口信息
     */
    private void createPathFile(TypeElement pathType) {
        // 判断是否有需要生成的类文件
        if (EmptyUtils.isEmpty(tempPathMap)) {
            return;
        }

        //创建方法返回类型
        ParameterizedTypeName methodReturns = ParameterizedTypeName.get(
                ClassName.get(Map.class),//Map<
                ClassName.get(String.class),//Map<String
                ClassName.get(RouterBean.class));//Map<String,RouterBean>


        // 遍历分组，每一个分组创建一个路径类文件，如：ARouter$$Path$$app
        for (Map.Entry<String, List<RouterBean>> entry : tempPathMap.entrySet()) {
            // 方法配置：public Map<String, RouterBean> loadPath() {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Cons.METHOD_PATH_NAME)//方法名
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class) //重写接口方法
                    .returns(methodReturns);

            // 遍历之前：创建方法体第一行Map<String, RouterBean> pathMap = new HashMap<>();
            methodBuilder.addStatement("$T<$T,$T> $N=new $T<>()",
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(RouterBean.class),
                    Cons.PARAMETER_PATH_NAME,
                    HashMap.class);

            // 一个分组，如：ARouter$$Path$$app。有很多详细路径信息，如：/app/MainActivity、/app/OtherActivity
            List<RouterBean> pathList = entry.getValue();

            // 方法内容配置（遍历每个分组中每个路由详细路径）
            for (RouterBean bean : pathList) {
                // 类似String.format("hello %s net163 %d", "net", 163)通配符
                // pathMap.put("/app/MainActivity", RouterBean.create(
                //        RouterBean.Type.ACTIVITY, MainActivity.class, "/app/MainActivity", "app"));
                methodBuilder.addStatement(
                        "$N.put($S,$T.create($T.$L,$T.class,$S,$S))",
                        Cons.PARAMETER_PATH_NAME,
                        bean.getPath(),
                        ClassName.get(RouterBean.class),
                        ClassName.get(RouterBean.Type.class),
                        bean.getType(),
                        ClassName.get((TypeElement) bean.getElement()),
                        bean.getPath(),
                        bean.getGroup()
                );
            }

            // 遍历之后：return pathMap;
            methodBuilder.addStatement("return $N", Cons.PARAMETER_PATH_NAME);

            // 最终生成的类文件名,前缀加组名
            String finalClassName = Cons.PRE_PATH_SUFFIX + entry.getKey();
            messager.printMessage(Diagnostic.Kind.NOTE, "APT生成路由Path类文件：" +
                    packageNameForAPT + "." + finalClassName);

            //生成类文件
            try {
                JavaFile.builder(packageNameForAPT,
                        TypeSpec.classBuilder(finalClassName)
                                .addSuperinterface(ClassName.get(pathType))
                                .addModifiers(Modifier.PUBLIC)
                                .addMethod(methodBuilder.build())
                                .build()).build()
                        .writeTo(filer);

                // 非常重要一步！！！！！路径文件生成出来了，才能赋值路由组tempGroupMap
                tempGroupMap.put(entry.getKey(), finalClassName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private void valuesOfMap(RouterBean bean) {
        //校验注解值
        if (checkRouterPath(bean)) {
            // 开始赋值Map
            List<RouterBean> routerBeans = tempPathMap.get(bean.getGroup());
            if (EmptyUtils.isEmpty(routerBeans)) {
                routerBeans = new ArrayList<>();
                routerBeans.add(bean);
                tempPathMap.put(bean.getGroup(), routerBeans);
            } else {
                routerBeans.add(bean);
            }
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, "@Router注解未按照规范，如：/app/MainActivity");
        }
    }

    /**
     * 校验@ARouter注解的值，如果group未填写就从必填项path中截取数据
     *
     * @param bean 路由详细信息，最终实体封装类
     */
    private boolean checkRouterPath(RouterBean bean) {
        //忽略空格
        String group = bean.getGroup();
        //忽略空格
        String path = bean.getPath().trim();

        //避免开发者第一种情况app/MainActivity
        if (EmptyUtils.isEmpty(path) || !path.startsWith("/")) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@Router注解中的path值，必须以/开头"+path+","+group);
            return false;
        }

        //避免开发者的第二种情况 /MainActivity
        if (path.lastIndexOf("/") == 0) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@Router注解未按照规范，如：/app/MainActivity"+path+","+group);
            return false;
        }

        //避免开发者第三种情况 /app/ssfsf/MainActivity
        // 从第一个 / 到第二个 / 中间截取，如：/app/MainActivity 截取出 app 作为group
        String finalGroup = path.substring(1, path.indexOf("/", 1));

        //考虑@Router注解中group有值的情况
        if (!EmptyUtils.isEmpty(group) && !group.equals(moduleName)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@Router注解中的group值必须和模块名一致");
            return false;
        } else {
            bean.setGroup(finalGroup);
        }

        return true;
    }
}
