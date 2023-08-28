package uab.kopi.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExcelProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ExcelProcessor.class);
    private static final String fileNameToBeSaved = "rezultatas.xlsx";

    public static void processFile(File file, File folder, double number, boolean isPercent) {
        try {
            logger.info("Staring to process the file at: " + file.getAbsolutePath());
            String filename = file.getName();

            if(filename.startsWith("~$")) {
                logger.warn("The file needs to be renamed as it starts with ~$ at the beginning.");
                String newName = filename.substring(2); // Remove the first two characters
                File newFile = new File(file.getParent(), newName);
                if (file.renameTo(newFile)) {
                    logger.info("Renamed: " + filename + " -> " + newName);
                } else {
                    logger.error("Failed to rename: " + filename);
                    Alerter.displayError("Nepavyko perskaityti failo pavadinimo. Patikrinkite, ar failas nebrokuotas ir bandykite iš naujo.");
                    return;
                }
            }

            Workbook workbook = WorkbookFactory.create(file);
            Sheet sheet = workbook.getSheetAt(0);

            int infoRowIdx = findInfoRow(sheet);
            // The row after the info row is the one that contains the first entry
            int firstDataIdx = infoRowIdx + 1;
            int lastDataIdx = lastDataIdx(sheet);

            // Log the retrieved indexes
            logger.info("Info row index is: " + infoRowIdx);
            logger.info("First data row index is: " + firstDataIdx);
            logger.info("Last data row index is: " + lastDataIdx);

            int rowCount = lastDataIdx - firstDataIdx + 1;
            logger.info("There is a total of " + rowCount + " rows in the sheet");

            int numRowsToTake = (int) (isPercent ? (number / 100.0) * rowCount : Math.min(number, rowCount));
            if (isPercent) {
               logger.info("The specification mentioned taking " + number + " percent of rows to be taken.");
               logger.info("The specification mentioned taking " + number + " percent of rows to be taken.");
            }
            logger.info(numRowsToTake + " data rows will be taken randomly");

            // To check whether the indexes were found successfully
            if (rowCount < 0 || numRowsToTake < 0) {
                logger.error("Something wrong happened when getting the indexes");
                Alerter.displayError("Įvyko klaida. Iš viso duomenų eilučių yra " + rowCount + ".\n" +
                        "Nuspręsta atrankos būdu pasirinkti " + numRowsToTake + " eilučių.");
                return;
            } else if (infoRowIdx < 0) {
                logger.error("Something wrong happened when getting the indexes");
                Alerter.displayError("Nepavyko surasti eilutės nusakančios kas vaizduojama stulpeliuose.");
                return;
            }

            List<Integer> selectedRows = new ArrayList<>();
            Random random = new Random();

            // Select the rows randomly from all the data rows
            while (selectedRows.size() < numRowsToTake) {
                int randomRow = random.nextInt(lastDataIdx - firstDataIdx + 1) + firstDataIdx;
                if (!selectedRows.contains(randomRow)) {
                    selectedRows.add(randomRow);
                }
            }

            // Creating a new Excel workbook and sheet
            Workbook newWorkbook = new XSSFWorkbook();
            Sheet newSheet = newWorkbook.createSheet("Parinkti duomenys");
            Row headerRow = newSheet.createRow(0);

            logger.info("Copying the info row");
            Row infoRow = sheet.getRow(infoRowIdx);
            for (int cellNum = 0; cellNum < infoRow.getLastCellNum(); cellNum++) {
                Cell oldCell = infoRow.getCell(cellNum);
                Cell newCell = headerRow.createCell(cellNum);
                newCell.setCellValue(getCellValueAsString(oldCell));
            }

            try {
                logger.info("Copying the selected data rows");
                int newRowIdx = 1;
                for (int rowNum : selectedRows) {
                    Row oldRow = sheet.getRow(rowNum);
                    Row newRow = newSheet.createRow(newRowIdx);
                    for (int cellNum = 0; cellNum < oldRow.getLastCellNum(); cellNum++) {
                        Cell oldCell = oldRow.getCell(cellNum);
                        Cell newCell = newRow.createCell(cellNum);
                        newCell.setCellValue(getCellValueAsString(oldCell));
                    }
                    newRowIdx++;
                }
            }
            catch (Exception e) {
                logger.error("Something wrong happened when copying the data rows", e);
            }

            logger.info("Writing data to the new excel file");
            File outputFile = new File(folder, fileNameToBeSaved);
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                newWorkbook.write(outputStream);
                logger.info("The new file was successfully saved. Its name is: " + fileNameToBeSaved);
                Alerter.displayResult("Duomenys sėkmingai apdoroti ir išsaugoti.");
            } catch (IOException e) {
                logger.error("Something wrong happened when saving the new file", e);
                Alerter.displayError("Klaida saugant failą.");
            }
        } catch (IOException e) {
            logger.error("Something wrong happened when processing the file", e);
            Alerter.displayError("Klaida skaitant failą.");
        } catch (Exception e) {
            logger.error("Something wrong happened", e);
            Alerter.displayError("Nežinoma klaida.");
        }
    }

    private static int findInfoRow(Sheet sheet) {
        logger.info("Searching for the info row");
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                for (Cell cell : row) {
                    String cellValue = getCellValueAsString(cell);
                    if (!cellValue.isEmpty()) {
                        return rowIndex; // Return the first non-empty row as info row
                    }
                }
            }
        }
        return -1; // Info row not found
    }

    private static int lastDataIdx(Sheet sheet) {
        logger.info("Searching for the last data row");
        return sheet.getLastRowNum();
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
            case ERROR:
            case FORMULA:
            default:
                return "";
        }
    }
}

