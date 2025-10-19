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

package com.aurora.store.view.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aurora.Constants
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.Installer
import androidx.core.content.edit
import com.aurora.store.data.providers.SpoofProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Properties
import com.aurora.store.util.Preferences.PREFERENCE_AUTO_DELETE
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_DISPENSER_URLS
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_AURORA_ONLY
import com.aurora.store.util.Preferences.PREFERENCE_FILTER_FDROID
import com.aurora.store.util.Preferences.PREFERENCE_FOR_YOU
import com.aurora.store.util.Preferences.PREFERENCE_INSTALLER_ID
import com.aurora.store.util.Preferences.PREFERENCE_THEME_STYLE
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL
import com.aurora.store.util.Preferences.PREFERENCE_UPDATES_EXTENDED
import com.aurora.store.util.Preferences.PREFERENCE_VENDING_VERSION
import com.aurora.store.util.save
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingFragment : BaseFlavouredOnboardingFragment() {

    @Inject
    lateinit var spoofProvider: SpoofProvider

    @Inject
    lateinit var json: Json

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Immediately finish onboarding and skip to splash screen
        view.post {
            loadDefaultPreferences()
            finishOnboarding()
        }
    }

    override fun loadDefaultPreferences() {
        /*Filters*/
        save(PREFERENCE_FILTER_AURORA_ONLY, false)
        save(PREFERENCE_FILTER_FDROID, true)

        /*Network*/
        save(PREFERENCE_DISPENSER_URLS, setOf(Constants.URL_DISPENSER))
        save(PREFERENCE_VENDING_VERSION, 0)

        /*Customization*/
        save(PREFERENCE_THEME_STYLE, 0)
        save(PREFERENCE_DEFAULT_SELECTED_TAB, 0)
        save(PREFERENCE_FOR_YOU, true)

        /*Device Spoof - Default to reloaded_beryllium for better compatibility*/
        setDefaultDeviceSpoof()

        /*Installer*/
        save(PREFERENCE_AUTO_DELETE, true)

        // Smart installer selection: prefer Aurora Services, then ROOT, then SESSION
        val installerId = when {
            AppInstaller.hasAuroraService(requireContext()) -> Installer.SERVICE.ordinal
            AppInstaller.hasRootAccess() -> Installer.ROOT.ordinal
            else -> Installer.SESSION.ordinal
        }
        save(PREFERENCE_INSTALLER_ID, installerId)

        /*Updates*/
        save(PREFERENCE_UPDATES_EXTENDED, false)
        save(PREFERENCE_UPDATES_CHECK_INTERVAL, 3)
    }

    private fun setDefaultDeviceSpoof() {
        // Find and set reloaded_beryllium as default device spoof
        val availableDevices = spoofProvider.availableSpoofDeviceProperties
        val berylliumDevice = availableDevices.find { properties ->
            val name = properties.getProperty("UserReadableName", "")
            // Just search for "beryllium" in the UserReadableName
            name.contains("beryllium", ignoreCase = true)
        }

        berylliumDevice?.let { properties ->
            // Write spoof synchronously to ensure it's available immediately
            val prefs = requireContext().getSharedPreferences(requireContext().packageName + "_preferences", android.content.Context.MODE_PRIVATE)
            prefs.edit(true) {
                putBoolean("DEVICE_SPOOF_ENABLED", true)
                putString("DEVICE_SPOOF_PROPERTIES", json.encodeToString(properties))
            }
        }
    }

    override fun onboardingPages(): List<Fragment> {
        // Return empty list to skip all onboarding screens
        return emptyList()
    }

    override fun setupAutoUpdates() {
        super.setupAutoUpdates()

        // Remove super & implement variant logic here
    }

    override fun finishOnboarding() {
        // Skip onboarding screens and go straight to app
        setupAutoUpdates()
        com.aurora.store.data.work.CacheWorker.scheduleAutomatedCacheCleanup(requireContext())
        com.aurora.store.util.Preferences.putBooleanNow(requireContext(), com.aurora.store.util.Preferences.PREFERENCE_INTRO, true)

        // Don't restart - just navigate to main screen
        findNavController().navigate(
            com.aurora.store.view.ui.onboarding.OnboardingFragmentDirections.actionOnboardingFragmentToSplashFragment()
        )
    }
}
