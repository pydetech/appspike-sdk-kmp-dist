import ProjectDescription

// The Xcode project is generated from this file (`tuist generate`), so nothing under
// iosApp/ needs a checked-in .xcodeproj. The app links the `shared` KMP framework that
// the Gradle build produces — the pre-action below is the standard Kotlin Multiplatform
// Xcode integration (`embedAndSignAppleFrameworkForXcode`), which builds the framework
// for the configuration/SDK/arch Xcode is currently building and drops it in
// shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME).
let project = Project(
    name: "SampleApp",
    targets: [
        .target(
            name: "SampleApp",
            destinations: [.iPhone, .iPad],
            product: .app,
            bundleId: "dev.appspike.sample.kmp",
            deploymentTargets: .iOS("15.0"),
            infoPlist: .extendingDefault(with: [
                "UILaunchScreen": .dictionary([:]),
            ]),
            sources: ["Sources/**"],
            scripts: [
                .pre(
                    script: #"""
                    cd "$SRCROOT/.."
                    ./gradlew :shared:embedAndSignAppleFrameworkForXcode
                    """#,
                    name: "Compile Kotlin Framework",
                    basedOnDependencyAnalysis: false
                ),
            ],
            settings: .settings(
                base: [
                    "FRAMEWORK_SEARCH_PATHS":
                        "$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
                    "OTHER_LDFLAGS": "$(inherited) -framework shared",
                    // The Gradle build writes outside the derived-data sandbox.
                    "ENABLE_USER_SCRIPT_SANDBOXING": "NO",
                    "SWIFT_VERSION": "5.0",
                ]
            )
        ),
    ]
)
