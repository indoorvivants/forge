# Forge

Small SBT plugins to aid in application development and distribution in Scala

## Native Binary Plugin

**project/plugins.sbt**
```
addSbtPlugin("com.indoorvivants" % "sbt-forge-native-binary" % "0.2.0")
```

This is a small plugin that simplifies some tasks associated with building and distributing Scala Native applications.

SBT Commands to build the binary:
  - `buildBinaryDebug` – produces `./out/debug/app`
  - `buildBinaryRelease` – produces `./out/release/app`
  - `buildBinaryPlatformDebug` - produces `./out/debug/app-...` with platform specific information encoded in the name (e.g. `./out/debug/app-aarch64-apple-darwin`)
  - `buildBinaryPlatformRelease` - produces `./out/release/app-...` with platform specific information encoded in the name (e.g. `./out/release/app-aarch64-apple-darwin`)

Use `buildBinaryConfig` to configure the name of the binary.
