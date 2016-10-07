/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2016 The MoKee Open Source Project
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
package org.mokee.internal.mkparts;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.android.internal.R.styleable.Preference;
import static com.android.internal.R.styleable.Preference_fragment;
import static com.android.internal.R.styleable.Preference_icon;
import static com.android.internal.R.styleable.Preference_key;
import static com.android.internal.R.styleable.Preference_summary;
import static com.android.internal.R.styleable.Preference_title;

import static mokee.platform.R.styleable.mk_Searchable;
import static mokee.platform.R.styleable.mk_Searchable_xmlRes;

public class PartsList {

    public static final String ACTION_PART_CHANGED = "org.mokee.mkparts.PART_CHANGED";

    public static final String EXTRA_PART = "part";
    public static final String EXTRA_PART_KEY = "key";

    public static final String MKPARTS_PACKAGE = "org.mokee.mkparts";
    public static final ComponentName MKPARTS_ACTIVITY = new ComponentName(
            MKPARTS_PACKAGE, MKPARTS_PACKAGE + ".PartsActivity");

    public static final String PARTS_ACTION_PREFIX = MKPARTS_PACKAGE + ".parts";

    private static final Map<String, PartInfo> sParts = new ArrayMap<>();

    private static final AtomicBoolean sCatalogLoaded = new AtomicBoolean(false);

    public static void loadParts(Context context) {
        synchronized (sParts) {
            if (sCatalogLoaded.get()) {
                return;
            }

            final PackageManager pm = context.getPackageManager();
            try {
                final Resources r = pm.getResourcesForApplication(MKPARTS_PACKAGE);
                if (r == null) {
                    return;
                }
                int resId = r.getIdentifier("parts_catalog", "xml", MKPARTS_PACKAGE);
                if (resId > 0) {
                    loadPartsFromResourceLocked(r, resId, sParts);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // no mkparts installed
            }
        }
    }

    public static Set<String> getPartsList(Context context) {
        synchronized (sParts) {
            if (!sCatalogLoaded.get()) {
                loadParts(context);
            }
            return sParts.keySet();
        }
    }

    public static PartInfo getPartInfo(Context context, String key) {
        synchronized (sParts) {
            if (!sCatalogLoaded.get()) {
                loadParts(context);
            }
            return sParts.get(key);
        }
    }

    public static final PartInfo getPartInfoForClass(Context context, String clazz) {
        synchronized (sParts) {
            if (!sCatalogLoaded.get()) {
                loadParts(context);
            }
            for (PartInfo info : sParts.values()) {
                if (info.getFragmentClass() != null && info.getFragmentClass().equals(clazz)) {
                    return info;
                }
            }
            return null;
        }
    }

    private static void loadPartsFromResourceLocked(Resources res, int resid,
                                                    Map<String, PartInfo> target) {
        if (sCatalogLoaded.get()) {
            return;
        }

        XmlResourceParser parser = null;

        try {
            parser = res.getXml(resid);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String nodeName = parser.getName();
            if (!"parts-catalog".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <parts-catalog> tag; found"
                                + nodeName + " at " + parser.getPositionDescription());
            }

            final int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("part".equals(nodeName)) {
                    TypedArray sa = res.obtainAttributes(attrs, Preference);

                    String key = null;
                    TypedValue tv = sa.peekValue(Preference_key);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            key = res.getString(tv.resourceId);
                        } else {
                            key = String.valueOf(tv.string);
                        }
                    }
                    if (key == null) {
                        throw new RuntimeException("Attribute 'key' is required");
                    }

                    final PartInfo info = new PartInfo(key);

                    tv = sa.peekValue(Preference_title);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            info.setTitle(res.getString(tv.resourceId));
                        } else {
                            info.setTitle(String.valueOf(tv.string));
                        }
                    }

                    tv = sa.peekValue(Preference_summary);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            info.setSummary(res.getString(tv.resourceId));
                        } else {
                            info.setSummary(String.valueOf(tv.string));
                        }
                    }

                    info.setFragmentClass(sa.getString(Preference_fragment));
                    info.setIconRes(sa.getResourceId(Preference_icon, 0));

                    sa = res.obtainAttributes(attrs, mk_Searchable);
                    info.setXmlRes(sa.getResourceId(mk_Searchable_xmlRes, 0));

                    sa.recycle();

                    target.put(key, info);

                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } finally {
            if (parser != null) parser.close();
        }
        sCatalogLoaded.set(true);
    }
}