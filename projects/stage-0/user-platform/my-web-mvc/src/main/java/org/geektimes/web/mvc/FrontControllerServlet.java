package org.geektimes.web.mvc;

import org.apache.commons.lang.StringUtils;
import org.geektimes.web.mvc.controller.Controller;
import org.geektimes.web.mvc.controller.PageController;
import org.geektimes.web.mvc.controller.RestController;
import org.geektimes.web.mvc.header.annotation.PaddingParam;
import org.geektimes.web.mvc.header.annotation.ResponseJson;
import org.json.JSONObject;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.substringAfter;

public class FrontControllerServlet extends HttpServlet {

    /**
     * 请求路径和 {@link HandlerMethodInfo} 映射关系缓存
     */
    private Map<String, HandlerMethodInfo> handleMethodInfoMapping = new HashMap<>();

    /**
     * 初始化 Servlet
     *
     * @param servletConfig
     */
    public void init(ServletConfig servletConfig) {
        initHandleMethods();
    }

    /**
     * 读取所有的 RestController 的注解元信息 @Path
     * 利用 ServiceLoader 技术（Java SPI）
     */
    private void initHandleMethods() {
        for (Controller controller : ServiceLoader.load(Controller.class)) {
            Class<?> controllerClass = controller.getClass();
            Path pathFromClass = controllerClass.getAnnotation(Path.class);
            String requestPath = pathFromClass.value();
            Method[] publicMethods = controllerClass.getDeclaredMethods();
            // 处理方法支持的 HTTP 方法集合
            for (Method method : publicMethods) {
                String path = requestPath;
                Set<String> supportedHttpMethods = findSupportedHttpMethods(method);
                Path pathFromMethod = method.getAnnotation(Path.class);
                if (pathFromMethod != null) {
                    path += pathFromMethod.value();
                }
                handleMethodInfoMapping.put(path,
                        new HandlerMethodInfo(path, method, supportedHttpMethods, controller));
            }
        }
    }

    /**
     * 获取处理方法中标注的 HTTP方法集合
     *
     * @param method 处理方法
     * @return
     */
    private Set<String> findSupportedHttpMethods(Method method) {
        Set<String> supportedHttpMethods = new LinkedHashSet<>();
        for (Annotation annotationFromMethod : method.getAnnotations()) {
            HttpMethod httpMethod = annotationFromMethod.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null) {
                supportedHttpMethods.add(httpMethod.value());
            }
        }

        if (supportedHttpMethods.isEmpty()) {
            supportedHttpMethods.addAll(asList(HttpMethod.GET, HttpMethod.POST,
                    HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS));
        }

