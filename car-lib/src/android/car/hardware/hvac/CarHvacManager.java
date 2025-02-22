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

package android.car.hardware.hvac;

import android.annotation.IntDef;
import android.annotation.SystemApi;
import android.car.Car;
import android.car.CarManagerBase;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.car.hardware.property.CarPropertyManager.CarPropertyEventCallback;
import android.car.hardware.property.ICarProperty;
import android.os.IBinder;
import android.util.ArraySet;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * API for controlling HVAC system in cars
 * @hide
 * @deprecated Use {@link CarPropertyManager} instead.
 */
@Deprecated
@SystemApi
public final class CarHvacManager extends CarManagerBase {
    private static final String TAG = "CarHvacManager";
    private final CarPropertyManager mCarPropertyMgr;
    @GuardedBy("mLock")
    private CarPropertyEventListenerToBase mListenerToBase = null;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final ArraySet<CarHvacEventCallback> mCallbacks = new ArraySet<>();

    /**
     * HVAC property IDs for get/set methods
     */
    /**
     * Mirror defrosters state, int type
     * Positive values indicate mirror defroster is on
     */
    public static final int ID_MIRROR_DEFROSTER_ON = 0x1440050c;
    /**
     * Steering wheel temp, int type
     * Positive values indicate heating.
     * Negative values indicate cooling
     */
    public static final int ID_STEERING_WHEEL_HEAT = 0x1140050d;
    /**
     * Outside air temperature, float type
     * Value is in degrees Celsius
     */
    public static final int ID_OUTSIDE_AIR_TEMP = 0x11600703;
    /**
     * Temperature units being used, int type
     *  0x30 = Celsius
     *  0x31 = Fahrenheit
     */
    public static final int ID_TEMPERATURE_DISPLAY_UNITS = 0x1140050e;

    /**
     * Temperature setpoint, float type
     * Temperature set by the user, units are in degrees Celsius.
     */
    public static final int ID_ZONED_TEMP_SETPOINT = 0x15600503;
    /**
     * Actual temperature, float type
     * Actual zone temperature is read only value, in terms of F or C.
     */
    public static final int ID_ZONED_TEMP_ACTUAL = 0x15600502;
    /**
     * HVAC system powered on / off, bool type
     * In many vehicles, if the HVAC system is powered off, the SET and GET command will
     * throw an IllegalStateException.  To correct this, need to turn on the HVAC module first
     * before manipulating a parameter.
     */
    public static final int ID_ZONED_HVAC_POWER_ON = 0x15200510;
    /**
     * Fan speed setpoint, int type
     * Fan speed is an integer from 0-n, depending on number of fan speeds available.
     */
    public static final int ID_ZONED_FAN_SPEED_SETPOINT = 0x15400500;
    /**
     * Actual fan speed, int type
     * Actual fan speed is a read-only value, expressed in RPM.
     */
    public static final int ID_ZONED_FAN_SPEED_RPM = 0x1540050f;
    /**
     *  Fan direction available, int vector type
     *  Fan direction is a bitmask of directions available for each zone.
     */
    public static final int ID_ZONED_FAN_DIRECTION_AVAILABLE = 0x15410511;
    /**
     * Current fan direction setting, int type. The value must be one of the FAN_DIRECTION_AVAILABLE
     * values declared above.
     */
    public static final int ID_ZONED_FAN_DIRECTION = 0x15400501;
    /**
     * Seat temperature, int type
     * Seat temperature is negative for cooling, positive for heating.  Temperature is a
     * setting, i.e. -3 to 3 for 3 levels of cooling and 3 levels of heating.
     */
    public static final int ID_ZONED_SEAT_TEMP = 0x1540050b;
    /**
     * Air ON, bool type
     * true indicates AC is ON.
     */
    public static final int ID_ZONED_AC_ON = 0x15200505;
    /**
     * Automatic Mode ON, bool type
     * true indicates HVAC is in automatic mode
     */
    public static final int ID_ZONED_AUTOMATIC_MODE_ON = 0x1520050A;
    /**
     * Air recirculation ON, bool type
     * true indicates recirculation is active.
     */
    public static final int ID_ZONED_AIR_RECIRCULATION_ON = 0x15200508;
    /**
     * Max AC ON, bool type
     * true indicates MAX AC is ON
     */
    public static final int ID_ZONED_MAX_AC_ON = 0x15200506;
    /** Dual zone ON, bool type
     * true indicates dual zone mode is ON
     */
    public static final int ID_ZONED_DUAL_ZONE_ON = 0x15200509;
    /**
     * Max Defrost ON, bool type
     * true indicates max defrost is active.
     */
    public static final int ID_ZONED_MAX_DEFROST_ON = 0x15200507;
    /**
     * Automatic recirculation mode ON
     * true indicates recirculation is in automatic mode
     */
    public static final int ID_ZONED_HVAC_AUTO_RECIRC_ON = 0x15200512;
    /**
     * Defroster ON, bool type
     * Defroster controls are based on window position.
     * True indicates the defroster is ON.
     */
    public static final int ID_WINDOW_DEFROSTER_ON = 0x13200504;

