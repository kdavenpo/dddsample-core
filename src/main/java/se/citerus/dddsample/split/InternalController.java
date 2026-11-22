package se.citerus.dddsample.split;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * The other side of {@link RemoteCall}: POST /internal/&lt;Class&gt;/&lt;method&gt; calls a method of a
 * class this service owns and returns its result as JSON (build specification, convention 4).
 * A null argument whose parameter is a bean type (a repository) is given this service's bean.
 * Only classes this service owns can be called. Created only in a deployed service.
 */
@RestController
@ConditionalOnProperty("split.service")
public class InternalController {

    private static final Logger log = LoggerFactory.getLogger(InternalController.class);

    private final ApplicationContext context;
    private final Split split;
    private final ObjectMapper json = SplitJson.mapper();
    private volatile Map<String, Object> owned;

    public InternalController(ApplicationContext context, Split split) {
        this.context = context;
        this.split = split;
    }

    @PostMapping(value = "/internal/{cls}/{method}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> call(@PathVariable("cls") String cls, @PathVariable("method") String method,
                                       @RequestHeader(RemoteCall.SIGNATURE) String signature,
                                       @RequestBody String body) throws Exception {
        Object bean = owned().get(cls);
        if (bean == null) {
            return error(HttpStatus.NOT_FOUND, new IllegalArgumentException("service " + split.me() + " owns no " + cls));
        }
        ClassLoader loader = bean.getClass().getClassLoader();
        String[] typeNames = signature.isBlank() ? new String[0] : signature.split(",");
        Class<?>[] types = new Class<?>[typeNames.length];
        for (int i = 0; i < typeNames.length; i++) {
            types[i] = ClassUtils.forName(typeNames[i], loader);
        }
        Method target = ReflectionUtils.findMethod(bean.getClass(), method, types);
        if (target == null) {
            return error(HttpStatus.NOT_FOUND, new NoSuchMethodException(cls + "." + method + "(" + signature + ")"));
        }

        JsonNode given = json.readTree(body);
        Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            JsonNode node = given.get(i);
            if (node == null || node.isNull()) {
                arguments[i] = context.getBeanProvider(types[i]).getIfUnique();   // a bean passed as a parameter; else null
            } else {
                arguments[i] = json.readerFor(json.getTypeFactory().constructType(target.getGenericParameterTypes()[i]))
                        .readValue(node);
            }
        }

        Object result;
        try {
            ReflectionUtils.makeAccessible(target);
            result = target.invoke(bean, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            log.debug("internal call {}.{} failed", cls, method, cause);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, cause);
        }
        if (target.getReturnType() == void.class) {
            return ResponseEntity.ok("");
        }
        return ResponseEntity.ok(json.writeValueAsString(result));    // inside the request: lazy data still loads
    }

    private ResponseEntity<String> error(HttpStatus status, Throwable cause) throws Exception {
        Map<String, String> error = Map.of("exception", cause.getClass().getName(),
                "message", cause.getMessage() == null ? "" : cause.getMessage());
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(new ObjectMapper().writeValueAsString(error));
    }

    /** {simple class name: bean} for the classes this service owns, found once. */
    private Map<String, Object> owned() {
        Map<String, Object> found = owned;
        if (found == null) {
            found = new HashMap<>();
            for (String name : context.getBeanDefinitionNames()) {
                Object bean;
                try {
                    bean = context.getBean(name);
                } catch (Exception e) {
                    continue;      // abstract or otherwise not gettable: not one of ours
                }
                String node = RemoteBeans.nodeClass(bean, split);
                if (node != null && split.me().equals(split.owner(node))) {
                    found.put(RemoteCall.simpleName(node), bean);
                }
            }
            owned = found;
        }
        return found;
    }
}
