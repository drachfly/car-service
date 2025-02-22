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

import android.annotation.SystemApi;
import android.app.Service;
import android.car.Car;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;

/**
 * Service to be implemented by Service which wants to control app blocking policy.
 * App should require android.car.permission.CONTROL_APP_BLOCKING to launch Service
 * implementation. Additionally the APK should have the permission to be launched by Car Service.
 * The implementing service should declare {@link #SERVICE_INTERFACE} in its intent filter as
 * action.
 * @hide
 */
@SystemApi
public abstract class CarAppBlockingPolicyService extends Service {

    private static final String TAG = CarAppBlockingPolicyService.class.getSimpleName();

    public static final String SERVICE_INTERFACE =
            "android.car.content.pm.CarAppBlockingPolicyService";

    private final ICarAppBlockingPolicyImpl mBinder = new ICarAppBlockingPolicyImpl();

    /**
     * Return the app blocking policy. This is called from binder thread.
     * @return
     */
    protected abstract CarAppBlockingPolicy getAppBlockingPolicy();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Slog.i(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Slog.i(TAG, "onUnbind");
        stopSelf();
        return false;
    }


    private class ICarAppBlockingPolicyImpl extends ICarAppBlockingPolicy.Stub {

        @Override
        public void setAppBlockingPolicySetter(ICarAppBlockingPolicySetter setter) {
            Slog.i(TAG, "setAppBlockingPolicySetter will set policy");
            CarAppBlockingPolicy policy = CarAppBlockingPolicyService.this.getAppBlockingPolicy();
            try {
                setter.setAppBlockingPolicy(policy);
            } catch (RemoteException e) {
                Car.handleRemoteExceptionFromCarService(CarAppBlockingPolicyService.this, e);
            }
        }
    }
}
