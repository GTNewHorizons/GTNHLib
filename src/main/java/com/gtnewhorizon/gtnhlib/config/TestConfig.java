package com.gtnewhorizon.gtnhlib.config;

@Config(modid = "gtnhlib", category = "test")
public class TestConfig {

    public enum TestEnum {
        TEST1,
        TEST2,
        TEST3
    }

    @Config.Comment("This is a sub config")
    @Config.LangKey("test.subconfig")
    public static final TestSubConfig subConfig = new TestSubConfig();

    @Config.Comment("This is a test boolean")
    @Config.DefaultBoolean(true)
    public static boolean testBoolean;

    @Config.Comment("This is a test int")
    @Config.DefaultInt(1)
    public static int testInt;

    @Config.Comment("This is a test double")
    @Config.DefaultDouble(1.0)
    public static double testDouble;

    @Config.Comment("This is a test enum")
    @Config.DefaultEnum("TEST1")
    public static TestEnum testEnum;

    public static class TestSubConfig {

        public static class TestDoubleSubConfig {

            @Config.Comment("This is a test boolean")
            @Config.DefaultBoolean(true)
            public boolean testBoolean;

            @Config.Comment("This is a test int")
            @Config.DefaultInt(1)
            public int testInt;

            @Config.Comment("This is a test double")
            @Config.DefaultDouble(1.0)
            public double testDouble;

            @Config.Comment("This is a test enum")
            @Config.DefaultEnum("TEST1")
            public TestEnum testEnum;
        }

        @Config.Comment("This is a nested sub config")
        public final TestDoubleSubConfig doubleSubConfig = new TestDoubleSubConfig();

        @Config.Comment("This is a test boolean")
        @Config.DefaultBoolean(true)
        public boolean testBoolean;

        @Config.Comment("This is a test int")
        @Config.DefaultInt(1)
        public int testInt;

        @Config.Comment("This is a test double")
        @Config.DefaultDouble(1.0)
        public double testDouble;

        @Config.Comment("This is a test enum")
        @Config.DefaultEnum("TEST1")
        public TestEnum testEnum;
    }
}
