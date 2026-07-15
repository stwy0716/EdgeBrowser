# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.edge.browser.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class org.jsoup.** { *; }
-dontwarn org.jspecify.annotations.**