package org.example.hrmanagement.module.document.constant;

import java.util.Map;

public final class DocumentType {

    public static final int LABOR_CONTRACT = 1;
    public static final int NDA = 2;
    public static final int SALARY_CONFIRM = 3;
    public static final int OTHER = 4;

    public static final Map<Integer, String> LABELS = Map.of(
            LABOR_CONTRACT, "劳动合同",
            NDA, "保密协议",
            SALARY_CONFIRM, "薪资确认单",
            OTHER, "其他"
    );

    private DocumentType() {
    }

    public static String label(Integer type) {
        if (type == null) {
            return "其他";
        }
        return LABELS.getOrDefault(type, "其他");
    }

    public static boolean valid(Integer type) {
        return type != null && LABELS.containsKey(type);
    }
}
