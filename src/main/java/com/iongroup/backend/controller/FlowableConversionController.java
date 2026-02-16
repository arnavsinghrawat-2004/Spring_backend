package com.iongroup.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iongroup.backend.model.FlowableConversionResponse;
import com.example.flow.UiToFlowableConverter;
import com.example.flow.UiToFlowableConverter.ConverterConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for converting UI flow graphs to BPMN and executing them
 */
@RestController
@RequestMapping("/api/flowable")
public class FlowableConversionController {

    private static final Logger logger = LoggerFactory.getLogger(FlowableConversionController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Endpoint to convert UI JSON graph to BPMN and execute it
     * POST /api/flowable/convert-and-execute
     * Request body: UI graph JSON
     */
    @PostMapping("/convert-and-execute")
    public ResponseEntity<FlowableConversionResponse> convertAndExecute(
            @RequestBody Map<String, Object> uiJsonMap) {

        try {
            logger.info("Starting conversion process for UI JSON");

            // Convert Map to ObjectNode for processing
            ObjectNode uiJson = objectMapper.valueToTree(uiJsonMap);
            logger.debug("Converted request to ObjectNode: {}", uiJson.asText());

            // Step 1: Convert UI JSON to Flowable JSON
            logger.debug("Step 1: Converting UI JSON to Flowable JSON");
            ConverterConfig cfg = ConverterConfig.defaultConfig();
            ObjectNode flowableJson = UiToFlowableConverter.convert(uiJson, cfg);
            logger.debug("Flowable JSON generated successfully");

            // Step 2: Convert Flowable JSON to BPMN XML
            logger.debug("Step 2: Converting Flowable JSON to BPMN XML");
            BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
            BpmnModel bpmnModel = jsonConverter.convertToBpmnModel(flowableJson);

            if (bpmnModel == null || bpmnModel.getProcesses().isEmpty()) {
                throw new IllegalStateException("No BPMN processes generated from Flowable JSON");
            }

            // Enrich service tasks with extension elements
            enrichServiceTasks(bpmnModel, flowableJson);

            BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
            byte[] bpmnXml = xmlConverter.convertToXML(bpmnModel);
            logger.debug("BPMN XML generated successfully, length: {} bytes", bpmnXml.length);

            // Step 3: Try to execute the BPMN process (optional, doesn't fail the conversion)
            Map<String, Object> executionResult = null;
            String executionMessage = "Successfully converted UI JSON to BPMN. ";
            
            try {
                logger.debug("Step 3: Executing BPMN process");
                executionResult = executeBpmnProcess(bpmnXml, bpmnModel);
                logger.info("BPMN process executed successfully");
                executionMessage += "Process executed successfully.";
            } catch (Exception executionError) {
                // Log execution error but don't fail the response - BPMN generation was successful
                logger.warn("Process execution failed (non-fatal, BPMN still valid): {}", 
                    executionError.getMessage());
                executionMessage += String.format("Note: Process execution encountered an error: %s. " +
                    "This may be due to missing input variables for service tasks. " +
                    "BPMN XML is still valid and can be deployed separately.", 
                    executionError.getMessage());
            }

            FlowableConversionResponse response = new FlowableConversionResponse(
                    true,
                    executionMessage,
                    new String(bpmnXml, StandardCharsets.UTF_8),
                    flowableJson,
                    executionResult
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during BPMN conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FlowableConversionResponse(
                            false,
                            "Error: " + e.getMessage(),
                            null,
                            null,
                            null
                    ));
        }
    }

    /**
     * Execute BPMN process and collect results
     */
    private Map<String, Object> executeBpmnProcess(byte[] bpmnXml, BpmnModel bpmnModel) throws Exception {
        logger.info("Initializing Flowable engine");

        // Create in-memory Flowable engine
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration();
        cfg.setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000");
        cfg.setJdbcDriver("org.h2.Driver");
        cfg.setJdbcUsername("sa");
        cfg.setJdbcPassword("");
        cfg.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine engine = cfg.buildProcessEngine();

        try {
            RepositoryService repositoryService = engine.getRepositoryService();
            RuntimeService runtimeService = engine.getRuntimeService();
            HistoryService historyService = engine.getHistoryService();

            // Deploy the BPMN
            logger.info("Deploying BPMN process");
            Deployment deployment = repositoryService.createDeployment()
                    .name("ui-generated-process")
                    .addInputStream("process.bpmn20.xml", new ByteArrayInputStream(bpmnXml))
                    .deploy();
            logger.info("Deployment successful, deployment ID: {}", deployment.getId());

            // Start process with default variables
            logger.info("Starting process instance");
            String processKey = bpmnModel.getProcesses().get(0).getId();
            Map<String, Object> processVars = new HashMap<>();
            processVars.put("initiator", "flowbox-ui");

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, processVars);
            String processInstanceId = processInstance.getProcessInstanceId();
            logger.info("Process instance started, ID: {}", processInstanceId);

            // Collect results from history
            Map<String, Object> results = new HashMap<>();
            results.put("processInstanceId", processInstanceId);
            results.put("deploymentId", deployment.getId());
            results.put("processKey", processKey);

            // Collect all process variables from history
            List<HistoricVariableInstance> variables = historyService
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();

            Map<String, Object> processVariables = new HashMap<>();
            for (HistoricVariableInstance var : variables) {
                processVariables.put(var.getVariableName(), var.getValue());
                logger.debug("Process variable: {} = {}", var.getVariableName(), var.getValue());
            }
            results.put("processVariables", processVariables);

            logger.info("Process execution completed successfully");
            return results;

        } finally {
            engine.close();
            logger.debug("Flowable engine closed");
        }
    }

