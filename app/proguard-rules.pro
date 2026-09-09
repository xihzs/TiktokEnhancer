# Keep Xposed hook entry points
-keep class com.ash.tiktokregion.MainHook { *; }
-keep class com.ash.tiktokregion.WatermarkHook { *; }
-keep class com.ash.tiktokregion.AdsHook { *; }
-keep class com.ash.tiktokregion.ConfigProvider { *; }
-keep class com.ash.tiktokregion.CountryPreset { *; }
-keepclassmembers class com.ash.tiktokregion.MainActivity {
    public boolean isModuleActive();
}
