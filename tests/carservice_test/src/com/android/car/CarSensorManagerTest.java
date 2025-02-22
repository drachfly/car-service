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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.car.Car;
import android.car.hardware.CarSensorEvent;
import android.car.hardware.CarSensorManager;
import android.hardware.automotive.vehicle.VehicleGear;
import android.hardware.automotive.vehicle.VehicleIgnitionState;
import android.hardware.automotive.vehicle.VehiclePropValue;
import android.hardware.automotive.vehicle.VehicleProperty;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.android.car.hal.test.AidlVehiclePropValueBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the public entry points for the CarSensorManager
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class CarSensorManagerTest extends MockedCarTestBase {
    private static final String TAG = CarSensorManagerTest.class.getSimpleName();

    private CarSensorManager mCarSensorManager;

    @Override
    protected void configureMockedHal() {
        addAidlProperty(VehicleProperty.NIGHT_MODE,
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.NIGHT_MODE)
                        .addIntValues(0)
                        .build());
        addAidlProperty(VehicleProperty.PERF_VEHICLE_SPEED,
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.PERF_VEHICLE_SPEED)
                        .addFloatValues(0f)
                        .build());
        addAidlProperty(VehicleProperty.FUEL_LEVEL,
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.FUEL_LEVEL)
                        .addFloatValues(20000)  // ml
                        .build());
        addAidlProperty(VehicleProperty.PARKING_BRAKE_ON,
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.PARKING_BRAKE_ON)
                        .setBooleanValue(true)
                        .build());
        addAidlProperty(VehicleProperty.CURRENT_GEAR,
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.CURRENT_GEAR)
                        .addIntValues(0)
                        .build());
        addAidlProperty(VehicleProperty.GEAR_SELECTION,
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.GEAR_SELECTION)
                        .addIntValues(0)
                        .build());
        addAidlProperty(VehicleProperty.IGNITION_STATE,
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.IGNITION_STATE)
                        .addIntValues(CarSensorEvent.IGNITION_STATE_ACC)
                        .build());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Start the HAL layer and set up the sensor manager service
        mCarSensorManager = (CarSensorManager) getCar().getCarManager(Car.SENSOR_SERVICE);
    }

    /**
     * Test single sensor availability entry point
     */
    @Test
    public void testSensorAvailability() throws Exception {
        // NOTE:  Update this test if/when the reserved values put into use.  For now, we
        //        expect them to never be supported.
        assertFalse(mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_RESERVED1));
        assertFalse(mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_RESERVED13));
        assertFalse(mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_RESERVED21));

        // We expect these sensors to always be available
        assertTrue(mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_CAR_SPEED));
        assertTrue(mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_FUEL_LEVEL));
        assertTrue(mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_PARKING_BRAKE));
        assertTrue(mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_GEAR));
        assertTrue(mCarSensorManager.isSensorSupported(CarSensorManager.SENSOR_TYPE_NIGHT));
        assertTrue(mCarSensorManager.isSensorSupported(
                CarSensorManager.SENSOR_TYPE_IGNITION_STATE));
    }

    /**
     * Test sensor enumeration entry point
     */
    @Test
    public void testSensorEnumeration() throws Exception {
        int[] supportedSensors = mCarSensorManager.getSupportedSensors();
        assertNotNull(supportedSensors);

        Log.i(TAG, "Found " + supportedSensors.length + " supported sensors.");

        // Unfortunately, we don't have a definitive range for legal sensor values,
        // so we have set a "reasonable" range here.  The ending value, in particular,
        // will need to be updated if/when new sensor types are allowed.
        // Here we are ensuring that all the enumerated sensors also return supported.
        for (int candidate = 0; candidate <= CarSensorManager.SENSOR_TYPE_RESERVED21; ++candidate) {
            boolean supported = mCarSensorManager.isSensorSupported(candidate);
            boolean found = false;
            for (int sensor : supportedSensors) {
                if (candidate == sensor) {
                    found = true;
                    Log.i(TAG, "Sensor type " + sensor + " is supported.");
                    break;
                }
            }

            // Make sure the individual query on a sensor type is consistent
            assertEquals(found, supported);
        }
    }

    /**
     * Test sensor notification registration, delivery, and unregistration
     */
    @Test
    public void testEvents() throws Exception {
        // Set up our listener callback
        SensorListener listener = new SensorListener();
        mCarSensorManager.registerListener(listener,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE,
                CarSensorManager.SENSOR_RATE_FASTEST);

        VehiclePropValue value;
        CarSensorEvent event;
        CarSensorEvent.ParkingBrakeData data = null;

        // Clear event generated by registerCallback()
        listener.waitForSensorChange();
        listener.reset();

        // Set the value TRUE and wait for the event to arrive
        getAidlMockedVehicleHal().injectEvent(
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.PARKING_BRAKE_ON)
                        .setBooleanValue(true)
                        .setTimestamp(51L)
                        .build(), true);
        assertTrue(listener.waitForSensorChange(51L));

        // Ensure we got the expected event
        assertEquals(listener.getLastEvent().sensorType,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);

        // Ensure we got the expected value in our callback
        data = listener.getLastEvent().getParkingBrakeData(data);
        Log.d(TAG, "Parking: " + data.isEngaged + " at " + data.timestamp);
        assertTrue(data.isEngaged);

        // Ensure we have the expected value in the sensor manager's cache
        event = mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);
        assertNotNull(event);
        data = event.getParkingBrakeData(data);
        assertEquals("Unexpected event timestamp", data.timestamp, 51);
        assertTrue("Unexpected value", data.isEngaged);

        listener.reset();
        // Set the value FALSE
        getAidlMockedVehicleHal().injectEvent(
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.PARKING_BRAKE_ON)
                        .setTimestamp(1001)
                        .setBooleanValue(false)
                        .build(), true);
        assertTrue(listener.waitForSensorChange(1001));

        // Ensure we got the expected event
        assertEquals(listener.getLastEvent().sensorType,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);

        // Ensure we got the expected value in our callback
        data = listener.getLastEvent().getParkingBrakeData(data);
        assertEquals("Unexpected event timestamp", 1001, data.timestamp);
        assertFalse("Unexpected value", data.isEngaged);

        // Ensure we have the expected value in the sensor manager's cache
        event = mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);
        assertNotNull(event);
        data = event.getParkingBrakeData(data);
        assertFalse(data.isEngaged);

        // Unregister our handler (from all sensor types)
        mCarSensorManager.unregisterListener(listener);

        listener.reset();
        // Set the value TRUE again
        value = AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.PARKING_BRAKE_ON)
                .setTimestamp(2001)
                .setBooleanValue(true)
                .build();
        getAidlMockedVehicleHal().injectEvent(value, true);

        // Ensure we did not get a callback (should timeout)
        Log.i(TAG, "waiting for unexpected callback -- should timeout.");
        assertFalse(listener.waitForSensorChange(2001));

        // Despite us not having a callback registered, the Sensor Manager should see the update
        event = mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);
        assertNotNull(event);
        data = event.getParkingBrakeData(data);
        assertEquals("Unexpected event timestamp", data.timestamp, 2001);
        assertTrue("Unexpected value", data.isEngaged);
    }

    @Test
    public void testIgnitionState() {
        CarSensorEvent event = mCarSensorManager.getLatestSensorEvent(
                CarSensorManager.SENSOR_TYPE_IGNITION_STATE);
        assertNotNull(event);
        assertEquals(CarSensorEvent.IGNITION_STATE_ACC, event.intValues[0]);
    }

    @Test
    public void testIgnitionEvents() throws Exception {
        SensorListener listener = new SensorListener();
        mCarSensorManager.registerListener(listener, CarSensorManager.SENSOR_TYPE_IGNITION_STATE,
                CarSensorManager.SENSOR_RATE_NORMAL);
        // Clear event generated by registerCallback()
        listener.waitForSensorChange();

        // Mapping of HAL -> Manager ignition states.
        int[] ignitionStates = new int[] {
                VehicleIgnitionState.UNDEFINED, CarSensorEvent.IGNITION_STATE_UNDEFINED,
                VehicleIgnitionState.LOCK, CarSensorEvent.IGNITION_STATE_LOCK,
                VehicleIgnitionState.OFF, CarSensorEvent.IGNITION_STATE_OFF,
                VehicleIgnitionState.ACC, CarSensorEvent.IGNITION_STATE_ACC,
                VehicleIgnitionState.ON, CarSensorEvent.IGNITION_STATE_ON,
                VehicleIgnitionState.START, CarSensorEvent.IGNITION_STATE_START,
                VehicleIgnitionState.ON, CarSensorEvent.IGNITION_STATE_ON,
                VehicleIgnitionState.LOCK, CarSensorEvent.IGNITION_STATE_LOCK,
        };

        for (int i = 0; i < ignitionStates.length; i += 2) {
            injectIgnitionStateAndAssert(listener, ignitionStates[i], ignitionStates[i + 1]);
        }
    }

    private void injectIgnitionStateAndAssert(SensorListener listener, int halIgnitionState,
            int mgrIgnitionState) throws Exception{
        listener.reset();
        long time = SystemClock.elapsedRealtimeNanos();
        getAidlMockedVehicleHal().injectEvent(
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.IGNITION_STATE)
                        .addIntValues(halIgnitionState)
                        .setTimestamp(time)
                        .build(), true);
        assertTrue(listener.waitForSensorChange(time));

        CarSensorEvent eventReceived = listener.getLastEvent();
        assertEquals(CarSensorManager.SENSOR_TYPE_IGNITION_STATE, eventReceived.sensorType);
        assertEquals(mgrIgnitionState, eventReceived.intValues[0]);
    }

    @Test
    public void testGear() throws Exception {
        SensorListener listener = new SensorListener();

        mCarSensorManager.registerListener(listener, CarSensorManager.SENSOR_TYPE_GEAR,
                CarSensorManager.SENSOR_RATE_NORMAL);

        // Clear event generated by registerCallback()
        listener.waitForSensorChange();

        // Mapping of HAL -> Manager gear selection states.
        int[] gears = new int[] {
                VehicleGear.GEAR_PARK, CarSensorEvent.GEAR_PARK,
                VehicleGear.GEAR_DRIVE, CarSensorEvent.GEAR_DRIVE,
                VehicleGear.GEAR_NEUTRAL, CarSensorEvent.GEAR_NEUTRAL,
                VehicleGear.GEAR_REVERSE, CarSensorEvent.GEAR_REVERSE,
                VehicleGear.GEAR_1, CarSensorEvent.GEAR_FIRST,
                VehicleGear.GEAR_2, CarSensorEvent.GEAR_SECOND,
                VehicleGear.GEAR_3, CarSensorEvent.GEAR_THIRD,
                VehicleGear.GEAR_4, CarSensorEvent.GEAR_FOURTH,
                VehicleGear.GEAR_5, CarSensorEvent.GEAR_FIFTH,
                VehicleGear.GEAR_6, CarSensorEvent.GEAR_SIXTH,
                VehicleGear.GEAR_7, CarSensorEvent.GEAR_SEVENTH,
                VehicleGear.GEAR_8, CarSensorEvent.GEAR_EIGHTH,
                VehicleGear.GEAR_9, CarSensorEvent.GEAR_NINTH,
        };

        for (int i = 0; i < gears.length; i += 2) {
            injectGearEventAndAssert(listener, gears[i], gears[i + 1]);
        }
    }

    private void injectGearEventAndAssert(SensorListener listener, int halValue,
            int carSensorValue) throws Exception {
        listener.reset();
        long time = SystemClock.elapsedRealtimeNanos();
        getAidlMockedVehicleHal().injectEvent(
                AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.GEAR_SELECTION)
                        .addIntValues(halValue)
                        .setTimestamp(time)
                        .build(), true);
        assertTrue(listener.waitForSensorChange(time));
        CarSensorEvent event = mCarSensorManager.getLatestSensorEvent(
                CarSensorManager.SENSOR_TYPE_GEAR);
        assertNotNull(event);
        assertEquals(carSensorValue, event.intValues[0]);
    }

    /**
     * Test sensor multiple liseners notification registration, delivery and unregistration.
     */
    @Test
    public void testEventsWithMultipleListeners() throws Exception {
        // Set up our listeners callback
        SensorListener listener1 = new SensorListener();
        SensorListener listener2 = new SensorListener();
        SensorListener listener3 = new SensorListener();

        mCarSensorManager.registerListener(listener1,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE,
                CarSensorManager.SENSOR_RATE_NORMAL);

        mCarSensorManager.registerListener(listener2,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE,
                CarSensorManager.SENSOR_RATE_NORMAL);

        mCarSensorManager.registerListener(listener3,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE,
                CarSensorManager.SENSOR_RATE_FASTEST);

        CarSensorEvent.ParkingBrakeData data = null;
        VehiclePropValue value;
        CarSensorEvent event;

        // Clear event generated by registerCallback()
        listener1.waitForSensorChange();
        listener2.waitForSensorChange();
        listener3.waitForSensorChange();
        listener1.reset();
        listener2.reset();
        listener3.reset();

        // Set the value TRUE and wait for the event to arrive
        value = AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.PARKING_BRAKE_ON)
                .setTimestamp(1001L)
                .setBooleanValue(true)
                .build();
        getAidlMockedVehicleHal().injectEvent(value, true);

        assertTrue(listener1.waitForSensorChange(1001L));
        assertTrue(listener2.waitForSensorChange(1001L));
        assertTrue(listener3.waitForSensorChange(1001L));

        // Ensure we got the expected event
        assertEquals(listener1.getLastEvent().sensorType,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);
        assertEquals(listener2.getLastEvent().sensorType,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);
        assertEquals(listener3.getLastEvent().sensorType,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);

        // Ensure we got the expected value in our callback
        data = listener1.getLastEvent().getParkingBrakeData(data);
        Log.d(TAG, "Parking brake is " + data.isEngaged + " at " + data.timestamp);
        assertTrue(data.isEngaged);

        data = listener2.getLastEvent().getParkingBrakeData(data);
        Log.d(TAG, "Parking brake is " + data.isEngaged + " at " + data.timestamp);
        assertTrue(data.isEngaged);

        data = listener3.getLastEvent().getParkingBrakeData(data);
        Log.d(TAG, "Parking brake is" + data.isEngaged + " at " + data.timestamp);
        assertTrue(data.isEngaged);

        // Ensure we have the expected value in the sensor manager's cache
        event = mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);
        data = event.getParkingBrakeData(data);
        assertEquals("Unexpected event timestamp", 1001, data.timestamp);
        assertTrue("Unexpected value", data.isEngaged);

        listener1.reset();
        listener2.reset();
        listener3.reset();
        // Set the value FALSE
        value = AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.PARKING_BRAKE_ON)
                .setTimestamp(2001)
                .setBooleanValue(false)
                .build();
        getAidlMockedVehicleHal().injectEvent(value, true);
        assertTrue(listener1.waitForSensorChange(2001));
        assertTrue(listener2.waitForSensorChange(2001));
        assertTrue(listener3.waitForSensorChange(2001));

        // Ensure we got the expected event
        assertEquals(listener1.getLastEvent().sensorType,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);
        assertEquals(listener2.getLastEvent().sensorType,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);
        assertEquals(listener3.getLastEvent().sensorType,
                CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);

        // Ensure we got the expected value in our callback
        data = listener1.getLastEvent().getParkingBrakeData(data);
        assertEquals("Unexpected event timestamp", 2001, data.timestamp);
        assertFalse("Unexpected value", data.isEngaged);

        data = listener2.getLastEvent().getParkingBrakeData(data);
        assertEquals("Unexpected event timestamp", 2001, data.timestamp);
        assertFalse("Unexpected value", data.isEngaged);

        data = listener3.getLastEvent().getParkingBrakeData(data);
        assertEquals("Unexpected event timestamp", 2001, data.timestamp);
        assertFalse("Unexpected value", data.isEngaged);

        // Ensure we have the expected value in the sensor manager's cache
        event = mCarSensorManager.getLatestSensorEvent(CarSensorManager.SENSOR_TYPE_PARKING_BRAKE);
        data = event.getParkingBrakeData(data);
        assertFalse(data.isEngaged);

        Log.d(TAG, "Unregistering listener3");
        listener1.reset();
        listener2.reset();
        listener3.reset();
        mCarSensorManager.unregisterListener(listener3);
        Log.d(TAG, "Rate changed - expect sensor restart and change event sent.");
        value = AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.PARKING_BRAKE_ON)
                .setTimestamp(3002)
                .setBooleanValue(false)
                .build();
        getAidlMockedVehicleHal().injectEvent(value, true);
        assertTrue(listener1.waitForSensorChange());
        assertTrue(listener2.waitForSensorChange());
        assertFalse(listener3.waitForSensorChange());
        listener1.reset();
        listener2.reset();
        listener3.reset();
        // Set the value TRUE again
        value = AidlVehiclePropValueBuilder.newBuilder(VehicleProperty.PARKING_BRAKE_ON)
                .setCurrentTimestamp()
                .setBooleanValue(true)
                .build();
        getAidlMockedVehicleHal().injectEvent(value, true);

        assertTrue(listener1.waitForSensorChange());
        assertTrue(listener2.waitForSensorChange());
        listener1.reset();
        listener2.reset();

        // Ensure we did not get a callback (should timeout)
        Log.i(TAG, "waiting for unexpected callback -- should timeout.");
        assertFalse(listener3.waitForSensorChange());

        Log.d(TAG, "Unregistering listener2");
        mCarSensorManager.unregisterListener(listener2);

        Log.d(TAG, "Rate did nor change - dont expect sensor restart and change event sent.");
        assertFalse(listener1.waitForSensorChange());
        assertFalse(listener2.waitForSensorChange());
        assertFalse(listener3.waitForSensorChange());
    }


    /**
     * Callback function we register for sensor update notifications.
     * This tracks the number of times it has been called via the mAvailable semaphore,
     * and keeps a reference to the most recent event delivered.
     */
    private static final class SensorListener implements CarSensorManager.OnSensorChangedListener {
        private final Object mSync = new Object();

        private CarSensorEvent mLastEvent = null;

        CarSensorEvent getLastEvent() {
            return mLastEvent;
        }

        void reset() {
            synchronized (mSync) {
                mLastEvent = null;
            }
        }

        boolean waitForSensorChange() throws InterruptedException {
            return waitForSensorChange(0);
        }

        // Returns True to indicate receipt of a sensor event.  False indicates a timeout.
        boolean waitForSensorChange(long eventTimeStamp) throws InterruptedException {
            long start = SystemClock.elapsedRealtime();
            boolean matchTimeStamp = eventTimeStamp != 0;
            synchronized (mSync) {
                Log.d(TAG, "waitForSensorChange, mLastEvent: " + mLastEvent);
                while ((mLastEvent == null
                        || (matchTimeStamp && mLastEvent.timestamp != eventTimeStamp))
                        && (start + SHORT_WAIT_TIMEOUT_MS > SystemClock.elapsedRealtime())) {
                    mSync.wait(10L);
                }
                return mLastEvent != null &&
                        (!matchTimeStamp || mLastEvent.timestamp == eventTimeStamp);
            }
        }

        @Override
        public void onSensorChanged(CarSensorEvent event) {
            Log.d(TAG, "onSensorChanged, event: " + event);
            synchronized (mSync) {
                // We're going to hold a reference to this object
                mLastEvent = event;
                mSync.notifyAll();
            }
        }
    }

}
