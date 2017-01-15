package com.jsch.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

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
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		File file = new File("123.csv");
		List<String[]> list = readerCSVToList(file);
		for (String[] strings : list) {
			// System.out.println(strings[4]);
			StringBuilder sb = new StringBuilder();
			String str = strings[4];
			int index = str.indexOf("dis");
			if (index > -1) {
				String newStr = str.substring(str.indexOf("dis"), str.length());
				int h3cSoftVersionIndex = newStr.indexOf("Comware Software");
				int hwSoftVersionIndex = newStr.indexOf("VRP (R) software");
				if (h3cSoftVersionIndex > -1) {
					sb.append(newStr.substring(h3cSoftVersionIndex + 18, newStr.indexOf("\n", h3cSoftVersionIndex)));
					sb.append(" | ");
				}
				if (hwSoftVersionIndex > -1) {
					sb.append(newStr.substring(h3cSoftVersionIndex + 18, newStr.indexOf("\n", hwSoftVersionIndex)));
					sb.append(" | ");
				}

				int h3cVersionIndex = newStr.indexOf("H3C S");
				int hwVersionIndex = newStr.indexOf("Quidway S");
				int updateIndex = newStr.indexOf("uptime");
				if (h3cVersionIndex > -1 && updateIndex > -1) {
					sb.append(newStr.substring(h3cVersionIndex, updateIndex));
					sb.append(" | ");
				}

				if (hwVersionIndex > -1 && updateIndex > -1) {
					sb.append(newStr.substring(hwVersionIndex, updateIndex));
					sb.append(" | ");
				}
				System.out.println(sb.toString());
				System.out.println(sb.toString().split(" \\| ").length);
			}
		}
	}

}
