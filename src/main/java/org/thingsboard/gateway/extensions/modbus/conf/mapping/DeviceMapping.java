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

package org.thingsboard.gateway.extensions.modbus.conf.mapping;

import lombok.Data;
import org.thingsboard.gateway.extensions.modbus.conf.ModbusExtensionConstants;

import java.util.Collections;
import java.util.List;

@Data
public class DeviceMapping {
    private int unitId;
    private String deviceName;
    private int attributesPollPeriod = ModbusExtensionConstants.DEFAULT_POLL_PERIOD;
    private int timeseriesPollPeriod = ModbusExtensionConstants.DEFAULT_POLL_PERIOD;
    private List<PollingTagMapping> attributes = Collections.emptyList(); // FIXME: Is it a real case, what device is without attributes?
    private List<PollingTagMapping> timeseries = Collections.emptyList(); // FIXME: Is it a real case, what device is without timeseries?
}
