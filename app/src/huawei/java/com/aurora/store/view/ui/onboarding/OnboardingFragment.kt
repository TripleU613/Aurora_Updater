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

import androidx.fragment.app.Fragment
import com.aurora.extensions.isHuawei
import com.aurora.store.data.installer.AppInstaller
import com.aurora.store.data.model.Installer
import com.aurora.store.data.providers.BlacklistProvider
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.util.PackageUtil
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
    lateinit var blacklistProvider: BlacklistProvider

    @Inject
    lateinit var spoofProvider: SpoofProvider

    override fun loadDefaultPreferences() {
        /*Filters*/
        save(PREFERENCE_FILTER_AURORA_ONLY, false)
        save(PREFERENCE_FILTER_FDROID, true)

        /*Network*/
        save(PREFERENCE_DISPENSER_URLS, emptySet())
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
            name.contains("beryllium", ignoreCase = true) &&
            name.contains("xiaomi", ignoreCase = true)
        }

        berylliumDevice?.let {
            spoofProvider.setSpoofDeviceProperties(it)
        }
    }

    override fun onboardingPages(): List<Fragment> {
        var pages = mutableListOf(
            WelcomeFragment(),
            PermissionsFragment.newInstance()
        )

        /**
         * MicroG Fragment Preconditions:
         * 1. It should be a Huawei device
         * 2. Supported App Gallery should be available, i.e. v15.1.x or above
         * 3. MicroG bundle should not be already installed
         */
        if (
            isHuawei &&
            PackageUtil.hasSupportedAppGallery(requireContext()) &&
            !PackageUtil.isMicroGBundleInstalled(requireContext())
        ) {
            pages.add(MicroGFragment())
        }

        return pages
    }

    override fun setupAutoUpdates() {
        super.setupAutoUpdates()

        // Remove super & implement variant logic here
    }

    override fun finishOnboarding() {
        blacklistProvider.blacklist("com.android.vending")
        blacklistProvider.blacklist("com.google.android.gms")

        super.finishOnboarding()
    }
}
