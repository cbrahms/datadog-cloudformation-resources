// Unless explicitly stated otherwise all files in this repository are licensed under the Apache-2.0 License.
// This product includes software developed at Datadog (https://www.datadoghq.com/).
// Copyright 2019-Present Datadog, Inc.
package com.datadog.integrations.aws;

import java.util.HashMap;
import java.util.Map;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.datadog.cloudformation.common.clients.ApiClients;

import com.datadog.api.v1.client.ApiClient;
import com.datadog.api.v1.client.ApiException;
import com.datadog.api.v1.client.api.AwsIntegrationApi;
import com.datadog.api.v1.client.model.AWSAccount;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        logger.log("Starting the AWS Integration Resource Read Handler");

        ApiClient apiClient = ApiClients.V1Client(
            model.getDatadogCredentials().getApiKey(),
            model.getDatadogCredentials().getApplicationKey(),
            model.getDatadogCredentials().getApiURL()
        );
        AwsIntegrationApi awsApi = new AwsIntegrationApi(apiClient);


        AWSAccount awsAccount = null;
        try {
            awsAccount = awsApi.getAllAWSAccounts(model.getAccountID(), model.getRoleName(), model.getAccessKeyID()).getAccounts().get(0);
        } catch (ApiException e) {
            String err = "Failed to read AWS Integration Account: " + e.toString();
            logger.log(err);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .message(err)
                .build();
        }

        model.setAccountID(awsAccount.getAccountId());
        model.setRoleName(awsAccount.getRoleName());
        model.setAccessKeyID(awsAccount.getAccessKeyId());
        model.setHostTags(awsAccount.getHostTags());
        model.setFilterTags(awsAccount.getFilterTags());
        model.setAccountSpecificNamespaceRules(awsAccount.getAccountSpecificNamespaceRules());

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModel(model)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
