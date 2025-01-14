package com.gtnewhorizon.gtnhlib.client.tooltip;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.reflect.Fields;
import com.gtnewhorizon.gtnhlib.reflect.Fields.ClassFields;
import com.gtnewhorizon.gtnhlib.reflect.Fields.LookupType;

import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ASMDataTable.ASMData;

public class LoreHolderDiscoverer {

    /**
     * key: field to be updated; value: translation key to use
     */
    static final Map<ClassFields<?>.Field<String>, String> LORE_HOLDERS = new HashMap<>();

    public static void harvestData(ASMDataTable table) {
        for (ASMData asmData : table.getAll(LoreHolder.class.getName())) {
            String className = asmData.getClassName();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                GTNHLib.LOG.error("Class " + className + " could not be found!", e);
                continue;
            }

            String fieldName = asmData.getObjectName();
            ClassFields<?>.Field<String> field;
            try {
                field = Fields.ofClass(clazz).getField(LookupType.DECLARED, fieldName, String.class);
            } catch (ClassCastException e) {
                GTNHLib.LOG.error("Field " + fieldName + " of class " + className + " is not of type java.lang.String!", e);
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

            String key = (String) asmData.getAnnotationInfo().get("value");
            if (StringUtils.isNotBlank(key)) {
                LORE_HOLDERS.put(field, key);
            }
        }
    }
}
