package org.geektimes.projects.user.web.context;

import org.geektimes.function.ThrowableFunction;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.naming.*;
import javax.servlet.ServletContext;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @Author: menglinggang
 * @Date: 2021-03-10
 * @Time: 9:09 上午
 */

public class ComponentContext {

    private HashMap<String, Object> componentMap = new HashMap<>();

    private static ServletContext servletContext;

    Logger logger = Logger.getLogger(ComponentContext.class.getName());

    private ClassLoader classLoader;

    private Context envContext;

    private static final String CONTEXT_NAME = "ccomponent_name";

    public static ComponentContext getComponentContext(){
        return (ComponentContext) servletContext.getAttribute(CONTEXT_NAME);
    }

    public void init(ServletContext servletContext) {
        ComponentContext.servletContext = servletContext;
        this.classLoader = ComponentContext.class.getClassLoader();
        servletContext.setAttribute(CONTEXT_NAME, this);
        // 初始化JNDN基础环境
        initEnvContext();
        // 实例化（遍历env中所有的节点并，对每一个设置的对象进行实例化并用名字进行缓存）
        instantiateComponents();
        // 初始化
        initializeComponents();
    }

    private void instantiateComponents() {
        List<String> allBeanNames = findAllBeanNames("/");
        allBeanNames.stream().forEach(item -> {
            componentMap.put(item, lookupComponent(item));
        });
        servletContext.log("load component size:" + componentMap.size());
        servletContext.log("load component context:" + JSONObject.valueToString(componentMap));
    }

    private <C> C lookupComponent(String name) {
        return executeInContext(context -> (C) context.lookup(name), true);
    }

    /**
     * 在 Context 中执行，通过指定 ThrowableFunction 返回计算结果
     *
     * @param function         ThrowableFunction
     * @param ignoredException 是否忽略异常
     * @param <R>              返回结果类型
     * @return 返回
     * @see ThrowableFunction#apply(Object)
     */
    protected <R> R executeInContext(ThrowableFunction<Context, R> function, boolean ignoredException) {
        return executeInContext(this.envContext, function, ignoredException);
    }

    private <R> R executeInContext(Context context, ThrowableFunction<Context, R> function,
                                   boolean ignoredException) {
        R result = null;
        try {
            result = ThrowableFunction.execute(context, function);
        } catch (Throwable e) {
            if (ignoredException) {
                logger.warning(e.getMessage());
            } else {
                logger.warning("XXXXXXXXXXXXXXXXX：" + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public <C> C getComponent(String name) {
        return (C) componentMap.get(name);
    }

    public List<String> getComponentNames(){
        return new ArrayList<>(componentMap.keySet());
    }

    private List<String> findAllBeanNames(String path) {
        return executeInContext(context -> {
        NamingEnumeration<NameClassPair> nameClassPairNamingEnumeration = executeInContext(context, c -> c.list(path), true);
        if (nameClassPairNamingEnumeration == null) {
            return Collections.emptyList();
        }

        List<String> fullNames = new LinkedList<>();
        while (nameClassPairNamingEnumeration.hasMoreElements()){
            NameClassPair element = nameClassPairNamingEnumeration.nextElement();
            String className = element.getClassName();
            Class<?> targetClass = classLoader.loadClass(className);
            if (Context.class.isAssignableFrom(targetClass)) {
                // 如果当前名称是目录（Context 实现类）的话，递归查找
                fullNames.addAll(findAllBeanNames(element.getName()));
            } else {
                // 否则，当前名称绑定目标类型的话话，添加该名称到集合中
                String fullName = path.startsWith("/") ? element.getName() : path + "/" + element.getName();
                fullNames.add(fullName);
            }
        }
        return fullNames;
        }, true);
    }

    private void initializeComponents() {
        componentMap.values().stream().forEach(component ->{
            Class<?> componentClass = component.getClass();
            // 注入阶段 - {@link Resource}
            injectComponents(component, componentClass);
            // 初始阶段 - {@link PostConstruct}
            processPostConstruct(component, componentClass);
            // TODO 实现销毁阶段 - {@link PreDestroy}
            //processPreDestroy();
        });
    }

    private void processPostConstruct(Object component, Class<?> componentClass) {
        Method[] declaredMethods = componentClass.getDeclaredMethods();
        if (declaredMethods.length != 0) {
            Arrays.stream(declaredMethods).filter(method -> {
                int modifiers = method.getModifiers();
                return method.getParameterCount() == 0 && !Modifier.isStatic(modifiers) && method.isAnnotationPresent(PostConstruct.class);
            }).forEach(method -> {
                try {
                    method.invoke(component);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * 遍历找到所有需要注入的组件，对组件进行遍历（如果有多层级依赖的时候该方法不能正确的执行，）
     * @param component
     * @param componentClass
     */
    private void injectComponents(Object component, Class<?> componentClass) {
        Arrays.stream(componentClass.getDeclaredFields()).filter(field -> {
            int mods = field.getModifiers();
            return !Modifier.isStatic(mods) &&
                    field.isAnnotationPresent(Resource.class);
        }).forEach(field -> {
            Resource annotation = field.getAnnotation(Resource.class);
            field.setAccessible(true);
            try {
                servletContext.log("3333333333333333:" + annotation.name());
                field.set(component, lookupComponent(annotation.name()));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void initEnvContext() {
        try {
            InitialContext context = new InitialContext();
            envContext = (Context) context.lookup("java:comp/env");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {

    }


}
