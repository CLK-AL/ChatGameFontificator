# ProGuard rules for ChatGameFontificator legacy Java main class

-keep class com.glitchcog.fontificator.FontificatorMain {
    public static void main(java.lang.String[]);
}

# Keep Swing/AWT reflective entry points
-keep class com.glitchcog.fontificator.gui.** { *; }
