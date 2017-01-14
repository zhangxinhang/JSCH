package com.jsch.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class CSVUtil {

	public static List<String[]> readerCSVToList(File file) throws Exception {
		FileReader fReader = new FileReader(file);
		CSVReader csvReader = new CSVReader(fReader);
		List<String[]> list = csvReader.readAll();
		csvReader.close();
		return list;
	}

	public static void writeCSV(List<String[]> list) throws Exception {
		try {
			File file = new File("output.csv");
			Writer writer = new FileWriter(file);
			CSVWriter csvWriter = new CSVWriter(writer, ',');
			for (String[] strings : list) {
				csvWriter.writeNext(strings);
			}
			csvWriter.close();
		} catch (Exception e) {

		}
	}
	
	public static void main(String[] args) throws Exception {
		File file = new File("Succeedip.csv");
		writeCSV(readerCSVToList(file));
	}

}
