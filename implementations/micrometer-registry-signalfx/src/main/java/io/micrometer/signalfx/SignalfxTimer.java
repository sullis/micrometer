/*
 * Copyright 2022 VMware, Inc.
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
package io.micrometer.signalfx;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.StepBucketHistogram;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.step.StepTimer;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * A StepTimer which provides support for multiple flavours of Histograms to be recorded
 * based on {@link SignalFxConfig#publishCumulativeHistogram()} and
 * {@link SignalFxConfig#publishDeltaHistogram()}.
 *
 * @author Bogdan Drutu
 * @author Mateusz Rzeszutek
 * @author Lenin Jaganathan
 */
final class SignalfxTimer extends StepTimer {

    private final @Nullable StepBucketHistogram stepBucketHistogram;

    SignalfxTimer(Id id, Clock clock, DistributionStatisticConfig distributionStatisticConfig,
            PauseDetector pauseDetector, TimeUnit baseTimeUnit, long stepMillis, boolean isDelta) {
        super(id, clock, distributionStatisticConfig, pauseDetector, baseTimeUnit, stepMillis, defaultHistogram(clock,
                CumulativeHistogramConfigUtil.updateConfig(distributionStatisticConfig, isDelta), false));

        double[] slo = distributionStatisticConfig.getServiceLevelObjectiveBoundaries();
        if (slo != null && slo.length > 0 && isDelta) {
            stepBucketHistogram = new StepBucketHistogram(clock, stepMillis,
                    DistributionStatisticConfig.builder()
                        .serviceLevelObjectives(CumulativeHistogramConfigUtil.addPositiveInfBucket(slo))
                        .build()
                        .merge(distributionStatisticConfig),
                    false, true);
        }
        else {
            stepBucketHistogram = null;
        }
    }

    @Override
    protected void recordNonNegative(long amount, TimeUnit unit) {
        if (stepBucketHistogram != null) {
            stepBucketHistogram.recordLong(TimeUnit.NANOSECONDS.convert(amount, unit));
        }
        super.recordNonNegative(amount, unit);
    }

    @Override
    public long count() {
        if (stepBucketHistogram != null) {
            // Force the stepBucketHistogram to be aligned to step by calling count. This
            // ensures that all values exported by the Timer are measured for the same
            // step. See StepMeterRegistry#pollMetersToRollover.
            stepBucketHistogram.poll();
        }
        return super.count();
    }

    @Override
    public HistogramSnapshot takeSnapshot() {
        HistogramSnapshot currentSnapshot = super.takeSnapshot();
        if (stepBucketHistogram == null) {
            return currentSnapshot;
        }
        return new HistogramSnapshot(currentSnapshot.count(), currentSnapshot.total(), currentSnapshot.max(),
                currentSnapshot.percentileValues(), stepBucketHistogram.poll(), currentSnapshot::outputSummary);
    }

}
