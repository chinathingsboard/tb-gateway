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
package org.thingsboard.gateway.extensions.mqtt.client.conf.mapping;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.thingsboard.gateway.service.data.AttributeRequest;
import org.thingsboard.gateway.util.converter.AbstractJsonConverter;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

/**
 * Created by ashvayka on 22.02.17.
 */
@Data
@Slf4j
public class AttributeRequestsMapping extends AbstractJsonConverter {

    private String topicFilter;

    private String deviceNameJsonExpression;
    private String attributeKeyJsonExpression;
    private String requestIdJsonExpression;

    private String deviceNameTopicExpression;
    private String attributeKeyTopicExpression;
    private String requestIdTopicExpression;

    private Pattern deviceNameTopicPattern;
    private Pattern attributeKeyTopicPattern;
    private Pattern requestIdTopicPattern;

    private boolean clientScope;
    private String responseTopicExpression;
    private String valueExpression;

    public AttributeRequest convert(String topic, MqttMessage msg) {
        deviceNameTopicPattern = checkAndCompile(deviceNameTopicPattern, deviceNameTopicExpression);
        attributeKeyTopicPattern = checkAndCompile(attributeKeyTopicPattern, attributeKeyTopicExpression);
        requestIdTopicPattern = checkAndCompile(requestIdTopicPattern, requestIdTopicExpression);

        String data = new String(msg.getPayload(), Charset.forName("GBK"));
        DocumentContext document = JsonPath.parse(data);

        AttributeRequest.AttributeRequestBuilder builder = AttributeRequest.builder();
        builder.deviceName(eval(topic, deviceNameTopicPattern, document, deviceNameJsonExpression));
        builder.attributeKey(eval(topic, attributeKeyTopicPattern, document, attributeKeyJsonExpression));
        builder.requestId(Integer.parseInt(eval(topic, requestIdTopicPattern, document, requestIdJsonExpression)));

        builder.clientScope(this.isClientScope());
        builder.topicExpression(this.getResponseTopicExpression());
        builder.valueExpression(this.getValueExpression());
        return builder.build();
    }
}
