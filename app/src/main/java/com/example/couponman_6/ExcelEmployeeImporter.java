package com.example.couponman_6;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ExcelEmployeeImporter {
    public static List<CouponTargetDraft> importFile(Context context, Uri uri) throws Exception {
        String lower = uri.toString().toLowerCase();
        if (lower.endsWith(".csv") || lower.endsWith(".txt")) {
            return importCsv(context.getContentResolver(), uri);
        }
        return importWorkbook(context.getContentResolver(), uri);
    }

    private static List<CouponTargetDraft> importWorkbook(ContentResolver resolver, Uri uri) throws Exception {
        List<CouponTargetDraft> drafts = new ArrayList<>();
        try (InputStream inputStream = resolver.openInputStream(uri);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false;
                    continue;
                }
                drafts.add(toDraft(
                        formatter.formatCellValue(row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)),
                        formatter.formatCellValue(row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
                ));
            }
        }
        drafts.removeIf(item -> !item.isValid());
        return drafts;
    }

    private static List<CouponTargetDraft> importCsv(ContentResolver resolver, Uri uri) throws Exception {
        List<CouponTargetDraft> drafts = new ArrayList<>();
        try (InputStream inputStream = resolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean firstRow = true;
            while ((line = reader.readLine()) != null) {
                if (firstRow) {
                    firstRow = false;
                    continue;
                }
                String[] parts = line.split(",", -1);
                drafts.add(toDraft(
                        parts.length > 0 ? parts[0] : "",
                        parts.length > 1 ? parts[1] : ""
                ));
            }
        }
        drafts.removeIf(item -> !item.isValid());
        return drafts;
    }

    private static CouponTargetDraft toDraft(String name, String employeeCode) {
        CouponTargetDraft draft = new CouponTargetDraft();
        draft.setName(name != null ? name.trim() : "");
        draft.setEmployeeCode(employeeCode != null ? employeeCode.trim() : "");
        draft.setSelected(true);
        return draft;
    }
}
