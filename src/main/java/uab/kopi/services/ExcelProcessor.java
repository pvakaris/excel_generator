package uab.kopi.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * This class processes Excel files, extracts data, and saves it to a new Excel workbook and a text file.
 */
public class ExcelProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ExcelProcessor.class);
    private static final String EXCEL_FILE_NAME = "rezultatas.xlsx";
    private static final String TEXT_FILE_NAME = "paaiskinimas.txt";
    private static StringBuilder builder;

    private static int infoRowLength;

    /**
     * Processes the given Excel file, extracts data based on the provided criteria, and saves results.
     *
     * @param file      The Excel file to process.
     * @param folder    The folder where new files will be saved.
     * @param number    The number of rows or percentage of rows to be selected.
     * @param isPercent Indicates whether 'number' is a percentage.
     */
    public static void processFile(File file, File folder, double number, boolean isPercent) {
        builder = new StringBuilder();
        try {
            logger.info("Starting to process the file at: {}", file.getAbsolutePath());

            String filename = file.getName();
            builder.append("Atrenkami duomenys iš failo: ").append(file.getName()).append("\n");

            if (filename.startsWith("~$")) {
                // Handle files temporarily created by the Excel framework
                // Not in use currently
            }

            Workbook workbook = WorkbookFactory.create(file);
            Sheet sheet = workbook.getSheetAt(0);

            int infoRowIdx = findInfoRow(sheet);
            int firstDataIdx = infoRowIdx + 1;

            int lastDataIdx = lastDataIdx(sheet, firstDataIdx);
            int rowCount = lastDataIdx - firstDataIdx + 1;

            logGeneralInformation(infoRowIdx, firstDataIdx, lastDataIdx, rowCount);
            int numRowsToTake = calculateNumRowsToTake(number, isPercent, rowCount);
            logger.info("{} data rows will be taken randomly", numRowsToTake);

            if (rowCount < 0 || numRowsToTake < 0 || infoRowIdx < 0) {
                handleIndexError(rowCount, numRowsToTake);
                return;
            }

            List<Integer> selectedRows = selectRandomRows(firstDataIdx, lastDataIdx, numRowsToTake);

            Workbook newWorkbook = new XSSFWorkbook();
            Sheet newSheet = newWorkbook.createSheet("Parinkti duomenys");
            copyInfoRow(sheet.getRow(infoRowIdx), newSheet.createRow(0));

            copySelectedDataRows(sheet, newSheet, selectedRows);

            // Try creating a new Excel file with the processed data
            try {
                createNewExcelFile(newWorkbook, folder);
                logger.info("The new excel file was successfully saved. Its name is: {}", EXCEL_FILE_NAME);
            } catch (IOException e) {
                logger.error("An error occurred when writing to the Excel file", e);
                Alerter.displayError("Klaida bandant sukurti Excel'io failą.");
                return;
            } catch (Exception e) {
                handleUnknownError(e);
                return;
            }

            // Try to create a new text file explaining the processing
            try {
                createNewTextFile(folder);
                logger.info("The new text file was successfully saved. Its name is: {}", TEXT_FILE_NAME);
            } catch (IOException e) {
                logger.error("An error occurred when writing to the text file", e);
                Alerter.displayError("Klaida bandant išrašyti tekstinį failą.");
                return;
            } catch (Exception e) {
                handleUnknownError(e);
                return;
            }

            Alerter.displayResult("Duomenys sėkmingai apdoroti ir išsaugoti.\nNauja Excel'io lentelė išsaugota faile " + EXCEL_FILE_NAME + ".\nDuomenų apdorojimo paaiškinimas " +
                    "išsaugotas faile " + TEXT_FILE_NAME + ".\nAbu failai išsaugoti: " + folder.getAbsolutePath());
        } catch (Exception e) {
            handleUnknownError(e);
        }
    }

    /**
     * Handles unknown errors that occur during processing.
     *
     * @param e         The exception that occurred.
     */
    private static void handleUnknownError(Exception e) {
        logger.error("An unknown error occurred", e);
        Alerter.displayError("Nežinoma klaida.");
    }

    /**
     * Logs general information about the data and rows.
     *
     * @param infoRowIdx    Index of the info row.
     * @param firstDataIdx  Index of the first data row.
     * @param lastDataIdx   Index of the last data row.
     * @param rowCount      Total number of data rows.
     */
    private static void logGeneralInformation(int infoRowIdx, int firstDataIdx, int lastDataIdx, int rowCount) {
        logger.info("Info row index is: {}", infoRowIdx);
        logger.info("First data row index is: {}", firstDataIdx);
        logger.info("Last data row index is: {}", lastDataIdx);
        logger.info("Total data rows in the sheet: {}", rowCount);

        builder.append("Bendras duomenų eilučių skaičius: ").append(rowCount).append("\n");
        builder.append("Pirmos duomenų eilutės numeris: ").append(firstDataIdx + 1).append("\n");
        builder.append("Paskutinės duomenų eilutės numeris: ").append(lastDataIdx + 1).append("\n");
    }

    /**
     * Calculates the number of rows to be selected based on the given criteria.
     *
     * @param number     The number of rows or percentage.
     * @param isPercent  Indicates whether 'number' is a percentage.
     * @param rowCount   Total number of data rows.
     * @return The number of rows to be selected.
     */
    private static int calculateNumRowsToTake(double number, boolean isPercent, int rowCount) {
        if (isPercent) {
            int rowNumb = (int) (number / 100.0 * rowCount);
            logger.info("Taking {} percent of rows.", number);
            builder.append("Nustatyta atsitiktinės atrankos būdu atrinkti ").append(number).append("% visų duomenų eilučių.\n");
            builder.append("Bendras atriktų eilučių skaičius: ").append(rowNumb).append("\n");
            return rowNumb;
        } else {
            builder.append("Nustatyta atsitiktinės atrankos būdu atrinkti ").append((int) number).append(" duomenų eilutes.\n");
            return Math.min((int) number, rowCount);
        }
    }

    /**
     * Handles the case when index errors occur during processing.
     *
     * @param rowCount      Total number of data rows.
     * @param numRowsToTake Number of rows intended to be taken.
     */
    private static void handleIndexError(int rowCount, int numRowsToTake) {
        logger.error("An error occurred when getting the indexes");
        Alerter.displayError("Įvyko klaida. Iš viso duomenų eilučių yra " + rowCount + ".\n" +
                "Nuspręsta atrankos būdu pasirinkti " + numRowsToTake + " eilučių.");
    }

    /**
     * Selects random rows from the given range.
     *
     * @param firstDataIdx Index of the first data row.
     * @param lastDataIdx  Index of the last data row.
     * @param numRowsToTake Number of rows to be randomly selected.
     * @return List of randomly selected row indexes.
     */
    private static List<Integer> selectRandomRows(int firstDataIdx, int lastDataIdx, int numRowsToTake) {
        List<Integer> selectedRows = new ArrayList<>();
        Random random = new Random();
        builder.append("Atriktų eilučių numeriai:\n");
        builder.append("(Sename faile ---> naujame faile)\n\n");
        int newId = 2;

        while (selectedRows.size() < numRowsToTake) {
            int randomRow = random.nextInt(lastDataIdx - firstDataIdx + 1) + firstDataIdx;
            if (!selectedRows.contains(randomRow)) {
                selectedRows.add(randomRow);
                builder.append(randomRow + 1).append(" ---> ").append(newId).append("\n");
                newId++;
            }
        }

        return selectedRows;
    }

    /**
     * Copies the header row (info row) from the source sheet to the target sheet.
     *
     * @param oldInfoRow    Source header row.
     * @param newHeaderRow  Target header row.
     */
    private static void copyInfoRow(Row oldInfoRow, Row newHeaderRow) {
        logger.info("Copying the info row");
        for (int cellNum = 0; cellNum < oldInfoRow.getLastCellNum(); cellNum++) {
            Cell oldCell = oldInfoRow.getCell(cellNum);
            Cell newCell = newHeaderRow.createCell(cellNum);
            newCell.setCellValue(getCellValueAsString(oldCell));
        }
    }

    /**
     * Copies selected data rows from the old sheet to the new sheet.
     *
     * @param oldSheet      Source sheet.
     * @param newSheet      Target sheet.
     * @param selectedRows  List of selected row indexes.
     */
    private static void copySelectedDataRows(Sheet oldSheet, Sheet newSheet, List<Integer> selectedRows) {
        logger.info("Copying the selected data rows");
        int newRowIdx = 1;
        for (int rowNum : selectedRows) {
            Row oldRow = oldSheet.getRow(rowNum);
            Row newRow = newSheet.createRow(newRowIdx);
            for (int cellNum = 0; cellNum < infoRowLength; cellNum++) {
                Cell oldCell = oldRow.getCell(cellNum);
                Cell newCell = newRow.createCell(cellNum);
                newCell.setCellValue(getCellValueAsString(oldCell));
            }
            newRowIdx++;
        }
    }

    /**
     * Creates a new Excel file and writes the new workbook into it.
     *
     * @param newWorkbook New Excel workbook to be saved.
     * @param folder      Folder where the file will be saved.
     */
    private static void createNewExcelFile(Workbook newWorkbook, File folder) throws IOException{
        logger.info("Writing data to the new excel file");
        File outputFile = new File(folder, EXCEL_FILE_NAME);
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        newWorkbook.write(outputStream);
    }

    /**
     * Creates a new text file and writes the content from the StringBuilder into it.
     *
     * @param folder Folder where the file will be saved.
     */
    private static void createNewTextFile(File folder) throws IOException{
        File output = new File(folder, TEXT_FILE_NAME);
        try (FileWriter writer = new FileWriter(output)) {
            writer.write(builder.toString());
        }
    }

    /**
     * Finds the index of the first non-empty row in the given sheet.
     *
     * @param sheet The sheet to search for the info row.
     * @return Index of the info row, or -1 if not found.
     */
    private static int findInfoRow(Sheet sheet) {
        logger.info("Searching for the info row");
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                for (Cell cell : row) {
                    String cellValue = getCellValueAsString(cell);
                    if (!cellValue.isEmpty()) {
                        infoRowLength = row.getLastCellNum();
                        return rowIndex;
                    }
                }
            }
        }
        return -1; // Info row not found
    }

    /**
     * Determines the index of the last data row in the given sheet.
     *
     * @param sheet The sheet to find the last data row index.
     * @param firstDataIdx The index of the first data row.
     * @return Index of the last data row.
     */
    private static int lastDataIdx(Sheet sheet, int firstDataIdx) {
        logger.info("Searching for the last data row");

        int lastDataRowIdx = firstDataIdx;

        for (int rowIndex = firstDataIdx + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row currentRow = sheet.getRow(rowIndex);
            Row previousRow = sheet.getRow(rowIndex - 1);
            System.out.println("Current ID: " + rowIndex);
            System.out.println("Previous ID: " + (rowIndex-1));
            // Check if the current row is empty or shorter than the previous row
            if (currentRow == null || isRowShorter(currentRow, previousRow)) {
                break; // Data ends here
            }

            lastDataRowIdx = rowIndex;
        }

        return lastDataRowIdx;
    }

    /**
     * Determines if a given row is shorter than another row or is empty.
     *
     * @param row1 The first row for comparison.
     * @param row2 The second row for comparison.
     * @return True if row1 is shorter than row2 or is empty, false otherwise.
     */
    private static boolean isRowShorter(Row row1, Row row2) {
        if (row1 == null || row2 == null) {
            return false; // One of the rows is null, so not shorter
        }

        int numCellsInRow1 = row1.getLastCellNum();
        int numCellsInRow2 = row2.getLastCellNum();

        System.out.println("Current row length: " + numCellsInRow1 + ". First element ---> " + row1.getCell(0));
        System.out.println("Previous row length: " + numCellsInRow2 + ". First element ---> " + row2.getCell(0) + "\n");
        return numCellsInRow1 < numCellsInRow2;
    }


    /**
     * Retrieves the cell value as a string, handling different cell types.
     *
     * @param cell The cell from which to retrieve the value.
     * @return The cell value as a string.
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return formatDate(cell.getDateCellValue());
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
            case ERROR:
            case FORMULA:
            default:
                return "";
        }
    }

    /**
     * Formats a date object as a string.
     *
     * @param date The date to be formatted.
     * @return The formatted date string.
     */
    private static String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }
}