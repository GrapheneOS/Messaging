package com.android.messaging.ui

import com.android.messaging.ui.classzero.ClassZeroActivity
import com.android.messaging.ui.contact.AddContactActivity
import com.android.messaging.ui.conversation.LaunchConversationActivity
import com.android.messaging.ui.license.LicenseActivity
import com.android.messaging.ui.permissioncheck.PermissionCheckActivity
import com.android.messaging.ui.photoviewer.PhotoViewerActivity
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

internal class ActivityPermissionGateTest {

    private val applicationId = "com.android.messaging"

    private val gatingBases = setOf(
        BugleComponentActivity::class.java,
        BugleActionBarActivity::class.java,
        BaseBugleActivity::class.java,
        BaseBugleFragmentActivity::class.java,
    )

    private val intentionallyUngated = setOf<Class<*>>(
        PermissionCheckActivity::class.java,
        LaunchConversationActivity::class.java,
        PhotoViewerActivity::class.java,
        LicenseActivity::class.java,
        TestActivity::class.java,
        ClassZeroActivity::class.java,
        AddContactActivity::class.java,
    )

    @Test
    fun everyManifestActivityIsPermissionGated() {
        val activityNames = manifestActivityNames()

        assertTrue(
            "No <activity> entries parsed from the manifest; the gate test is not exercising anything",
            activityNames.isNotEmpty(),
        )

        val offenders = activityNames
            .map { name -> Class.forName(name) }
            .filterNot { activityClass -> activityClass in intentionallyUngated }
            .filterNot { activityClass -> isGated(activityClass) }
            .map { it.name }

        assertTrue(
            "Activities not extending a permission-gated base. Extend BugleComponentActivity, " +
                "or add to the allowlist in this test with a reason: $offenders",
            offenders.isEmpty(),
        )
    }

    private fun isGated(activityClass: Class<*>): Boolean {
        return gatingBases.any { base -> base.isAssignableFrom(activityClass) }
    }

    private fun manifestActivityNames(): List<String> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(manifestFile())

        val activities = document.getElementsByTagName("activity")
        return (0 until activities.length)
            .map { index -> activities.item(index) as Element }
            .map { element -> element.getAttribute("android:name") }
            .filter { name -> name.isNotEmpty() }
            .map { name -> resolveClassName(name) }
    }

    private fun resolveClassName(name: String): String {
        return when {
            name.startsWith(".") -> applicationId + name
            !name.contains(".") -> "$applicationId.$name"
            else -> name
        }
    }

    private fun manifestFile(): File {
        val workingDir = requireNotNull(System.getProperty("user.dir"))
        var directory: File? = File(workingDir)

        while (directory != null) {
            val candidate = File(directory, "AndroidManifest.xml")
            if (candidate.exists()) {
                return candidate
            }
            directory = directory.parentFile
        }

        error("Could not locate AndroidManifest.xml from $workingDir")
    }
}
