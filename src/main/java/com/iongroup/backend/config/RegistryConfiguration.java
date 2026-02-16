package com.iongroup.backend.config;

import com.iongroup.library.registry.DefaultOperationRegistry;
import com.iongroup.library.registry.DelegationType;
import com.iongroup.library.registry.OperationDescriptor;
import com.iongroup.library.registry.OperationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Configuration for the Operation Registry.
 * Initializes the registry with sample delegation operations.
 */
@Configuration
public class RegistryConfiguration {

        @Bean
        public OperationRegistry operationRegistry() {
                DefaultOperationRegistry registry = new DefaultOperationRegistry();

                // SERVICE delegations - Java code interacts with external systems
                registry.register(new OperationDescriptor(
                                "CheckEligibility",
                                "Check customer eligibility for loan/card",
                                Arrays.asList("customerProfile"),
                                Arrays.asList("eligibilityResult"),
                                "com.iongroup.library.adapter.flowable.CheckEligibilityTask",
                                "loan",
                                DelegationType.SERVICE,
                                new ArrayList<>(),
                                Arrays.asList("AMOUNT")
                        ));

                registry.register(new OperationDescriptor(
                                "GetLoanPolicy",
                                "Fetch loan policy from external system",
                                Arrays.asList("loanType"),
                                Arrays.asList("loanPolicy"),
                                "com.iongroup.library.adapter.flowable.GetLoanPolicyTask",
                                "loan",
                                DelegationType.SERVICE));

                registry.register(new OperationDescriptor(
                                "GetAvailableCreditCards",
                                "Fetch available credit cards from catalog",
                                Arrays.asList("customerSegment"),
                                Arrays.asList("availableCards"),
                                "com.iongroup.library.adapter.flowable.GetAvailableCreditCardsTask",
                                "card",
                                DelegationType.SERVICE));

                registry.register(new OperationDescriptor(
                                "IssueCreditCard",
                                "Issue credit card through external card provider",
                                Arrays.asList("customerId", "cardType"),
                                Arrays.asList("cardNumber", "issueStatus"),
                                "com.iongroup.library.adapter.flowable.IssueCreditCardTask",
                                "card",
                                DelegationType.SERVICE));

                // SCRIPT delegations - Company system only
                registry.register(new OperationDescriptor(
                                "CreateLoanOffer",
                                "Create loan offer based on eligibility and policy",
                                Arrays.asList("eligibilityResult", "loanPolicy"),
                                Arrays.asList("loanOffer"),
                                "com.iongroup.library.adapter.flowable.CreateLoanOfferTask",
                                "loan",
                                DelegationType.SERVICE));

                registry.register(new OperationDescriptor(
                                "CreateCardOffer",
                                "Create credit card offer based on customer profile",
                                Arrays.asList("customerProfile", "availableCards"),
                                Arrays.asList("cardOffer"),
                                "com.iongroup.library.adapter.flowable.CreateCardOfferTask",
                                "card",
                                DelegationType.SERVICE));

                registry.register(new OperationDescriptor(
                                "StartLoanApplication",
                                "Initialize loan application workflow",
                                Arrays.asList("customerId"),
                                Arrays.asList("applicationId", "customerProfile"),
                                "com.iongroup.library.adapter.flowable.StartLoanApplicationTask",
                                "loan",
                                DelegationType.SERVICE));

                registry.register(new OperationDescriptor(
                                "StartCardApplication",
                                "Initialize credit card application workflow",
                                Arrays.asList("customerId"),
                                Arrays.asList("applicationId", "customerProfile"),
                                "com.iongroup.library.adapter.flowable.StartCardApplicationTask",
                                "card",
                                DelegationType.SERVICE));

                registry.register(new OperationDescriptor(
                                "EndLoanApplication",
                                "Finalize loan application workflow",
                                Arrays.asList("applicationId", "approvalStatus"),
                                Arrays.asList("status", "result"),
                                "com.iongroup.library.adapter.flowable.EndLoanApplicationTask",
                                "loan",
                                DelegationType.SERVICE));

                // USER_TASK delegations - User interaction required
                registry.register(new OperationDescriptor(
                                "CustomerApproval",
                                "Customer approval for loan/card offer",
                                Arrays.asList("offer", "customerId"),
                                Arrays.asList("approvalStatus", "approvalTimestamp"),
                                "com.iongroup.library.adapter.flowable.CustomerApprovalTask",
                                "common",
                                DelegationType.SERVICE));

                registry.register(new OperationDescriptor(
                                "NotifyCustomer",
                                "Send notification to customer",
                                Arrays.asList("customerId", "notificationType", "content"),
                                Arrays.asList("notificationStatus"),
                                "com.iongroup.library.adapter.flowable.NotifyCustomerTask",
                                "common",
                                DelegationType.SERVICE));

                registry.register(new OperationDescriptor(
                                "GetCustomerProfile",
                                "Get customer profile",
                                Arrays.asList("customerId", "notificationType", "content"),
                                Arrays.asList("notificationStatus"),
                                "com.iongroup.library.adapter.flowable.GetCustomerProfileTask",
                                "common",
                                DelegationType.SERVICE));


                registry.register(new OperationDescriptor(
                                "EnterCustomerDetails",
                                "Collect customer details dynamically",
                                Arrays.asList("customerProfile"), // INPUTS
                                Arrays.asList("customerProfile"), // OUTPUTS
                                "com.iongroup.library.adapter.flowable.EnterCustomerDetailsTask",
                                "common",
                                DelegationType.SERVICE, // VERY IMPORTANT
                                Arrays.asList(
                                                "CUSTOMER_NAME",
                                                "CONTACT_NUMBER",
                                                "ADDRESS",
                                                "PAN",
                                                "AADHAR",
                                                "MONTHLY_INCOME")));

                return registry;
        }
}
