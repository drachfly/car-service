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

package com.android.car;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.car.Car;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.cabin.CarCabinManager;
import android.car.hardware.cabin.CarCabinManager.CarCabinEventCallback;
import android.car.hardware.cabin.CarCabinManager.PropertyId;
import android.hardware.automotive.vehicle.VehicleAreaDoor;
import android.hardware.automotive.vehicle.VehicleAreaWindow;
import android.hardware.automotive.vehicle.VehiclePropValue;
import android.hardware.automotive.vehicle.VehicleProperty;
import android.os.SystemClock;
import android.util.Log;
import android.util.MutableInt;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.android.car.hal.test.AidlMockedVehicleHal.VehicleHalPropertyHandler;
import com.android.car.hal.test.AidlVehiclePropValueBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class CarCabinManagerTest extends MockedCarTestBase {
    private static final String TAG = CarCabinManagerTest.class.getSimpleName();

    // Use this semaphore to block until the callback is heard of.
    private Semaphore mAvailable;

    private CarCabinManager mCarCabinManager;
    private boolean mEventBoolVal;
    private int mEventIntVal;
    private int mEventZoneVal;

    @Override
    protected void configureMockedHal() {
        CabinPropertyHandler handler = new CabinPropertyHandler();
        addAidlProperty(VehicleProperty.DOOR_LOCK, handler)
                .addAreaConfig(VehicleAreaDoor.ROW_1_LEFT, 0, 0);
        addAidlProperty(VehicleProperty.WINDOW_POS, handler)
                .addAreaConfig(VehicleAreaWindow.ROW_1_LEFT, 0, 0);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAvailable = new Semaphore(0);
        mCarCabinManager = (CarCabinManager) getCar().getCarManager(Car.CABIN_SERVICE);
    }

    // Test a boolean property
    @Test
    public void testCabinDoorLockOn() throws Exception {
        mCarCabinManager.setBooleanProperty(CarCabinManager.ID_DOOR_LOCK,
                VehicleAreaDoor.ROW_1_LEFT, true);
        boolean lock = mCarCabinManager.getBooleanProperty(CarCabinManager.ID_DOOR_LOCK,
                VehicleAreaDoor.ROW_1_LEFT);
        assertTrue(lock);

        mCarCabinManager.setBooleanProperty(CarCabinManager.ID_DOOR_LOCK,
                VehicleAreaDoor.ROW_1_LEFT, false);
        lock = mCarCabinManager.getBooleanProperty(CarCabinManager.ID_DOOR_LOCK,
                VehicleAreaDoor.ROW_1_LEFT);
        assertFalse(lock);
    }

    // Test an integer property
    @Test
    public void testCabinWindowPos() throws Exception {
        mCarCabinManager.setIntProperty(CarCabinManager.ID_WINDOW_POS,
                VehicleAreaWindow.ROW_1_LEFT, 50);
        int windowPos = mCarCabinManager.getIntProperty(CarCabinManager.ID_WINDOW_POS,
                VehicleAreaWindow.ROW_1_LEFT);
        assertEquals(50, windowPos);

        mCarCabinManager.setIntProperty(CarCabinManager.ID_WINDOW_POS,
                VehicleAreaWindow.ROW_1_LEFT, 25);
        windowPos = mCarCabinManager.getIntProperty(CarCabinManager.ID_WINDOW_POS,
                VehicleAreaWindow.ROW_1_LEFT);
        assertEquals(25, windowPos);
    }

    @Test
    public void testError() throws Exception {
        final int propId = VehicleProperty.DOOR_LOCK;
        final int areaId = VehicleAreaDoor.ROW_1_LEFT;
        final int errorCode = 42;

        CountDownLatch errorLatch = new CountDownLatch(1);
        MutableInt propertyIdReceived = new MutableInt(0);
        MutableInt areaIdReceived = new MutableInt(0);

        mCarCabinManager.registerCallback(new CarCabinEventCallback() {
            @Override
            public void onChangeEvent(CarPropertyValue value) {

            }

            @Override
            public void onErrorEvent(@PropertyId int propertyId, int area) {
                propertyIdReceived.value = propertyId;
                areaIdReceived.value = area;
                errorLatch.countDown();
            }
        });
        mCarCabinManager.setBooleanProperty(propId, areaId, true);
        getAidlMockedVehicleHal().injectError(errorCode, propId, areaId);
        assertTrue(errorLatch.await(DEFAULT_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(propId, propertyIdReceived.value);
        assertEquals(areaId, areaIdReceived.value);
    }


    // Test an event
    @Test
    public void testEvent() throws Exception {
        mCarCabinManager.registerCallback(new EventListener());
        // Wait for two events generated on registration
        assertTrue(mAvailable.tryAcquire(2L, TimeUnit.SECONDS));
        assertTrue(mAvailable.tryAcquire(2L, TimeUnit.SECONDS));
        // Inject a boolean event and wait for its callback in onPropertySet.
        VehiclePropValue v = AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.DOOR_LOCK)
                .setAreaId(VehicleAreaDoor.ROW_1_LEFT)
                .setTimestamp(SystemClock.elapsedRealtimeNanos())
                .addIntValues(1)
                .build();

        assertEquals(0, mAvailable.availablePermits());
        getAidlMockedVehicleHal().injectEvent(v);

        assertTrue(mAvailable.tryAcquire(2L, TimeUnit.SECONDS));
        assertTrue(mEventBoolVal);
        assertEquals(VehicleAreaDoor.ROW_1_LEFT, mEventZoneVal);

        // Inject an integer event and wait for its callback in onPropertySet.
        v = AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.WINDOW_POS)
                .setAreaId(VehicleAreaWindow.ROW_1_LEFT)
                .setTimestamp(SystemClock.elapsedRealtimeNanos())
                .addIntValues(75)
                .build();
        assertEquals(0, mAvailable.availablePermits());
        getAidlMockedVehicleHal().injectEvent(v);

        assertTrue(mAvailable.tryAcquire(2L, TimeUnit.SECONDS));
        assertEquals(mEventIntVal, 75);
        assertEquals(VehicleAreaWindow.ROW_1_LEFT, mEventZoneVal);
    }


    private static final class CabinPropertyHandler implements VehicleHalPropertyHandler {
        HashMap<Integer, VehiclePropValue> mMap = new HashMap<>();

        @Override
        public synchronized void onPropertySet(VehiclePropValue value) {
            mMap.put(value.prop, value);
        }

        @Override
        public synchronized VehiclePropValue onPropertyGet(VehiclePropValue value) {
            Log.d(TAG, "onPropertyGet property " + value.prop);
            if (mMap.get(value.prop) == null) {
                return AidlVehiclePropValueBuilder.newBuilder(value.prop)
                            .setAreaId(value.areaId)
                            .setTimestamp(SystemClock.elapsedRealtimeNanos())
                            .addIntValues(1)
                            .build();
            } else {
                return mMap.get(value.prop);
            }
        }

        @Override
        public synchronized void onPropertySubscribe(int property, float sampleRate) {
            Log.d(TAG, "onPropertySubscribe property " + property + " sampleRate " + sampleRate);
        }

        @Override
        public synchronized void onPropertyUnsubscribe(int property) {
            Log.d(TAG, "onPropertyUnSubscribe property " + property);
        }
    }

    private class EventListener implements CarCabinEventCallback {
        EventListener() { }

        @Override
        public void onChangeEvent(final CarPropertyValue value) {
            Log.d(TAG, "onChangeEvent: "  + value);
            Object o = value.getValue();
            mEventZoneVal = value.getAreaId();

            if (o instanceof Integer) {
                mEventIntVal = (Integer) o;
            } else if (o instanceof Boolean) {
                mEventBoolVal = (Boolean) o;
            } else {
                Log.e(TAG, "onChangeEvent:  Unknown instance type = " + o.getClass().getName());
            }
            mAvailable.release();
        }

        @Override
        public void onErrorEvent(final int propertyId, final int zone) {
            Log.d(TAG, "Error:  propertyId=" + propertyId + "  zone=" + zone);
        }
    }
}
