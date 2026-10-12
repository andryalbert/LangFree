# Add project specific ProGuard rules here.

# Keep Vosk native library classes
-keep class org.vosk.** { *; }
-keep interface org.vosk.** { *; }

# Keep DJL classes if used
-keep class ai.djl.** { *; }
-keep interface ai.djl.** { *; }

# Keep Apache Commons Logging
-keepnames class org.apache.commons.logging.**
-dontwarn org.apache.commons.logging.**

# Keep JNA classes
-keep class com.sun.jna.** { *; }
-keep interface com.sun.jna.** { *; }

# Optimize and obfuscate
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
