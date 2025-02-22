/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.car.test.mocks;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides common Mockito calls for core Java classes.
 */
public final class JavaMockitoHelper {

    static final long ASYNC_TIMEOUT_MS = 500;

    private static final String TAG = JavaMockitoHelper.class.getSimpleName();

    /**
     * Waits for a latch to be counted down.
     *
     * @param timeoutMs how long to wait for
     *
     * @throws IllegalStateException if it times out.
     */
    public static void await(@NonNull CountDownLatch latch, long timeoutMs)
            throws InterruptedException {
        Log.v(TAG, "waiting " + timeoutMs + "ms for latch " + latch);
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            Log.e(TAG, "latch timed out");
            throw new IllegalStateException(latch + " not called in " + timeoutMs + " ms");
        }
    }

    /**
     * Waits for a semaphore.
     *
     * @param timeoutMs how long to wait for
     *
     * @throws IllegalStateException if it times out.
     */
    public static void await(@NonNull Semaphore semaphore, long timeoutMs)
            throws InterruptedException {
        Log.v(TAG, "waiting " + timeoutMs + "ms for semaphore " + semaphore);
        if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
            Log.e(TAG, "semaphore timed out");
            throw new IllegalStateException(semaphore + " not released in " + timeoutMs + " ms");
        }
    }

    /**
     * Silently waits for a latch to be counted down, without throwing any exception if it isn't.
     *
     * @param timeoutMs how long to wait for
     *
     * @return whether the latch was counted down.
     */
    public static boolean silentAwait(@NonNull CountDownLatch latch, long timeoutMs) {
        boolean called;
        try {
            Log.v(TAG, "waiting " + timeoutMs + "ms for semaphore " + latch);
            called = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!called) {
                Log.w(TAG, latch + " not called in " + timeoutMs + " ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, latch + " interrupted", e);
            return false;
        }
        return called;
    }

    /**
     * Gets the result of a future, or throw a {@link IllegalStateException} if it times out after
     * {@value #ASYNC_TIMEOUT_MS} ms.
     */
    @NonNull
    public static <T> T getResult(@NonNull Future<T> future,
            @NonNull String messageFormat, @Nullable Object...messageArgs) {
        return getResult(future, ASYNC_TIMEOUT_MS, messageFormat, messageArgs);
    }

    /**
     * Gets the result of a future, or throw a {@link IllegalStateException} if it times out.
     */
    @NonNull
    public static <T> T getResult(@NonNull Future<T> future, long timeoutMs,
            @NonNull String messageFormat, @Nullable Object...messageArgs) {
        String msg = String.format(Objects.requireNonNull(messageFormat, "messageFormat"),
                messageArgs);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("future for '" + msg + "' interrupted", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("future for '" + msg + "' not called in "
                    + timeoutMs + "ms", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("failed to get future for '" + msg + "'", e);
        }
    }

    private JavaMockitoHelper() {
        throw new UnsupportedOperationException("contains only static methods");
    }
}
