package se.citerus.dddsample.split;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.data.repository.Repository;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces each bean whose class another service owns with an HTTP client of the same type, so
 * every call that crosses the decomposition's boundaries is a remote call and every other call
 * stays in-process (DECISIONS 65, choice A). The bean is still created here, but nothing here
 * calls it; its owner's copy is the one that runs.
 */
public class RemoteBeans implements BeanPostProcessor, Ordered {

    private final Split split;
    private final ObjectMapper json = SplitJson.mapper();
    private final ClientHttpRequestInterceptor passOn;
    private final Map<String, RestClient> clients = new ConcurrentHashMap<>();

    public RemoteBeans(Split split, ClientHttpRequestInterceptor passOn) {
        this.split = split;
        this.passOn = passOn;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;     // after the transaction and repository proxies
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        String node = nodeClass(bean, split);
        if (node == null || split.local(node)) {
            return bean;
        }
        String owner = split.owner(node);
        ProxyFactory proxy = new ProxyFactory();
        Class<?>[] interfaces = interfaces(bean);
        if (interfaces.length > 0) {
            proxy.setInterfaces(interfaces);
        } else {
            proxy.setTargetClass(ClassUtils.getUserClass(AopUtils.getTargetClass(bean)));
            proxy.setProxyTargetClass(true);
        }
        proxy.addAdvice(new RemoteCall(client(owner), node, json));
        return proxy.getProxy(bean.getClass().getClassLoader());
    }

    /** The analyzed class a bean stands for: a repository's interface, or the bean's own class. */
    static String nodeClass(Object bean, Split split) {
        if (bean instanceof Repository<?, ?>) {
            // the repository's own interface (CargoRepositoryJPA), not the domain interface it
            // extends (CargoRepository): a call counts at the implementation (DECISIONS 58)
            return Arrays.stream(AopProxyUtils.proxiedUserInterfaces(bean))
                    .filter(Repository.class::isAssignableFrom)
                    .map(Class::getName)
                    .filter(name -> split.owner(name) != null)
                    .findFirst().orElse(null);
        }
        String name = ClassUtils.getUserClass(AopUtils.getTargetClass(bean)).getName();
        return split.owner(name) != null ? name : null;
    }

    private static Class<?>[] interfaces(Object bean) {
        Class<?>[] all = bean instanceof Repository<?, ?>
                ? AopProxyUtils.proxiedUserInterfaces(bean)
                : ClassUtils.getAllInterfacesForClass(ClassUtils.getUserClass(AopUtils.getTargetClass(bean)));
        return Arrays.stream(all).filter(i -> i.getName().startsWith("se.citerus.dddsample.")
                || i.getName().startsWith("com.pathfinder.")).toArray(Class<?>[]::new);
    }

    private RestClient client(String service) {
        return clients.computeIfAbsent(service, s -> RestClient.builder()
                .baseUrl(split.url(s))
                .requestInterceptor(passOn)
                .build());
    }
}
