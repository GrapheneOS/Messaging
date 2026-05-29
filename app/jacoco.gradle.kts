import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

val unitTestIncludedPackages: List<String> = listOf(
    "com/android/messaging/data/**",
    "com/android/messaging/domain/**",
    "com/android/messaging/sms/**",
    "com/android/messaging/ui/appsettings/**",
    "com/android/messaging/ui/contact/**",
    "com/android/messaging/ui/conversation/**",
    "com/android/messaging/ui/core/**",
    "com/android/messaging/util/core/**",
    "com/android/messaging/util/db/**",
)

val androidTestIncludedPackages: List<String> = listOf(
    "com/android/messaging/data/appsettings/**",
    "com/android/messaging/domain/subscriptionsettings/**",
    "com/android/messaging/ui/appsettings/**",
    "com/android/messaging/ui/contact/**",
    "com/android/messaging/ui/conversation/**",
    "com/android/messaging/ui/core/**",
)

val coverageExcludedClasses: List<String> = listOf(
    "**/_Factory*",
    "**/Hilt_*",
    "**/_HiltModules*",
    "**/_GeneratedInjector*",
    "**/_MembersInjector*",
    "**/Dagger*",
    "**/*ComposableSingletons*",
    $$"**/*$serializer",
    "**/model/**",
    $$$"**/*$$inlined$*",
    $$"**/*$invokeSuspend$*",
    $$"**/*$DefaultImpls*",
) + (0..9).flatMap { i ->
    listOf(
        $$"**/*$$${i}.class",
        $$"**/*$?$${i}.class",
        $$"**/*$??$${i}.class",
    )
}

val unitTestExcludedClasses: List<String> = listOf(
    "**/*Exception.class",
    "**/*Exception$*.class",
    "**/*Activity.class",
    "**/*Activity$*.class",
    "**/ExceptionsKt.class",
    "**/*Fragment.class",
    "**/*Fragment$*.class",
    "**/*Receiver.class",
    "**/*Receiver$*.class",
    "**/*Service.class",
    "**/*Service$*.class",
    "**/data/appsettings/repository/AppSettingsRepositoryImpl.class",
    "**/domain/subscriptionsettings/usecase/SetSubscriptionPhoneNumberImpl.class",
    "**/ui/appsettings/general/mapper/AppSettingsUiStateMapperImpl.class",
    "**/ui/appsettings/subscription/mapper/SubscriptionSettingsUiStateMapperImpl.class",
    "**/ui/conversation/ConversationTestTagsKt.class",
    "**/ui/conversation/composer/ui/*.class",
    "**/ui/conversation/mediapicker/camera/ConversationCameraControllerImpl*.class",
    "**/ui/conversation/mediapicker/component/capture/ConversationMediaCaptureRecordingStopVisualState.class",
    "**/ui/conversation/mediapicker/component/capture/ConversationMediaCaptureShutterPhase.class",
    "**/ui/conversation/mediapicker/component/capture/ConversationMediaCaptureShutterSurfaceVisualState.class",
    "**/ui/conversation/mediapicker/component/capture/ConversationMediaCaptureShutterVisualState.class",
    "**/ui/conversation/mediapicker/component/capture/ConversationMediaCaptureVideoCenterDotVisualState.class",
    "**/ui/conversation/mediapicker/component/review/ConversationMediaReviewBackgroundSelection.class",
    "**/ui/conversation/mediapicker/component/review/ConversationMediaReviewBackgroundState.class",
    "**/ui/conversation/mediapicker/component/review/ConversationMediaReviewPageCardContentState.class",
    "**/ui/conversation/mediapicker/component/review/ConversationMediaReviewPageCardState.class",
    "**/ui/conversation/mediapicker/component/review/ConversationMediaReviewPagerCoordinator.class",
    "**/ui/conversation/mediapicker/component/review/ConversationMediaReviewPagerLayout.class",
    "**/ui/conversation/mediapicker/component/review/ConversationMediaReviewPagerState.class",
    "**/ui/conversation/messages/ui/attachment/ConversationInlineAudioAttachmentColors.class",
    "**/ui/conversation/messages/ui/attachment/ConversationInlineAudioAttachmentPlaybackState.class",
    "**/ui/conversation/metadata/ui/ConversationTopAppBarOverflowVisibility.class",
    "**/ui/conversation/metadata/ui/ConversationTopAppBarPresentation.class",
)

