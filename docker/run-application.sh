#!/bin/bash
#
# Copyright © 2017 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

dpkg -i /tb-gateway.deb

# Copying env variables into conf files
printenv | awk -F "=" '{print "export " $1 "='\''" $2 "'\''"}' >> /usr/share/tb-gateway/conf/tb-gateway.conf

cat /usr/share/tb-gateway/conf/tb-gateway.conf

echo "Starting 'TB-gateway' service..."
service tb-gateway start

# Wait until log file is created
sleep 5
tail -f /var/log/tb-gateway/tb-gateway.log