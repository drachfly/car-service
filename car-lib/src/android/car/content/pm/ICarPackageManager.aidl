/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.car.content.pm;

import android.app.PendingIntent;
import android.car.CarVersion;
import android.car.content.pm.CarAppBlockingPolicy;
import android.car.content.pm.ICarBlockingUiCommandListener;
import android.content.ComponentName;

/** @hide */
interface ICarPackageManager {
    void setAppBlockingPolicy(in String packageName, in CarAppBlockingPolicy policy, int flags) = 0;
    boolean isActivityDistractionOptimized(in String packageName, in String className) = 1;
    boolean isServiceDistractionOptimized(in String packageName, in String className) = 2;
    boolean isActivityBackedBySafeActivity(in ComponentName activityName) = 3;
    void setEnableActivityBlocking(boolean enable) = 4;
    void restartTask(int taskId) = 5;
    boolean isPendingIntentDistractionOptimized(in PendingIntent pendingIntent) = 6;
    String getCurrentDrivingSafetyRegion() = 7;
    void controlOneTimeActivityBlockingBypassingAsUser(String packageName, String activityClassName,
            boolean bypass, int userId) = 8;
    List<String> getSupportedDrivingSafetyRegionsForActivityAsUser(String packageName,
            String activityClassName, int userId) = 9;
    CarVersion getTargetCarVersion(String packageName) = 10;
    CarVersion getSelfTargetCarVersion(in String packageName) = 11;
    boolean requiresDisplayCompat(in String packageName) = 12;
    void registerBlockingUiCommandListener(in ICarBlockingUiCommandListener listener, int displayId) = 13;
    void unregisterBlockingUiCommandListener(in ICarBlockingUiCommandListener listener) = 14;
}
