package com.smartstudy.service;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Cancels oversized AI bodies before buffering them into application memory. */
final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
    private final CompletableFuture<byte[]> body = new CompletableFuture<>();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private Flow.Subscription subscription;

    @Override
    public CompletionStage<byte[]> getBody() {
        return body;
    }

    @Override
    public void onSubscribe(Flow.Subscription s) {
        subscription = s;
        s.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        for (ByteBuffer b : buffers) {
            if (bytes.size() + b.remaining() > 65536) {
                subscription.cancel();
                body.completeExceptionally(
                        new IllegalArgumentException("Phản hồi AI vượt quá 64 KB."));
                return;
            }
            byte[] part = new byte[b.remaining()];
            b.get(part);
            bytes.writeBytes(part);
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable error) {
        body.completeExceptionally(error);
    }

    @Override
    public void onComplete() {
        body.complete(bytes.toByteArray());
    }
}
