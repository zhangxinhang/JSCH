package com.jsch.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.jsch.util.CSVUtil;
import com.jsch.util.SSHClient;

public class MainApplication {

	private static String handleOutputString(String out) {
		StringBuilder sb = new StringBuilder();
		String str = out;
		int index = str.indexOf("dis");
		try {
			if (index > -1) {
				String newStr = str.substring(str.indexOf("dis"), str.length());
				int h3cSoftVersionIndex = newStr.indexOf("Comware Software");
				int hwSoftVersionIndex = newStr.indexOf("VRP (R) software");
				if (h3cSoftVersionIndex > -1) {
					sb.append(newStr.substring(h3cSoftVersionIndex + 18, newStr.indexOf("\n", h3cSoftVersionIndex)));
					sb.append(" | ");
				}
				if (hwSoftVersionIndex > -1) {
					sb.append(newStr.substring(hwSoftVersionIndex + 18, newStr.indexOf("\n", hwSoftVersionIndex)));
					sb.append(" | ");
				}

				int h3cVersionIndex = newStr.indexOf("H3C S");
				int hwVersionIndex = newStr.indexOf("Quidway S");
				int huaweiVersionIndex = newStr.indexOf("HUAWEI S");
				int updateIndex = newStr.indexOf("uptime");
				if (h3cVersionIndex > -1 && updateIndex > -1) {
					sb.append(newStr.substring(h3cVersionIndex, updateIndex));
					sb.append(" | ");
				}

				if (hwVersionIndex > -1 && updateIndex > -1) {
					sb.append(newStr.substring(hwVersionIndex, updateIndex));
					sb.append(" | ");
				}
				if (huaweiVersionIndex > -1 && updateIndex > -1) {
					sb.append(newStr.substring(huaweiVersionIndex, updateIndex));
					sb.append(" | ");
				}

			}
		} catch (Exception e) {
			System.out.println("origin:" + out);
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void main(String[] args) throws Exception {
		File file = new File("Succeedip.csv");
		List<String[]> list = CSVUtil.readerCSVToList(file);
		List<String[]> outputList = new ArrayList<String[]>();
		outputList.add(new String[] { "host", "username", "password", "sshVersion", "version1", "version2" });
		int size = list.size();
		int i = 0;
		System.out.println("***************begin:0,sum:" + size + "************************");
		for (String[] strings : list) {
			System.out.println("***************************************************************************");
			System.out.println("***************begin:" + (++i) + ",sum:" + size
					+ "***********************************************");
			System.out.println("***************************************************************************");
			String out = "";
			if (strings[0] != null && !"".equals(strings[0])) {
				String host = strings[0].trim();
				String user = strings[1].trim();
				String passwd = strings[2].trim();
				String sshVersion = strings[3].trim();
				System.out.println(" begin host:" + host);
				// out = ShellUtils.execCmd(cmd, user, passwd, host);
				out = SSHClient.execute(host, user, passwd, "display version");
				String[] retArr = handleOutputString(out).split(" \\| ");
				String version1 = retArr[0];
				String version2 = "";
				if (retArr.length > 1) {
					version2 = retArr[1];
				}
				outputList.add(new String[] { host, user, passwd, sshVersion, version1, version2 });
			}
		}

		CSVUtil.writeCSV(outputList);
	}

}
