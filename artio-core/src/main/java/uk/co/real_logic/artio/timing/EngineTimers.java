/*
 * Copyright 2015-2020 Real Logic Limited.
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
package uk.co.real_logic.artio.timing;

import uk.co.real_logic.artio.Clock;

import java.util.Arrays;
import java.util.List;

public class EngineTimers
{
    private final Timer outboundTimer;
    private final Timer sendTimer;
    private final List<Timer> timers;

    public EngineTimers(final Clock clock)
    {
        outboundTimer = new Timer(clock, "Outbound", 1);
        sendTimer = new Timer(clock, "Send", 2);
        timers = Arrays.asList(outboundTimer, sendTimer);
    }

    public Timer outboundTimer()
    {
        return outboundTimer;
    }

    public Timer sendTimer()
    {
        return sendTimer;
    }

    public List<Timer> all()
    {
        return timers;
    }
}
