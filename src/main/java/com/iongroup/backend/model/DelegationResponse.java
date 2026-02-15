package com.iongroup.backend.model;

import com.iongroup.library.registry.OperationDescriptor;
import java.util.List;

/**
 * API response model for delegation operations.
 */
public class DelegationResponse {
    private boolean success;
    private String message;
    private List<OperationDescriptor> data;
    private int count;

    public DelegationResponse() {}

    public DelegationResponse(boolean success, String message, List<OperationDescriptor> data, int count) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.count = count;
    }

    public DelegationResponse(boolean success, String message, List<OperationDescriptor> data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.count = data != null ? data.size() : 0;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<OperationDescriptor> getData() {
        return data;
    }

    public void setData(List<OperationDescriptor> data) {
        this.data = data;
        this.count = data != null ? data.size() : 0;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
