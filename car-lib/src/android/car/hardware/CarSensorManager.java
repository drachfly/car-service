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

package android.car.hardware;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.car.Car;
import android.car.CarManagerBase;
import android.car.VehiclePropertyType;
import android.car.hardware.property.CarPropertyManager;
import android.car.hardware.property.CarPropertyManager.CarPropertyEventCallback;
import android.car.hardware.property.ICarProperty;
import android.os.Bundle;
import android.os.IBinder;
import android.util.ArraySet;
import android.util.Slog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *  @deprecated Use {@link CarPropertyManager} instead.
 *  API for monitoring car sensor data.
 */
@Deprecated
public final class CarSensorManager extends CarManagerBase {
    private static final String TAG = "CarSensorManager";
    private final CarPropertyManager mCarPropertyMgr;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED1                   = 1;
    /**
     * This sensor represents vehicle speed in m/s.
     *
     * <p>Sensor data in {@link CarSensorEvent} is a float. When the vehicle is moving forward,
     * SENSOR_TYPE_CAR_SPEED is positive and negative when the vehicle is moving backward. Also,
     * this value is independent of SENSOR_TYPE_GEAR. For example, if SENSOR_TYPE_GEAR is {@link
     * CarSensorEvent#GEAR_NEUTRAL}, SENSOR_TYPE_CAR_SPEED is positive when the vehicle is moving
     * forward, negative when moving backward, and zero when not moving.
     */
    public static final int SENSOR_TYPE_CAR_SPEED                   = 0x11600207;
    /**
     * Represents engine RPM of the car. Sensor data in {@link CarSensorEvent} is a float.
     */
    public static final int SENSOR_TYPE_RPM                         = 0x11600305;
    /**
     * Total travel distance of the car in Kilometer. Sensor data is a float.
     */
    public static final int SENSOR_TYPE_ODOMETER                    = 0x11600204;
    /**
     * Indicates fuel level of the car.
     * In {@link CarSensorEvent}, represents fuel level in milliliters.
     * This requires {@link Car#PERMISSION_ENERGY} permission.
     */
    public static final int SENSOR_TYPE_FUEL_LEVEL                  = 0x11600307;
    /**
     * Represents the current status of parking brake. Sensor data in {@link CarSensorEvent} is an
     * intValues[0]. Value of 1 represents parking brake applied while 0 means the other way
     * around. For this sensor, rate in {@link #registerListener(OnSensorChangedListener, int, int)}
     * will be ignored and all changes will be notified.
     * This requires {@link Car#PERMISSION_POWERTRAIN} permission.
     */
    public static final int SENSOR_TYPE_PARKING_BRAKE               = 0x11200402;
    /**
     * This represents the current position of transmission gear. Sensor data in
     * {@link CarSensorEvent} is an intValues[0]. For the meaning of the value, check
     * {@link CarSensorEvent#GEAR_NEUTRAL} and other GEAR_*.
     * This requires {@link Car#PERMISSION_POWERTRAIN} permission.
     */
    public static final int SENSOR_TYPE_GEAR                        = 0x11400400;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED8                   = 8;
    /**
     * Day/night sensor. Sensor data is intValues[0].
     * This requires {@link Car#PERMISSION_EXTERIOR_ENVIRONMENT} permission.
     */
    public static final int SENSOR_TYPE_NIGHT                       = 0x11200407;
    /**
     * Outside Environment like temperature.
     * This requires {@link Car#PERMISSION_EXTERIOR_ENVIRONMENT} permission.
     */
    public static final int SENSOR_TYPE_ENV_OUTSIDE_TEMPERATURE     = 0x11600703;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED10                  = 10;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED11                  = 11;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED12                  = 12;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED13                  = 13;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED14                  = 14;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED15                  = 15;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED16                  = 16;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED17                  = 17;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED18                  = 18;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED19                  = 19;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED20                  = 20;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED21                  = 21;
    /**
     * Represents ignition state. The value should be one of the constants that starts with
     * IGNITION_STATE_* in {@link CarSensorEvent}.
     * This requires {@link Car#PERMISSION_POWERTRAIN} permission.
     */
    public static final int SENSOR_TYPE_IGNITION_STATE              = 0x11400409;
    /**
     * Represents wheel distance in millimeters.  Some cars may not have individual sensors on each
     * wheel.  If a value is not available, Long.MAX_VALUE will be reported.  The wheel distance
     * accumulates over time.  It increments on forward movement, and decrements on reverse.  Wheel
     * distance shall be reset to zero each time a vehicle is started by the user.
     * This requires {@link Car#PERMISSION_SPEED} permission.
     */
    public static final int SENSOR_TYPE_WHEEL_TICK_DISTANCE         = 0x11510306;
    /**
     * Set to true when ABS is active.  This sensor is event driven.
     */
    public static final int SENSOR_TYPE_ABS_ACTIVE                  = 0x1120040a;
    /**
     * Set to true when traction control is active.  This sensor is event driven.
     */
    public static final int SENSOR_TYPE_TRACTION_CONTROL_ACTIVE     = 0x1120040b;
    /** @hide */
    public static final int SENSOR_TYPE_RESERVED26                  = 26;
    /**
     * Set to true if the fuel door is open.
     * This requires {@link Car#PERMISSION_ENERGY_PORTS} permission.
     */
    public static final int SENSOR_TYPE_FUEL_DOOR_OPEN              = 0x11200308;

