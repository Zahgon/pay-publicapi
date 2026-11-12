package uk.gov.pay.api.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Reads enum constants leniently, as the service's API always has: case is ignored, whitespace is dropped and dashes
 * and dots stand for underscores; a constant's {@code toString()} is accepted too. Enums that declare their own
 * Jackson creator or annotate their constants keep Jackson's standard handling.
 */
public class LenientEnumModule extends SimpleModule {

    public LenientEnumModule() {
        super("LenientEnumModule");
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addDeserializers(new LenientEnumDeserializers());
    }

    static Enum<?> fromStringLeniently(String value, Enum<?>[] constants) {
        String text = value.replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "")
                .replace('-', '_').replace('.', '_');
        for (Enum<?> constant : constants) {
            if (constant.name().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        for (Enum<?> constant : constants) {
            if (constant.toString().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        return null;
    }

    private static class LenientEnumDeserializer extends StdScalarDeserializer<Enum<?>> {

        private final Enum<?>[] constants;
        private final List<String> acceptedValues;

        @SuppressWarnings("unchecked")
        LenientEnumDeserializer(Class<Enum<?>> enumType) {
            super(enumType);
            this.constants = (Enum<?>[]) handledType().getEnumConstants();
            this.acceptedValues = Arrays.stream(constants).map(Enum::name).toList();
        }

        @Override
        public Enum<?> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            Enum<?> constant = fromStringLeniently(parser.getText(), constants);
            if (constant != null) {
                return constant;
            }
            throw context.weirdStringException(parser.getText(), handledType(), "must be one of " + acceptedValues);
        }
    }

    private static class LenientEnumDeserializers extends Deserializers.Base {

        @Override
        @SuppressWarnings("unchecked")
        public JsonDeserializer<?> findEnumDeserializer(Class<?> type, DeserializationConfig config, BeanDescription description) {
            if (config.hasDeserializationFeatures(DeserializationFeature.READ_ENUMS_USING_TO_STRING.getMask())
                    || config.hasDeserializationFeatures(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL.getMask())
                    || config.hasDeserializationFeatures(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE.getMask())) {
                return null;
            }
            for (AnnotatedMethod factory : description.getFactoryMethods()) {
                if (factory.hasAnnotation(JsonCreator.class)) {
                    return null;
                }
            }
            for (Field field : type.getFields()) {
                for (Annotation annotation : field.getAnnotations()) {
                    if (annotation.annotationType().getPackage().getName().equals("com.fasterxml.jackson.annotation")) {
                        return null;
                    }
                }
            }
            return new LenientEnumDeserializer((Class<Enum<?>>) type);
        }
    }
}
