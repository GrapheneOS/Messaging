import java.io.File
import java.math.BigDecimal
import org.gradle.api.file.FileCollection
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoReportBase

private val jacocoRulesDirPath = "jacoco-rules"
private val sourceDirPath = "../src"
private val uiSourceDirPath = "../src/com/android/messaging/ui"
private val buildDirPath = "build"

private val unitClassRoot =
    "build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
private val instrumentedClassRoot =
    "build/intermediates/classes/debug/transformDebugClassesWithAsm/dirs"
private val debugJavaClassRoot =
    "build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"

private val unitExecutionDataPattern =
    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
private val instrumentedExecutionDataPattern =
    "outputs/code_coverage/debugAndroidTest/connected/**/*.ec"

private val unitMinInstructionCoverageProperty = "unitTestMinCoverage"
private val unitMinBranchCoverageProperty = "unitTestMinBranchCoverage"
private val instrumentedMinInstructionCoverageProperty = "androidTestMinCoverage"
private val instrumentedMinBranchCoverageProperty = "androidTestMinBranchCoverage"

private val reportingGroup = "Reporting"
private val verificationGroup = "Verification"
private val kotlinExtension = "kt"
private val composableAnnotation = "@Composable"
private val ruleCommentPrefix = "#"
private val coveragePercentDivisor = BigDecimal("100")

private enum class CoverageTrack {
    UNIT,
    INSTRUMENTED,
}

private data class JacocoRuleLine(
    val rule: String,
    val location: String,
)

private data class TrackRules(
    val includedClassPatterns: List<String>,
    val excludedClassPatterns: List<String>,
    val measuredComposableSourcePaths: Set<String>,
    val measuredNonComposableSourcePaths: Set<String> = emptySet(),
)

private data class CoverageMinimum(
    val counter: String,
    val propertyName: String,
)

private data class UiKotlinSourceFile(
    val sourceFile: File,
    val relativePath: String,
    val packagePath: String,
    val content: String,
) {
    val hasComposableAnnotation: Boolean
        get() {
            return content.contains(composableAnnotation)
        }
}

private val jacocoRulesDir: File = file(jacocoRulesDirPath)
private val uiSourceDir: File = file(uiSourceDirPath)
private val kotlinTypeNameRegex = Regex(
    """\b(?:(?:annotation|data|enum|fun|sealed|value)\s+)?(?:class|interface|object)\s+([A-Za-z_][A-Za-z0-9_]*)""",
)

// Rule Loading

private fun readJacocoRuleLines(relativePath: String): List<String> {
    val rulesFile = jacocoRulesDir.resolve(relativePath)
    check(rulesFile.isFile) {
        "Missing Jacoco rule file: ${rulesFile.path}"
    }

    val ruleLines = rulesFile.readLines()
        .mapIndexedNotNull { lineIndex, rawLine ->
            val rule = rawLine.trim()
            when {
                rule.isEmpty() || rule.startsWith(ruleCommentPrefix) -> null
                else -> JacocoRuleLine(
                    rule = rule,
                    location = "${rulesFile.path}:${lineIndex + 1}",
                )
            }
        }

    validateUniqueRuleLines(ruleLines = ruleLines)
    return ruleLines.map { ruleLine -> ruleLine.rule }
}

private fun validateUniqueRuleLines(ruleLines: List<JacocoRuleLine>) {
    val seenRules = mutableSetOf<String>()
    ruleLines.forEach { ruleLine ->
        check(!ruleLine.rule.contains('\\')) {
            "Jacoco rule must use '/' separators at ${ruleLine.location}: ${ruleLine.rule}"
        }
        check(seenRules.add(ruleLine.rule)) {
            "Duplicate Jacoco rule at ${ruleLine.location}: ${ruleLine.rule}"
        }
    }
}

private fun readJacocoClassPathPatterns(relativePath: String): List<String> {
    val patterns = readJacocoRuleLines(relativePath = relativePath)
    patterns.forEach { pattern ->
        validateClassPathPattern(
            pattern = pattern,
            relativePath = relativePath,
        )
    }
    return patterns
}

private fun validateClassPathPattern(pattern: String, relativePath: String) {
    check(!pattern.startsWith("/")) {
        "Jacoco class pattern must be relative in $relativePath: $pattern"
    }
    check(!pattern.endsWith(".kt")) {
        "Jacoco class pattern must not point to source in $relativePath: $pattern"
    }
}

private fun readJacocoUiSourcePaths(relativePath: String): Set<String> {
    val sourcePaths = readJacocoRuleLines(relativePath = relativePath)
    sourcePaths.forEach { sourcePath ->
        validateUiSourcePath(
            sourcePath = sourcePath,
            relativePath = relativePath,
        )
    }
    return LinkedHashSet(sourcePaths)
}

