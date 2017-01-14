package com.jsch.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ShellUtils {
	private static JSch jsch;
	private static Session session;

	public static void connect(String user, String passwd, String host) throws JSchException {

		jsch = new JSch();
		session = jsch.getSession(user, host, 22);
		session.setPassword(passwd);

		java.util.Properties config = new java.util.Properties();
		config.put("kex",
				"diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256");
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		System.out.println("connnect host:" + host);
		session.connect(3000);
	}

	public static String execCmd(String command, String user, String passwd, String host) {
		BufferedReader reader = null;
		Channel channel = null;
		StringBuilder sb = new StringBuilder();
		try {
			connect(user, passwd, host);
			if (command != null) {
				channel = session.openChannel("exec");
				((ChannelExec) channel).setCommand(command);

				channel.setInputStream(new ByteArrayInputStream(passwd.getBytes()));
				((ChannelExec) channel).setErrStream(System.err);

				channel.connect();
				System.out.println("exec comman for host:" + host);
				InputStream in = channel.getInputStream();
				reader = new BufferedReader(new InputStreamReader(in));
				String buf = null;
				while ((buf = reader.readLine()) != null) {
					sb.append(buf + "\n");
				}
			}
		} catch (Exception e) {
			sb.append("ERROR: no connect");
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (channel != null) {
				channel.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
		return sb.length() == 0 ? "ERROR: no output" : sb.toString();
	}

	public static void main(String[] args) throws Exception {
		System.out.println(execCmd("echo hello && lsb_release -a ", "root", "", "139.129.22.180"));
	}

}
