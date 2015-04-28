#Jackson specifics

# Otherwise VisibilityChecker.java:191 throws NPE
-keepattributes *Annotations*

-dontwarn com.fasterxml.jackson.databind.ext.DOMSerializer
# Otherwise get java.lang.NoSuchFieldError: PUBLIC_ONLY at java.lang.Class.getDeclaredAnnotation
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class org.acra.annotation.** { *; }

# Otherwise java.lang.Enum#sharedConstantsCache@40 returns null.
-keepclassmembers enum com.fasterxml.jackson.databind.* {
	public static **[] values();
}

# Not encountered, but suggested at http://stackoverflow.com/questions/8405225.
#-keepattributes Signature, EnclosingMethod
#-dontwarn com.fasterxml.jackson.databind.**


# Not tested -- keep getters and setters used by Jackson off proguard.
-keep public class name.dlazerka.mf.api.** {
  public void set*(***);
  public *** get*();
}
