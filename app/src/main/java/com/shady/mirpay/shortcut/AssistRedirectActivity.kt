package com.shady.mirpay.shortcut

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Headless Digital Assistant trampoline for Wear OS / Pixel Watch.
 *
 * Handles system assistant triggers without any UI, settings, permissions, or background services,
 * immediately launching Mir Pay Watch and finishing itself to keep battery and memory impact at
 * zero.
 */
class AssistRedirectActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    launchMirPayWatch()
    finish()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    launchMirPayWatch()
    finish()
  }

  private fun launchMirPayWatch() {
    val launchIntent = resolveTargetLaunchIntent()
    if (launchIntent == null) {
      Toast.makeText(applicationContext, R.string.mir_pay_not_installed, Toast.LENGTH_SHORT).show()
      return
    }

    launchIntent.addFlags(
      Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
    )

    try {
      startActivity(launchIntent)
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(applicationContext, R.string.mir_pay_not_installed, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
      Toast.makeText(applicationContext, R.string.mir_pay_launch_failed, Toast.LENGTH_SHORT).show()
    }
  }

  private fun resolveTargetLaunchIntent(): Intent? {
    packageManager.getLaunchIntentForPackage(TARGET_PACKAGE)?.let { return it }

    val fallbackIntent = Intent(Intent.ACTION_MAIN).setPackage(TARGET_PACKAGE)
    val resolveInfo = packageManager.resolveActivity(fallbackIntent, 0) ?: return null
    val activityInfo = resolveInfo.activityInfo ?: return null
    if (!activityInfo.exported) return null

    return Intent(Intent.ACTION_MAIN).setClassName(activityInfo.packageName, activityInfo.name)
  }

  companion object {
    const val TARGET_PACKAGE = "ru.nspk.mirpay.wear"
  }
}
