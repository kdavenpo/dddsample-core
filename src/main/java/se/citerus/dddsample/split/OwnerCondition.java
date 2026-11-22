package se.citerus.dddsample.split;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/** The condition behind {@link ConditionalOnOwner}. */
class OwnerCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnOwner.class.getName());
        String className = attributes == null ? null : (String) attributes.get("value");
        Split split = new Split(context.getEnvironment());
        if (split.seedOnly()) {
            return false;
        }
        return className == null || split.local(className);
    }
}