private fun validateUiSourcePath(sourcePath: String, relativePath: String) {
    check(!sourcePath.startsWith("/")) {
        "Jacoco UI source path must be relative in $relativePath: $sourcePath"
    }
    check(!sourcePath.startsWith("ui/")) {
        "Jacoco UI source path is relative to src/com/android/messaging/ui: $sourcePath"
    }
    check(sourcePath.endsWith(".kt")) {
        "Jacoco UI source path must point to a Kotlin file in $relativePath: $sourcePath"
    }
}

private fun getGeneratedSyntheticClassPatterns(): List<String> {
    return (0..9).flatMap { syntheticSuffixIndex ->
        listOf(
            "**/*\$$syntheticSuffixIndex.class",
            "**/*\$?\$$syntheticSuffixIndex.class",
            "**/*\$??\$$syntheticSuffixIndex.class",
        )
    }
}

private val commonExcludedClassPatterns: List<String> = readJacocoClassPathPatterns(
    relativePath = "common/excluded-class-patterns.txt",
) + getGeneratedSyntheticClassPatterns()

private val unitRules = TrackRules(
    includedClassPatterns = readJacocoClassPathPatterns(
        relativePath = "unit/included-class-patterns.txt",
    ),
    excludedClassPatterns = readJacocoClassPathPatterns(
        relativePath = "unit/excluded-class-patterns.txt",
    ),
    measuredComposableSourcePaths = readJacocoUiSourcePaths(
        relativePath = "unit/measured-composable-sources.txt",
    ),
)

private val instrumentedRules = TrackRules(
    includedClassPatterns = readJacocoClassPathPatterns(
        relativePath = "instrumented/included-class-patterns.txt",
    ),
    excludedClassPatterns = readJacocoClassPathPatterns(
        relativePath = "instrumented/excluded-class-patterns.txt",
    ),
    measuredComposableSourcePaths = readJacocoUiSourcePaths(
        relativePath = "instrumented/measured-composable-sources.txt",
    ),
    measuredNonComposableSourcePaths = readJacocoUiSourcePaths(
        relativePath = "instrumented/measured-non-composable-sources.txt",
    ),
)

// Track Configuration

private fun getTrackRules(track: CoverageTrack): TrackRules {
    return when (track) {
        CoverageTrack.UNIT -> unitRules
        CoverageTrack.INSTRUMENTED -> instrumentedRules
    }
}

private fun getClassRoot(track: CoverageTrack): String {
    return when (track) {
        CoverageTrack.UNIT -> unitClassRoot
        CoverageTrack.INSTRUMENTED -> instrumentedClassRoot
    }
}

private fun getExecutionDataPattern(track: CoverageTrack): String {
    return when (track) {
        CoverageTrack.UNIT -> unitExecutionDataPattern
        CoverageTrack.INSTRUMENTED -> instrumentedExecutionDataPattern
    }
}

private fun getCoverageMinimums(track: CoverageTrack): List<CoverageMinimum> {
    return when (track) {
        CoverageTrack.UNIT -> listOf(
            CoverageMinimum(
                counter = "INSTRUCTION",
                propertyName = unitMinInstructionCoverageProperty,
            ),
            CoverageMinimum(
                counter = "BRANCH",
                propertyName = unitMinBranchCoverageProperty,
            ),
        )
        CoverageTrack.INSTRUMENTED -> listOf(
            CoverageMinimum(
                counter = "INSTRUCTION",
                propertyName = instrumentedMinInstructionCoverageProperty,
            ),
            CoverageMinimum(
                counter = "BRANCH",
                propertyName = instrumentedMinBranchCoverageProperty,
            ),
        )
    }
}

// Kotlin Source Analysis

private fun getUiKotlinSourceFiles(): List<UiKotlinSourceFile> {
    if (!uiSourceDir.exists()) {
        return emptyList()
    }

    return uiSourceDir
        .walkTopDown()
        .filter { sourceFile -> sourceFile.isFile && sourceFile.extension == kotlinExtension }
        .map { sourceFile -> createUiKotlinSourceFile(sourceFile = sourceFile) }
        .toList()
}

private fun createUiKotlinSourceFile(sourceFile: File): UiKotlinSourceFile {
    return UiKotlinSourceFile(
        sourceFile = sourceFile,
        relativePath = getUiSourceRelativePath(sourceFile = sourceFile),
        packagePath = getUiPackagePath(sourceFile = sourceFile),
        content = sourceFile.readText(),
    )
}

