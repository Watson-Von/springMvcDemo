package com.xiaofong.springmvc.dispatcherServlet;

import com.xiaofong.springmvc.annotation.MyController;
import com.xiaofong.springmvc.annotation.MyControllerMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MydispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, Method> handlerMapping = new HashMap<>();

    private Map<String, Object> controllerMap = new HashMap<>();


    @Override
    public void init(ServletConfig config) throws ServletException {

        loadConfig(config);

        getClassNamesFromProperties(properties.getProperty("scanPackage"));

        initInstanceToIoc();

        initHandlerMapping();

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //处理请求
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {

        if (handlerMapping.isEmpty()) {
            return;
        }

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();

        requestURI = requestURI.replace(contextPath, "").replaceAll("/+", "/");

        if (!handlerMapping.containsKey(requestURI)) {
            try {
                resp.getWriter().write("404 NOT FOUND");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 根据 url 获取要执行的方法体
        Method method = this.handlerMapping.get(requestURI);

        // 获取方法参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 获取请求参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];

        String paramName;
        for (int i = 0; i < parameterTypes.length; i++) {
            paramName = parameterTypes[i].getSimpleName();

            if ("HttpServletRequest".equals(paramName)) {
                paramValues[i] = req;
                continue;
            }

            if ("HttpServletResponse".equals(paramName)) {
                paramValues[i] = resp;
                continue;
            }

            if ("String".equals(paramName)) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                    paramValues[i] = value;
                }
            }
        }

        //利用反射机制来调用
        try {
            //obj是method所对应的实例 在ioc容器中
            method.invoke(this.controllerMap.get(requestURI), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void initHandlerMapping() {

        if (ioc.isEmpty()) {
            return;
        }

        Class<?> clazz;
        String baseUrl = null;
        String url;
        MyControllerMapping annotation;
        Method[] methods;

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            clazz = entry.getValue().getClass();
            // 如果类上没有标识 @MyController 注解则跳过
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }

            // 如果类上存在 @MyControllerMapping 注解则作为 baseUrl
            if (clazz.isAnnotationPresent(MyControllerMapping.class)) {
                annotation = clazz.getAnnotation(MyControllerMapping.class);
                baseUrl = annotation.value();
            }

            methods = clazz.getMethods();
            if (methods.length <= 0) {
                return;
            }

            for (Method method : methods) {
                // 如果方法上没有标识 @MyControllerMapping 注解则跳过
                if (!method.isAnnotationPresent(MyControllerMapping.class)) {
                    continue;
                }

                annotation = method.getAnnotation(MyControllerMapping.class);
                url = annotation.value();

                url = (baseUrl + '/' + url).replace("/+", "/");

                //这里应该放置实例和method
                handlerMapping.put(url, method);

                controllerMap.put(url, entry.getValue());

            }
        }
    }

    /**
     * 初始化实例到IOC 容器中
     */
    private void initInstanceToIoc() {

        if (classNames.isEmpty()) {
            return;
        }

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);

                // 如果这个类标识了 MyController 注解, 则进行实例化并放入到IOC 容器中
                if (clazz.isAnnotationPresent(MyController.class)) {
                    ioc.put(toLowerCaseFirstOne(clazz.getSimpleName()), clazz.newInstance());
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将字符串转换成首字母小写
     *
     * @param s
     * @return
     */
    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }

    /**
     * 扫描用户设定的包下面所有的类
     */
    private void getClassNamesFromProperties(String packageName) {

        // 从配置文件中取得key 为 scanPackage 对应的值, 将 . 替换成 /
        String scanPackageValue = "/" + packageName.replaceAll("\\.", "/");
        // 取得 url
        URL url = this.getClass().getClassLoader().getResource(scanPackageValue);

        File dir = new File(url.getFile());
        String className;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                getClassNamesFromProperties(packageName + '.' + f.getName());
            } else {
                className = packageName + '.' + f.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }


    /**
     * 加载配置文件
     *
     * @param config
     */
    private void loadConfig(ServletConfig config) {
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关流
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
