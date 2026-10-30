package com.andrerinas.openheadunit.main

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.andrerinas.openheadunit.R
import com.andrerinas.openheadunit.connection.SharedWifiNetwork
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.text.DateFormat
import java.util.Date

/** The networks the car stopped sending the phone to, each with a way to try it again. */
class SharedWifiFragment : Fragment(R.layout.fragment_shared_wifi) {

    override fun onViewCreated(view: View, state: Bundle?) {
        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().popBackStack() }
        showNetworks(view)
    }

    private fun showNetworks(view: View) {
        val context = requireContext()
        val list = view.findViewById<LinearLayout>(R.id.shared_wifi_list)
        val networks = SharedWifiNetwork.leftAlone(context)
        view.findViewById<View>(R.id.shared_wifi_empty).visibility = if (networks.isEmpty()) View.VISIBLE else View.GONE
        list.removeAllViews()
        val dates = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        val dp = resources.displayMetrics.density
        for ((ssid, at) in networks) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt())
                setBackgroundResource(R.drawable.da_card)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = (10 * dp).toInt() }
            }
            val text = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            text.addView(TextView(context).apply {
                this.text = ssid
                textSize = 20f
                setTextColor(context.getColor(R.color.da_text))
            })
            text.addView(TextView(context).apply {
                this.text = getString(R.string.shared_wifi_marked, dates.format(Date(at)))
                textSize = 15f
                setTextColor(context.getColor(R.color.da_muted))
            })
            row.addView(text)
            row.addView(MaterialButton(context).apply {
                this.text = getString(R.string.shared_wifi_allow_again)
                setOnClickListener {
                    SharedWifiNetwork.allowAgain(context, ssid)
                    showNetworks(view)
                }
            })
            list.addView(row)
        }
    }
}
