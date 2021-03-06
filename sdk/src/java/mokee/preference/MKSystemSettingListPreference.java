/*
 * Copyright (C) 2016 The CyanogenMod project
 * Copyright (C) 2016 The MoKee Open Source project
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
package mokee.preference;

import android.content.Context;
import android.util.AttributeSet;

import mokee.providers.MKSettings;


public class MKSystemSettingListPreference extends SelfRemovingListPreference {
    public MKSystemSettingListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MKSystemSettingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean persistString(String value) {
        if (shouldPersist()) {
            if (value == getPersistedString(null)) {
                // It's already there, so the same as persisting
                return true;
            }
            MKSettings.System.putString(getContext().getContentResolver(), getKey(), value);
            return true;
        }
        return false;
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        String value = MKSettings.System.getString(getContext().getContentResolver(), getKey());
        return value == null ? defaultReturnValue : value;
    }

    @Override
    protected boolean isPersisted() {
        return MKSettings.System.getString(getContext().getContentResolver(), getKey()) != null;
    }

    public int getIntValue(int defValue) {
        return getValue() == null ? defValue : Integer.valueOf(getValue());
    }
}
