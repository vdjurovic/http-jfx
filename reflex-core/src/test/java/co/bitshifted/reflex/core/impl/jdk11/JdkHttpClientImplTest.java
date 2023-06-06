/*
 *
 *  * Copyright (c) 2023-2023  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.reflex.core.impl.jdk11;

import static co.bitshifted.reflex.core.Reflex.client;
import static co.bitshifted.reflex.core.Reflex.context;
import static co.bitshifted.reflex.core.http.RFXHttpMethod.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import co.bitshifted.reflex.core.exception.HttpStatusException;
import co.bitshifted.reflex.core.http.*;
import co.bitshifted.reflex.core.serialize.PlainTextBodySerializer;
import co.bitshifted.reflex.core.serialize.file.FileOperationSerializer;
import co.bitshifted.reflex.core.serialize.file.FileUploadDetails;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@WireMockTest(httpPort = 9010)
public class JdkHttpClientImplTest {

  @Test
  void basicGetRequestSuccess() throws Exception {
    stubFor(
        get("/test/endpoint")
            .withHeader("custom-header", equalTo("custom-value"))
            .willReturn(ok("test body").withHeader(RFXHttpHeaders.CONTENT_TYPE, "text/plain")));
    context().registerBodySerializer(RFXMimeTypes.TEXT_PLAIN, new PlainTextBodySerializer());
    context().configuration().commonHeader("custom-header", "custom-value");
    var client = new JdkReflexClient();
    var request =
        RFXHttpRequestBuilder.newBuilder()
            .method(GET)
            .requestUri(new URI("http://localhost:9010/test/endpoint"))
            .build();
    var response = client.sendHttpRequest(request);
    assertNotNull(response);
    assertNotNull(response.body());
    var responseBody = response.bodyToValue(String.class);
    assertEquals("test body", responseBody);
  }

  @Test
  @Disabled
  void basicPostRequestWhenResponseStatusOKSuccess() throws Exception {
    stubFor(
        post("/test/post")
            .willReturn(
                ok("response body")
                    .withHeader(
                        RFXHttpHeaders.CONTENT_TYPE, RFXMimeTypes.TEXT_PLAIN.toMimeTypeString())));
    context().registerBodySerializer(RFXMimeTypes.TEXT_PLAIN, new PlainTextBodySerializer());
    var headers = new RFXHttpHeaders();
    headers.setHeader(RFXHttpHeaders.CONTENT_TYPE, RFXMimeTypes.TEXT_PLAIN.toMimeTypeString());
    var client = new JdkReflexClient();
    var request =
        RFXHttpRequestBuilder.newBuilder("content")
            .method(POST)
            .requestUri(new URI("http://localhost:9010/test/post"))
            .build();
    var response = client.sendHttpRequest(request);
    assertNotNull(response);
    assertEquals(RFXHttpStatus.OK, response.status());
    assertEquals("response body", response.bodyToValue(String.class));
  }

  @Test
  void invalidResponseStatusShouldThrowException() throws Exception {
    stubFor(get("/test/wrong-status").willReturn(badRequest()));
    var request =
        RFXHttpRequestBuilder.newBuilder()
            .method(GET)
            .requestUri(new URI("http://localhost:9010/test/wrong-status"))
            .build();
    assertThrows(HttpStatusException.class, () -> client().sendHttpRequest(request));
  }

  @Test
  void asyncGetRequestReturnsSuccess() throws Exception {
    stubFor(
        get("/test/endpoint")
            .willReturn(ok("test body").withHeader(RFXHttpHeaders.CONTENT_TYPE, "text/plain")));
    context().registerBodySerializer(RFXMimeTypes.TEXT_PLAIN, new PlainTextBodySerializer());
    var client = new JdkReflexClient();
    var request =
        RFXHttpRequestBuilder.newBuilder()
            .method(GET)
            .requestUri(new URI("http://localhost:9010/test/endpoint"))
            .build();
    var response = client.sendHttpRequestAsync(request).get();
    assertNotNull(response);
    assertNotNull(response.body());
    var responseBody = response.bodyToValue(String.class);
    assertEquals("test body", responseBody);
  }

  @Test
  void asyncGetRequestThrowsException() {
    stubFor(get("/test/fail-async").willReturn(status(500)));
    context().configuration().baseUri("http://localhost:9010");
    var client = new JdkReflexClient();
    var request = RFXHttpRequestBuilder.newBuilder().method(GET).path("/test/fail-async").build();
    var result = client.sendHttpRequestAsync(request);
    assertThrows(ExecutionException.class, () -> result.get());
  }

  @Test
  void fileUploadWithProgressSuccess() throws Exception {
    stubFor(post("/file-upload").withHeader("Content-Type", equalTo("application/octet-stream")).willReturn(noContent()));
    var uploadFile = Files.createTempFile("test", "upload");
    System.out.println("upload file: " + uploadFile.toAbsolutePath());
    System.out.println("Creating random content");
    var random = new Random();
    var data = new byte[8192];
    for(int i = 0;i < 1000;i++) {
      random.nextBytes(data);
      Files.write(uploadFile, data, StandardOpenOption.APPEND);
    }
    var fileDetails = new FileUploadDetails(uploadFile);
    context().configuration().baseUri("http://localhost:9010");
    context().registerBodySerializer(RFXMimeTypes.fromString("application/octet-stream"), new FileOperationSerializer());
    var request = RFXHttpRequestBuilder.newBuilder(fileDetails)
            .method(POST)
            .header(RFXHttpHeaders.CONTENT_TYPE, "application/octet-stream")
            .urlTemplate(RFXHttpRequestBuilder.UrlTemplateBuilder.urlTemplate("/file-upload")).build();
    var client = new JdkReflexClient();
//    var th = new Thread(() -> {
//      try {
//        client.sendHttpRequest(request);
//      } catch(Exception ex) {
//        ex.printStackTrace();
//      }
//    });
//    th.start();
    System.out.println("total bytes: " + fileDetails.getFileSize());
    var response = client.sendHttpRequestAsync(request);
    CompletableFuture.runAsync(() -> {
      System.out.println("total bytes: " + fileDetails.getFileSize());
      while (fileDetails.getReadBytesCount() < fileDetails.getFileSize()) {
//        System.out.println("read bytes count: " + fileDetails.getReadBytesCount());
      }
    });
    var result = response.get();
    assertEquals(RFXHttpStatus.NO_CONTENT, result.status());
    assertEquals(fileDetails.getFileSize(), fileDetails.getReadBytesCount());
//    while (fileDetails.getReadBytesCount() <= fileDetails.getFileSize()) {
//      Thread.sleep(3000);
////      System.out.println("read bytes count: " + fileDetails.getReadBytesCount());
//    }
  }
}
