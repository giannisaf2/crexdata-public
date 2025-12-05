/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 */
package com.rapidminer.extension.admin;

import com.rapidminer.extension.admin.operator.aihubapi.exceptions.AdminToolsIllegalArgumentException;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Set;

public final class AssertUtility {
    private AssertUtility() {
        throw new IllegalStateException("Utility class");
    }

    public static void hasText(String value, String message) throws AdminToolsIllegalArgumentException {
        if (StringUtils.isBlank((CharSequence)value)) {
            throw new AdminToolsIllegalArgumentException(message);
        }
    }

    public static void notNull(Object value, String message) throws AdminToolsIllegalArgumentException {
        if (value == null) {
            throw new AdminToolsIllegalArgumentException(message);
        }
    }

    public static void atLeastOneNotNull(Collection values2, String message) throws AdminToolsIllegalArgumentException {
        for (Object value : values2) {
            if (value == null) continue;
            return;
        }
        throw new AdminToolsIllegalArgumentException(message);
    }

    public static void allHaveText(Set<String> values2, String message) throws AdminToolsIllegalArgumentException {
        if (values2 != null) {
            for (String group : values2) {
                AssertUtility.hasText(group, message);
            }
        }
    }

    public static void verifyCondition(boolean condition, String message) throws AdminToolsIllegalArgumentException {
        if (!condition) {
            throw new AdminToolsIllegalArgumentException(message);
        }
    }

    public static void verifyLengthLE(String property, String propertyName, int maxLength) throws AdminToolsIllegalArgumentException {
        AssertUtility.hasText(property, String.format("'%s' cannot be blank", propertyName));
        AssertUtility.hasText(propertyName, "'propertyName' cannot be blank");
        AssertUtility.verifyCondition(property.length() <= maxLength, String.format("'%s' cannot be longer than %s characters", propertyName, maxLength));
    }
}

