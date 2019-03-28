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
package org.thingsboard.gateway.extensions.mqtt.client.listener;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.util.StringUtils;
import org.thingsboard.gateway.extensions.mqtt.client.conf.mapping.DeviceStateChangeMapping;
import org.thingsboard.gateway.util.converter.AbstractJsonConverter;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ashvayka on 07.03.17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class MqttDeviceStateChangeMessageListener extends AbstractJsonConverter implements IMqttMessageListener {

    private final DeviceStateChangeMapping mapping;
    private final BiConsumer<String, String> deviceNameConsumer;
    private Pattern deviceNameTopicPattern;

    @Override
    public void messageArrived(String topic, MqttMessage msg) throws Exception {
        try {
            String deviceName = null;
            String deviceType = null;
            if (!StringUtils.isEmpty(mapping.getDeviceNameTopicExpression())) {
                deviceName = eval(topic);
            } else {
                String data = new String(msg.getPayload(), Charset.forName("GBK"));
                DocumentContext document = JsonPath.parse(data);
                deviceName = eval(document, mapping.getDeviceNameJsonExpression());
            }

            if (!StringUtils.isEmpty(mapping.getDeviceTypeTopicExpression())) {
                deviceType = eval(topic);
            } else if (!StringUtils.isEmpty(mapping.getDeviceTypeJsonExpression())) {
                String data = new String(msg.getPayload(), Charset.forName("GBK"));
                DocumentContext document = JsonPath.parse(data);
                deviceType = eval(document, mapping.getDeviceTypeJsonExpression());
            }

            if (deviceName != null) {
                deviceNameConsumer.accept(deviceName, deviceType);
            }
        } catch (Exception e) {
            log.error("Failed to convert msg", e);
        }
    }

    private String eval(String topic) {
        if (deviceNameTopicPattern == null) {
            deviceNameTopicPattern = Pattern.compile(mapping.getDeviceNameTopicExpression());
        }
        Matcher matcher = deviceNameTopicPattern.matcher(topic);
        while (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
