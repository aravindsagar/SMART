package com.cs565project.smart.fragments


import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.cs565project.smart.R
import com.cs565project.smart.util.PreferencesHelper.setPreference
import com.cs565project.smart.util.UsageStatsUtil


/**
 * Fragment to request necessary permissions from the user during onboarding.
 */
class PermissionsFragment : Fragment() {
    private var v: View? = null

    private val usageAccessListener = View.OnClickListener {
        var intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (intent.resolveActivity(it.context.packageManager) != null) {
            startActivity(intent)
        } else {
            intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            intent.component = ComponentName("com.android.settings",
                    "com.android.settings.Settings\$SecuritySettingsActivity")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        setPreference(it.context, KEY_CHECK_USAGE_ACCESS_ON_RESUME, true)
    }

    private val overlayAccessListener = View.OnClickListener {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + it.context.packageName))
        startActivityForResult(intent, 10)
    }

    private val cameraAccessListener = View.OnClickListener {
        if (activity == null) return@OnClickListener
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(activity!!,
                    arrayOf(Manifest.permission.CAMERA),
                    MY_PERMISSIONS_REQUEST_EXTERNAL_FILE)
        }
    }

    private val externalStorageAccessListener = View.OnClickListener {
        if (activity == null) return@OnClickListener
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(activity!!,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_CAMERA)
        }
        //            TextView tv4 = v.findViewById(R.id.externalStorageEnabledText);
        //            tv4.setText(externalStorageEnabled());
    }

    private fun externalStorageEnabled(): String {
        if (activity == null) return ""
        return if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            "Not Enabled"
        else
            "Enabled"
    }

    private fun cameraEnabled(): String {
        if (activity == null) return ""
        return if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            "Not Enabled"
        else
            "Enabled"
    }

    private fun usageAccessEnabled(): String {
        if (activity == null) return ""
        return if (!UsageStatsUtil.hasUsageAccess(activity!!)) "Not Enabled" else "Enabled"
    }

    private fun overlayAccessEnabled(): String {
        return if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) "Not Enabled" else "Enabled"
    }


    override fun onResume() {
        super.onResume()
        val tv1 = v!!.findViewById<TextView>(R.id.appMonitoringEnabledText)
        val tv2 = v!!.findViewById<TextView>(R.id.overlayEnabledText)
        val tv3 = v!!.findViewById<TextView>(R.id.cameraEnabledText)
        val tv4 = v!!.findViewById<TextView>(R.id.externalStorageEnabledText)

        tv1.text = usageAccessEnabled()
        tv2.text = overlayAccessEnabled()
        tv3.text = cameraEnabled()
        tv4.text = externalStorageEnabled()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_permissions, container, false)
        val usageAccessButton = v!!.findViewById<Button>(R.id.usageEnableButton)
        val overlayAccessButton = v!!.findViewById<Button>(R.id.overlayEnableButton)
        val cameraAccessButton = v!!.findViewById<Button>(R.id.cameraEnableButton)
        val externalStorageAccessButton = v!!.findViewById<Button>(R.id.externalStorageEnableButton)
        usageAccessButton.setOnClickListener(usageAccessListener)
        overlayAccessButton.setOnClickListener(overlayAccessListener)
        cameraAccessButton.setOnClickListener(cameraAccessListener)
        externalStorageAccessButton.setOnClickListener(externalStorageAccessListener)

        return v
    }

    companion object {
        private val KEY_CHECK_USAGE_ACCESS_ON_RESUME = "check_usage_access_on_resume"
        val MY_PERMISSIONS_REQUEST_CAMERA = 100
        val MY_PERMISSIONS_REQUEST_EXTERNAL_FILE = 101
    }
}// Required empty public constructor
