package com.aurora.store.view.ui.splash

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aurora.Constants.PACKAGE_NAME_PLAY_STORE
import com.aurora.extensions.getPackageName
import com.aurora.extensions.navigate
import com.aurora.store.R
import com.aurora.store.compose.navigation.Screen
import com.aurora.store.data.model.AuthState
import com.aurora.store.databinding.FragmentSplashBinding
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
import com.aurora.store.util.Preferences.PREFERENCE_INTRO
import com.aurora.store.util.Preferences.PREFERENCE_MICROG_AUTH
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.view.ui.commons.BaseFragment
import com.aurora.store.view.ui.sheets.PasscodeDialogSheet
import com.aurora.store.viewmodel.auth.AuthViewModel
import com.aurora.store.util.PasscodeUtil
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseFlavouredSplashFragment : BaseFragment<FragmentSplashBinding>() {

    private val TAG = BaseFlavouredSplashFragment::class.java.simpleName

    val viewModel: AuthViewModel by activityViewModels()

    @Inject
    lateinit var spoofProvider: SpoofProvider

    private var restartRequired = false
    protected var isManualLogin = false

    val canLoginWithMicroG: Boolean
        get() = PackageUtil.hasSupportedMicroGVariant(requireContext()) &&
                Preferences.getBoolean(requireContext(), PREFERENCE_MICROG_AUTH, true)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!Preferences.getBoolean(requireContext(), PREFERENCE_INTRO)) {
            findNavController().navigate(
                SplashFragmentDirections.actionSplashFragmentToOnboardingFragment()
            )
            return
        }

        // Toolbar
        binding.toolbar.apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_blacklist_manager -> {
                        handleBlacklistAccess()
                    }

                    R.id.menu_spoof_manager -> {
                        findNavController().navigate(R.id.spoofFragment)
                    }

                    R.id.menu_settings -> {
                        findNavController().navigate(R.id.settingsFragment)
                    }

                    R.id.menu_about -> requireContext().navigate(Screen.About)
                }
                true
            }
        }

        attachActions()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collectLatest {
                when (it) {
                    AuthState.Init -> updateStatus(getString(R.string.session_init))

                    AuthState.Fetching -> {
                        updateStatus(getString(R.string.requesting_new_session))
                    }

                    AuthState.Valid -> {
                        if (isManualLogin) {
                            // Manual login successful - show restart button
                            updateStatus(getString(R.string.session_login) + " - Restart Required")
                            showRestartButton()
                        } else {
                            // Auto login - navigate normally
                            val packageName =
                                requireActivity().intent.getPackageName(requireArguments())
                            if (!packageName.isNullOrBlank()) {
                                requireArguments().remove("packageName")
                            }
                            navigateToDefaultTab()
                        }
                    }

                    AuthState.Available -> {
                        updateStatus(getString(R.string.session_verifying))
                        updateActionLayout(false)
                    }

                    AuthState.Unavailable -> {
                        updateStatus(getString(R.string.session_login))
                        updateActionLayout(true)
                    }

                    AuthState.SignedIn -> {
                        if (isManualLogin) {
                            // Manual login successful - show restart button
                            updateStatus("Login Successful - Restart Required")
                            showRestartButton()
                        } else {
                            // Auto login - navigate normally
                            val packageName =
                                requireActivity().intent.getPackageName(requireArguments())
                            if (!packageName.isNullOrBlank()) {
                                requireArguments().remove("packageName")
                            }
                            navigateToDefaultTab()
                        }
                    }

                    AuthState.SignedOut -> {
                        updateStatus(getString(R.string.session_scrapped))
                        updateActionLayout(true)
                    }

                    AuthState.Verifying -> {
                        updateStatus(getString(R.string.verifying_new_session))
                    }

                    is AuthState.PendingAccountManager -> {
                        // Google authentication not supported, treat as failed
                        updateStatus(getString(R.string.session_login))
                        updateActionLayout(true)
                        resetActions()
                    }

                    is AuthState.Failed -> {
                        updateStatus(it.status)
                        updateActionLayout(true)
                        resetActions()
                    }
                }
            }
        }
    }

    private fun updateStatus(string: String?) {
        activity?.runOnUiThread { binding.txtStatus.text = string }
    }

    private fun updateActionLayout(isVisible: Boolean) {
        binding.layoutAction.isVisible = isVisible
        binding.toolbar.isVisible = isVisible
    }

    private fun navigateToDefaultTab() {
        val defaultDestination =
            Preferences.getInteger(requireContext(), PREFERENCE_DEFAULT_SELECTED_TAB)
        val directions =
            when (requireArguments().getInt("destinationId", defaultDestination)) {
                R.id.updatesFragment -> {
                    requireArguments().remove("destinationId")
                    SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                }

                1 -> SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                2 -> SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
                else -> SplashFragmentDirections.actionSplashFragmentToUpdatesFragment()
            }
        requireActivity().viewModelStore.clear() // Clear ViewModelStore to avoid bugs with logout
        findNavController().navigate(directions)
    }


    open fun attachActions() {
        binding.btnAnonymous.addOnClickListener {
            if (restartRequired) {
                // Restart the app
                ProcessPhoenix.triggerRebirth(requireContext())
            } else if (viewModel.authState.value != AuthState.Fetching) {
                isManualLogin = true  // Mark this as a manual login
                binding.btnAnonymous.updateProgress(true)

                // Post to next frame to allow progress indicator to render
                binding.btnAnonymous.post {
                    // Automatically set device spoof to reloaded_beryllium before logging in
                    setReloadedBerylliumSpoof()

                    viewModel.buildAnonymousAuthData()
                }
            }
        }

        binding.btnSpoofManager?.addOnClickListener {
            findNavController().navigate(R.id.spoofFragment)
        }
    }

    open fun resetActions() {
        binding.btnAnonymous.apply {
            updateProgress(false)
            isEnabled = true
        }
    }

    private fun handleBlacklistAccess() {
        if (PasscodeUtil.hasBlacklistPassword(requireContext())) {
            // Password is set, show verification dialog
            val passwordDialog = PasscodeDialogSheet.newInstanceForVerify()
            passwordDialog.show(childFragmentManager, PasscodeDialogSheet.TAG)
        } else {
            // No password set, navigate directly
            requireContext().navigate(Screen.Blacklist)
        }
    }

    protected fun setReloadedBerylliumSpoof() {
        try {
            // Find reloaded_beryllium device from available devices
            val availableDevices = spoofProvider.availableSpoofDeviceProperties
            val reloadedBeryllium = availableDevices.find { properties ->
                val name = properties.getProperty("UserReadableName", "")
                name.contains("reloaded", ignoreCase = true) &&
                name.contains("beryllium", ignoreCase = true)
            }

            reloadedBeryllium?.let { properties ->
                Log.i(TAG, "Setting device spoof to: ${properties.getProperty("UserReadableName")}")
                spoofProvider.setSpoofDeviceProperties(properties)
            } ?: run {
                Log.w(TAG, "reloaded_beryllium device not found in available devices")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set reloaded_beryllium spoof", e)
        }
    }

    private fun showRestartButton() {
        activity?.runOnUiThread {
            restartRequired = true
            binding.btnAnonymous.updateProgress(false)

            // Access the internal button and change its text
            val btnView = binding.btnAnonymous.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn)
            btnView?.text = "Restart App"
            btnView?.setIconResource(R.drawable.sync)
        }
    }
}
