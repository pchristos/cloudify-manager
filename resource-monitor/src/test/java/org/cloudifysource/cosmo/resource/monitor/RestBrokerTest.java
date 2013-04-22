/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.cosmo.resource.monitor;

import com.google.common.base.Throwables;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.ProgressAsyncHandler;
import com.ning.http.client.Response;
import org.cloudifysource.cosmo.broker.RestBrokerServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Tests {@link RestBrokerServer} and {@link org.cloudifysource.cosmo.broker.RestBrokerServlet}.
 * @author itaif
 * @since 0.1
 */
public class RestBrokerTest {

    RestBrokerServer server;
    private AsyncHttpClient asyncHttpClient;
    private URI uri;

    @BeforeMethod
    @Parameters({"port" })
    public void startRestServer(@Optional("8080") int port) {
        server = new RestBrokerServer();
        server.start(port);
        asyncHttpClient = new AsyncHttpClient();
        uri = URI.create("http://localhost:" + port);
    }

    @AfterMethod(alwaysRun = true)
    public void stopRestServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testPubSub() throws InterruptedException, IOException, ExecutionException {
        final String key = "r1";
        final String value = "v1";
        final String id = uri.resolve("/" + key).toString();
        final CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<String> future =
            asyncHttpClient
                    .prepareGet(id)
                    .execute(new ProgressAsyncHandler<String>() {

                        @Override
                        public void onThrowable(Throwable t) {
                            throw Throwables.propagate(t);
                        }

                        @Override
                        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                            String part = new String(bodyPart.getBodyPartBytes(), Charset.defaultCharset());
                            assertThat(part).isEqualTo(value);
                            latch.countDown();
                            return STATE.CONTINUE;
                        }

                        @Override
                        public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
                            if (responseStatus.getStatusCode() == 200) {
                                return STATE.CONTINUE;
                            } else {
                                return STATE.ABORT;
                            }
                        }

                        @Override
                        public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                            return STATE.CONTINUE;
                        }

                        @Override
                        public String onCompleted() throws Exception {
                            return null;  //To change body of implemented methods use File | Settings | File Templates.
                        }

                        @Override
                        public STATE onHeaderWriteCompleted() {
                            return STATE.CONTINUE;
                        }

                        @Override
                        public STATE onContentWriteCompleted() {
                            return STATE.CONTINUE;
                        }

                        @Override
                        public STATE onContentWriteProgress(long amount, long current, long total) {
                            return STATE.CONTINUE;
                        }
                    });
        while (latch.getCount() > 0) {
            Thread.sleep(100);
            asyncHttpClient
                .preparePost(id)
                .addHeader("Content-Length", String.valueOf(value.length()))
                .addHeader("Content-Type", MediaType.TEXT_PLAIN)
                .setBody(value)
                .execute(new AsyncCompletionHandler<Void>() {

                    @Override
                    public Void onCompleted(Response response) throws Exception {
                        assertThat(response.getStatusCode()).isEqualTo(200);
                        return null;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        throw Throwables.propagate(t);
                    }
                });
        }
    }

}