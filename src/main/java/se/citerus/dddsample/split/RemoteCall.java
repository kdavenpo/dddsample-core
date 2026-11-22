package se.citerus.dddsample.split;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.data.repository.Repository;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * Turns a call to a class another service owns into an HTTP call (build specification,
 * convention 4): POST /internal/&lt;Class&gt;/&lt;method&gt; with the arguments as a JSON array, the
 * result as JSON. The parameter types go in a header, so an overloaded method is found exactly.
 * An argument that is itself a bean (a repository passed as a parameter) is sent as null; the
 * called service passes its own.
 */
class RemoteCall implements MethodInterceptor {

    static final String SIGNATURE = "X-Split-Signature";

    private final RestClient client;
    private final String className;
    private final ObjectMapper json;

    RemoteCall(RestClient client, String className, ObjectMapper json) {
        this.client = client;
        this.className = className;
        this.json = json;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object[] arguments = invocation.getArguments();
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> "remote " + className;
                case "hashCode" -> System.identityHashCode(invocation.getThis());
                case "equals" -> invocation.getThis() == arguments[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
        StringJoiner body = new StringJoiner(",", "[", "]");
        for (Object argument : arguments) {
            body.add(isBean(argument) ? "null" : json.writeValueAsString(argument));   // each on its own: no references between them
        }
        String signature = String.join(",", Arrays.stream(method.getParameterTypes()).map(Class::getName).toList());

        Reply reply = client.post()
                .uri("/internal/{cls}/{method}", simpleName(className), method.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .header(SIGNATURE, signature)
                .body(body.toString())
                .exchange((request, response) -> new Reply(response.getStatusCode().isError(),
                        new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        if (reply.error()) {
            throw failure(method, reply.text());
        }
        if (method.getReturnType() == void.class || reply.text().isEmpty()) {
            return null;
        }
        return json.readValue(reply.text(), json.getTypeFactory().constructType(method.getGenericReturnType()));
    }

    private record Reply(boolean error, String text) {
    }

    static String simpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private static boolean isBean(Object argument) {
        return argument != null && (argument instanceof Repository<?, ?> || AopUtils.isAopProxy(argument)
                || Proxy.isProxyClass(argument.getClass()));
    }

    /** The called service's exception, as the same class when it can be made, else a RuntimeException. */
    private Exception failure(Method method, String text) {
        String type = RuntimeException.class.getName();
        String message = text;
        try {
            JsonNode error = json.readTree(text);
            type = error.path("exception").asText(type);
            message = error.path("message").asText("");
        } catch (Exception ignored) {
            // not our error body: keep the text
        }
        try {
            Class<?> exceptionClass = Class.forName(type);
            boolean allowed = RuntimeException.class.isAssignableFrom(exceptionClass)
                    || Arrays.stream(method.getExceptionTypes()).anyMatch(t -> t.isAssignableFrom(exceptionClass));
            if (allowed) {
                Constructor<?> constructor = exceptionClass.getConstructor(String.class);
                return (Exception) constructor.newInstance(message);
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // fall through
        }
        return new RuntimeException("remote call " + simpleName(className) + "." + method.getName()
                + " failed: " + type + ": " + message);
    }
}