fun getComposableUnitExcludes(): List<String> {
    val excludes = mutableListOf<String>()
    val uiSrcDir = file("../src/com/android/messaging/ui")
    if (uiSrcDir.exists()) {
        uiSrcDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "kt") {
                val content = file.readText()
                if (content.contains("@Composable")) {
                    val baseName = file.nameWithoutExtension
                    excludes.add("**/ui/**/${baseName}Kt*.class")
                }
            }
        }
    }
    return excludes
}

fun getJavaExclusions(): List<String> {
    val javaExcludes = mutableListOf<String>()
    val javacDir = file("build/intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    if (javacDir.exists()) {
        javacDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "class") {
                val relativePath = file.relativeTo(javacDir).path
                javaExcludes.add(relativePath)
            }
        }
    }
    return javaExcludes
}

fun getKotlinClassTree(isUnitTestTrack: Boolean): FileTree {
    val includedPackages =
        if (isUnitTestTrack) unitTestIncludedPackages else androidTestIncludedPackages
    val baseDir = if (isUnitTestTrack) {
        "build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
    } else {
        "build/intermediates/classes/debug/transformDebugClassesWithAsm/dirs"
    }
    return fileTree(baseDir) {
        include(includedPackages)
        exclude(coverageExcludedClasses)
        if (isUnitTestTrack) {
            exclude(getComposableUnitExcludes())
            exclude(unitTestExcludedClasses)
        } else {
            exclude(getJavaExclusions())
        }
    }
}

tasks.register<JacocoReport>("jacocoUnitTestReport") {
    dependsOn("testDebugUnitTest")
    group = "Reporting"
    description = "Generate Jacoco coverage report for the unit tests."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(getKotlinClassTree(isUnitTestTrack = true))
    sourceDirectories.setFrom(files("../src"))
    executionData.setFrom(
        fileTree("build") {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )
}

tasks.register<JacocoCoverageVerification>("jacocoUnitTestVerification") {
    dependsOn("jacocoUnitTestReport")
    group = "Verification"
    description = "Verify Jacoco coverage for unit tests."

    classDirectories.setFrom(getKotlinClassTree(isUnitTestTrack = true))
    sourceDirectories.setFrom(files("../src"))
    executionData.setFrom(
        fileTree("build") {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                val minCoverage = project
                    .findProperty("unitTestMinCoverage")
                    ?.toString()
                    ?.toDoubleOrNull()
                    ?: 0.0
                minimum = (minCoverage / 100.0).toBigDecimal()
            }
        }
    }
}

tasks.register<JacocoReport>("jacocoAndroidTestReport") {
    group = "Reporting"
    description = "Generate Jacoco coverage report for instrumented (Android) tests."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(getKotlinClassTree(isUnitTestTrack = false))
    sourceDirectories.setFrom(files("../src"))
    executionData.setFrom(
        fileTree("build") {
            include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
        },
    )
}

tasks.register<JacocoCoverageVerification>("jacocoAndroidTestVerification") {
    dependsOn("jacocoAndroidTestReport")
    group = "Verification"
    description = "Verify Jacoco coverage for instrumented (Android) tests."

    classDirectories.setFrom(getKotlinClassTree(isUnitTestTrack = false))
    sourceDirectories.setFrom(files("../src"))
    executionData.setFrom(
        fileTree("build") {
            include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
        },
    )

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                val minCoverage = project
                    .findProperty("androidTestMinCoverage")
                    ?.toString()
                    ?.toDoubleOrNull()
                    ?: 0.0

                minimum = (minCoverage / 100.0).toBigDecimal()
            }
        }
    }
}
