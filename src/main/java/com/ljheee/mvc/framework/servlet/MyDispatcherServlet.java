package com.ljheee.mvc.framework.servlet;

import com.ljheee.mvc.framework.annotation.MyAutowired;
import com.ljheee.mvc.framework.annotation.MyController;
import com.ljheee.mvc.framework.annotation.MyRequestMapping;
import com.ljheee.mvc.framework.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lijianhua04 on 2018/9/24.
 */
public class MyDispatcherServlet extends HttpServlet {

    //和web.xml中的param-name 一致
    private static final String LOCATION = "contextConfigLocation";

    //保存所有配置信息
    private Properties p = new Properties();

    //保存 扫描到的[全路径]类名
    private List<String> classNames = new ArrayList<String>();

    //IOC容器，保存初始化到bean
    private Map<String, Object> ioc = new ConcurrentHashMap<String, Object>();

    // 保存URL 和 method映射
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();


    public MyDispatcherServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1、加載配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        System.out.println("---" + p.getProperty("scanPackage"));//null
        //2、扫描所有 相关类
//        doScanner(p.getProperty("scanPackage"));
        doScanner("com.ljheee.mvc.demo");
        //3、初始化所有相美炎的実例,并保存到IOc容器中
        doInstance();
        //4、依頼注入
        doAutowired();
        //5、枸造HandlerMapping
        initHandlerMapping();
        //6、等待靖求,匹配URL,定位方法, 反射凋用抉行
        //澗用doGet或者doPost方法
        System.out.println("ljheee mvcframework is init");
    }

    /**
     * 加載配置文件
     *
     * @param location
     */
    private void doLoadConfig(String location) {

        InputStream ips = null;
        try {
//            ips = this.getServletContext().getResourceAsStream(location);
            ips = this.getClass().getClassLoader().getResourceAsStream(location);
            p.load(ips);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ips != null) {
                    ips.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 扫描所有 相关类
     *
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {

        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));

        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                classNames.add(scanPackage + "." + file.getName().replace(".class", "").trim());
            }
        }

    }

    /**
     * 实例化java bean 保存到ioc容器
     */
    private void doInstance() {

        if (classNames.size() == 0) {
            return;
        }

        try {
            for (String className : classNames) {

                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {

                    //默认将首字母小写作为beanName
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());

                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    MyService myService = clazz.getAnnotation(MyService.class);

                    //如果用户设置了别名，就用别名
                    String beanName = myService.value();

                    //如果没设别名，默认将首字母小写作为beanName
                    if ("".equals(beanName.trim())) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }

                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);//把service具体实现类 或 service class保存在ioc

                    // service 通常是按接口注入的
                    // 按接口类型 创建实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化所有相关的类，并放入到IOC容器之中。
     * IOC容器的key默认是类名首字母小写，如果是自己设置类名，则优先使用自定义的。
     *
     * @param str
     * @return
     */
    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }


    /**
     * 将初始化到IOC容器中的类，需要赋值的字段进行赋值
     */
    private void doAutowired() {

        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //拿到示例 所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }

                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();

                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 把RequestMapping中配置的信息和Method进行关联，并保存这些关系。
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {

            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {

                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }

                MyRequestMapping methodMapping = method.getAnnotation(MyRequestMapping.class);
                String url = ("/" + baseUrl + "/" + methodMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("url=" + url + ",method=" + method);
            }
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            doDispatcher(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Interal error\r\n" +
                    Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]]", "").replaceAll(",\\s", "\r\n")
            );
        }
    }

    /**
     * 根据请求到URL，找到handlerMapping
     * 拿到执行到方法，反射执行
     *
     * @param request
     * @param response
     * @throws IOException
     */
    private void doDispatcher(HttpServletRequest request, HttpServletResponse response) throws IOException {


        if (this.handlerMapping.isEmpty()) {
            return;
        }


        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!handlerMapping.containsKey(url)) {
            response.getWriter().write("404 Not Found!!!");
            return;
        }

        Method method = handlerMapping.get(url);
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] paramValues = new Object[parameterTypes.length];

        Map<String, String[]> parameterMap = request.getParameterMap();


        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];

            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = request;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = response;
                continue;
            } else if (parameterType == String.class) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {

                    String value = Arrays.toString(param.getValue())
                            .replaceAll("\\[|\\]", "")
                            .replaceAll(",\\s", ",");
                    paramValues[i] = value;
                }
            }
        }

        try {
            // 此方式 拿beanName 性能不高
            String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
            method.invoke(ioc.get(beanName), paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }


}