/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.data.providers

import android.content.Context
import android.util.Log
import com.aurora.store.BuildConfig
import com.aurora.store.util.PathUtil
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties
import javax.inject.Singleton

/**
 * Provider class to work with device spoof configs imported by users & shipped by GPlayAPI library
 *
 * Do not use this class directly. Consider using [SpoofProvider] instead.
 */
@Singleton
open class SpoofDeviceProvider(private val context: Context) {

    private val TAG = SpoofDeviceProvider::class.java.simpleName

    private val SUFFIX = ".properties"

    val availableDeviceProperties: MutableList<Properties>
        get() {
            val propertiesList: MutableList<Properties> = ArrayList()
            propertiesList.addAll(spoofDevicesFromApk)
            propertiesList.addAll(spoofDevicesFromUser)
            propertiesList.sortBy { it.getProperty("UserReadableName") }
            return propertiesList
        }

    private val spoofDevicesFromApk: List<Properties>
        get() {
            val propertiesList: MutableList<Properties> = ArrayList()
            try {
                val files = context.assets.list("spoofs")
                if (files != null) {
                    for (file in files) {
                        if (filenameValid(file)) {
                            val properties = Properties()
                            properties.load(context.assets.open("spoofs/$file"))
                            properties.setProperty("CONFIG_NAME", "spoofs/$file")
                            if (properties.getProperty("UserReadableName") != null) {
                                propertiesList.add(properties)
                            }
                        }
                    }
                }
            } catch (exception: IOException) {
                Log.e(TAG, "Could not read spoof files from assets", exception)
            }
            return propertiesList
        }

    private val spoofDevicesFromUser: List<Properties>
        get() {
            val deviceNames: MutableList<Properties> = ArrayList()
            val defaultDir = PathUtil.getSpoofDirectory(context)
            val files = defaultDir.listFiles()
            if (defaultDir.exists() && files != null) {
                for (file in files) {
                    if (!file.isFile || !filenameValid(file.name)) {
                        continue
                    }
                    deviceNames.add(getProperties(file))
                }
            }
            return deviceNames
        }

    private fun getProperties(file: File): Properties {
        val properties = Properties()
        try {
            properties.load(BufferedInputStream(FileInputStream(file)))
            properties.setProperty("CONFIG_NAME", file.name)
        } catch (exception: IOException) {
            Log.e(TAG, "Could not read ${file.name}", exception)
        }
        return properties
    }

    private fun filenameValid(filename: String): Boolean {
        return filename.endsWith(SUFFIX)
    }
}
