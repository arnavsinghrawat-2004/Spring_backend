package com.iongroup.backend.service;

import com.iongroup.library.registry.DelegationType;
import com.iongroup.library.registry.OperationDescriptor;
import com.iongroup.library.registry.OperationRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing and retrieving delegation operations from the registry.
 */
@Service
public class DelegationService {

    private final OperationRegistry operationRegistry;

    public DelegationService(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

    /**
     * Get all operations of a specific delegation type.
     *
     * @param delegationType the type of delegation (SERVICE, SCRIPT, USER_TASK)
     * @return list of all operations matching the delegation type
     */
    public List<OperationDescriptor> getDelegationsByType(DelegationType delegationType) {
        return operationRegistry.getOperationsByDelegationType(delegationType);
    }

    /**
     * Get all registered operations.
     *
     * @return list of all operation descriptors
     */
    public List<OperationDescriptor> getAllDelegations() {
        return operationRegistry.getAllOperations();
    }

    /**
     * Get the total count of registered operations.
     *
     * @return operation count
     */
    public int getDelegationCount() {
        return operationRegistry.getOperationCount();
    }
}
