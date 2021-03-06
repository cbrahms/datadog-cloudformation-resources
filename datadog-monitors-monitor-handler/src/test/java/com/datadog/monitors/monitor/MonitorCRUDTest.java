// Unless explicitly stated otherwise all files in this repository are licensed under the Apache-2.0 License.
// This product includes software developed at Datadog (https://www.datadoghq.com/).
// Copyright 2019-Present Datadog, Inc.
package com.datadog.monitors.monitor;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class MonitorCRUDTest {

    private final List<String> testTagsUpdated = new ArrayList<String>(
        Arrays.asList("app:UpdatedCF")
    );
    private final List<String> testTags = new ArrayList<String>(
        Arrays.asList("app:CF", "key2:val2")
    );
    private final DatadogCredentials datadogCredentials = new DatadogCredentials(
        System.getenv("DD_TEST_CF_API_KEY"),
        System.getenv("DD_TEST_CF_APP_KEY"),
        System.getenv("DD_TEST_CF_API_URL")
    );

    private double id;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @AfterEach
    public void deleteMonitor() {
        final DeleteHandler deleteHandler = new DeleteHandler();
        final ResourceModel model = ResourceModel.builder().build();
        model.setId(id);
        model.setDatadogCredentials(datadogCredentials);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = deleteHandler.handleRequest(proxy, request, null, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void testMonitorCRUD() {
        final CreateHandler createHandler = new CreateHandler();
        final UpdateHandler updateHandler = new UpdateHandler();

        final ResourceModel model = ResourceModel.builder().build();
        model.setTags(testTags);
        model.setDatadogCredentials(datadogCredentials);
        model.setType("query alert");
        model.setQuery("avg(last_5m):sum:system.net.bytes_rcvd{host:host0} > 100");
        MonitorOptions options = new MonitorOptions();
        MonitorThresholds thresholds = new MonitorThresholds();
        thresholds.setCritical(100.);
        thresholds.setOK(50.);
        options.setThresholds(thresholds);
        model.setOptions(options);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = createHandler.handleRequest(proxy, request, null, logger);

        logger.log("Response is: %v" + response);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ResourceModel read = response.getResourceModel();
        assertThat(read.getTags()).isEqualTo(testTags);
        assertThat(read.getOptions().getThresholds().getCritical()).isEqualTo(100.);
        assertThat(read.getOptions().getThresholds().getOK()).isEqualTo(50.);
        id = read.getId();

        // Update the resource
        options = new MonitorOptions();
        options.setAggregation("in total");
        options.setEnableLogsSample(true);
        options.setEscalationMessage("escalation message");
        options.setIncludeTags(false);
        options.setLocked(true);
        options.setNotifyAudit(true);
        options.setNotifyNoData(true);
        options.setRequireFullWindow(false);
        options.setTimeoutH(10);
        options.setEvaluationDelay(300.);
        options.setMinLocationFailed(2.);
        options.setNewHostDelay(10.);
        options.setNoDataTimeframe(20.);
        options.setRenotifyInterval(10.);

        thresholds = new MonitorThresholds();
        thresholds.setCritical(1.);
        thresholds.setCriticalRecovery(0.5);
        thresholds.setOK(0.25);
        thresholds.setWarning(0.45);
        thresholds.setWarningRecovery(0.4);
        options.setThresholds(thresholds);

        MonitorThresholdWindows thresholdWindows = new MonitorThresholdWindows();
        thresholdWindows.setRecoveryWindow("last_30m");
        thresholdWindows.setTriggerWindow("last_30m");
        options.setThresholdWindows(thresholdWindows);

        model.setOptions(options);
        model.setTags(testTagsUpdated);
        String updatedQuery = "avg(last_1h):anomalies(avg:system.net.bytes_rcvd{host:host0}, 'basic', 2, direction='both', alert_window='last_30m', interval=120, count_default_zero='true') >= 1";
        model.setQuery(updatedQuery);
        model.setMessage("updated message");
        model.setName("updated name");

        final ResourceHandlerRequest<ResourceModel> updateRequest = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> updateResponse
            = updateHandler.handleRequest(proxy, updateRequest, null, logger);
        assertThat(updateResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);

        ResourceModel updateRead = updateResponse.getResourceModel();
        assertThat(updateRead.getTags()).isEqualTo(testTagsUpdated);
        assertThat(updateRead.getQuery()).isEqualTo(updatedQuery);
        assertThat(updateRead.getMessage()).isEqualTo("updated message");
        assertThat(updateRead.getName()).isEqualTo("updated name");
        assertThat(updateRead.getType()).isEqualTo("query alert");
        assertThat(updateRead.getOptions().getAggregation()).isEqualTo("in total");
        assertThat(updateRead.getOptions().getEnableLogsSample()).isTrue();
        assertThat(updateRead.getOptions().getEscalationMessage()).isEqualTo("escalation message");
        assertThat(updateRead.getOptions().getIncludeTags()).isFalse();
        assertThat(updateRead.getOptions().getLocked()).isTrue();
        assertThat(updateRead.getOptions().getNotifyAudit()).isTrue();
        assertThat(updateRead.getOptions().getNotifyNoData()).isTrue();
        assertThat(updateRead.getOptions().getRequireFullWindow()).isFalse();
        assertThat(updateRead.getOptions().getTimeoutH()).isEqualTo(10);
        assertThat(updateRead.getOptions().getEvaluationDelay()).isEqualTo(300.);
        assertThat(updateRead.getOptions().getMinLocationFailed()).isEqualTo(2.);
        assertThat(updateRead.getOptions().getNewHostDelay()).isEqualTo(10.);
        assertThat(updateRead.getOptions().getNoDataTimeframe()).isEqualTo(20.);
        assertThat(updateRead.getOptions().getRenotifyInterval()).isEqualTo(10.);
        assertThat(updateRead.getOptions().getThresholds().getCritical()).isEqualTo(1., within(.00001));
        assertThat(updateRead.getOptions().getThresholds().getCriticalRecovery()).isEqualTo(0.5, within(.00001));
        assertThat(updateRead.getOptions().getThresholds().getOK()).isEqualTo(.25, within(.00001));
        assertThat(updateRead.getOptions().getThresholds().getWarning()).isEqualTo(.45, within(.00001));
        assertThat(updateRead.getOptions().getThresholds().getWarningRecovery()).isEqualTo(.4, within(.00001));
        assertThat(updateRead.getOptions().getThresholdWindows().getTriggerWindow()).isEqualTo("last_30m");
        assertThat(updateRead.getOptions().getThresholdWindows().getRecoveryWindow()).isEqualTo("last_30m");
    }
}
