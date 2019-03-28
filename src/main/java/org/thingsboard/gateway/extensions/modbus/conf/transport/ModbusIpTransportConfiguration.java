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
 
 package org.thingsboard.gateway.extensions.modbus.conf.transport;

import lombok.Data;
import org.thingsboard.gateway.extensions.modbus.conf.ModbusExtensionConstants;

@Data
public class ModbusIpTransportConfiguration implements ModbusTransportConfiguration {
    private String host;
    private int port = ModbusExtensionConstants.DEFAULT_MODBUS_TCP_PORT;
    private int timeout = ModbusExtensionConstants.DEFAULT_SOCKET_TIMEOUT;
}