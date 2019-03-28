/**
 * Copyright © 2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.gateway.extensions.ExtensionService;
import org.thingsboard.gateway.extensions.file.DefaultFileTailService;
import org.thingsboard.gateway.extensions.http.DefaultHttpService;
import org.thingsboard.gateway.extensions.http.HttpService;
import org.thingsboard.gateway.extensions.modbus.DefaultModbusService;
import org.thingsboard.gateway.extensions.mqtt.client.DefaultMqttClientService;
import org.thingsboard.gateway.extensions.opc.DefaultOpcUaService;
import org.thingsboard.gateway.service.conf.TbExtensionConfiguration;
import org.thingsboard.gateway.service.gateway.GatewayService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ashvayka on 29.09.17.
 */
@Slf4j
@Data
public class TenantServiceRegistry implements ExtensionServiceCreation {

    private GatewayService service;
    private final Map<String, ExtensionService> extensions;
    private final Map<String, HttpService> httpServices;

    private static final String STATUS_INIT = "Initialized";
    private static final String STATUS_UPDATE = "Updated";
    private static final String STATUS_DELETE = "Removed";
    private static final String HTTP_EXTENSION = "HTTP";
    private static final String OPC_EXTENSION = "OPC UA";
    private static final String MQTT_EXTENSION = "MQTT";
    private static final String FILE_EXTENSION = "FILE";
    private static final String MODBUS_EXTENSION = "MODBUS";

    public TenantServiceRegistry() {
        this.extensions = new HashMap<>();
        this.httpServices = new HashMap<>();
    }

    public void updateExtensionConfiguration(String config) {
        log.info("[{}] Updating extension configuration", service.getTenantLabel());
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<TbExtensionConfiguration> updatedConfigurations = new ArrayList<>();
            for (JsonNode updatedExtension : mapper.readTree(config)) {
                updatedConfigurations.add(mapper.treeToValue(updatedExtension, TbExtensionConfiguration.class));
            }
            for (String existingExtensionId : extensions.keySet()) {
                if (!extensionIdContainsInArray(existingExtensionId, updatedConfigurations)) {
                    log.info("Destroying extension: [{}]", existingExtensionId);
                    extensions.get(existingExtensionId).destroy();
                    extensions.remove(existingExtensionId);
                    httpServices.remove(existingExtensionId);
                    service.onConfigurationStatus(existingExtensionId, STATUS_DELETE);
                }
            }
            for (TbExtensionConfiguration updatedConfiguration : updatedConfigurations) {
                if (!extensions.containsKey(updatedConfiguration.getId())) {
                    log.info("Initializing extension: [{}][{}]", updatedConfiguration.getId(), updatedConfiguration.getType());
                    ExtensionService extension = createExtensionServiceByType(service, updatedConfiguration.getType());
                    extension.init(updatedConfiguration, true);
                    service.onConfigurationStatus(updatedConfiguration.getId(), STATUS_INIT);
                    if (HTTP_EXTENSION.equals(updatedConfiguration.getType())) {
                        httpServices.put(updatedConfiguration.getId(), (HttpService) extension);
                    }
                    extensions.put(updatedConfiguration.getId(), extension);
                } else {
                    if (!updatedConfiguration.equals(extensions.get(updatedConfiguration.getId()).getCurrentConfiguration())) {
                        log.info("Updating extension: [{}][{}]", updatedConfiguration.getId(), updatedConfiguration.getType());
                        extensions.get(updatedConfiguration.getId()).update(updatedConfiguration);
                        service.onConfigurationStatus(updatedConfiguration.getId(), STATUS_UPDATE);
                    }
                }
            }
        } catch (Exception e) {
            log.info("Failed to read configuration attribute", e);
            throw new RuntimeException("Failed to update configuration", e);
        }
    }

    private boolean extensionIdContainsInArray(String extensionId, List<TbExtensionConfiguration> array) {
        for (TbExtensionConfiguration configuration : array) {
            if (configuration.getId().equalsIgnoreCase(extensionId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ExtensionService createExtensionServiceByType(GatewayService gateway, String type) {
        switch (type) {
            case FILE_EXTENSION:
                return new DefaultFileTailService(gateway);
            case OPC_EXTENSION:
                return new DefaultOpcUaService(gateway);
            case HTTP_EXTENSION:
                return new DefaultHttpService(gateway);
            case MQTT_EXTENSION:
                return new DefaultMqttClientService(gateway);
            case MODBUS_EXTENSION:
                return new DefaultModbusService(gateway);
            default:
                throw new IllegalArgumentException("Extension: " + type + " is not supported!");
        }
    }

    public void processRequest(String converterId, String token, String body) throws Exception {
        for (HttpService service : httpServices.values()) {
            service.processRequest(converterId, token, body);
        }
    }
}
