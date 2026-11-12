package uk.gov.pay.api.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.Delay;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.jackson.ObjectMapperFactory;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.json.CreateAgreementRequestDeserializer;
import uk.gov.pay.api.json.CreateCardPaymentRequestDeserializer;
import uk.gov.pay.api.json.CreatePaymentRefundRequestDeserializer;
import uk.gov.pay.api.json.LenientEnumModule;
import uk.gov.pay.api.json.StringDeserializer;
import uk.gov.pay.api.model.CreateCardPaymentRequest;
import uk.gov.pay.api.model.CreatePaymentRefundRequest;
import uk.gov.pay.api.validation.PaymentRefundRequestValidator;
import uk.gov.pay.api.validation.URLValidator;

import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static uk.gov.pay.api.validation.URLValidator.urlValidatorValueOf;

@Factory
public class PublicApiFactory {

    /**
     * The mapper for the API's own requests and answers, replacing Micronaut's default one: the modules and
     * features the service has always used, its request deserializers, no null fields in answers and no float
     * accepted where an integer is expected.
     */
    @Singleton
    @Primary
    @Replaces(factory = ObjectMapperFactory.class, bean = ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = newObjectMapper();

        SimpleModule publicApiDeserializationModule = new SimpleModule("publicApiDeserializationModule");
        publicApiDeserializationModule.addDeserializer(CreateAgreementRequest.class, new CreateAgreementRequestDeserializer());
        publicApiDeserializationModule.addDeserializer(CreateCardPaymentRequest.class, new CreateCardPaymentRequestDeserializer());
        publicApiDeserializationModule.addDeserializer(CreatePaymentRefundRequest.class,
                new CreatePaymentRefundRequestDeserializer(new PaymentRefundRequestValidator()));
        publicApiDeserializationModule.addDeserializer(String.class, new StringDeserializer());

        objectMapper.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false);
        objectMapper.registerModule(publicApiDeserializationModule);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    @Bean
    @Singleton
    public URLValidator urlValidator(PublicApiConfig configuration) {
        return urlValidatorValueOf(configuration.getAllowHttpForReturnUrl());
    }

    /**
     * The Lettuce resources (event loops, reconnect delays) of the Redis client, shut down with the application.
     */
    @Singleton
    @Bean(preDestroy = "shutdown")
    public ClientResources redisClientResources(PublicApiConfig configuration) {
        var redisConfiguration = configuration.getRedisConfiguration();
        return DefaultClientResources.builder()
                .reconnectDelay(
                        Delay.fullJitter(
                                redisConfiguration.getReconnectDelayLowerBound(),
                                redisConfiguration.getReconnectDelayUpperBound(),
                                redisConfiguration.getReconnectDelayBase(),
                                TimeUnit.MILLISECONDS))
                .build();
    }

    @Singleton
    @Bean(preDestroy = "shutdown")
    public RedisClient redisClient(PublicApiConfig configuration, ClientResources clientResources) {
        var redisConfiguration = configuration.getRedisConfiguration();
        RedisClient client = RedisClient.create(clientResources, redisConfiguration.getUrl());

        SocketOptions socketOptions = SocketOptions
                .builder()
                .keepAlive(true)
                .connectTimeout(Duration.ofMillis(redisConfiguration.getConnectTimeout()))
                .build();
        ClientOptions clientOptions = ClientOptions
                .builder()
                .socketOptions(socketOptions)
                .build();
        client.setOptions(clientOptions);
        client.setDefaultTimeout(Duration.ofMillis(redisConfiguration.getCommandTimeout()));
        return client;
    }

    /**
     * The Jackson configuration the service has always used for its API: Guava, Java 8 and java.time types,
     * constructor parameter names, enums read leniently and unknown properties ignored.
     */
    static ObjectMapper newObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new BlackbirdModule());
        mapper.registerModule(new LenientEnumModule());
        mapper.registerModule(new ParameterNamesModule());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.setPropertyNamingStrategy(new ExplicitNamesNamingStrategy());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /**
     * Keeps every property's own name. Having a naming strategy at all is what places implicitly named properties
     * before explicitly named ones in the API's JSON, as it always has.
     */
    static class ExplicitNamesNamingStrategy extends PropertyNamingStrategy {
        private static final long serialVersionUID = 1L;
    }
}
