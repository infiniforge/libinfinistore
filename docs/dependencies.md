# Dependencies and portability

The first extraction retains Android SDK/core, Commons Compress/XZ/zstd,
JSch, Commons Net, jcifs-ng, OkHttp, Bt and the Android-compatible Guice no_aop
artifact at the versions already used by InfiniFolder. Store uses Stream contracts;
playlist-container parsing and its Sound API dependency live in Vault. Artifact declarations are in store-android's
build.gradle.kts; transitive notices and license obligations still apply.

No bundled native binaries were added. zstd is the existing AAR dependency and
requires F-Droid source-build treatment. Original code is licensed under MIT; third-party license
obligations continue to apply.
Android Uri/SAF and storage services remain in store-android. Separating a pure
resource model module is a later compatibility checkpoint, not an implicit claim
that these Android implementations already run on desktop or iOS.

## SFTP resource URI construction

Store 0.1.1 constructs SFTP resource URIs from a decoded remote path using
component-aware percent encoding. A display filename such as
`Awesome Golf (1991).lnx` becomes `Awesome%20Golf%20%281991%29.lnx` in its URI. The
connection ID and path separators retain their roles. Literal percent signs,
query/fragment characters and Unicode are quoted as filename content. Reading
`Uri.path` decodes the remote path once; parent navigation rebuilds it without
double encoding. Call `SftpUri.build` with a filesystem path, not an encoded URI.

Path encoding preserves UTF-8 filename spelling, including decomposed Unicode;
it does not normalize distinct remote names. Exclamation marks become `%21`,
and literal plus/percent characters remain filename content after one decode.
