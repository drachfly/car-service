/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.car.audio;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.Objects;

final class ContentObserverFactory {

    private final Uri mUri;

    ContentObserverFactory(Uri uri) {
        mUri = Objects.requireNonNull(uri, "Uri cannot be null");
    }

    ContentObserver createObserver(ContentChangeCallback wrapper) {
        Objects.requireNonNull(wrapper, "Content Change Callback cannot be null");

        return new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (mUri.equals(uri)) {
                    wrapper.onChange();
                }
            }
        };
    }

    interface ContentChangeCallback {
        void onChange();
    }
}
