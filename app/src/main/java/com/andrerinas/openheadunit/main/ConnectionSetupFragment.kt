package com.andrerinas.openheadunit.main

import android.content.Intent
import android.os.Bundle
import android.provider.Settings as SystemSettings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.andrerinas.openheadunit.R
import com.andrerinas.openheadunit.utils.Settings
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/** A guided entry point to wireless Android Auto: Wi-Fi is automatic, only pairing is left to the user. */
class ConnectionSetupFragment : Fragment() {
    private val settings get() = Settings(requireContext())
    private var content: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        ScrollView(requireContext()).apply {
            setBackgroundColor(color(R.color.da_background))
            isFillViewport = true
            content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(32), dp(20), dp(32), dp(32))
            }
            addView(content)
        }

    override fun onResume() {
        super.onResume()
        // The car hotspot is the only wireless route, and DiAuto drives it by itself.
        settings.wifiConnectionMode = 3
        render()
    }
    override fun onDestroyView() { content = null; super.onDestroyView() }

    private fun render() {
        val root = content ?: return
        val scroll = root.parent as? ScrollView
        val previousScroll = scroll?.scrollY ?: 0
        root.removeAllViews()
        root.addView(MaterialToolbar(requireContext()).apply {
            title = getString(R.string.da_connection_setup)
            setTitleTextColor(color(R.color.da_text))
            setNavigationIcon(R.drawable.ic_arrow_back_white)
            setNavigationOnClickListener { findNavController().popBackStack() }
        })
        text(root, getString(R.string.setup_intro), 28)
        card(root, getString(R.string.setup_wifi_automatic)) { box ->
            text(box, getString(R.string.setup_wifi_automatic_hint), 16)
        }
        card(root, getString(R.string.setup_pair_and_connect)) { box ->
            text(box, getString(R.string.setup_pair_instructions), 16)
            button(box, getString(R.string.setup_open_bluetooth_settings)) { openSystem(Intent(SystemSettings.ACTION_BLUETOOTH_SETTINGS)) }
            button(box, getString(R.string.setup_review_app_permissions)) { findNavController().navigate(R.id.permissionsFragment) }
            button(box, getString(R.string.setup_done_return_to_diauto)) { requireActivity().finish() }
        }
        card(root, getString(R.string.setup_prefer_a_cable)) { box ->
            text(box, getString(R.string.setup_usb_instructions), 16)
        }
        scroll?.post { scroll.scrollTo(0, previousScroll) }
    }

    private fun openSystem(intent: Intent) {
        if (requireContext().packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName == "com.byd.carsettings") {
            runCatching { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)) }
        }
        runCatching { startActivity(intent) }.onFailure {
            runCatching { startActivity(Intent(SystemSettings.ACTION_SETTINGS)) }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.setup_open_settings_manually), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun card(parent: LinearLayout, title: String, body: (LinearLayout) -> Unit) {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
            setBackgroundResource(R.drawable.da_card)
        }
        parent.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })
        text(box, title, 22)
        body(box)
    }
    private fun text(parent: LinearLayout, value: String, size: Int) {
        parent.addView(TextView(requireContext()).apply {
            text = value; textSize = size.toFloat()
            setTextColor(color(if (size >= 22) R.color.da_text else R.color.da_muted))
            setPadding(0, dp(8), 0, dp(12))
        })
    }
    private fun button(parent: LinearLayout, title: String, selected: Boolean = false, action: () -> Unit): MaterialButton =
        MaterialButton(requireContext()).apply {
            text = title; isAllCaps = false; textSize = 18f; minHeight = dp(60); cornerRadius = dp(16)
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(if (selected) R.color.da_accent else R.color.da_surface))
            setTextColor(color(if (selected) R.color.da_background else R.color.da_text))
            strokeWidth = dp(1)
            strokeColor = android.content.res.ColorStateList.valueOf(color(if (selected) R.color.da_accent else R.color.da_muted))
            setOnClickListener { action() }
            parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(requireContext(), id)
}
