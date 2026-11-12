package uk.gov.pay.api.app.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties("rate-limiter")
public class RateLimiterConfig {

    @Min(1)
    private int noOfReq;

    @Min(1)
    private int noOfReqForPost;

    @Min(1)
    private int noOfReqForElevatedAccounts;

    @Min(1)
    private int noOfPostReqForElevatedAccounts;

    @Min(1)
    private int noOfReqPerNode;

    @Min(1)
    private int noOfReqForPostPerNode;

    @Valid
    @JsonDeserialize(converter = StringToListConverter.class)
    private List<String> elevatedAccounts;

    @Min(500)
    @Max(60000)
    private int perMillis;

    @Valid
    @JsonDeserialize(converter = StringToListConverter.class)
    private List<String> lowTrafficAccounts;

    @Min(1)
    private int noOfReqForLowTrafficAccounts;

    @Min(1)
    private int noOfPostReqForLowTrafficAccounts;

    @Min(1000)
    @Max(3_599_999)
    private int intervalInMillisForLowTrafficAccounts;

    public int getNoOfReq() {
        return noOfReq;
    }

    public int getPerMillis() {
        return perMillis;
    }

    public int getNoOfReqForPost() {
        return noOfReqForPost;
    }

    public int getNoOfReqPerNode() {
        return noOfReqPerNode;
    }

    public int getNoOfReqForPostPerNode() {
        return noOfReqForPostPerNode;
    }

    public int getNoOfReqForElevatedAccounts() {
        return noOfReqForElevatedAccounts;
    }

    public int getNoOfPostReqForElevatedAccounts() {
        return noOfPostReqForElevatedAccounts;
    }

    public List<String> getElevatedAccounts() {
        return Optional.ofNullable(elevatedAccounts).orElse(Collections.emptyList());
    }

    public List<String> getLowTrafficAccounts() {
        return Optional.ofNullable(lowTrafficAccounts).orElse(Collections.emptyList());
    }

    public int getNoOfReqForLowTrafficAccounts() {
        return noOfReqForLowTrafficAccounts;
    }

    public int getNoOfPostReqForLowTrafficAccounts() {
        return noOfPostReqForLowTrafficAccounts;
    }

    public int getIntervalInMillisForLowTrafficAccounts() {
        return intervalInMillisForLowTrafficAccounts;
    }

    public void setNoOfReq(int noOfReq) {
        this.noOfReq = noOfReq;
    }

    public void setNoOfReqForPost(int noOfReqForPost) {
        this.noOfReqForPost = noOfReqForPost;
    }

    public void setNoOfReqForElevatedAccounts(int noOfReqForElevatedAccounts) {
        this.noOfReqForElevatedAccounts = noOfReqForElevatedAccounts;
    }

    public void setNoOfPostReqForElevatedAccounts(int noOfPostReqForElevatedAccounts) {
        this.noOfPostReqForElevatedAccounts = noOfPostReqForElevatedAccounts;
    }

    public void setNoOfReqPerNode(int noOfReqPerNode) {
        this.noOfReqPerNode = noOfReqPerNode;
    }

    public void setNoOfReqForPostPerNode(int noOfReqForPostPerNode) {
        this.noOfReqForPostPerNode = noOfReqForPostPerNode;
    }

    public void setElevatedAccounts(@Nullable String elevatedAccounts) {
        this.elevatedAccounts = new StringToListConverter().convert(elevatedAccounts);
    }

    public void setPerMillis(int perMillis) {
        this.perMillis = perMillis;
    }

    public void setLowTrafficAccounts(@Nullable String lowTrafficAccounts) {
        this.lowTrafficAccounts = new StringToListConverter().convert(lowTrafficAccounts);
    }

    public void setNoOfReqForLowTrafficAccounts(int noOfReqForLowTrafficAccounts) {
        this.noOfReqForLowTrafficAccounts = noOfReqForLowTrafficAccounts;
    }

    public void setNoOfPostReqForLowTrafficAccounts(int noOfPostReqForLowTrafficAccounts) {
        this.noOfPostReqForLowTrafficAccounts = noOfPostReqForLowTrafficAccounts;
    }

    public void setIntervalInMillisForLowTrafficAccounts(int intervalInMillisForLowTrafficAccounts) {
        this.intervalInMillisForLowTrafficAccounts = intervalInMillisForLowTrafficAccounts;
    }
}
