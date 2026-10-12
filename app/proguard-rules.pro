# Don't obfuscate (readable crash reports, GPL sources are public anyway),
# but strip unused library code.
-dontobfuscate
-optimizations !code/allocation/variable

# App code is small and partly reached via reflection / XML / WebView JS bridges
-keep class pw.thedrhax.** { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# jsoup
-keeppackagenames org.jsoup.nodes

# dnsjava: resolver config providers and record types are looked up reflectively
-keep class org.xbill.DNS.** { *; }
-dontwarn org.xbill.DNS.**
-dontwarn com.sun.jna.**
-dontwarn lombok.**
-dontwarn org.slf4j.**

# json-path / json-smart / json-simple: optional providers and reflection-based mappers
-keep class com.jayway.jsonpath.** { *; }
-keep class net.minidev.** { *; }
-keep class org.json.simple.** { *; }
-dontwarn com.jayway.jsonpath.**
-dontwarn net.minidev.**
-dontwarn com.google.gson.**
-dontwarn com.fasterxml.jackson.**
-dontwarn org.codehaus.jettison.**
-dontwarn jakarta.json.**
-dontwarn org.apache.tapestry5.**
-dontwarn org.json.**

# Okio / OkHttp
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# rangebar
-keep class com.edmodo.rangebar.** { *; }