    /** @hide */
    @IntDef({
            ID_MIRROR_DEFROSTER_ON,
            ID_STEERING_WHEEL_HEAT,
            ID_OUTSIDE_AIR_TEMP,
            ID_TEMPERATURE_DISPLAY_UNITS,
            ID_ZONED_TEMP_SETPOINT,
            ID_ZONED_TEMP_ACTUAL,
            ID_ZONED_FAN_SPEED_SETPOINT,
            ID_ZONED_FAN_SPEED_RPM,
            ID_ZONED_FAN_DIRECTION_AVAILABLE,
            ID_ZONED_FAN_DIRECTION,
            ID_ZONED_SEAT_TEMP,
            ID_ZONED_AC_ON,
            ID_ZONED_AUTOMATIC_MODE_ON,
            ID_ZONED_AIR_RECIRCULATION_ON,
            ID_ZONED_MAX_AC_ON,
            ID_ZONED_DUAL_ZONE_ON,
            ID_ZONED_MAX_DEFROST_ON,
            ID_ZONED_HVAC_POWER_ON,
            ID_ZONED_HVAC_AUTO_RECIRC_ON,
            ID_WINDOW_DEFROSTER_ON
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PropertyId {}
    private final ArraySet<Integer> mHvacPropertyIds = new ArraySet<>(Arrays.asList(new Integer [] {
            ID_MIRROR_DEFROSTER_ON,
            ID_STEERING_WHEEL_HEAT,
            ID_OUTSIDE_AIR_TEMP,
            ID_TEMPERATURE_DISPLAY_UNITS,
            ID_ZONED_TEMP_SETPOINT,
            ID_ZONED_TEMP_ACTUAL,
            ID_ZONED_FAN_SPEED_SETPOINT,
            ID_ZONED_FAN_SPEED_RPM,
            ID_ZONED_FAN_DIRECTION_AVAILABLE,
            ID_ZONED_FAN_DIRECTION,
            ID_ZONED_SEAT_TEMP,
            ID_ZONED_AC_ON,
            ID_ZONED_AUTOMATIC_MODE_ON,
            ID_ZONED_AIR_RECIRCULATION_ON,
            ID_ZONED_MAX_AC_ON,
            ID_ZONED_DUAL_ZONE_ON,
            ID_ZONED_MAX_DEFROST_ON,
            ID_ZONED_HVAC_POWER_ON,
            ID_ZONED_HVAC_AUTO_RECIRC_ON,
            ID_WINDOW_DEFROSTER_ON
    }));


    /**
     * Use {@link android.car.hardware.CarHvacFanDirection#FACE} instead.
     * Represents fan direction when air flows through face directed vents.
     * This constant must be used with {@link #ID_ZONED_FAN_DIRECTION} property.
     */
    public static final int FAN_DIRECTION_FACE = 0x1;
    /**
     * Use {@link android.car.hardware.CarHvacFanDirection#FLOOR} instead.
     * Represents fan direction when air flows through floor directed vents.
     * This constant must be used with {@link #ID_ZONED_FAN_DIRECTION} property.
     */
    public static final int FAN_DIRECTION_FLOOR = 0x2;
    /**
     * Use {@link android.car.hardware.CarHvacFanDirection#DEFROST} instead.
     * Represents fan direction when air flows through defrost vents.
     * This constant must be used with {@link #ID_ZONED_FAN_DIRECTION} property.
     */
    public static final int FAN_DIRECTION_DEFROST = 0x4;

    /**
     * Application registers {@link CarHvacEventCallback} object to receive updates and changes to
     * subscribed Car HVAC properties.
     */
    public interface CarHvacEventCallback {
        /**
         * Called when a property is updated
         * @param value Property that has been updated.
         */
        void onChangeEvent(CarPropertyValue value);

        /**
         * Called when an error is detected with a property
         * @param propertyId
         * @param zone
         */
        void onErrorEvent(@PropertyId int propertyId, int zone);
    }

    private static class CarPropertyEventListenerToBase implements CarPropertyEventCallback {
        private final WeakReference<CarHvacManager> mManager;

        CarPropertyEventListenerToBase(CarHvacManager manager) {
            mManager = new WeakReference<>(manager);
        }

        @Override
        public void onChangeEvent(CarPropertyValue value) {
            CarHvacManager manager = mManager.get();
            if (manager != null) {
                manager.handleOnChangeEvent(value);
            }
        }

        @Override
        public void onErrorEvent(int propertyId, int zone) {
            CarHvacManager manager = mManager.get();
            if (manager != null) {
                manager.handleOnErrorEvent(propertyId, zone);
            }
        }
    }

    private void handleOnChangeEvent(CarPropertyValue value) {
        Collection<CarHvacEventCallback> callbacks;
        synchronized (mLock) {
            callbacks = new ArraySet<>(mCallbacks);
        }
        if (!callbacks.isEmpty()) {
            for (CarHvacEventCallback l: callbacks) {
                l.onChangeEvent(value);
            }
        }
    }

    private void handleOnErrorEvent(int propertyId, int zone) {
        Collection<CarHvacEventCallback> callbacks;
        synchronized (mLock) {
            callbacks = new ArraySet<>(mCallbacks);
        }
        if (!callbacks.isEmpty()) {
            for (CarHvacEventCallback l: callbacks) {
                l.onErrorEvent(propertyId, zone);
            }
        }
    }

    /**
     * Get an instance of the CarHvacManager.
     *
     * Should not be obtained directly by clients, use {@link Car#getCarManager(String)} instead.
     * @param service
     *
     * @hide
     */
    public CarHvacManager(Car car, IBinder service) {
        super(car);
        ICarProperty mCarPropertyService = ICarProperty.Stub.asInterface(service);
        mCarPropertyMgr = new CarPropertyManager(car, mCarPropertyService);
    }

    /**
     * Implement wrappers for contained CarPropertyManager object
     * @param callback
     */
    public void registerCallback(CarHvacEventCallback callback) {
        synchronized (mLock) {
            if (mCallbacks.isEmpty()) {
                mListenerToBase = new CarPropertyEventListenerToBase(this);
            }
            List<CarPropertyConfig> configs = getPropertyList();
            for (CarPropertyConfig c : configs) {
                // Register each individual propertyId
                mCarPropertyMgr.registerCallback(mListenerToBase, c.getPropertyId(), 0);
            }
            mCallbacks.add(callback);
        }
    }

    /**
     * Stop getting property updates for the given callback. If there are multiple registrations for
     * this listener, all listening will be stopped.
     * @param callback
     */
    public void unregisterCallback(CarHvacEventCallback callback) {
        synchronized (mLock) {
            mCallbacks.remove(callback);
            try {
                List<CarPropertyConfig> configs = getPropertyList();
                for (CarPropertyConfig c : configs) {
                    // Register each individual propertyId
                    mCarPropertyMgr.unregisterCallback(mListenerToBase, c.getPropertyId());

                }
            } catch (RuntimeException e) {
                Slog.e(TAG, "getPropertyList exception ", e);
            }
            if (mCallbacks.isEmpty()) {
                mCarPropertyMgr.unregisterCallback(mListenerToBase);
                mListenerToBase = null;
            }
        }
    }

    /**
     * Get list of properties represented by Car Hvac Manager for this car.
     * @return List of CarPropertyConfig objects available via Car Hvac Manager.
     */
    public List<CarPropertyConfig> getPropertyList() {
        return mCarPropertyMgr.getPropertyList(mHvacPropertyIds);
    }

    /**
     * Check whether a given property is available or disabled based on the cars current state.
     * @return true if the property is AVAILABLE, false otherwise
     */
    public boolean isPropertyAvailable(@PropertyId int propertyId, int area) {
        return mCarPropertyMgr.isPropertyAvailable(propertyId, area);
    }

    /**
     * Get value of boolean property
     * @param propertyId
     * @param area
     * @return value of requested boolean property
     */
    public boolean getBooleanProperty(@PropertyId int propertyId, int area) {
        return mCarPropertyMgr.getBooleanProperty(propertyId, area);
    }

    /**
     * Get value of float property
     * @param propertyId
     * @param area
     * @return value of requested float property
     */
    public float getFloatProperty(@PropertyId int propertyId, int area) {
        return mCarPropertyMgr.getFloatProperty(propertyId, area);
    }

    /**
     * Get value of integer property
     * @param propertyId
     * @param area
     * @return value of requested integer property
     */
    public int getIntProperty(@PropertyId int propertyId, int area) {
        return mCarPropertyMgr.getIntProperty(propertyId, area);
    }

    /**
     * Set the value of a boolean property
     * @param propertyId
     * @param area
     * @param val
     */
    public void setBooleanProperty(@PropertyId int propertyId, int area, boolean val) {
        if (mHvacPropertyIds.contains(propertyId)) {
            mCarPropertyMgr.setBooleanProperty(propertyId, area, val);
        }
    }

    /**
     * Set the value of a float property
     * @param propertyId
     * @param area
     * @param val
     */
    public void setFloatProperty(@PropertyId int propertyId, int area, float val) {
        if (mHvacPropertyIds.contains(propertyId)) {
            mCarPropertyMgr.setFloatProperty(propertyId, area, val);
        }
    }

    /**
     * Set the value of an integer property
     * @param propertyId
     * @param area
     * @param val
     */
    public void setIntProperty(@PropertyId int propertyId, int area, int val) {
        if (mHvacPropertyIds.contains(propertyId)) {
            mCarPropertyMgr.setIntProperty(propertyId, area, val);
        }
    }

    /** @hide */
    public void onCarDisconnected() {
        synchronized (mLock) {
            mCallbacks.clear();
        }
        mCarPropertyMgr.onCarDisconnected();
    }
}
