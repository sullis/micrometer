/*
 * Copyright 2017 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.atlas;

import io.micrometer.core.instrument.Statistic;
import org.jspecify.annotations.Nullable;

import static com.netflix.spectator.api.Statistic.*;

public class AtlasUtils {

    static com.netflix.spectator.api.@Nullable Statistic toSpectatorStatistic(Statistic stat) {
        switch (stat) {
            case COUNT:
                return count;
            case TOTAL_TIME:
                return totalTime;
            case TOTAL:
                return totalAmount;
            case VALUE:
                return gauge;
            case ACTIVE_TASKS:
                return activeTasks;
            case DURATION:
                return duration;
            case MAX:
                return max;
        }
        return null;
    }

}
