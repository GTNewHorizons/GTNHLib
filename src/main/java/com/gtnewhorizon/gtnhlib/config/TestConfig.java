package com.gtnewhorizon.gtnhlib.config;

@Config(modid = "gtnhlib", category = "Test")
@Config.LangKeyPattern(fullyQualified = true)
@Config.Sync
public class TestConfig {

    public static final SubConfig subConfig = new SubConfig();

    public enum TestEnum {
        TEST1,
        TEST2,
        TEST3
    }

    @Config.DefaultInt(1)
    public static int testInt;

    @Config.DefaultBoolean(true)
    public static boolean testBool;

    @Config.DefaultDouble(1.0)
    public static double testDouble;

    @Config.DefaultString("test")
    public static String testString;

    @Config.DefaultEnum("TEST1")
    public static TestEnum testEnum;

    @Config.DefaultIntList({ 1, 2, 3 })
    public static int[] testIntList;

    @Config.Comment({ "This is a test comment", "It spans multiple lines" })
    public static class SubConfig {

        @Config.LangKey("nested.subconfig.test")
        public final SubSubConfig subSubConfig = new SubSubConfig();

        @Config.DefaultInt(2)
        @Config.Sync(false)
        public int nestedInt;

        @Config.Comment("This is a test comment")
        @Config.Sync(false)
        public static class SubSubConfig {

            @Config.DefaultInt(3)
            public int nestedNestedInt;
        }
    }
}
