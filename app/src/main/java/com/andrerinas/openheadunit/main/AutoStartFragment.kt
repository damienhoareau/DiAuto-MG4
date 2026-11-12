package com.andrerinas.openheadunit.main

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.R
import com.andrerinas.openheadunit.main.settings.SettingItem
import com.andrerinas.openheadunit.main.settings.SettingsAdapter
import com.andrerinas.openheadunit.aap.AapService
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.AppPermissions
import com.andrerinas.openheadunit.utils.Settings
import com.andrerinas.openheadunit.utils.SystemUI
import com.andrerinas.openheadunit.utils.BluetoothHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AutoStartFragment : Fragment() {
    private lateinit var settings: Settings
    private lateinit var recyclerView: RecyclerView
    private lateinit var settingsAdapter: SettingsAdapter
    private lateinit var toolbar: MaterialToolbar
    private var saveButton: MaterialButton? = null

    private val pendingAutoStartBtMacs = mutableSetOf<String>()

    private var hasChanges = false
    private val SAVE_ITEM_ID = 1001

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showBluetoothDeviceSelector()
        } else {
            showBluetoothPermissionDeniedDialog()
        }
    }

    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            showBluetoothDeviceSelector()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_auto_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settings = App.provide(requireContext()).settings

        pendingAutoStartBtMacs.clear()
        pendingAutoStartBtMacs.addAll(settings.autoStartBluetoothDeviceMacs)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        toolbar = view.findViewById(R.id.toolbar)
        settingsAdapter = SettingsAdapter()
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = settingsAdapter

        updateSettingsList()
        setupToolbar()
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            handleBackPress()
        }

        val saveItem = toolbar.menu.add(0, SAVE_ITEM_ID, 0, getString(R.string.save))
        saveItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        saveItem.setActionView(R.layout.layout_save_button)

        saveButton = saveItem.actionView?.findViewById(R.id.save_button_widget)
        saveButton?.setOnClickListener {
            saveSettings()
        }

        updateSaveButtonState()
    }

    private fun handleBackPress() {
        if (hasChanges) {
            MaterialAlertDialogBuilder(requireContext(), R.style.DarkAlertDialog)
                .setTitle(R.string.unsaved_changes)
                .setMessage(R.string.unsaved_changes_message)
                .setPositiveButton(R.string.discard) { _, _ ->
                    navigateBack()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            navigateBack()
        }
    }

    private fun navigateBack() {
        try {
            val navController = findNavController()
            if (!navController.navigateUp()) {
                requireActivity().finish()
            }
        } catch (e: Exception) {
            requireActivity().finish()
        }
    }

    private fun updateSaveButtonState() {
        saveButton?.isEnabled = hasChanges
        saveButton?.text = getString(R.string.save)
    }

    private fun saveSettings() {
        settings.autoStartBluetoothDeviceMacs = pendingAutoStartBtMacs
        settings.autoStartBluetoothDevicesChosen = true
        Settings.syncAutoStartBtDevicesChosenToDeviceStorage(requireContext(), true)
        Settings.syncAutoStartBtMacsToDeviceStorage(requireContext(), pendingAutoStartBtMacs)
        if (pendingAutoStartBtMacs.isNotEmpty()) {
            val firstMac = pendingAutoStartBtMacs.first()
            val adapter = BluetoothHelper.getBluetoothAdapter(requireContext())
            val hasBtConnectPermission = if (Build.VERSION.SDK_INT >= 31) {
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
            var name = ""
            if (hasBtConnectPermission && adapter?.isEnabled == true) {
                try {
                    val device = adapter.getRemoteDevice(firstMac)
                    name = device.name ?: ""
                } catch (e: Exception) {}
            }
            if (name.isEmpty()) {
                name = if (settings.autoStartBluetoothDeviceMac == firstMac) {
                    settings.autoStartBluetoothDeviceName
                } else {
                    "Unknown Device"
                }
            }
            settings.autoStartBluetoothDeviceName = name
        } else {
            settings.autoStartBluetoothDeviceName = ""
        }
        // Check for Overlay permission if any auto-start is configured
        if (pendingAutoStartBtMacs.isNotEmpty()) {
            if (!AppPermissions.isOverlayGranted(requireContext())) {
                MaterialAlertDialogBuilder(requireContext(), R.style.DarkAlertDialog)
                    .setTitle(R.string.overlay_permission_title)
                    .setMessage(R.string.overlay_permission_description)
                    .setPositiveButton(R.string.open_settings) { _, _ ->
                        try {
                            val intent = Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${requireContext().packageName}")
                            )
                            startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                            } catch (e2: Exception) {
                                try {
                                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${requireContext().packageName}")
                                    })
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }

        hasChanges = false
        updateSaveButtonState()

        Toast.makeText(context, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
    }

    private fun checkChanges() {
        hasChanges = pendingAutoStartBtMacs != settings.autoStartBluetoothDeviceMacs

        updateSaveButtonState()
    }

    private fun updateSettingsList() {
        val scrollState = recyclerView.layoutManager?.onSaveInstanceState()
        val items = mutableListOf<SettingItem>()

        items.add(SettingItem.CategoryHeader("autoStart", R.string.auto_start_settings))

        items.add(SettingItem.SettingEntry(
            stableId = "autoStartBt",
            nameResId = R.string.auto_start_bt_label,
            value = getBluetoothSummaryText(),
            onClick = {
                showBluetoothDeviceSelector()
            }
        ))

        // Hide options that do not apply to the chosen connection types. Bluetooth bridges
        // WiFi, so it is treated as WiFi scope.
        val filtered = items.filterNot { it.stableId == "autoStartBt" && !settings.showsWifi() }

        settingsAdapter.submitList(filtered) {
            scrollState?.let { recyclerView.layoutManager?.onRestoreInstanceState(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check overlay permission. If the user returned from system settings
        // without granting it, disable auto-start settings that require it.
        if (!AppPermissions.isOverlayGranted(requireContext())) {
            var disabled = false
            if (settings.autoStartBluetoothDeviceMacs.isNotEmpty()) {
                settings.autoStartBluetoothDeviceMacs = emptySet()
                settings.autoStartBluetoothDeviceName = ""
                Settings.syncAutoStartBtMacsToDeviceStorage(requireContext(), emptySet())
                pendingAutoStartBtMacs.clear()
                disabled = true
            }
            if (disabled) {
                AppLog.w("Overlay permission not granted, disabling auto-start settings")
                Toast.makeText(requireContext(), getString(R.string.overlay_permission_denied_auto_start_disabled), Toast.LENGTH_LONG).show()
                checkChanges()
                updateSettingsList()
            }
        }
    }

    private fun showBluetoothDeviceSelector() {
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
            return
        }

        val adapter = BluetoothHelper.getBluetoothAdapter(requireContext())

        if (adapter == null || !adapter.isEnabled) {
            val enableIntent = Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableIntent)
            return
        }

        val bondedDevices = adapter.bondedDevices.toList()

        if (bondedDevices.isEmpty()) {
            Toast.makeText(requireContext(), "No paired Bluetooth devices found", Toast.LENGTH_LONG).show()
            return
        }

        val deviceNames = bondedDevices.map { device ->
            val hardwareName = device.name ?: "Unknown Device"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val alias = device.alias
                if (!alias.isNullOrEmpty() && alias != hardwareName) {
                    "$alias ($hardwareName)"
                } else {
                    alias ?: hardwareName
                }
            } else {
                hardwareName
            }
        }.toTypedArray()

        val checkedItems = bondedDevices.map { pendingAutoStartBtMacs.contains(it.address) }.toBooleanArray()
        val selectedMacs = pendingAutoStartBtMacs.toMutableSet()

        MaterialAlertDialogBuilder(requireContext(), R.style.DarkAlertDialog)
            .setTitle(R.string.select_bt_device)
            .setMultiChoiceItems(deviceNames, checkedItems) { _, which, isChecked ->
                val device = bondedDevices[which]
                if (isChecked) {
                    selectedMacs.add(device.address)
                } else {
                    selectedMacs.remove(device.address)
                }
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                pendingAutoStartBtMacs.clear()
                pendingAutoStartBtMacs.addAll(selectedMacs)
                checkChanges()
                updateSettingsList()
            }
            .setNeutralButton(R.string.remove) { _, _ ->
                pendingAutoStartBtMacs.clear()
                checkChanges()
                updateSettingsList()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also { dialog ->
                // A dialog is a window of its own: when it takes focus the car's side bar comes back over the
                // left of the screen. Shown without focus, with the same bar settings as the screen, it never does.
                val window = dialog.window ?: return@also dialog.show()
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                dialog.show()
                SystemUI.apply(window, window.decorView, settings.fullscreenMode)
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
    }

    private fun getBluetoothSummaryText(): String {
        if (pendingAutoStartBtMacs.isEmpty()) {
            return getString(R.string.bt_device_not_set)
        }

        val adapter = BluetoothHelper.getBluetoothAdapter(requireContext())
        val hasBtConnectPermission = if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        val bondedAddresses = if (adapter?.isEnabled == true && hasBtConnectPermission) {
            try {
                adapter.bondedDevices.map { it.address }.toSet()
            } catch (e: SecurityException) { null }
        } else null

        val validSelectedMacs = if (bondedAddresses != null) {
            pendingAutoStartBtMacs.filter { bondedAddresses.contains(it) }
        } else {
            pendingAutoStartBtMacs.toList()
        }

        if (validSelectedMacs.isEmpty()) {
            return getString(R.string.bt_device_not_set)
        }

        return if (validSelectedMacs.size == 1) {
            val mac = validSelectedMacs.first()
            if (hasBtConnectPermission && adapter?.isEnabled == true) {
                try {
                    val device = adapter.getRemoteDevice(mac)
                    val hardwareName = device.name ?: mac
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        device.alias ?: hardwareName
                    } else {
                        hardwareName
                    }
                } catch (e: Exception) {
                    mac
                }
            } else {
                if (settings.autoStartBluetoothDeviceMac == mac && settings.autoStartBluetoothDeviceName.isNotEmpty()) {
                    settings.autoStartBluetoothDeviceName
                } else {
                    mac
                }
            }
        } else {
            "${validSelectedMacs.size} ${getString(R.string.bt_devices_selected)}"
        }
    }

    private fun showBluetoothPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.DarkAlertDialog)
            .setTitle(R.string.bt_permission_denied_title)
            .setMessage(R.string.bt_permission_denied_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                try {
                    val intent = Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${requireContext().packageName}")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
