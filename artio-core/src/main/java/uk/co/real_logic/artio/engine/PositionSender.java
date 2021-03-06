/*
 * Copyright 2015-2020 Real Logic Limited, Adaptive Financial Consulting Ltd.
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
package uk.co.real_logic.artio.engine;

import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.LongLongConsumer;
import uk.co.real_logic.artio.Pressure;
import uk.co.real_logic.artio.engine.logger.Index;
import uk.co.real_logic.artio.engine.logger.IndexedPositionConsumer;
import uk.co.real_logic.artio.messages.FixMessageDecoder;
import uk.co.real_logic.artio.messages.MessageHeaderDecoder;
import uk.co.real_logic.artio.protocol.GatewayPublication;

public class PositionSender implements Index
{
    private static final int MISSING_LIBRARY = -1;

    private final MessageHeaderDecoder messageHeader = new MessageHeaderDecoder();
    private final FixMessageDecoder fixMessage = new FixMessageDecoder();
    private final Long2LongHashMap libraryIdToPosition = new Long2LongHashMap(MISSING_LIBRARY);
    private final LongLongConsumer resendPositionFunc = this::endPosition;

    private final GatewayPublication publication;

    private int resendCount;

    public PositionSender(final GatewayPublication publication)
    {
        this.publication = publication;
    }

    @SuppressWarnings("FinalParameters")
    public void onFragment(
        final DirectBuffer buffer,
        int offset,
        final int length,
        final Header header)
    {
        messageHeader.wrap(buffer, offset);

        if (messageHeader.templateId() == FixMessageDecoder.TEMPLATE_ID)
        {
            offset += MessageHeaderDecoder.ENCODED_LENGTH;

            fixMessage.wrap(buffer, offset, messageHeader.blockLength(), messageHeader.version());

            newPosition(fixMessage.libraryId(), header.position());
        }
    }

    public void newPosition(final int libraryId, final long endPosition)
    {
        libraryIdToPosition.put(libraryId, endPosition);
    }

    public int doWork()
    {
        resendCount = 0;
        libraryIdToPosition.longForEach(resendPositionFunc);
        return resendCount;
    }

    private void endPosition(final long libraryId, final long endPosition)
    {
        if (saveNewSentPosition((int)libraryId, endPosition))
        {
            libraryIdToPosition.remove(libraryId);
            resendCount++;
        }
    }

    private boolean saveNewSentPosition(final int libraryId, final long endPosition)
    {
        return !Pressure.isBackPressured(publication.saveNewSentPosition(libraryId, endPosition));
    }

    public void close()
    {
    }

    public void readLastPosition(final IndexedPositionConsumer consumer)
    {
    }

    public void onCatchup(final DirectBuffer buffer, final int offset, final int length, final Header header,
        final long recordingId)
    {
        onFragment(buffer, offset, length, header);
    }
}
