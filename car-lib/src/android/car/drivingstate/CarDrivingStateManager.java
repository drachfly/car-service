/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.car.drivingstate;

import static android.car.Car.PERMISSION_CONTROL_APP_BLOCKING;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.car.Car;
import android.car.CarManagerBase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;

import java.lang.ref.WeakReference;

/**
 * API to register and get driving state related information in a car.
 *
 * @hide
 */
@SystemApi
public final class CarDrivingStateManager extends CarManagerBase {
    private static final String TAG = "CarDrivingStateMgr";
    private static final boolean VDBG = false;
    private static final int MSG_HANDLE_DRIVING_STATE_CHANGE = 0;

    private final ICarDrivingState mDrivingService;
    private final EventCallbackHandler mEventCallbackHandler;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private CarDrivingStateEventListener mDrvStateEventListener;

    @GuardedBy("mLock")
    private CarDrivingStateChangeListenerToService mListenerToService;

    /** @hide */
    public CarDrivingStateManager(Car car, IBinder service) {
        super(car);
        mDrivingService = ICarDrivingState.Stub.asInterface(service);
        mEventCallbackHandler = new EventCallbackHandler(this, getEventHandler().getLooper());
    }

    /** @hide */
    @Override
    public void onCarDisconnected() {
        synchronized (mLock) {
            mListenerToService = null;
            mDrvStateEventListener = null;
        }
    }

    /**
     * Listener Interface for clients to implement to get updated on driving state changes.
     *
     * @hide
     */
    @SystemApi
    public interface CarDrivingStateEventListener {
        /**
         * Called when the car's driving state changes.
         *
         * @param event Car's driving state.
         */
        void onDrivingStateChanged(CarDrivingStateEvent event);
    }

    /**
     * Register a {@link CarDrivingStateEventListener} to listen for driving state changes.
     *
     * @param listener {@link CarDrivingStateEventListener}
     * @hide
     */
    @SystemApi
    public void registerListener(@NonNull CarDrivingStateEventListener listener) {
        if (listener == null) {
            if (VDBG) {
                Slog.v(TAG, "registerCarDrivingStateEventListener(): null listener");
            }
            throw new IllegalArgumentException("Listener is null");
        }
        CarDrivingStateChangeListenerToService localListenerToService;
        synchronized (mLock) {
            // Check if the listener has been already registered for this event type
            if (mDrvStateEventListener != null) {
                Slog.w(TAG, "Listener already registered");
                return;
            }
            if (mListenerToService == null) {
                mListenerToService = new CarDrivingStateChangeListenerToService(this);
            }
            localListenerToService = mListenerToService;
            mDrvStateEventListener = listener;
        }
        try {
            // register to the Service for getting notified
            mDrivingService.registerDrivingStateChangeListener(localListenerToService);
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
    }

    /**
     * Unregister the registered {@link CarDrivingStateEventListener} for the given driving event
     * type.
     *
     * @hide
     */
    @SystemApi
    public void unregisterListener() {
        CarDrivingStateChangeListenerToService localListenerToService;
        synchronized (mLock) {
            if (mDrvStateEventListener == null) {
                Slog.w(TAG, "Listener was not previously registered");
                return;
            }
            localListenerToService = mListenerToService;
            mDrvStateEventListener = null;
            mListenerToService = null;
        }
        try {
            mDrivingService.unregisterDrivingStateChangeListener(localListenerToService);
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
    }

    /**
     * Get the current value of the car's driving state.
     *
     * @return {@link CarDrivingStateEvent} corresponding to the given eventType
     * @hide
     */
    @Nullable
    @SystemApi
    public CarDrivingStateEvent getCurrentCarDrivingState() {
        try {
            return mDrivingService.getCurrentDrivingState();
        } catch (RemoteException e) {
            return handleRemoteExceptionFromCarService(e, null);
        }
    }

    /**
     * Notify registered driving state change listener about injected event.
     * Requires Permission: {@link Car#PERMISSION_CONTROL_APP_BLOCKING}
     *
     * @param drivingState Value in {@link CarDrivingStateEvent.CarDrivingState}.
     * @hide
     */
    @TestApi
    @RequiresPermission(PERMISSION_CONTROL_APP_BLOCKING)
    public void injectDrivingState(int drivingState) {
        CarDrivingStateEvent event = new CarDrivingStateEvent(
                drivingState, SystemClock.elapsedRealtimeNanos());
        try {
            mDrivingService.injectDrivingState(event);
        } catch (RemoteException e) {
            handleRemoteExceptionFromCarService(e);
        }
    }

    /**
     * Class that implements the listener interface and gets called back from the
     * {@link com.android.car.CarDrivingStateService} across the binder interface.
     */
    private static class CarDrivingStateChangeListenerToService extends
            ICarDrivingStateChangeListener.Stub {
        private final WeakReference<CarDrivingStateManager> mDrvStateMgr;

        CarDrivingStateChangeListenerToService(CarDrivingStateManager manager) {
            mDrvStateMgr = new WeakReference<>(manager);
        }

        @Override
        public void onDrivingStateChanged(CarDrivingStateEvent event) {
            CarDrivingStateManager manager = mDrvStateMgr.get();
            if (manager != null) {
                manager.handleDrivingStateChanged(event);
            }
        }
    }

    /**
     * Gets the {@link CarDrivingStateEvent} from the service listener
     * {@link CarDrivingStateChangeListenerToService} and dispatches it to a handler provided
     * to the manager
     *
     * @param event {@link CarDrivingStateEvent} that has been registered to listen on
     */
    private void handleDrivingStateChanged(CarDrivingStateEvent event) {
        // send a message to the handler
        mEventCallbackHandler.sendMessage(
                mEventCallbackHandler.obtainMessage(MSG_HANDLE_DRIVING_STATE_CHANGE, event));

    }

    /**
     * Callback Handler to handle dispatching the driving state changes to the corresponding
     * listeners
     */
    private static final class EventCallbackHandler extends Handler {
        private final WeakReference<CarDrivingStateManager> mDrvStateMgr;

        EventCallbackHandler(CarDrivingStateManager manager, Looper looper) {
            super(looper);
            mDrvStateMgr = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            CarDrivingStateManager mgr = mDrvStateMgr.get();
            if (mgr != null) {
                mgr.dispatchDrivingStateChangeToClient((CarDrivingStateEvent) msg.obj);
            }
        }

    }

    /**
     * Checks for the listener to {@link CarDrivingStateEvent} and calls it back
     * in the callback handler thread
     *
     * @param event {@link CarDrivingStateEvent}
     */
    private void dispatchDrivingStateChangeToClient(CarDrivingStateEvent event) {
        if (event == null) {
            return;
        }
        CarDrivingStateEventListener listener;
        synchronized (mLock) {
            listener = mDrvStateEventListener;
        }
        if (listener != null) {
            listener.onDrivingStateChanged(event);
        }
    }

}
