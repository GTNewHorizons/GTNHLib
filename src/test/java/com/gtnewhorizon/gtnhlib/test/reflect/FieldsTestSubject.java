package com.gtnewhorizon.gtnhlib.test.reflect;

public class FieldsTestSubject {

    static {
        // Prevent constant inlining by javac
        staticFinalChar = Character.valueOf('0');
        staticFinalBool = Boolean.valueOf(false);
        staticFinalByte = Byte.valueOf((byte) 0).byteValue();
        staticFinalShort = Short.valueOf((short) 0).shortValue();
        staticFinalInt = Integer.valueOf(0).intValue();
        staticFinalLong = Long.valueOf(0).longValue();
        staticFinalFloat = Float.valueOf(0.0f).floatValue();
        staticFinalDouble = Double.valueOf(0.0d).doubleValue();
        staticFinalObject = String.format("in%s", "it");
    }

    private static final char staticFinalChar;
    private static final boolean staticFinalBool;
    private static final byte staticFinalByte;
    private static final short staticFinalShort;
    private static final int staticFinalInt;
    private static final long staticFinalLong;
    private static final float staticFinalFloat;
    private static final double staticFinalDouble;
    private static final String staticFinalObject;
    private static char staticChar = '0';
    private static boolean staticBool = false;
    private static byte staticByte = 0;
    private static short staticShort = 0;
    private static int staticInt = 0;
    private static long staticLong = 0;
    private static float staticFloat = 0;
    private static double staticDouble = 0;
    private static String staticObject = "init";
    private char dynamicChar = '0';
    private boolean dynamicBool = false;
    private byte dynamicByte = 0;
    private short dynamicShort = 0;
    private int dynamicInt = 0;
    private long dynamicLong = 0;
    private float dynamicFloat = 0;
    private double dynamicDouble = 0;
    private String dynamicObject = "init";

    public static char getStaticFinalChar() {
        return staticFinalChar;
    }

    public static boolean isStaticFinalBool() {
        return staticFinalBool;
    }

    public static byte getStaticFinalByte() {
        return staticFinalByte;
    }

    public static short getStaticFinalShort() {
        return staticFinalShort;
    }

    public static int getStaticFinalInt() {
        return staticFinalInt;
    }

    public static long getStaticFinalLong() {
        return staticFinalLong;
    }

    public static float getStaticFinalFloat() {
        return staticFinalFloat;
    }

    public static double getStaticFinalDouble() {
        return staticFinalDouble;
    }

    public static String getStaticFinalObject() {
        return staticFinalObject;
    }

    public static char getStaticChar() {
        return staticChar;
    }

    public static boolean isStaticBool() {
        return staticBool;
    }

    public static byte getStaticByte() {
        return staticByte;
    }

    public static short getStaticShort() {
        return staticShort;
    }

    public static int getStaticInt() {
        return staticInt;
    }

    public static long getStaticLong() {
        return staticLong;
    }

    public static float getStaticFloat() {
        return staticFloat;
    }

    public static double getStaticDouble() {
        return staticDouble;
    }

    public static String getStaticObject() {
        return staticObject;
    }

    public char getDynamicChar() {
        return dynamicChar;
    }

    public boolean isDynamicBool() {
        return dynamicBool;
    }

    public byte getDynamicByte() {
        return dynamicByte;
    }

    public short getDynamicShort() {
        return dynamicShort;
    }

    public int getDynamicInt() {
        return dynamicInt;
    }

    public long getDynamicLong() {
        return dynamicLong;
    }

    public String getDynamicObject() {
        return dynamicObject;
    }

    public float getDynamicFloat() {
        return dynamicFloat;
    }

    public double getDynamicDouble() {
        return dynamicDouble;
    }
}
