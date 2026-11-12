package uk.gov.pay.api.auth;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.bind.binders.PostponedRequestArgumentBinder;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;

import jakarta.inject.Singleton;

/**
 * Hands resource methods the account the request was authenticated as.
 */
@Singleton
public class AccountArgumentBinder implements TypedRequestArgumentBinder<Account>, PostponedRequestArgumentBinder<Account> {

    public static final String ACCOUNT_ATTRIBUTE = "uk.gov.pay.api.auth.Account";

    @Override
    public Argument<Account> argumentType() {
        return Argument.of(Account.class);
    }

    @Override
    public BindingResult<Account> bind(ArgumentConversionContext<Account> context, HttpRequest<?> source) {
        return () -> source.getAttribute(ACCOUNT_ATTRIBUTE, Account.class);
    }
}
