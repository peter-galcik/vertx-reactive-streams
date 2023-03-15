/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.reactivestreams.test;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import io.vertx.test.core.TestUtils;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReactiveWriteStreamTest extends ReactiveStreamTestBase {

  @Test
  public void testWriteNoTokensInitially() throws Exception {
    ReactiveWriteStream<Buffer> rws = ReactiveWriteStream.writeStream(vertx);

    MySubscriber subscriber = new MySubscriber();
    rws.subscribe(subscriber);

    assertWaitUntil(() -> subscriber.subscription != null);

    List<Buffer> buffers = createRandomBuffers(4);
    for (Buffer buffer: buffers) {
      rws.write(buffer);
    }

    assertTrue(subscriber.buffers.isEmpty());

    subscriber.subscription.request(1);

    assertWaitUntil(() -> subscriber.buffers.size() == 1);
    assertEquals(1, subscriber.buffers.size());
    assertSame(buffers.get(0), subscriber.buffers.get(0));

    subscriber.subscription.request(2);
    assertWaitUntil(() -> subscriber.buffers.size() == 3);
    assertEquals(3, subscriber.buffers.size());
    assertSame(buffers.get(1), subscriber.buffers.get(1));
    assertSame(buffers.get(2), subscriber.buffers.get(2));

  }

  @Test
  public void testWriteInitialTokens() throws Exception {
    ReactiveWriteStream<Buffer> rws = ReactiveWriteStream.writeStream(vertx);

    MySubscriber subscriber = new MySubscriber();
    rws.subscribe(subscriber);

    assertWaitUntil(() -> subscriber.subscription != null);
    subscriber.subscription.request(3);

    List<Buffer> buffers = createRandomBuffers(4);
    for (Buffer buffer: buffers) {
      rws.write(buffer);
    }

    assertWaitUntil(() -> subscriber.buffers.size() == 3);
    assertEquals(3, subscriber.buffers.size());
    assertSame(buffers.get(0), subscriber.buffers.get(0));
    assertSame(buffers.get(1), subscriber.buffers.get(1));
    assertSame(buffers.get(2), subscriber.buffers.get(2));

  }

  // TODO test setters for max writestreamsize and buffer size and valid values

  // TODO test cancel subscription

  @Test
  public void testMultipleSubscribers() throws Exception {
    ReactiveWriteStream<Buffer> rws = ReactiveWriteStream.writeStream(vertx);

    MySubscriber subscriber1 = new MySubscriber();
    rws.subscribe(subscriber1);
    MySubscriber subscriber2 = new MySubscriber();
    rws.subscribe(subscriber2);
    MySubscriber subscriber3 = new MySubscriber();
    rws.subscribe(subscriber3);

    assertWaitUntil(() -> subscriber1.subscription != null);
    assertWaitUntil(() -> subscriber2.subscription != null);
    assertWaitUntil(() -> subscriber3.subscription != null);

    List<Buffer> buffers = createRandomBuffers(10);
    for (Buffer buffer: buffers) {
      rws.write(buffer);
    }

    assertEquals(0, subscriber1.buffers.size());
    assertEquals(0, subscriber2.buffers.size());
    assertEquals(0, subscriber3.buffers.size());

    // We go at the speed of the slowest consumer
    subscriber1.subscription.request(1);
    assertEquals(0, subscriber1.buffers.size());
    assertEquals(0, subscriber2.buffers.size());
    assertEquals(0, subscriber3.buffers.size());

    subscriber2.subscription.request(1);
    assertEquals(0, subscriber1.buffers.size());
    assertEquals(0, subscriber2.buffers.size());
    assertEquals(0, subscriber3.buffers.size());

    subscriber3.subscription.request(1);
    assertWaitUntil(() -> subscriber1.buffers.size() == 1);
    assertWaitUntil(() -> subscriber2.buffers.size() == 1);
    assertWaitUntil(() -> subscriber3.buffers.size() == 1);
    assertEquals(1, subscriber1.buffers.size());
    assertEquals(1, subscriber2.buffers.size());
    assertEquals(1, subscriber3.buffers.size());
    assertEquals(buffers.get(0), subscriber1.buffers.get(0));
    assertEquals(buffers.get(0), subscriber2.buffers.get(0));
    assertEquals(buffers.get(0), subscriber3.buffers.get(0));

    subscriber1.subscription.request(4);
    assertEquals(1, subscriber1.buffers.size());
    assertEquals(1, subscriber2.buffers.size());
    assertEquals(1, subscriber3.buffers.size());
    subscriber2.subscription.request(3);
    assertEquals(1, subscriber1.buffers.size());
    assertEquals(1, subscriber2.buffers.size());
    assertEquals(1, subscriber3.buffers.size());
    subscriber3.subscription.request(2);
    assertWaitUntil(() -> subscriber1.buffers.size() == 3);
    assertWaitUntil(() -> subscriber2.buffers.size() == 3);
    assertWaitUntil(() -> subscriber3.buffers.size() == 3);
    assertEquals(3, subscriber1.buffers.size());
    assertEquals(3, subscriber2.buffers.size());
    assertEquals(3, subscriber3.buffers.size());
    assertEquals(buffers.get(0), subscriber1.buffers.get(0));
    assertEquals(buffers.get(1), subscriber1.buffers.get(1));
    assertEquals(buffers.get(2), subscriber1.buffers.get(2));
    assertEquals(buffers.get(0), subscriber2.buffers.get(0));
    assertEquals(buffers.get(1), subscriber2.buffers.get(1));
    assertEquals(buffers.get(2), subscriber2.buffers.get(2));
    assertEquals(buffers.get(0), subscriber3.buffers.get(0));
    assertEquals(buffers.get(1), subscriber3.buffers.get(1));
    assertEquals(buffers.get(2), subscriber3.buffers.get(2));

    subscriber2.subscription.request(1);
    assertEquals(3, subscriber1.buffers.size());
    assertEquals(3, subscriber2.buffers.size());
    assertEquals(3, subscriber3.buffers.size());
    subscriber3.subscription.request(2);
    assertWaitUntil(() -> subscriber1.buffers.size() == 5);
    assertWaitUntil(() -> subscriber2.buffers.size() == 5);
    assertWaitUntil(() -> subscriber3.buffers.size() == 5);
    assertEquals(5, subscriber1.buffers.size());
    assertEquals(5, subscriber2.buffers.size());
    assertEquals(5, subscriber3.buffers.size());
    assertEquals(buffers.get(0), subscriber1.buffers.get(0));
    assertEquals(buffers.get(1), subscriber1.buffers.get(1));
    assertEquals(buffers.get(2), subscriber1.buffers.get(2));
    assertEquals(buffers.get(3), subscriber1.buffers.get(3));
    assertEquals(buffers.get(4), subscriber1.buffers.get(4));
    assertEquals(buffers.get(0), subscriber2.buffers.get(0));
    assertEquals(buffers.get(1), subscriber2.buffers.get(1));
    assertEquals(buffers.get(2), subscriber2.buffers.get(2));
    assertEquals(buffers.get(3), subscriber2.buffers.get(3));
    assertEquals(buffers.get(4), subscriber2.buffers.get(4));
    assertEquals(buffers.get(0), subscriber3.buffers.get(0));
    assertEquals(buffers.get(1), subscriber3.buffers.get(1));
    assertEquals(buffers.get(2), subscriber3.buffers.get(2));
    assertEquals(buffers.get(3), subscriber3.buffers.get(3));
    assertEquals(buffers.get(4), subscriber3.buffers.get(4));
  }

  @Test
  public void testWriteQueueFullAndDrainDefaultQueueSize() throws Exception {
    ReactiveWriteStream<Buffer> rws = ReactiveWriteStream.writeStream(vertx);
    testWriteQueueFullAndDrain(rws, 10);
  }

  private void testWriteQueueFullAndDrain(ReactiveWriteStream<Buffer> rws, int writeQueueMaxSize) throws Exception {
    rws.setWriteQueueMaxSize(writeQueueMaxSize);
    MySubscriber subscriber = new MySubscriber();
    rws.subscribe(subscriber);
    for (int i = 0; i < writeQueueMaxSize - 1; i++) {
      rws.write(TestUtils.randomBuffer(50));
    }
    assertFalse(rws.writeQueueFull());
    Buffer buff2 = TestUtils.randomBuffer(100);
    rws.write(buff2);
    assertTrue(rws.writeQueueFull());
    rws.drainHandler(v -> {
      assertFalse(rws.writeQueueFull());
      testComplete();
    });
    assertWaitUntil(() -> subscriber.subscription != null);
    subscriber.subscription.request(2);
    await();
  }


  class MySubscriber implements Subscriber<Buffer> {

    final List<Buffer> buffers = new CopyOnWriteArrayList<>();
    volatile Subscription subscription;

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
    }

    @Override
    public void onNext(Buffer buffer) {
      buffers.add(buffer);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
  }

  @Test
  public void testCancelSubscriptionOnError1() {
    ReactiveWriteStream<Buffer> rws = ReactiveWriteStream.writeStream(vertx);
    AtomicBoolean failed = new AtomicBoolean();
    MySubscriber subscriber1 = new MySubscriber() {
      @Override
      public void onSubscribe(Subscription subscription) {
        super.onSubscribe(subscription);
        throw new RuntimeException();
      }
      @Override
      public void onNext(Buffer buffer) {
        fail();
      }
      @Override
      public void onError(Throwable throwable) {
        failed.set(true);
      }
    };
    rws.subscribe(subscriber1);
    MySubscriber subscriber2 = new MySubscriber() {
      @Override
      public void onSubscribe(Subscription subscription) {
        subscription.request(1);
        super.onSubscribe(subscription);
      }
      int count = 0;
      @Override
      public void onNext(Buffer buffer) {
        if (++count == 1) {
          testComplete();
        }
      }
    };
    rws.subscribe(subscriber2);
    assertWaitUntil(() -> subscriber1.subscription != null);
    assertWaitUntil(() -> subscriber2.subscription != null);
    assertWaitUntil(failed::get);
    rws.write(createRandomBuffers(1).get(0));
    await();
  }

  @Test
  public void testCancelSubscriptionOnError2() {
    ReactiveWriteStream<Buffer> rws = ReactiveWriteStream.writeStream(vertx);
    AtomicBoolean failed = new AtomicBoolean();
    MySubscriber subscriber1 = new MySubscriber() {
      @Override
      public void onSubscribe(Subscription subscription) {
        subscription.request(2);
        super.onSubscribe(subscription);
      }
      @Override
      public void onNext(Buffer buffer) {
        if (!failed.get()) {
          throw new RuntimeException();
        } else {
          fail();
        }
      }
      @Override
      public void onError(Throwable throwable) {
        failed.set(true);
      }
    };
    rws.subscribe(subscriber1);
    MySubscriber subscriber2 = new MySubscriber() {
      @Override
      public void onSubscribe(Subscription subscription) {
        subscription.request(3);
        super.onSubscribe(subscription);
      }
      int count = 0;
      @Override
      public void onNext(Buffer buffer) {
        if (++count == 3) {
          testComplete();
        }
      }
    };
    rws.subscribe(subscriber2);
    assertWaitUntil(() -> subscriber1.subscription != null);
    assertWaitUntil(() -> subscriber2.subscription != null);
    rws.write(createRandomBuffers(1).get(0));
    assertWaitUntil(failed::get);
    rws.write(createRandomBuffers(1).get(0));
    rws.write(createRandomBuffers(1).get(0));
    await();
  }

  @Test
  public void testWriteHandler() {
    ReactiveWriteStream<Buffer> rws = ReactiveWriteStream.writeStream(vertx);
    Promise<Void> f1 = Promise.promise();
    rws.write(createRandomBuffers(1).get(0)).onComplete(f1);
    assertFalse(f1.future().isComplete());
    rws.subscribe(new MySubscriber() {
      @Override
      public void onSubscribe(Subscription subscription) {
        subscription.request(1);
      }
    });
    waitUntil(f1.future()::succeeded);
  }

  @Test
  public void testWriteHandlerFailure() {
    ReactiveWriteStream<Buffer> rws = ReactiveWriteStream.writeStream(vertx);
    Promise<Void> f1 = Promise.promise();
    rws.write(createRandomBuffers(1).get(0)).onComplete(f1);
    assertFalse(f1.future().isComplete());
    Promise<Void> f2 = Promise.promise();
    rws.end().onComplete(f2);
    waitUntil(f1.future()::failed);
    waitUntil(f2.future()::succeeded);
  }
}
