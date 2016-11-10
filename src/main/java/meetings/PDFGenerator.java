package meetings;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PDFGenerator {
    private final Document document = new Document(PageSize.A4);
    private final Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private final Font contentFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);


    public PDFGenerator(Path path) {
        try {
            Files.createDirectories(path.getParent());
            PdfWriter.getInstance(document, Files.newOutputStream(path));
            document.open();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("Couldn't save File");
            a.setContentText(e.getMessage());
            a.show();
        }
    }

    public void generate(Event event) {
        try {
            document.add(new Paragraph("Participants of event " + event.getName(), titleFont));
            int prod = 0, proj = 0;
            PdfPTable prodtable = new PdfPTable(3);
            prodtable.setWidthPercentage(100);
            PdfPTable projtable = new PdfPTable(3);
            projtable.setWidthPercentage(100);
            for (Participant p : event.participants) {
                if (p instanceof Producer) {
                    prodtable.addCell(contentCell(p.getName()));
                    prod++;
                } else {
                    proj++;
                    projtable.addCell(contentCell(p.getName()));
                }
            }
            for (int i = 0; i < 3 - prod % 3; i++) {
                prodtable.addCell(contentCell(""));
            }
            for (int i = 0; i < 3 - proj % 3; i++) {
                projtable.addCell(contentCell(""));
            }
            prodtable.setSpacingBefore(5);
            prodtable.setSpacingAfter(20);
            projtable.setSpacingBefore(5);

            document.add(new Paragraph("Participants:"));
            document.add(prodtable);
            document.add(new Paragraph("Projects:"));
            document.add(projtable);
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
    }

    public void generate(Activity activity) {
        try {
            Paragraph timeStamp = new Paragraph("Generated on: " + LocalDateTime.now().
                    format(DateTimeFormatter.ofPattern("E d/M HH:mm")));

            timeStamp.setAlignment(Element.ALIGN_RIGHT);
            timeStamp.setSpacingAfter(20);
            document.add(timeStamp);

            List<TimeSlot> timeSlots = DB.meetings.stream()
                    .map(m -> m.timeSlot)
                    .filter(ts -> ts.activity == activity)
                    .distinct()
                    .collect(Collectors.toList());
            timeSlots.sort((o1, o2) -> o1.start.compareTo(o2.start));
            for (TimeSlot ts : timeSlots) {
                Paragraph header = new Paragraph(ts.toString() + " - Meeting Sheet", new Font(Font.FontFamily.HELVETICA, 14));
                header.setSpacingBefore(20);
                header.setSpacingAfter(15);
                document.add(header);

                PdfPTable table = new PdfPTable(2);
                table.addCell(titleCell("Project"));
                table.addCell(titleCell("Participant"));

                List<Meeting> list = DB.meetings.stream().filter(m -> ts.equals(m.timeSlot)).collect(Collectors.toList());
                for (Meeting m : list) {
                    table.addCell(contentCell(m.project.getName()));
                    table.addCell(contentCell(m.producer.getName()));
                }
                table.setSpacingAfter(20);
                document.add(table);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
    }

    public void generate(Participant... participants) {
        if (participants.length == 0) return;
        if(Export.simpleExport.get()) {
            if(participants[0] instanceof Project) generateSimple(Arrays.copyOf(participants, participants.length, Project[].class));
            if(participants[0] instanceof Producer) generateSimple(Arrays.copyOf(participants, participants.length, Producer[].class));
        } else {
            generateComplex(participants);
        }
    }

    /* public void generateReport() {
        try {
            for (Activity activity : DB.activities) {
                document.newPage();

                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new int[]{12, 12, 10, 12});

                table.addCell(titleCell("Activity"));
                table.addCell(titleCell("Info"));
                table.addCell(titleCell("Location"));
                table.addCell(titleCell("Time Slot"));

                List<ScheduleEntry> list = p.getSchedule();

                //if (list.isEmpty()) return;

                for (ScheduleEntry tr : list) {
                    table.addCell(contentCell(tr.activity));
                    table.addCell(contentCell(tr.info));
                    table.addCell(contentCell(tr.location));
                    table.addCell(contentCell(tr.timeSlot.toString()));
                }

                table.setSpacingAfter(20);
                document.add(table);
                footer();
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
    }
*/
    private void generateComplex(Participant... participants) {
        try {
            for (Participant p : participants) {
                document.newPage();
                header(p.getName());

                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new int[]{12, 12, 10, 12});

                table.addCell(titleCell("Activity"));
                table.addCell(titleCell("Info"));
                table.addCell(titleCell("Location"));
                table.addCell(titleCell("Time Slot"));

                List<ScheduleEntry> list = p.getSchedule();

                //if (list.isEmpty()) return;

                for (ScheduleEntry tr : list) {
                    table.addCell(contentCell(tr.activity));
                    table.addCell(contentCell(tr.info));
                    table.addCell(contentCell(tr.location));
                    table.addCell(contentCell(tr.timeSlot.toString()));
                }

                table.setSpacingAfter(20);
                document.add(table);
                footer();
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
    }

    private void generateSimple(Producer... producers) {
        try {
            for (Producer p : producers) {
                document.newPage();
                header(p.getName());

                PdfPTable table = new PdfPTable(2);
                table.addCell(titleCell("Project"));
                table.addCell(titleCell("Time Slot"));

                List<Meeting> list = DB.meetings.stream().filter(m -> p.equals(m.producer)).collect(Collectors.toList());
                list.sort((o1, o2) -> o1.timeSlot.start.compareTo(o2.timeSlot.start));
                for (Meeting m : list) {
                    table.addCell(contentCell(m.project.getName()));
                    table.addCell(contentCell(m.timeSlot.toString()));
                }
                table.setSpacingAfter(20);
                document.add(table);
                footer();
            }
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private void generateSimple(Project... projects) {
        try {
            for (Project p : projects) {
                document.newPage();
                header(p.getName());

                PdfPTable table = new PdfPTable(3);
                table.addCell(titleCell("Representative"));
                table.addCell(titleCell("Company"));
                table.addCell(titleCell("Time Slot"));

                List<Meeting> list = DB.meetings.stream().filter(m -> p.equals(m.project)).collect(Collectors.toList());
                list.sort((o1, o2) -> o1.timeSlot.start.compareTo(o2.timeSlot.start));
                for (Meeting m : list) {
                    table.addCell(contentCell(m.producer.getName()));
                    table.addCell(contentCell(m.producer.getCompany()));
                    table.addCell(contentCell(m.timeSlot.toString()));
                }
                table.setSpacingAfter(20);
                document.add(table);
                footer();
            }
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private void header(String name) throws DocumentException {
        Paragraph timeStamp = new Paragraph("Generated on: " + LocalDateTime.now().
                format(DateTimeFormatter.ofPattern("E d/M HH:mm")));

        timeStamp.setAlignment(Element.ALIGN_RIGHT);
        timeStamp.setSpacingAfter(20);
        document.add(timeStamp);

        try {
            Image logo = Image.getInstance(getClass().getResource("/logo.jpg"));
            float scale = ((document.getPageSize().getWidth()
                    - document.leftMargin() - document.rightMargin()) / logo.getWidth()) * 50;
            logo.scalePercent(scale);
            document.add(logo);
        } catch (IOException e) {
            e.printStackTrace();
        }



        String text = Export.header.get();
        text = text.replaceAll("#name", name);
        Paragraph header = new Paragraph(text, new Font(Font.FontFamily.HELVETICA, 14));
        header.setSpacingBefore(20);
        header.setSpacingAfter(15);
        document.add(header);
    }

    private void footer() throws DocumentException {
        Paragraph footer = new Paragraph(Export.footer.get(), new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC));
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);
    }

    private PdfPCell titleCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content, titleFont));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setCellEvent((cell1, position, canvases) -> {
            PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
            canvas.saveState();
            canvas.moveTo(position.getRight(), position.getBottom());
            canvas.lineTo(position.getLeft(), position.getBottom());
            canvas.stroke();
            canvas.restoreState();
        });
        return cell;
    }

    private PdfPCell contentCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content, contentFont));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setCellEvent((cell1, position, canvases) -> {
            PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
            canvas.saveState();
            canvas.setLineCap(PdfContentByte.LINE_CAP_ROUND);
            canvas.setLineDash(0, 4, 2);
            canvas.moveTo(position.getRight(), position.getBottom());
            canvas.lineTo(position.getLeft(), position.getBottom());
            canvas.stroke();
            canvas.restoreState();
        });
        return cell;
    }

}
