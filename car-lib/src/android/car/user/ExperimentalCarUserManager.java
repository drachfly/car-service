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

package android.car.user;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.UserIdInt;
import android.car.Car;
import android.car.CarManagerBase;
import android.car.IExperimentalCarUserService;
import android.car.annotation.ExperimentalFeature;
import android.car.builtin.os.UserManagerHelper;
import android.car.util.concurrent.AndroidFuture;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Temporary class containing {@link CarUserManager} features that are not ready yet.
 *
 * <p>New instances are created through {@link #from(CarUserManager)}.
 *
 * @hide
 * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
 * supported. It will be marked {@code @removed} in the next major release and hard removed in the
 * release after that.
 */
@ExperimentalFeature
@Deprecated
public final class ExperimentalCarUserManager extends CarManagerBase {

    /** @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported.
     */
    @Deprecated
    public static final String TAG = ExperimentalCarUserManager.class.getSimpleName();

    /**
     *  User id representing invalid user.
     */
    private static final int INVALID_USER_ID = UserManagerHelper.USER_NULL;

    private final IExperimentalCarUserService mService;

    /**
     * @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported. It will be marked {@code @removed} in the next major release and hard removed in
     * the release after that.
     */
    @Deprecated
    public ExperimentalCarUserManager(@NonNull Car car, @NonNull IBinder service) {
        this(car, IExperimentalCarUserService.Stub.asInterface(service));
    }

    @VisibleForTesting
    public ExperimentalCarUserManager(
            @NonNull Car car, @NonNull IExperimentalCarUserService service) {
        super(car);

        mService = service;
    }

    /**
     * Creates a driver who is a regular user and is allowed to login to the driving occupant zone.
     *
     * @param name The name of the driver to be created.
     * @param admin Whether the created driver will be an admin.
     * @return an {@link AndroidFuture} that can be used to track operation's completion and
     *         retrieve its result (if any).
     *
     * @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.MANAGE_USERS)
    public AndroidFuture<UserCreationResult> createDriver(@NonNull String name, boolean admin) {
        try {
            return mService.createDriver(name, admin);
        } catch (RemoteException e) {
            AndroidFuture<UserCreationResult> future = new AndroidFuture<>();
            future.complete(new UserCreationResult(UserCreationResult.STATUS_HAL_INTERNAL_FAILURE));
            handleRemoteExceptionFromCarService(e);
            return future;
        }
    }

    /**
     * Creates a passenger who is a profile of the given driver.
     *
     * @param name The name of the passenger to be created.
     * @param driverId User id of the driver under whom a passenger is created.
     * @return user id of the created passenger, or {@code INVALID_USER_ID} if the passenger
     *         could not be created.
     *
     * @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.MANAGE_USERS)
    @Nullable
    public int createPassenger(@NonNull String name, @UserIdInt int driverId) {
        try {
            UserHandle ui = mService.createPassenger(name, driverId);
            return ui != null ? ui.getIdentifier() : INVALID_USER_ID;
        } catch (RemoteException e) {
            return handleRemoteExceptionFromCarService(e, null);
        }
    }

    /**
     * Switches a driver to the given user.
     *
     * @param driverId User id of the driver to switch to.
     * @return an {@link AndroidFuture} that can be used to track operation's completion and
     *         retrieve its result (if any).
     *
     * @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.MANAGE_USERS)
    public AndroidFuture<UserSwitchResult> switchDriver(@UserIdInt int driverId) {
        try {
            AndroidFuture<UserSwitchResult> future = new AndroidFuture<>() {
                @Override
                protected void onCompleted(UserSwitchResult result, Throwable err) {
                    if (result == null) {
                        Slog.w(TAG, "switchDriver(" + driverId + ") failed: " + err);
                    }
                    super.onCompleted(result, err);
                }
            };
            mService.switchDriver(driverId, future);
            return future;
        } catch (RemoteException e) {
            AndroidFuture<UserSwitchResult> future = new AndroidFuture<>();
            future.complete(
                    new UserSwitchResult(UserSwitchResult.STATUS_HAL_INTERNAL_FAILURE, null));
            handleRemoteExceptionFromCarService(e);
            return future;
        }
    }

    /**
     * Returns all drivers who can occupy the driving zone. Guest users are included in the list.
     *
     * @return the list of user ids who can be a driver on the device.
     *
     * @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.MANAGE_USERS)
    @NonNull
    public List<Integer> getAllDrivers() {
        try {
            return getUserIdsFromUserHandles(mService.getAllDrivers());
        } catch (RemoteException e) {
            return handleRemoteExceptionFromCarService(e, Collections.emptyList());
        }
    }

    /**
     * Returns all passengers under the given driver.
     *
     * @param driverId User id of a driver.
     * @return the list of user ids who are passengers under the given driver.
     *
     * @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.MANAGE_USERS)
    @NonNull
    public List<Integer> getPassengers(@UserIdInt int driverId) {
        try {
            return getUserIdsFromUserHandles(mService.getPassengers(driverId));
        } catch (RemoteException e) {
            return handleRemoteExceptionFromCarService(e, Collections.emptyList());
        }
    }

    /**
     * Assigns the passenger to the zone and starts the user if it is not started yet.
     *
     * @param passengerId User id of the passenger to be started.
     * @param zoneId Zone id to which the passenger is assigned.
     * @return {@code true} if the user is successfully started or the user is already running.
     *         Otherwise, {@code false}.
     *
     * @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.MANAGE_USERS)
    public boolean startPassenger(@UserIdInt int passengerId, int zoneId) {
        try {
            return mService.startPassenger(passengerId, zoneId);
        } catch (RemoteException e) {
            return handleRemoteExceptionFromCarService(e, false);
        }
    }

    /**
     * Stops the given passenger.
     *
     * @param passengerId User id of the passenger to be stopped.
     * @return {@code true} if successfully stopped, or {@code false} if failed.
     *
     * @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.MANAGE_USERS)
    public boolean stopPassenger(@UserIdInt int passengerId) {
        try {
            return mService.stopPassenger(passengerId);
        } catch (RemoteException e) {
            return handleRemoteExceptionFromCarService(e, false);
        }
    }

    /** @hide
     * @deprecated {@link ExperimentalCarUserManager} was an experimental feature and is no longer
     * supported.
     */
    @Deprecated
    @Override
    public void onCarDisconnected() {
        // nothing to do
    }

    private List<Integer> getUserIdsFromUserHandles(List<UserHandle> infos) {
        List<Integer> ids = new ArrayList<>(infos.size());
        for (UserHandle ui : infos) {
            ids.add(ui.getIdentifier());
        }
        return ids;
    }
}
