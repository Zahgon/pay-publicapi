package uk.gov.pay.api.resources.binding;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.bind.binders.AnnotatedRequestArgumentBinder;
import uk.gov.pay.api.utils.QueryParameters;

import jakarta.inject.Singleton;
import java.util.Optional;

@Singleton
public class QueryParameterBinder implements AnnotatedRequestArgumentBinder<QueryParameter, String> {

    @Override
    public Class<QueryParameter> getAnnotationType() {
        return QueryParameter.class;
    }

    @Override
    public BindingResult<String> bind(ArgumentConversionContext<String> context, HttpRequest<?> source) {
        String name = context.getAnnotationMetadata().stringValue(QueryParameter.class).orElseThrow();
        Optional<String> value = Optional.ofNullable(QueryParameters.of(source).getFirst(name));
        return () -> value;
    }
}
