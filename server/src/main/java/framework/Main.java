package framework;

import framework.api.HeaderParam;
import framework.api.Path;
import framework.api.QueryParam;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public String pageNotFound() {
        return "Page not found\n";
    }

    public static void main(String[] args) {
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler((HttpServerExchange exchange) -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

                    try {

                        Consumer println = System.out::println;

                        // For this to work, first do gradle build of application,
                        // so that application.jar is created
                        File file = new File("application/build/libs/application.jar");

                        ClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});

                        JarFile jarFile = new JarFile(file);

                        Stream<Class<?>> classes = jarFile.stream()
                            .map(entry -> entry.getName())
                            .filter(name -> name.endsWith(".class"))
                            .map(name -> name.replace('/', '.'))
                            .map(name -> name.substring(0, name.length() - ".class".length()))
                            .map(name -> {
                                try {
                                    return classLoader.loadClass(name);
                                } catch (ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            });


                        Stream<Method> methods = classes.flatMap(c -> Stream.of(c.getDeclaredMethods()));

                        Stream<Method> methodsWithPath = methods.filter(m -> m.getAnnotation(Path.class) != null);

                        Map<String, List<Method>> methodByPath = methodsWithPath.collect(Collectors.groupingBy(m -> m.getAnnotation(Path.class).value()));

                        List<Method> pathMethods = methodByPath.getOrDefault(exchange.getRequestPath(), Arrays.asList(Main.class.getDeclaredMethod("pageNotFound")));

                        Method method = null;
                        if (pathMethods.size() > 1) {
                            throw new RuntimeException("These methods have the same value in @Path: " + pathMethods.toString());
                        } else {
                            method = pathMethods.get(0);
                        }
                        Stream<Parameter> paramsStream = Stream.of(method.getParameters());
                        Object[] arguments = paramsStream.map(p -> {
                            String paramValue;
                            if(p.getAnnotation(QueryParam.class)!=null)
                                paramValue=exchange.getQueryParameters().get(p.getAnnotation(QueryParam.class).value()).getFirst();
                            else
                                paramValue=exchange.getRequestHeaders().get(p.getAnnotation(HeaderParam.class).value()).getFirst();
                            Class<?> paramType = p.getType();
                            if(paramType.equals(Double.class))
                                return Double.valueOf(paramValue);
                            if(paramType.equals(Boolean.class))
                                return Boolean.valueOf(paramValue);
                            if(paramType.equals(Integer.class))
                                return Integer.valueOf(paramValue);
                            if(paramType.equals(Float.class))
                                return Float.valueOf(paramValue);
                            try{
                                return paramType.getMethod("valueOf",String.class).invoke(null,paramValue);
                            }
                            catch (Exception e){}
                            return paramValue;
                        }).toArray();

                        Object o = method.invoke(method.getDeclaringClass().newInstance(), arguments);

                        String result = String.valueOf(o);

                        exchange.getResponseSender().send(result);


                    } catch (Exception e) {
//                        e.printStackTrace();
                        StringWriter writer = new StringWriter();
                        e.printStackTrace(new PrintWriter(writer));
                        String errorMessage = writer.toString();
                        exchange.getResponseSender().send(errorMessage);
                    }

                }).build();
        server.start();
    }
}