        return supportedHttpMethods;
    }

    /**
     * SCWCD
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
         request.getServletContext().log("进入");
        // 建立映射关系
        // requestURI = /a/hello/world
        String requestURI = request.getRequestURI();
        // contextPath  = /a or "/" or ""
        String servletContextPath = request.getContextPath();
        String prefixPath = servletContextPath;
        // 映射路径（子路径）
        String requestMappingPath = substringAfter(requestURI,
                StringUtils.replace(prefixPath, "//", "/"));
        // 映射到 Controller
        request.getServletContext().log("requestMappingPath " + requestMappingPath);
        HandlerMethodInfo handlerMethodInfo = handleMethodInfoMapping.get(requestMappingPath);
        for (String s : handleMethodInfoMapping.keySet()) {
            request.getServletContext().log("handleMethodInfoMapping " + s);
        }
        if (handlerMethodInfo == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Controller controller = handlerMethodInfo.getTarget();
            try {
                if (controller != null) {
                    String httpMethod = request.getMethod();

                    if (!handlerMethodInfo.getSupportedHttpMethods().contains(httpMethod)) {
                        // HTTP 方法不支持
                        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                    if (controller instanceof PageController) {
                        Method handlerMethod = handlerMethodInfo.getHandlerMethod();
                        request.getServletContext().log("handlerMethod： " + handlerMethod );
                        String viewPath = String.valueOf(handlerMethod.invoke(controller, request, response));
                        request.getServletContext().log("viewPath： " + viewPath );
                        // 页面请求 forward
                        // request -> RequestDispatcher forward
                        // RequestDispatcher requestDispatcher = request.getRequestDispatcher(viewPath);
                        // ServletContext -> RequestDispatcher forward
                        // ServletContext -> RequestDispatcher 必须以 "/" 开头
                        ServletContext servletContext = request.getServletContext();
                        if (!viewPath.startsWith("/")) {
                            viewPath = "/" + viewPath;
                        }
                        RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(viewPath);
                        requestDispatcher.forward(request, response);
                        return;
                    } else if (controller instanceof RestController) {
                        Method handlerMethod = handlerMethodInfo.getHandlerMethod();

                        Parameter[] parameters = handlerMethod.getParameters();
                        // 获取使用的Content-Type
                        String header_content_Type = request.getHeader("Content-Type");
                        if (StringUtils.isBlank(header_content_Type)) {
                            header_content_Type = "application/x-www-form-urlencoded";
                        }
                        if (header_content_Type.indexOf("application/x-www-form-urlencoded") > -1) {
                            String bodyStr = getRequestPostStr(request);
                            request.getServletContext().log(bodyStr);
                            Map<String, Object> paramMap = parseQueryString(bodyStr);
                            request.getServletContext().log(paramMap.toString());

                            List<Object> params =  new ArrayList<>();
                            for (Parameter parameter : parameters) {
                                PaddingParam paddingParam = parameter.getAnnotation(PaddingParam.class);
                                if (paddingParam == null) {
                                    if (parameter.getType().getSimpleName().equals(HttpServletRequest.class.getSimpleName())){
                                        params.add(request);
                                        continue;
                                    } else if (parameter.getType().getSimpleName().equals(HttpServletResponse.class.getSimpleName())){
                                        params.add(response);
                                        continue;
                                    } else {
                                        params.add(null);
                                    }

                                    continue;
                                }
                                Class<?>[] type = paddingParam.type();
                                request.getServletContext().log("deal type" + type);
                                if (type.length == 0) {
                                    Object object = parameter.getType().newInstance();
                                    BeanInfo beanInfo = Introspector.getBeanInfo(parameter.getType(), Object.class);
                                    PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                                    for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                                        propertyDescriptor.getWriteMethod().invoke(object, paramMap.get(propertyDescriptor.getName()));
                                    }
                                    params.add(object);
                                    continue;
                                } else if (type.length == 1) {
                                    Class<?> xclass = type[0];
                                    String paramName = StringUtils.isNotBlank(paddingParam.value()) ? paddingParam.value() : parameter.getName();
                                    Object o = paramMap.get(paramName);
                                    Object cast = xclass.cast(o);
                                    // 判断类为基础类，然后赋值
                                    params.add(cast);
                                    continue;
                                } else {
                                    params.add(null);
                                }
                            }
                            request.getServletContext().log("deal controller" + controller);
                            request.getServletContext().log("deal params " + params);
                            Object invoke = handlerMethod.invoke(controller, params.toArray());
                            ResponseJson annotation = handlerMethod.getAnnotation(ResponseJson.class);
                            if (annotation != null) {
                                response.setCharacterEncoding("utf-8");
                                response.setContentType("application/json; charset=utf-8");
                                PrintWriter writer = response.getWriter();
                                String resultStr = new JSONObject(invoke).toString();
                                response.setContentLength(resultStr.getBytes(StandardCharsets.UTF_8).length);
                                writer.write(resultStr);
                                writer.flush();
                                writer.close();
                            } else {
                                String viewPath = (String) invoke;
                                ServletContext servletContext = request.getServletContext();
                                if (!viewPath.startsWith("/")) {
                                    viewPath = "/" + viewPath;
                                }
                                RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(viewPath);
                                requestDispatcher.forward(request, response);
                            }

                            request.getServletContext().log("" + invoke);

                        } else if (header_content_Type.indexOf("application/json") > -1) {
                            String bodyStr = getRequestPostJsonStr(request);
                            request.getServletContext().log(bodyStr);
                        }

                        for (Parameter parameter : parameters) {
                            request.getServletContext().log(parameter.getType().getSimpleName());
                        }

                        // TODO
                    }

                }
            } catch (Throwable throwable) {
                if (throwable.getCause() instanceof IOException) {
                    throw (IOException) throwable.getCause();
                } else {
                    throwable.fillInStackTrace();
                    throw new ServletException(throwable.getCause());
                }
            }
    }

    public static Map<String, Object> parseQueryString(String bodyStr) {
        if (StringUtils.isBlank(bodyStr)) {
            return Collections.emptyMap();
        }
        HashMap<String, Object > paramMap = new HashMap<>();
        String[] splitStrings = bodyStr.split("&");
        for (String splitString : splitStrings) {
            if (StringUtils.isNotBlank(splitString) && splitString.indexOf("=") > -1) {
                String[] splits = splitString.split("=");
                if (splits.length == 2) {
                  if (paramMap.containsKey(splits[0])) {
                      Object value = paramMap.get(splits[0]);
                      if (value == null) {
                          value = "";
                      }
                      if (value instanceof List) {
                          List temp = List.class.cast(value);
                          temp.add(splits[0]);
                          paramMap.put(splits[0], temp);
                      } else {
                          List temp = new ArrayList();
                          temp.add(value);
                          temp.add(splits[0]);
                          paramMap.put(splits[0], temp);
                      }
                  } else {
                      paramMap.put(splits[0], splits[1]);
                  }
                }
            }
        }
        return paramMap;
    }


    public static String getRequestPostStr(HttpServletRequest request)
            throws IOException {
        byte buffer[] = readBody(request);
        String charEncoding = request.getCharacterEncoding();
        if (charEncoding == null) {
            charEncoding = "UTF-8";
        }

        return URLDecoder.decode(new String(buffer, charEncoding).toString(), "UTF-8");
    }

    public static String getRequestPostJsonStr(HttpServletRequest request)
            throws IOException {
        byte buffer[] = readBody(request);
        String charEncoding = request.getCharacterEncoding();
        if (charEncoding == null) {
            charEncoding = "UTF-8";
        }

        return new String(buffer, charEncoding).toString();
    }

    private static byte[] readBody(HttpServletRequest request) throws IOException {
            int contentLength = request.getContentLength();
            if(contentLength<0){
                return null;
            }
            byte buffer[] = new byte[contentLength];
            for (int i = 0; i < contentLength;)
            {
                int readlen = request.getInputStream().read(buffer, i,
                        contentLength - i);
                if (readlen == -1) {
                    break;
                }
                i += readlen;
            }
            return buffer;
    }
//    private void beforeInvoke(Method handleMethod, HttpServletRequest request, HttpServletResponse response) {
//
//        CacheControl cacheControl = handleMethod.getAnnotation(CacheControl.class);
//
//        Map<String, List<String>> headers = new LinkedHashMap<>();
//
//        if (cacheControl != null) {
//            CacheControlHeaderWriter writer = new CacheControlHeaderWriter();
//            writer.write(headers, cacheControl.value());
//        }
//    }
}