    /**
     * Indicates battery level of the car.
     * In {@link CarSensorEvent}, represents battery level in WH.  floatValues of
     * INDEX_EV_BATTERY_CAPACITY_ACTUAL property represents the actual battery capacity in
     * WH.  The battery degrades over time, so this value is expected to drop slowly over the life
     * of the vehicle.
     * This requires {@link Car#PERMISSION_ENERGY} permission.
     */
    public static final int SENSOR_TYPE_EV_BATTERY_LEVEL            = 0x11600309;
    /**
     * Set to true if EV charging port is open.
     * This requires {@link Car#PERMISSION_ENERGY_PORTS} permission.
     */
    public static final int SENSOR_TYPE_EV_CHARGE_PORT_OPEN         = 0x1120030a;
    /**
     * Set to true if EV charging port is connected.
     * This requires {@link Car#PERMISSION_ENERGY_PORTS} permission.
     */
    public static final int SENSOR_TYPE_EV_CHARGE_PORT_CONNECTED    = 0x1120030b;
    /**
     *  Indicates the instantaneous battery charging rate in mW.
     *  This requires {@link Car#PERMISSION_ENERGY} permission.
     */
    public static final int SENSOR_TYPE_EV_BATTERY_CHARGE_RATE      = 0x1160030c;
    /**
     * Oil level sensor.
     */
    public static final int SENSOR_TYPE_ENGINE_OIL_LEVEL            = 0x11400303;