private fun getUiSourceRelativePath(sourceFile: File): String {
    return sourceFile
        .relativeTo(base = uiSourceDir)
        .path
        .replace(oldChar = File.separatorChar, newChar = '/')
}

private fun getUiPackagePath(sourceFile: File): String {
    val relativeParentPath = sourceFile.parentFile
        .relativeTo(base = uiSourceDir)
        .path
        .replace(oldChar = File.separatorChar, newChar = '/')

    return when {
        relativeParentPath.isEmpty() -> "ui"
        else -> "ui/$relativeParentPath"
    }
}

private fun getKotlinTypeNames(content: String): List<String> {
    return kotlinTypeNameRegex.findAll(input = content)
        .map { matchResult -> matchResult.groupValues[1] }
        .toList()
}

private fun UiKotlinSourceFile.fileFacadeClassPattern(): String {
    val baseName = sourceFile.nameWithoutExtension
    return "**/$packagePath/${baseName}Kt*.class"
}

private fun UiKotlinSourceFile.classPatterns(): List<String> {
    val baseName = sourceFile.nameWithoutExtension
    val classNames = (listOf("${baseName}Kt") + getKotlinTypeNames(content = content))
        .distinct()

    return classNames.map { className ->
        "**/$packagePath/$className*.class"
    }
}

private fun getMeasuredUiSourceClassPatterns(
    sourcePaths: Set<String>,
    expectedComposable: Boolean,
    description: String,
): List<String> {
    if (!uiSourceDir.exists()) {
        return emptyList()
    }

    return sourcePaths
        .flatMap { sourcePath ->
            val sourceFile = uiSourceDir.resolve(sourcePath)
            check(sourceFile.isFile) {
                "Missing $description: ${sourceFile.path}"
            }

            val source = createUiKotlinSourceFile(sourceFile = sourceFile)
            check(source.hasComposableAnnotation == expectedComposable) {
                "Unexpected composable state for $description: ${sourceFile.path}"
            }

            source.classPatterns()
        }
        .distinct()
}

// Class Pattern Assembly

private fun getExplicitIncludeClassPatterns(track: CoverageTrack): List<String> {
    return when (track) {
        CoverageTrack.UNIT -> getUnitMeasuredComposableClassPatterns()
        CoverageTrack.INSTRUMENTED -> getInstrumentedMeasuredSourceClassPatterns()
    }
}

private fun getUnitMeasuredComposableClassPatterns(): List<String> {
    return getMeasuredUiSourceClassPatterns(
        sourcePaths = unitRules.measuredComposableSourcePaths,
        expectedComposable = true,
        description = "unit-measured composable source file",
    )
}

private fun getInstrumentedMeasuredSourceClassPatterns(): List<String> {
    val composableClassPatterns = getMeasuredUiSourceClassPatterns(
        sourcePaths = instrumentedRules.measuredComposableSourcePaths,
        expectedComposable = true,
        description = "instrumented-test-measured composable source file",
    )
    val nonComposableClassPatterns = getMeasuredUiSourceClassPatterns(
        sourcePaths = instrumentedRules.measuredNonComposableSourcePaths,
        expectedComposable = false,
        description = "instrumented-test-measured non-composable source file",
    )

    return composableClassPatterns + nonComposableClassPatterns
}

private fun getTrackExcludedClassPatterns(track: CoverageTrack): List<String> {
    return when (track) {
        CoverageTrack.UNIT -> getUnitTrackExcludedClassPatterns()
        CoverageTrack.INSTRUMENTED -> getInstrumentedTrackExcludedClassPatterns()
    }
}

private fun getUnitTrackExcludedClassPatterns(): List<String> {
    return getUnmeasuredComposableFileFacadePatterns() + unitRules.excludedClassPatterns
}

private fun getUnmeasuredComposableFileFacadePatterns(): List<String> {
    return getUiKotlinSourceFiles()
        .filter { source -> source.hasComposableAnnotation }
        .filter { source -> source.relativePath !in unitRules.measuredComposableSourcePaths }
        .map { source -> source.fileFacadeClassPattern() }
}

private fun getInstrumentedTrackExcludedClassPatterns(): List<String> {
    return getInstrumentedLogicClassPatterns() +
        getUnitMeasuredComposableClassPatterns() +
        instrumentedRules.excludedClassPatterns
}

private fun getInstrumentedLogicClassPatterns(): List<String> {
    return getUiKotlinSourceFiles()
        .filter { source -> !source.hasComposableAnnotation }
        .filter { source ->
            source.relativePath !in instrumentedRules.measuredNonComposableSourcePaths
        }
        .flatMap { source -> source.classPatterns() }
        .distinct()
}

