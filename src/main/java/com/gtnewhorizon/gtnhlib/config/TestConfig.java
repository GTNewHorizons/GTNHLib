package com.gtnewhorizon.gtnhlib.config;

@Config(modid = "gtnhlib", category = "test")
public class TestConfig {

    public enum TestEnum {
        TEST1,
        TEST2,
        TEST3
    }

    @Config.ModDetectedDefault(modID = "hodgepodge", value = "false")
    @Config.DefaultBoolean(true)
    public static boolean testBoolean;

    @Config.ModDetectedDefault(coremod = "coremodthatdoesntexistorsomething", value = "false")
    @Config.DefaultBoolean(true)
    public static boolean testBooleanButCooler;

    @Config.ModDetectedDefaultList(
            values = { @Config.ModDetectedDefault(modID = "modthatdoesntexistorsomething", value = "1111"),
                    @Config.ModDetectedDefault(modID = "secondmodthatdoesntexistorsomething", value = "2222"),
                    @Config.ModDetectedDefault(modID = "hodgepodge", value = "3333") })
    @Config.DefaultInt(1)
    public static int testInt;

    @Config.ModDetectedDefaultList(
            values = { @Config.ModDetectedDefault(modID = "modthatdoesntexistorsomething", value = "1111"),
                    @Config.ModDetectedDefault(modID = "secondmodthatdoesntexistorsomething", value = "2222"),
                    @Config.ModDetectedDefault(modID = "thirdmodthatdoesntexist", value = "3333") })
    @Config.DefaultInt(1)
    public static int testIntButCooler;

    @Config.ModDetectedDefault(coremod = "com.mitchej123.hodgepodge.core.HodgepodgeCore", value = "3333.0")
    @Config.DefaultDouble(1.0)
    public static double testDouble;

    @Config.ModDetectedDefault(coremod = "coremodthatdoesntexistorsomething", value = "3333.0")
    @Config.DefaultDouble(1.0)
    public static double testDoubleButCooler;

    @Config.ModDetectedDefault(modID = "hodgepodge", value = "3333.0f")
    @Config.DefaultFloat(1.0f)
    public static float testFloat;

    @Config.ModDetectedDefault(modID = "modthatdoesntexistorsomething", value = "3333.0f")
    @Config.DefaultFloat(1.0f)
    public static float testFloatButCooler;

    @Config.ModDetectedDefault(coremod = "com.mitchej123.hodgepodge.core.HodgepodgeCore", value = "STRING")
    @Config.DefaultString("STRING")
    public static String testString;

    @Config.ModDetectedDefault(coremod = "coremodthatdoesntexistorsomething", value = "STRING")
    @Config.DefaultString("STRING")
    public static String testStringButCooler;

    @Config.ModDetectedDefault(modID = "hodgepodge", value = "TEST2")
    @Config.DefaultEnum("TEST1")
    public static TestEnum testEnum;

    @Config.ModDetectedDefault(modID = "modthatdoesntexistorsomething", value = "TEST3")
    @Config.DefaultEnum("TEST1")
    public static TestEnum testEnumButCooler;

    @Config.Comment("This is a test int list")
    @Config.ModDetectedDefault(modID = "hodgepodge", value = "4, 5, 6")
    @Config.DefaultIntList({ 1, 2, 3 })
    public static int[] testIntList;

    @Config.Comment("This is a test int list")
    @Config.ModDetectedDefault(modID = "modthatdoesntexistorsomething", value = "4, 5, 6")
    @Config.DefaultIntList({ 1, 2, 3 })
    public static int[] testIntListButCooler;

    @Config.Comment("This is a test double list")
    @Config.ModDetectedDefault(modID = "hodgepodge", value = "4.0, 5.0, 6.0")
    @Config.DefaultDoubleList({ 1.0, 2.0, 3.0 })
    public static double[] testDoubleList;

    @Config.Comment("This is a test double list (or not)")
    @Config.ModDetectedDefault(modID = "modthatdoesntexistorsomething", value = "4.0, 5.0, 6.0")
    @Config.DefaultDoubleList({ 1.0, 2.0, 3.0 })
    public static double[] testDoubleListButCooler;

    @Config.Comment("This is a test string list")
    @Config.ModDetectedDefault(modID = "hodgepodge", values = { "test4", "test5", "test6" })
    @Config.DefaultStringList({ "test1", "test2", "test3" })
    public static String[] testStringList;

    @Config.Comment("This is a test string list (or not)")
    @Config.ModDetectedDefault(modID = "modthatdoesntexistorsomething", value = "test4, test5, test6")
    @Config.DefaultStringList({ "test1", "test2", "test3" })
    public static String[] testStringListButCooler;

}
