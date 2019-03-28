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

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.util.StringUtils;
import org.thingsboard.gateway.service.data.DeviceData;
import org.thingsboard.gateway.util.converter.BasicJsonConverter;
import org.thingsboard.server.common.data.kv.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.text.ParseException;

/**
 * Created by ashvayka on 23.01.17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class MqttJsonConverter extends BasicJsonConverter implements MqttDataConverter {

    private String deviceNameTopicExpression;
    private Pattern deviceNameTopicPattern;

    private String deviceTypeTopicExpression;
    private Pattern deviceTypeTopicPattern;
    private int timeout;

    @Override
    public List<DeviceData> convert(String topic, MqttMessage msg) throws Exception {
        String data = new String(msg.getPayload(), StandardCharsets.UTF_8);
        if(isMessyCode(data)){
            data = new String(msg.getPayload(), Charset.forName("GBK"));
        }
        log.trace("Parsing json message: {}", data);

        if (!filterExpression.isEmpty()) {
            try {
                log.debug("Data before filtering {}", data);
                DocumentContext document = JsonPath.parse(data);
                document = JsonPath.parse((Object) document.read(filterExpression));
                data = document.jsonString();
                log.debug("Data after filtering {}", data);
            } catch (RuntimeException e) {
                log.debug("Failed to apply filter expression: {}", filterExpression);
                throw new RuntimeException("Failed to apply filter expression " + filterExpression);
            }
        }

        JsonNode node = mapper.readTree(data);
        List<String> srcList;
        if (node.isArray()) {
            srcList = new ArrayList<>(node.size());
            for (int i = 0; i < node.size(); i++) {
                srcList.add(mapper.writeValueAsString(node.get(i)));
            }
        } else {
            srcList = Collections.singletonList(data);
        }

        return parse(topic, srcList);
    }

    private List<DeviceData> parse(String topic, List<String> srcList) throws ParseException{
        List<DeviceData> result = new ArrayList<>(srcList.size());
        for (String src : srcList) {
            Configuration conf = Configuration.builder()
                    .options(Option.SUPPRESS_EXCEPTIONS).build();

            DocumentContext document = JsonPath.using(conf).parse(src);
            long ts = System.currentTimeMillis();
            String deviceName;
            String deviceType = null;
            if (!StringUtils.isEmpty(deviceNameTopicExpression)) {
                deviceName = evalDeviceName(topic);
            } else {
                deviceName = eval(document, deviceNameJsonExpression);
            }
            if (!StringUtils.isEmpty(deviceTypeTopicExpression)) {
                deviceType = evalDeviceType(topic);
            } else if (!StringUtils.isEmpty(deviceTypeJsonExpression)) {
                deviceType = eval(document, deviceTypeJsonExpression);
                if (deviceType.equals(deviceTypeJsonExpression)){deviceType="default";}
            }

            if (!StringUtils.isEmpty(deviceName)) {
                List<KvEntry> attrData = getKvEntries(document, attributes);
                List<TsKvEntry> tsData = getTsKvEntries(document, timeseries, ts);
                result.add(new DeviceData(deviceName, deviceType, attrData, tsData, timeout));
            }
        }
        return result;
    }

    private String evalDeviceName(String topic) {
        if (deviceNameTopicPattern == null) {
            deviceNameTopicPattern = Pattern.compile(deviceNameTopicExpression);
        }
        Matcher matcher = deviceNameTopicPattern.matcher(topic);
        while (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String evalDeviceType(String topic) {
        if (deviceTypeTopicPattern == null) {
            deviceTypeTopicPattern = Pattern.compile(deviceTypeTopicExpression);
        }
        Matcher matcher = deviceTypeTopicPattern.matcher(topic);
        while (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * 判断是否是中日韩文字
     * @param c     要判断的字符
     * @return      true或false
     */
    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }


    /**
     * 判断是否是数字或者是英文字母
     * @param c
     * @return
     */
    private static boolean judge(char c){
        if((c >='0' && c<='9')||(c >='a' && c<='z' ||  c >='A' && c<='Z')){
            return true;
        }
        return false;
    }
    private static boolean isMessyCode(String strName) {
        //去除字符串中的空格 制表符 换行 回车
        Pattern p = Pattern.compile("\\s*|\t*|\r*|\n*");
        Matcher m = p.matcher(strName);
        String after = m.replaceAll("");
        //去除字符串中的标点符号
        String temp = after.replaceAll("\\p{P}", "");
        //处理之后转换成字符数组
        char[] ch = temp.trim().toCharArray();
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            //判断是否是数字或者英文字符
            if (!judge(c)) {
                //判断是否是中日韩文
                if (!isChinese(c)) {
                    //如果不是数字或者英文字符也不是中日韩文则表示是乱码返回true
                    return true;
                }
            }
        }
        //表示不是乱码 返回false
        return false;
    }

}
