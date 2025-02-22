/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.google.android.car.kitchensink.display;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.car.kitchensink.R;

/**
 * Displays info about the display this is run on.
 */
public class DisplayInfoFragment extends Fragment {

    private LinearLayout mList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.display_info, container, false);

        mList = view.findViewById(R.id.list);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Point screenSize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);
        addTextView("display id: " + getContext().getDisplayId());
        addTextView("window size(px): " + screenSize.x + " x " + screenSize.y);
        addTextView("display density(dpi): " + getResources().getDisplayMetrics().densityDpi);
        addTextView("display default density(dpi): "
                + getResources().getDisplayMetrics().DENSITY_DEFAULT);

        addTextView("======================================");

        ActivityManager am =
                (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo ci = am.getDeviceConfigurationInfo();
        addTextView("OpenGL ES version: " + ci.getGlEsVersion());

        addTextView("======================================");
        addTextView("All size are in DP.");
        View rootView = getActivity().findViewById(android.R.id.content);
        addTextView("view size: "
                + convertPixelsToDp(rootView.getWidth(), getContext())
                + " x " + convertPixelsToDp(rootView.getHeight(), getContext()));

        addTextView("window size: "
                + convertPixelsToDp(screenSize.x, getContext())
                + " x " + convertPixelsToDp(screenSize.y, getContext()));

        addDimenText("car_keyline_1");
        addDimenText("car_keyline_2");
        addDimenText("car_keyline_3");
        addDimenText("car_keyline_4");
        addDimenText("car_margin");
        addDimenText("car_gutter_size");
        addDimenText("car_primary_icon_size");
        addDimenText("car_secondary_icon_size");
        addDimenText("car_title_size");
        addDimenText("car_title2_size");
        addDimenText("car_headline1_size");
        addDimenText("car_headline2_size");
        addDimenText("car_headline3_size");
        addDimenText("car_headline4_size");
        addDimenText("car_body1_size");
        addDimenText("car_body2_size");
        addDimenText("car_body3_size");
        addDimenText("car_body4_size");
        addDimenText("car_body5_size");
        addDimenText("car_action1_size");
        addDimenText("car_touch_target_size");
        addDimenText("car_action_bar_height");
    }

    private void addDimenText(String dimenName) {
        String value;
        try {
            float dimen = convertPixelsToDp(
                    getResources().getDimensionPixelSize(
                            getResources().getIdentifier(
                                    dimenName, "dimen", getContext().getPackageName())),
                    getContext());
            value = Float.toString(dimen);
        } catch (Resources.NotFoundException e) {
            value = "Resource Not Found";
        }
        addTextView(dimenName + " : " + value);
    }

    private void addTextView(String text) {
        TextView textView = new TextView(getContext());
        textView.setTextAppearance(R.style.TextAppearance_CarUi_Body2);
        textView.setText(text);
        mList.addView(textView);
    }

    private static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }
}