    /**
     * Enrich BPMN model with service task configuration from Flowable JSON
     */
    private void enrichServiceTasks(BpmnModel bpmnModel, ObjectNode flowableJson) {
        logger.debug("Enriching BPMN model with extension elements from Flowable JSON");

        // Extract task properties from Flowable JSON
        Map<String, ObjectNode> taskProperties = new HashMap<>();
        JsonNode childShapes = flowableJson.get("childShapes");
        if (childShapes != null && childShapes.isArray()) {
            for (JsonNode shape : childShapes) {
                String resourceId = shape.get("resourceId").asText();
                JsonNode props = shape.get("properties");
                if (props != null && props.isObject()) {
                    taskProperties.put(resourceId, (ObjectNode) props);
                }
            }
        }

        // Enrich service tasks in BPMN model
        bpmnModel.getProcesses().forEach(process ->
            process.getFlowElements().forEach(element -> {
                if (element instanceof ServiceTask) {
                    ServiceTask task = (ServiceTask) element;
                    ObjectNode props = taskProperties.get(element.getId());

                    if (props != null) {
                        // Add delegationId as extension element if present
                        String delegationId = props.has("delegationId") 
                            ? props.get("delegationId").asText() 
                            : null;
                        
                        if (delegationId != null && !delegationId.isBlank()) {
                            addExtensionElement(task, "delegationId", delegationId);
                            logger.debug("Added delegationId '{}' to service task '{}'", delegationId, element.getId());
                        }

                        // Add other extension elements
                        if (props.has("delegationType")) {
                            addExtensionElement(task, "delegationType", 
                                props.get("delegationType").asText());
                        }
                        if (props.has("selectedFields")) {
                            addExtensionElement(task, "selectedFields", 
                                props.get("selectedFields").asText());
                        }
                        if (props.has("requiredFields")) {
                            addExtensionElement(task, "requiredFields", 
                                props.get("requiredFields").asText());
                        }
                    }
                }
            })
        );
    }

    /**
     * Helper to add extension element to a service task
     */
    private void addExtensionElement(ServiceTask task, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        
        ExtensionElement element = new ExtensionElement();
        element.setNamespace("http://flowable.org/bpmn");
        element.setNamespacePrefix("flowable");
        element.setName(name);
        element.setElementText(value);
        task.addExtensionElement(element);
    }
}
