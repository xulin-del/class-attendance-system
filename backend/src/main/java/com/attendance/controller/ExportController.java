package com.attendance.controller;

import com.attendance.mapper.AttendanceRecordMapper;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
public class ExportController {
    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @GetMapping("/monthlyAttendance")
    public void exportMonthlyAttendance(@RequestParam String teacherId, HttpServletResponse response) throws IOException {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String endDate = sdf.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String startDate = sdf.format(cal.getTime());

        String currentMonth = new SimpleDateFormat("yyyy年MM月").format(Calendar.getInstance().getTime());
        List<Map<String, Object>> dailyStats = attendanceRecordMapper.selectDailyStatsByTeacherId(teacherId, startDate, endDate);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(currentMonth + "考勤报表");

        CellStyle titleStyle = createCenteredStyle(workbook, (short) 18, true);
        CellStyle headerStyle = createCenteredStyle(workbook, (short) 12, true);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        CellStyle dataStyle = createCenteredStyle(workbook, (short) 11, false);
        CellStyle summaryStyle = createCenteredStyle(workbook, (short) 12, true);
        summaryStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        summaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(34);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(currentMonth + " 考勤统计报表");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        Row headerRow = sheet.createRow(2);
        headerRow.setHeightInPoints(26);
        String[] headers = {"日期", "总人数", "已签到", "出勤率"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 3;
        int totalAll = 0;
        int signedAll = 0;
        for (Map<String, Object> stat : dailyStats) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(24);

            String date = String.valueOf(stat.get("date"));
            int total = ((Number) stat.get("total")).intValue();
            int signed = ((Number) stat.get("signed")).intValue();
            double rate = total > 0 ? (signed * 100.0 / total) : 0;

            createCell(row, 0, date, dataStyle);
            createCell(row, 1, total, dataStyle);
            createCell(row, 2, signed, dataStyle);
            createCell(row, 3, String.format("%.1f%%", rate), dataStyle);

            totalAll += total;
            signedAll += signed;
        }

        Row summaryRow = sheet.createRow(rowIndex + 1);
        summaryRow.setHeightInPoints(26);
        createCell(summaryRow, 0, "合计", summaryStyle);
        createCell(summaryRow, 1, totalAll, summaryStyle);
        createCell(summaryRow, 2, signedAll, summaryStyle);
        double totalRate = totalAll > 0 ? (signedAll * 100.0 / totalAll) : 0;
        createCell(summaryRow, 3, String.format("%.1f%%", totalRate), summaryStyle);

        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 14 * 256);
        sheet.setColumnWidth(2, 14 * 256);
        sheet.setColumnWidth(3, 14 * 256);
        sheet.createFreezePane(0, 3);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_" + new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime()) + ".xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private CellStyle createCenteredStyle(Workbook workbook, short fontSize, boolean bold) {
        Font font = workbook.createFont();
        font.setFontName("Microsoft YaHei");
        font.setFontHeightInPoints(fontSize);
        font.setBold(bold);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int column, int value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
