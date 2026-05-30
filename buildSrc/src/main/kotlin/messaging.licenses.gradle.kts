import messaging.licenses.CopyrightOverride
import messaging.licenses.ExtraNotice
import messaging.licenses.GenerateLicensesTask

tasks.register<GenerateLicensesTask>("generateLicenses") {
    group = "documentation"
    description = "Generates assets/licenses.html from the release runtime classpath."

    output.set(rootProject.file("assets/licenses.html"))

    copyrightOverrides.add(
        CopyrightOverride(
            coordinates = "com.github.bumptech.glide:*",
            copyright = "Copyright 2014 Google, Inc. All rights reserved.",
        ),
    )

    extraNotices.addAll(
        ExtraNotice(
            name = "AOSP",
            spdxId = "Apache-2.0",
            text = "Copyright (c) 2005-2008, The Android Open Source Project",
        ),
        ExtraNotice(
            name = "GIFLIB",
            spdxId = "MIT",
            text = "The GIFLIB distribution is Copyright (c) 1997  Eric S. Raymond",
        ),
    )
}
