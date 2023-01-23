package com.gtnewhorizon.gtnhlib.test.reflect;

import static org.junit.jupiter.api.Assertions.*;

import com.gtnewhorizon.gtnhlib.reflect.Fields;
import org.junit.jupiter.api.Test;

public class FieldsTests {

    @Test
    void printJavaVersion() {
        // For manual testing
        String jvmVersion = System.getProperty("java.version");
        System.err.println(jvmVersion);
    }

    @Test
    void objectFields() {
        final FieldsTestSubject subject = new FieldsTestSubject();
        final Fields.ClassFields<FieldsTestSubject> classFields = Fields.ofClass(FieldsTestSubject.class);
        final Fields.ClassFields<FieldsTestSubject>.Field<String> dynField =
                classFields.getField(Fields.LookupType.DECLARED, "dynamicObject", String.class);
        final Fields.ClassFields<FieldsTestSubject>.Field<String> staField =
                classFields.getField(Fields.LookupType.DECLARED, "staticObject", String.class);
        final Fields.ClassFields<FieldsTestSubject>.Field<String> finField =
                classFields.getField(Fields.LookupType.DECLARED, "staticFinalObject", String.class);

        assertAll(
                () -> assertThrows(
                        ClassCastException.class,
                        () -> classFields.getField(Fields.LookupType.DECLARED, "dynamicObject", Long.class)),
                () -> assertNull(classFields.getField(Fields.LookupType.DECLARED, "doesNotExist", Long.class)),
                () -> assertNotNull(dynField),
                () -> assertNotNull(staField),
                () -> assertNotNull(finField));

        assertAll(
                () -> assertEquals("init", subject.getDynamicObject()),
                () -> assertEquals("init", dynField.getValue(subject)),
                () -> assertEquals("init", FieldsTestSubject.getStaticObject()),
                () -> assertEquals("init", staField.getValue(null)),
                () -> assertEquals("init", FieldsTestSubject.getStaticFinalObject()),
                () -> assertEquals("init", finField.getValue(null)));

        dynField.setValue(subject, "new1");

        assertAll(
                () -> assertEquals("new1", subject.getDynamicObject()),
                () -> assertEquals("new1", dynField.getValue(subject)),
                () -> assertEquals("init", FieldsTestSubject.getStaticObject()),
                () -> assertEquals("init", staField.getValue(null)),
                () -> assertEquals("init", FieldsTestSubject.getStaticFinalObject()),
                () -> assertEquals("init", finField.getValue(null)));

        staField.setValue(subject, "new2");

        assertAll(
                () -> assertEquals("new1", subject.getDynamicObject()),
                () -> assertEquals("new1", dynField.getValue(subject)),
                () -> assertEquals("new2", FieldsTestSubject.getStaticObject()),
                () -> assertEquals("new2", staField.getValue(null)),
                () -> assertEquals("init", FieldsTestSubject.getStaticFinalObject()),
                () -> assertEquals("init", finField.getValue(null)));

        finField.setValue(subject, "new3");

        assertAll(
                () -> assertEquals("new1", subject.getDynamicObject()),
                () -> assertEquals("new1", dynField.getValue(subject)),
                () -> assertEquals("new2", FieldsTestSubject.getStaticObject()),
                () -> assertEquals("new2", staField.getValue(null)),
                () -> assertEquals("new3", FieldsTestSubject.getStaticFinalObject()),
                () -> assertEquals("new3", finField.getValue(null)));

        // cleanup
        staField.setValue(null, "init");
        finField.setValue(null, "init");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void objectUntypedFields() {
        final FieldsTestSubject subject = new FieldsTestSubject();
        final Fields.ClassFields classFields = Fields.ofClass(FieldsTestSubject.class);
        final Fields.ClassFields.Field dynField =
                classFields.getUntypedField(Fields.LookupType.DECLARED, "dynamicObject");
        final Fields.ClassFields.Field staField =
                classFields.getUntypedField(Fields.LookupType.DECLARED, "staticObject");
        final Fields.ClassFields.Field finField =
                classFields.getUntypedField(Fields.LookupType.DECLARED, "staticFinalObject");

        assertAll(
                () -> assertThrows(
                        ClassCastException.class,
                        () -> classFields.getField(Fields.LookupType.DECLARED, "dynamicObject", Long.class)),
                () -> assertNull(classFields.getField(Fields.LookupType.DECLARED, "doesNotExist", Long.class)),
                () -> assertNotNull(dynField),
                () -> assertNotNull(staField),
                () -> assertNotNull(finField));

        assertAll(
                () -> assertEquals("init", subject.getDynamicObject()),
                () -> assertEquals("init", dynField.getValue(subject)),
                () -> assertEquals("init", FieldsTestSubject.getStaticObject()),
                () -> assertEquals("init", staField.getValue(null)),
                () -> assertEquals("init", FieldsTestSubject.getStaticFinalObject()),
                () -> assertEquals("init", finField.getValue(null)));

        dynField.setValue(subject, "new1");

        assertAll(
                () -> assertEquals("new1", subject.getDynamicObject()),
                () -> assertEquals("new1", dynField.getValue(subject)),
                () -> assertEquals("init", FieldsTestSubject.getStaticObject()),
                () -> assertEquals("init", staField.getValue(null)),
                () -> assertEquals("init", FieldsTestSubject.getStaticFinalObject()),
                () -> assertEquals("init", finField.getValue(null)));

        staField.setValue(subject, "new2");

        assertAll(
                () -> assertEquals("new1", subject.getDynamicObject()),
                () -> assertEquals("new1", dynField.getValue(subject)),
                () -> assertEquals("new2", FieldsTestSubject.getStaticObject()),
                () -> assertEquals("new2", staField.getValue(null)),
                () -> assertEquals("init", FieldsTestSubject.getStaticFinalObject()),
                () -> assertEquals("init", finField.getValue(null)));

        finField.setValue(subject, "new3");

        assertAll(
                () -> assertEquals("new1", subject.getDynamicObject()),
                () -> assertEquals("new1", dynField.getValue(subject)),
                () -> assertEquals("new2", FieldsTestSubject.getStaticObject()),
                () -> assertEquals("new2", staField.getValue(null)),
                () -> assertEquals("new3", FieldsTestSubject.getStaticFinalObject()),
                () -> assertEquals("new3", finField.getValue(null)));

        // cleanup
        staField.setValue(null, "init");
        finField.setValue(null, "init");
    }

    @Test
    void intFields() {
        final FieldsTestSubject subject = new FieldsTestSubject();
        final Fields.ClassFields<FieldsTestSubject> classFields = Fields.ofClass(FieldsTestSubject.class);
        final Fields.ClassFields<FieldsTestSubject>.Field<Integer> dynField =
                classFields.getIntField(Fields.LookupType.DECLARED, "dynamicInt");
        final Fields.ClassFields<FieldsTestSubject>.Field<Integer> staField =
                classFields.getIntField(Fields.LookupType.DECLARED, "staticInt");
        final Fields.ClassFields<FieldsTestSubject>.Field<Integer> finField =
                classFields.getIntField(Fields.LookupType.DECLARED, "staticFinalInt");

        assertAll(
                () -> assertThrows(
                        ClassCastException.class,
                        () -> classFields.getField(Fields.LookupType.DECLARED, "dynamicInt", Long.class)),
                () -> assertThrows(
                        ClassCastException.class,
                        () -> classFields.getField(Fields.LookupType.DECLARED, "dynamicInt", Integer.class)),
                () -> assertNull(classFields.getField(Fields.LookupType.DECLARED, "doesNotExist", Long.class)),
                () -> assertNotNull(dynField),
                () -> assertNotNull(staField),
                () -> assertNotNull(finField));

        assertAll(
                () -> assertEquals(0, (int) subject.getDynamicInt()),
                () -> assertEquals(0, (int) dynField.getValue(subject)),
                () -> assertEquals(0, (int) FieldsTestSubject.getStaticInt()),
                () -> assertEquals(0, (int) staField.getValue(null)),
                () -> assertEquals(0, (int) FieldsTestSubject.getStaticFinalInt()),
                () -> assertEquals(0, (int) finField.getValue(null)));

        dynField.setValue(subject, 1);

        assertAll(
                () -> assertEquals(1, (int) subject.getDynamicInt()),
                () -> assertEquals(1, (int) dynField.getValue(subject)),
                () -> assertEquals(0, (int) FieldsTestSubject.getStaticInt()),
                () -> assertEquals(0, (int) staField.getValue(null)),
                () -> assertEquals(0, (int) FieldsTestSubject.getStaticFinalInt()),
                () -> assertEquals(0, (int) finField.getValue(null)));

        staField.setValue(subject, 2);

        assertAll(
                () -> assertEquals(1, (int) subject.getDynamicInt()),
                () -> assertEquals(1, (int) dynField.getValue(subject)),
                () -> assertEquals(2, (int) FieldsTestSubject.getStaticInt()),
                () -> assertEquals(2, (int) staField.getValue(null)),
                () -> assertEquals(0, (int) FieldsTestSubject.getStaticFinalInt()),
                () -> assertEquals(0, (int) finField.getValue(null)));

        finField.setValue(subject, 3);

        assertAll(
                () -> assertEquals(1, (int) subject.getDynamicInt()),
                () -> assertEquals(1, (int) dynField.getValue(subject)),
                () -> assertEquals(2, (int) FieldsTestSubject.getStaticInt()),
                () -> assertEquals(2, (int) staField.getValue(null)),
                () -> assertEquals(3, (int) FieldsTestSubject.getStaticFinalInt()),
                () -> assertEquals(3, (int) finField.getValue(null)));

        // cleanup
        staField.setValue(null, 0);
        finField.setValue(null, 0);
    }
}
