package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun appName_matchesMirPayShortcut() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Mir Pay Shortcut", context.getString(R.string.app_name))
  }

  @Test
  fun manifest_hasNoPermissionsAndNoLauncherIconAndHandlesAssist() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pm = context.packageManager

    val packageInfo =
      pm.getPackageInfo(
        context.packageName,
        PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS,
      )
    assertTrue(
      "App should request zero permissions",
      packageInfo.requestedPermissions.isNullOrEmpty(),
    )
    assertTrue("App should declare zero background services", packageInfo.services.isNullOrEmpty())
    assertTrue("App should declare zero receivers", packageInfo.receivers.isNullOrEmpty())

    val launcherIntent =
      Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setPackage(context.packageName)
    assertTrue(
      "App should have no launcher icon",
      pm.queryIntentActivities(launcherIntent, 0).isEmpty(),
    )

    val assistIntent =
      Intent(Intent.ACTION_ASSIST)
        .addCategory(Intent.CATEGORY_DEFAULT)
        .setPackage(context.packageName)
    val assistActivities = pm.queryIntentActivities(assistIntent, 0)
    assertEquals(1, assistActivities.size)
    assertEquals(AssistRedirectActivity::class.java.name, assistActivities.first().activityInfo.name)
  }

  @Test
  fun assistRedirect_launchesMirPayWatchAndFinishesImmediately() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val shadowPm = shadowOf(context.packageManager)

    val targetComponent =
      ComponentName(AssistRedirectActivity.TARGET_PACKAGE, "ru.nspk.mirpay.wear.MainActivity")
    val activityInfo = shadowPm.addActivityIfNotPresent(targetComponent)
    activityInfo.exported = true
    shadowPm.addIntentFilterForActivity(
      targetComponent,
      IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
    )

    val controller =
      Robolectric.buildActivity(AssistRedirectActivity::class.java, Intent(Intent.ACTION_ASSIST))
        .create()
    val activity = controller.get()

    val startedIntent = shadowOf(activity).nextStartedActivity
    assertNotNull("Mir Pay Watch launch intent should be started", startedIntent)
    assertEquals(AssistRedirectActivity.TARGET_PACKAGE, startedIntent.component?.packageName)
    assertTrue(
      (startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) == Intent.FLAG_ACTIVITY_NEW_TASK
    )
    assertTrue(
      (startedIntent.flags and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) ==
        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
    )
    assertTrue("Redirect activity must finish immediately", activity.isFinishing)
  }

  @Test
  fun assistRedirect_whenMirPayMissing_finishesSafelyWithToast() {
    val controller =
      Robolectric.buildActivity(AssistRedirectActivity::class.java, Intent(Intent.ACTION_ASSIST))
        .create()
    val activity = controller.get()

    assertTrue("Redirect activity must finish immediately even when target is missing", activity.isFinishing)
    assertEquals(
      activity.getString(R.string.mir_pay_not_installed),
      ShadowToast.getTextOfLatestToast(),
    )
  }
}
