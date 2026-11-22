package se.citerus.dddsample.split;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;

import java.lang.reflect.Modifier;

/**
 * The JSON used for calls between services (build specification, convention 4). Domain objects are
 * read and written field by field, as Hibernate does: DDDSample's classes have few getters and
 * setters. Each object is written once and referred to by id after that, since a cargo's last
 * handling event points back to the cargo. Lazily loaded associations are loaded before they are
 * written (convention 6). A class with no no-argument constructor is created without one, as
 * Hibernate creates entities.
 */
public final class SplitJson {

    private SplitJson() {
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    abstract static class Identity {
    }

    public static ObjectMapper mapper() {
        Hibernate6Module hibernate = new Hibernate6Module();
        hibernate.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, true);
        hibernate.configure(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true);

        SimpleModule creation = new SimpleModule("split-creation") {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addValueInstantiators(new WithoutConstructor());
            }
        };

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(hibernate);
        mapper.registerModule(creation);
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.addMixIn(Object.class, Identity.class);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /** For the project's own classes with no no-argument constructor: create them without one. */
    static final class WithoutConstructor extends ValueInstantiators.Base {

        private final Objenesis objenesis = new ObjenesisStd(true);

        @Override
        public ValueInstantiator findValueInstantiator(DeserializationConfig config, BeanDescription beanDesc,
                                                       ValueInstantiator defaultInstantiator) {
            Class<?> type = beanDesc.getBeanClass();
            if (defaultInstantiator.canCreateUsingDefault() || !ours(type) || type.isInterface()
                    || type.isEnum() || Modifier.isAbstract(type.getModifiers())) {
                return defaultInstantiator;
            }
            return new StdValueInstantiator(config, beanDesc.getType()) {
                @Override
                public boolean canCreateUsingDefault() {
                    return true;
                }

                @Override
                public Object createUsingDefault(DeserializationContext context) {
                    return objenesis.newInstance(type);
                }
            };
        }

        private static boolean ours(Class<?> type) {
            String name = type.getName();
            return name.startsWith("se.citerus.dddsample.") || name.startsWith("com.pathfinder.");
        }
    }
}