private fun getNonOverridableExcludedClassPatterns(track: CoverageTrack): List<String> {
    val generatedOrStructuralExcludes = commonExcludedClassPatterns
    val compiledJavaExcludes = when (track) {
        CoverageTrack.UNIT -> emptyList()
        CoverageTrack.INSTRUMENTED -> getDebugJavaClassFilePaths()
    }

    return generatedOrStructuralExcludes + compiledJavaExcludes
}

private fun getDebugJavaClassFilePaths(): List<String> {
    val javaClassRoot = file(debugJavaClassRoot)
    if (!javaClassRoot.exists()) {
        return emptyList()
    }

    return javaClassRoot
        .walkTopDown()
        .filter { classFile -> classFile.isFile && classFile.extension == "class" }
        .map { classFile -> classFile.relativeTo(base = javaClassRoot).path }
        .toList()
}

private fun getKotlinClassTree(track: CoverageTrack): FileCollection {
    val rules = getTrackRules(track = track)
    val classRoot = getClassRoot(track = track)
    val explicitIncludes = getExplicitIncludeClassPatterns(track = track)
    val nonOverridableExcludes = getNonOverridableExcludedClassPatterns(track = track)

    // Gradle excludes win inside one PatternSet, so explicit includes live in a second tree.
    val filteredClassTree = fileTree(classRoot) {
        include(rules.includedClassPatterns)
        exclude(nonOverridableExcludes)
        exclude(explicitIncludes)
        exclude(getTrackExcludedClassPatterns(track = track))
    }
    val explicitlyIncludedClassTree = fileTree(classRoot) {
        include(explicitIncludes)
        exclude(nonOverridableExcludes)
    }

    return files(filteredClassTree, explicitlyIncludedClassTree)
}

// Task Configuration

private fun JacocoReportBase.configureCoverageInputs(track: CoverageTrack) {
    classDirectories.setFrom(getKotlinClassTree(track = track))
    sourceDirectories.setFrom(files(sourceDirPath))
    executionData.setFrom(
        fileTree(buildDirPath) {
            include(getExecutionDataPattern(track = track))
        },
    )
}

private fun JacocoReport.configureCoverageReport(track: CoverageTrack) {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    configureCoverageInputs(track = track)
}

private fun JacocoCoverageVerification.configureCoverageVerification(track: CoverageTrack) {
    configureCoverageInputs(track = track)

    violationRules {
        rule {
            element = "BUNDLE"
            getCoverageMinimums(track = track).forEach { coverageMinimum ->
                limit {
                    counter = coverageMinimum.counter
                    value = "COVEREDRATIO"
                    minimum = getMinimumCoverage(propertyName = coverageMinimum.propertyName)
                }
            }
        }
    }
}

private fun getMinimumCoverage(propertyName: String): BigDecimal {
    val rawMinimumPercent = project.findProperty(propertyName)?.toString()
        ?: return BigDecimal.ZERO

    val minimumPercent = rawMinimumPercent.toBigDecimalOrNull()
    check(minimumPercent != null) {
        "Jacoco minimum coverage property must be numeric: -P$propertyName=$rawMinimumPercent"
    }
    check(minimumPercent in BigDecimal.ZERO..coveragePercentDivisor) {
        "Jacoco minimum coverage property must be between 0 and 100: " +
            "-P$propertyName=$rawMinimumPercent"
    }

    return minimumPercent.divide(coveragePercentDivisor)
}

tasks.register<JacocoReport>("jacocoUnitTestReport") {
    dependsOn("testDebugUnitTest")
    group = reportingGroup
    description = "Generate Jacoco coverage report for the unit tests."

    configureCoverageReport(track = CoverageTrack.UNIT)
}

tasks.register<JacocoCoverageVerification>("jacocoUnitTestVerification") {
    dependsOn("jacocoUnitTestReport")
    group = verificationGroup
    description = "Verify Jacoco coverage for unit tests."

    configureCoverageVerification(track = CoverageTrack.UNIT)
}

tasks.register<JacocoReport>("jacocoAndroidTestReport") {
    group = reportingGroup
    description = "Generate Jacoco coverage report for instrumented (Android) tests."

    configureCoverageReport(track = CoverageTrack.INSTRUMENTED)
}

tasks.register<JacocoCoverageVerification>("jacocoAndroidTestVerification") {
    dependsOn("jacocoAndroidTestReport")
    group = verificationGroup
    description = "Verify Jacoco coverage for instrumented (Android) tests."

    configureCoverageVerification(track = CoverageTrack.INSTRUMENTED)
}
