# JNI + RPC — required for release minification (R8).
-keep class com.b44t.messenger.** { *; }
-keep class chat.delta.rpc.** { *; }
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }

# Legacy crypto helper referenced by JSON utils.
-keep class org.thoughtcrime.securesms.crypto.KeyStoreHelper* { *; }

-dontwarn com.google.firebase.analytics.connector.AnalyticsConnector
