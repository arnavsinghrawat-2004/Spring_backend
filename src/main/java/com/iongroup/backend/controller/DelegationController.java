package com.iongroup.backend.controller;

import com.iongroup.backend.model.DelegationResponse;
import com.iongroup.backend.service.DelegationService;
import com.iongroup.library.registry.DelegationType;
import com.iongroup.library.registry.OperationDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing and retrieving delegation operations.
 * Provides endpoints to query operations by delegation type.
 */
@RestController
@RequestMapping("/api/delegations")
public class DelegationController {

    private final DelegationService delegationService;

    public DelegationController(DelegationService delegationService) {
        this.delegationService = delegationService;
    }

    /**
     * Example:
     * GET /api/delegations/type/SERVICE
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<DelegationResponse> getDelegationsByType(
            @PathVariable String type) {

        try {
            DelegationType delegationType = DelegationType.valueOf(type.toUpperCase());

            List<OperationDescriptor> delegations = delegationService.getDelegationsByType(delegationType);

            DelegationResponse response = new DelegationResponse(
                    true,
                    "Successfully retrieved " + delegations.size() + " delegations of type " + type,
                    delegations);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            DelegationResponse response = new DelegationResponse(
                    false,
                    "Invalid delegation type: " + type +
                            ". Valid types are: SERVICE, SCRIPT, USER_TASK",
                    null,
                    0);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {

            DelegationResponse response = new DelegationResponse(
                    false,
                    "Error retrieving delegations: " + e.getMessage(),
                    null,
                    0);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * GET /api/delegations/all
     */
    @GetMapping("/all")
    public ResponseEntity<DelegationResponse> getAllDelegations() {

        try {

            List<OperationDescriptor> delegations = delegationService.getAllDelegations();

            DelegationResponse response = new DelegationResponse(
                    true,
                    "Successfully retrieved all delegations",
                    delegations);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            DelegationResponse response = new DelegationResponse(
                    false,
                    "Error retrieving delegations: " + e.getMessage(),
                    null,
                    0);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * GET /api/delegations/count
     */
    @GetMapping("/count")
    public ResponseEntity<DelegationResponse> getDelegationCount() {

        try {

            int count = delegationService.getDelegationCount();

            DelegationResponse response = new DelegationResponse(
                    true,
                    "Total delegations count: " + count,
                    null,
                    count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            DelegationResponse response = new DelegationResponse(
                    false,
                    "Error retrieving delegation count: " + e.getMessage(),
                    null,
                    0);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * GET /api/delegations/types
     */
    @GetMapping("/types")
    public ResponseEntity<?> getValidDelegationTypes() {

        try {

            String[] types = { "SERVICE", "SCRIPT", "USER_TASK" };
            String[] descriptions = {
                    "Java code interacts with other software/external systems",
                    "Task happens in the company system only",
                    "Task is executed on the user end"
            };

            return ResponseEntity.ok(new java.util.HashMap<String, Object>() {
                {
                    put("success", true);
                    put("message", "Valid delegation types");
                    put("types", types);
                    put("details", new java.util.HashMap<String, String>() {
                        {
                            put("SERVICE", descriptions[0]);
                            put("SCRIPT", descriptions[1]);
                            put("USER_TASK", descriptions[2]);
                        }
                    });
                }
            });

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new java.util.HashMap<String, Object>() {
                        {
                            put("success", false);
                            put("message", "Error retrieving delegation types: " + e.getMessage());
                        }
                    });
        }
    }
}
