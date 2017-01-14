package com.jsch.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.jsch.util.CSVUtil;
import com.jsch.util.ShellUtils;

public class MainApplication {

	private static String handleOutputString(String out) {
		return out;
	}

	public static void main(String[] args) throws Exception {
		File file = new File("Succeedip.csv");
		List<String[]> list = CSVUtil.readerCSVToList(file);
		List<String[]> outputList = new ArrayList<String[]>();
		outputList.add(new String[] { "host", "username", "password", "sshVersion", "version" });
		for (String[] strings : list) {
			String out = "";
			if (strings[0] != null && !"".equals(strings[0])) {
				String host = strings[0].trim();
				String user = strings[1].trim();
				String passwd = strings[2].trim();
				String sshVersion = strings[3].trim();
				System.out.println(" begin host:" + host);
				out = ShellUtils.execCmd("lsb_release", user, passwd, host);
				outputList.add(new String[] { host, user, passwd, sshVersion, handleOutputString(out) });
			}
		}

		CSVUtil.writeCSV(outputList);
	}

}