    /** @hide */
    @IntDef({
            SENSOR_TYPE_CAR_SPEED,
            SENSOR_TYPE_RPM,
            SENSOR_TYPE_ODOMETER,
            SENSOR_TYPE_FUEL_LEVEL,
            SENSOR_TYPE_PARKING_BRAKE,
            SENSOR_TYPE_GEAR,
            SENSOR_TYPE_NIGHT,
            SENSOR_TYPE_ENV_OUTSIDE_TEMPERATURE,
            SENSOR_TYPE_IGNITION_STATE,
            SENSOR_TYPE_WHEEL_TICK_DISTANCE,
            SENSOR_TYPE_ABS_ACTIVE,
            SENSOR_TYPE_TRACTION_CONTROL_ACTIVE,
            SENSOR_TYPE_FUEL_DOOR_OPEN,
            SENSOR_TYPE_EV_BATTERY_LEVEL,
            SENSOR_TYPE_EV_CHARGE_PORT_OPEN,
            SENSOR_TYPE_EV_CHARGE_PORT_CONNECTED,
            SENSOR_TYPE_EV_BATTERY_CHARGE_RATE,
            SENSOR_TYPE_ENGINE_OIL_LEVEL,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SensorType {}

    private final ArraySet<Integer> mSensorConfigIds = new ArraySet<>(Arrays.asList(new Integer[]{
            SENSOR_TYPE_CAR_SPEED,
            SENSOR_TYPE_RPM,
            SENSOR_TYPE_ODOMETER,
            SENSOR_TYPE_FUEL_LEVEL,
            SENSOR_TYPE_PARKING_BRAKE,
            SENSOR_TYPE_GEAR,
            SENSOR_TYPE_NIGHT,
            SENSOR_TYPE_ENV_OUTSIDE_TEMPERATURE,
            SENSOR_TYPE_IGNITION_STATE,
            SENSOR_TYPE_WHEEL_TICK_DISTANCE,
            SENSOR_TYPE_ABS_ACTIVE,
            SENSOR_TYPE_TRACTION_CONTROL_ACTIVE,
            SENSOR_TYPE_FUEL_DOOR_OPEN,
            SENSOR_TYPE_EV_BATTERY_LEVEL,
            SENSOR_TYPE_EV_CHARGE_PORT_OPEN,
            SENSOR_TYPE_EV_CHARGE_PORT_CONNECTED,
            SENSOR_TYPE_EV_BATTERY_CHARGE_RATE,
            SENSOR_TYPE_ENGINE_OIL_LEVEL,
    }));

    /** Read on_change type sensors */
    public static final int SENSOR_RATE_ONCHANGE = 0;
    /** Read sensor in default normal rate set for each sensors. This is default rate. */
    public static final int SENSOR_RATE_NORMAL  = 1;
    public static final int SENSOR_RATE_UI = 5;
    public static final int SENSOR_RATE_FAST = 10;
    /** Read sensor at the maximum rate. Actual rate will be different depending on the sensor. */
    public static final int SENSOR_RATE_FASTEST = 100;

    /** @hide */
    @IntDef({
            SENSOR_RATE_ONCHANGE,
            SENSOR_RATE_NORMAL,
            SENSOR_RATE_UI,
            SENSOR_RATE_FAST,
            SENSOR_RATE_FASTEST
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SensorRate {}

    private CarPropertyEventListenerToBase mCarPropertyEventListener = null;

    /**
     * To keep record of CarPropertyEventListenerToBase
     */
    private final HashMap<OnSensorChangedListener, CarPropertyEventListenerToBase> mListenerMap =
            new HashMap<>();
    /**
     * Listener for car sensor data change.
     * Callbacks are called in the Looper context.
     */
    public interface OnSensorChangedListener {
        /**
         * Called when there is a new sensor data from car.
         * @param event Incoming sensor event for the given sensor type.
         */
        void onSensorChanged(CarSensorEvent event);
    }

    private static class CarPropertyEventListenerToBase implements CarPropertyEventCallback {
        private final WeakReference<CarSensorManager> mManager;
        private final OnSensorChangedListener mListener;
        CarPropertyEventListenerToBase(CarSensorManager manager, OnSensorChangedListener listener) {
            mManager = new WeakReference<>(manager);
            mListener = listener;
        }

        @Override
        public void onChangeEvent(CarPropertyValue value) {
            CarSensorManager manager = mManager.get();
            if (manager != null) {
                manager.handleOnChangeEvent(value, mListener);
            }
        }

        @Override
        public void onErrorEvent(int propertyId, int zone) {

        }
    }

    private void handleOnChangeEvent(CarPropertyValue value, OnSensorChangedListener listener) {
        synchronized (mListenerMap) {
            CarSensorEvent event = createCarSensorEvent(value);
            listener.onSensorChanged(event);
        }
    }

    /** @hide */
    public CarSensorManager(Car car, IBinder service) {
        super(car);
        ICarProperty mCarPropertyService = ICarProperty.Stub.asInterface(service);
        mCarPropertyMgr = new CarPropertyManager(car, mCarPropertyService);
    }

    /** @hide */
    @Override
    public void onCarDisconnected() {
        synchronized (mListenerMap) {
            mListenerMap.clear();
        }
        mCarPropertyMgr.onCarDisconnected();
    }

    /**
     * Give the list of CarSensors available in the connected car.
     * @return array of all sensor types supported. Sensor types is the same as
     * property id.
     */
    @NonNull
    public int[] getSupportedSensors() {
        List<CarPropertyConfig> carPropertyConfigList = getPropertyList();
        int[] supportedSensors = new int[carPropertyConfigList.size()];
        for (int i = 0; i < supportedSensors.length; i++) {
            supportedSensors[i] = carPropertyConfigList.get(i).getPropertyId();
        }
        return supportedSensors;
    }

    /**
     * Get list of properties represented by CarSensorManager for this car.
     * @return List of CarPropertyConfig objects available via Car Sensor Manager.
     */
    @NonNull
    public List<CarPropertyConfig> getPropertyList() {
        return mCarPropertyMgr.getPropertyList(mSensorConfigIds);
    }

    /**
     * Tells if given sensor is supported or not.
     * @param sensorType
     * @return true if the sensor is supported.
     */
    public boolean isSensorSupported(@SensorType int sensorType) {
        int[] sensors = getSupportedSensors();
        for (int sensorSupported: sensors) {
            if (sensorType == sensorSupported) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if given sensorList is including the sensorType.
     * @param sensorList
     * @param sensorType
     * @return true if sensor is supported.
     * @hide
     */
    public static boolean isSensorSupported(int[] sensorList, @SensorType int sensorType) {
        for (int sensorSupported: sensorList) {
            if (sensorType == sensorSupported) {
                return true;
            }
        }
        return false;
    }

    /**
     * Register {@link OnSensorChangedListener} to get repeated sensor updates. Multiple listeners
     * can be registered for a single sensor or the same listener can be used for different sensors.
     * If the same listener is registered again for the same sensor, it will be either ignored or
     * updated depending on the rate.
     * <p>
     *
     * @param listener
     * @param sensorType sensor type to subscribe.
     * @param rate how fast the sensor events are delivered. It should be one of
     *        {@link #SENSOR_RATE_FASTEST}, {@link #SENSOR_RATE_FAST}, {@link #SENSOR_RATE_UI},
     *        {@link #SENSOR_RATE_NORMAL}. Rate may not be respected especially when the same sensor
     *        is registered with different listener with different rates. Also, rate might be
     *        ignored when vehicle property raises events only when the value is actually changed,
     *        for example {@link #SENSOR_TYPE_PARKING_BRAKE} will raise an event only when parking
     *        brake was engaged or disengaged.
     * @return if the sensor was successfully enabled.
     * @throws IllegalArgumentException for wrong argument like wrong rate
     * @throws SecurityException if missing the appropriate permission
     */
    @RequiresPermission(anyOf = {Car.PERMISSION_SPEED, Car.PERMISSION_CAR_ENGINE_DETAILED,
            Car.PERMISSION_MILEAGE, Car.PERMISSION_ENERGY, Car.PERMISSION_POWERTRAIN,
            Car.PERMISSION_EXTERIOR_ENVIRONMENT, Car.PERMISSION_CAR_DYNAMICS_STATE,
            Car.PERMISSION_ENERGY_PORTS}, conditional = true)
    public boolean registerListener(@NonNull OnSensorChangedListener listener,
            @SensorType int sensorType, @SensorRate int rate) {
        if (rate != SENSOR_RATE_FASTEST && rate != SENSOR_RATE_NORMAL
                && rate != SENSOR_RATE_UI && rate != SENSOR_RATE_FAST
                && rate != SENSOR_RATE_ONCHANGE) {
            throw new IllegalArgumentException("wrong rate " + rate);
        }
        if (mListenerMap.get(listener) == null) {
            mCarPropertyEventListener = new CarPropertyEventListenerToBase(this, listener);
        } else {
            mCarPropertyEventListener = mListenerMap.get(listener);
        }
        if (mCarPropertyMgr.registerCallback(mCarPropertyEventListener, sensorType, rate)) {
            mListenerMap.put(listener, mCarPropertyEventListener);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Stop getting sensor update for the given listener.
     * If there are multiple registrations for this listener, all listening will be stopped.
     * @param listener Listener for car sensor data change.
     */
    public void unregisterListener(@NonNull OnSensorChangedListener listener) {
        synchronized (mListenerMap) {
            mCarPropertyEventListener = mListenerMap.get(listener);
            mCarPropertyMgr.unregisterCallback(mCarPropertyEventListener);
            mListenerMap.remove(listener);
        }
    }

    /**
     * Stop getting sensor update for the given listener and sensor.
     * If the same listener is used for other sensors, those subscriptions will not be affected.
     * @param listener Listener for car sensor data change.
     * @param sensorType Property Id
     */
    public void unregisterListener(@NonNull OnSensorChangedListener listener,
            @SensorType int sensorType) {
        synchronized (mListenerMap) {
            mCarPropertyEventListener = mListenerMap.get(listener);
        }
        mCarPropertyMgr.unregisterCallback(mCarPropertyEventListener, sensorType);
    }

    /**
     * Get the most recent CarSensorEvent for the given type. Note that latest sensor data from car
     * will not be available if it was never subscribed before. This call will return immediately
     * with null if there is no data available.
     * @param type A sensor to request
     * @return null if there was no sensor update since connected to the car.
     */
    @Nullable
    public CarSensorEvent getLatestSensorEvent(@SensorType int type) {
        CarPropertyValue propertyValue = mCarPropertyMgr.getProperty(type, 0);
        return createCarSensorEvent(propertyValue);
    }

    private CarSensorEvent createCarSensorEvent(CarPropertyValue propertyValue) {
        CarSensorEvent event = null;
        switch (propertyValue.getPropertyId() & VehiclePropertyType.MASK) {
            case VehiclePropertyType.FLOAT:
                event = new CarSensorEvent(propertyValue.getPropertyId(),
                        propertyValue.getTimestamp(), 1, 0, 0);
                event.floatValues[0] = (float) propertyValue.getValue();
                break;
            case VehiclePropertyType.INT32:
                event = new CarSensorEvent(propertyValue.getPropertyId(),
                        propertyValue.getTimestamp(), 0, 1, 0);
                event.intValues[0] = (int) propertyValue.getValue();
                break;
            case VehiclePropertyType.BOOLEAN:
                event = new CarSensorEvent(propertyValue.getPropertyId(),
                        propertyValue.getTimestamp(), 0, 1, 0);
                event.intValues[0] = (boolean) propertyValue.getValue() ? 1 : 0;
                break;
            case VehiclePropertyType.INT64_VEC:
                Object[] value = (Object[]) propertyValue.getValue();
                event = new CarSensorEvent(propertyValue.getPropertyId(),
                        propertyValue.getTimestamp(), 0, 0, value.length);
                for (int i = 0; i < value.length; i++) {
                    event.longValues[i] = (Long) value[i];
                }
                break;
            default:
                Slog.e(TAG, "unhandled VehiclePropertyType for propId="
                        + propertyValue.getPropertyId());
                break;
        }
        return event;
    }

    /**
     * Get the config data for the given type.
     *
     * A CarSensorConfig object is returned for every sensor type.  However, if there is no
     * config, the data will be empty.
     *
     * @param type sensor type to request
     * @return CarSensorConfig object
     * @hide
     */
    public CarSensorConfig getSensorConfig(@SensorType int type) {
        Bundle b = null;
        switch (type) {
            case SENSOR_TYPE_WHEEL_TICK_DISTANCE:
                List<CarPropertyConfig> propertyConfigs = mCarPropertyMgr.getPropertyList();
                for (CarPropertyConfig p : propertyConfigs) {
                    if (p.getPropertyId() == type) {
                        b = createWheelDistanceTickBundle(p.getConfigArray());
                        break;
                    }
                }
                break;
            default:
                b = Bundle.EMPTY;
                break;
        }
        return new CarSensorConfig(type, b);
    }

    private static final int INDEX_WHEEL_DISTANCE_ENABLE_FLAG = 0;
    private static final int INDEX_WHEEL_DISTANCE_FRONT_LEFT = 1;
    private static final int INDEX_WHEEL_DISTANCE_FRONT_RIGHT = 2;
    private static final int INDEX_WHEEL_DISTANCE_REAR_RIGHT = 3;
    private static final int INDEX_WHEEL_DISTANCE_REAR_LEFT = 4;
    private static final int WHEEL_TICK_DISTANCE_BUNDLE_SIZE = 6;

    private Bundle createWheelDistanceTickBundle(List<Integer> configArray) {
        Bundle b = new Bundle(WHEEL_TICK_DISTANCE_BUNDLE_SIZE);
        b.putInt(CarSensorConfig.WHEEL_TICK_DISTANCE_SUPPORTED_WHEELS,
                configArray.get(INDEX_WHEEL_DISTANCE_ENABLE_FLAG));
        b.putInt(CarSensorConfig.WHEEL_TICK_DISTANCE_FRONT_LEFT_UM_PER_TICK,
                configArray.get(INDEX_WHEEL_DISTANCE_FRONT_LEFT));
        b.putInt(CarSensorConfig.WHEEL_TICK_DISTANCE_FRONT_RIGHT_UM_PER_TICK,
                configArray.get(INDEX_WHEEL_DISTANCE_FRONT_RIGHT));
        b.putInt(CarSensorConfig.WHEEL_TICK_DISTANCE_REAR_RIGHT_UM_PER_TICK,
                configArray.get(INDEX_WHEEL_DISTANCE_REAR_RIGHT));
        b.putInt(CarSensorConfig.WHEEL_TICK_DISTANCE_REAR_LEFT_UM_PER_TICK,
                configArray.get(INDEX_WHEEL_DISTANCE_REAR_LEFT));
        return b;
    }
}
