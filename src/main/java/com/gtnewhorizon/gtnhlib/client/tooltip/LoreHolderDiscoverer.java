package com.gtnewhorizon.gtnhlib.client.tooltip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizon.gtnhlib.reflect.Fields.ClassFields;
import com.gtnewhorizon.gtnhlib.reflect.Fields.LookupType;

import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ASMDataTable.ASMData;
import lombok.Data;

public class LoreHolderDiscoverer {

    /**
     * key: field to be updated; value: translation key to use
     */
    static final Map<ClassFields<?>.Field<String>, String> LORE_HOLDERS = new HashMap<>();

    static final List<LoreField> LORE_FIELDS = new ArrayList<>();

    public static void harvestData(ASMDataTable table) {
        for (ASMData asmData : table.getAll(LoreHolder.class.getName())) {
            String className = asmData.getClassName();
            String fieldName = asmData.getObjectName();
            String key = (String) asmData.getAnnotationInfo().get("value");
            if (StringUtils.isNotBlank(key)) {
                LORE_FIELDS.add(new LoreField(className, fieldName, key));
            }
        }
    }

    static void register() {
        for (LoreField loreField : LORE_FIELDS) {
            String className = loreField.getClassName();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                GTNHLib.LOG.error("Class {} could not be found!", className, e);
                continue;
            }

            String fieldName = loreField.getFieldName();
            ClassFields<?>.Field<String> field;
            try {
                field = Fields.ofClass(clazz).getField(LookupType.DECLARED, fieldName, String.class);
            } catch (ClassCastException e) {
                GTNHLib.LOG.error("Field {} of class {} is not of type java.lang.String!", fieldName, className, e);
                continue;
            }

            if (field == null) {
                GTNHLib.LOG.error("Field {} of class {} could not be found!", fieldName, className);
                continue;
            }

            if (!field.isStatic) {
                GTNHLib.LOG.error("Field {} of class {} is not static!", fieldName, className);
                continue;
            }

            LORE_HOLDERS.put(field, loreField.getKey());
        }
    }

    @Data
    private static class LoreField {

        private final String className;
        private final String fieldName;
        private final String key;
    }
}
