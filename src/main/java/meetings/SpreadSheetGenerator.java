package meetings;

import meetings.Export;
import meetings.Main;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SpreadSheetGenerator {

    public static void generate() {
        Path path = Paths.get(Main.exportDir + "/report.xlsx");
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Sheet 1");

        int rownum = 0;

        Row row = sheet.createRow(rownum++);
        row.createCell(0).setCellValue("Participants with at least one meeting:");
        row.createCell(1).setCellValue(DB.meetings.stream().map(m -> m.producer).distinct().count());

        row = sheet.createRow(rownum++);
        row.createCell(0).setCellValue("Participants with at least one event:");
        row.createCell(1).setCellValue(DB.producers.stream().map(p -> DB.events.stream().anyMatch(e -> e.has(p))).count());
        rownum++;

        row = sheet.createRow(rownum++);
        row.createCell(0).setCellValue("Project");
        row.createCell(1).setCellValue("Number of meetings");

        for (Project project : DB.projects) {
            row = sheet.createRow(rownum++);

            row.createCell(0).setCellValue(project.getName());
            row.createCell(1).setCellValue(DB.meetings.stream().filter(m -> m.has(project)).count());
        }


        try (OutputStream out = Files.newOutputStream(path)) {
            workbook.write(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Export.openFile(path);

    }

}
