/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.http.netty;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonMessage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Netty based implementation for Carbon Message.
 */
public class NettyCarbonMessage extends CarbonMessage {

    private static final Logger LOG = LoggerFactory.getLogger(NettyCarbonMessage.class);

    private BlockingQueue<HttpContent> httpContentQueue = new LinkedBlockingQueue<>();

    public void addHttpContent(HttpContent httpContent) {
        if (httpContent instanceof LastHttpContent) {
            setEndOfMsgAdded(true);
        }
        httpContentQueue.add(httpContent);
    }

    public HttpContent getHttpContent() {
        try {
            return httpContentQueue.take();
        } catch (InterruptedException e) {
            LOG.error("Error while retrieving http content from queue.", e);
            return null;
        }
    }

    @Override
    public ByteBuffer getMessageBody() {
        try {
            HttpContent httpContent = httpContentQueue.take();
            return httpContent.content().nioBuffer();
        } catch (InterruptedException e) {
            LOG.error("Error while retrieving message body from queue.", e);
            return null;
        }
    }

    @Override
    public List<ByteBuffer> getFullMessageBody() {
        List<ByteBuffer> byteBufferList = new ArrayList<>();

        while (true) {
            try {
                HttpContent httpContent = httpContentQueue.take();
                byteBufferList.add(httpContent.content().nioBuffer());
                if (isEndOfMsgAdded() && isEmpty()) {
                    break;
                }
            } catch (InterruptedException e) {
                LOG.error("Error while getting full message body", e);
            }
        }

        return byteBufferList;
    }

    @Override
    public boolean isEmpty() {
        return this.httpContentQueue.isEmpty();
    }

    public int getMessageBodyLength() {
        int length = 0;
        for (HttpContent httpContent : httpContentQueue) {
            length += httpContent.content().readableBytes();
        }

        return length;
    }
}
