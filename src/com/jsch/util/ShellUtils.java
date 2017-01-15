package com.jsch.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import expect4j.Expect4j;

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

	public static Channel superSession(Session session, String sudo_pass) throws Exception {

		ChannelExec channel = (ChannelExec) session.openChannel("exec");
		// SUDO to bamboo user
		String command = "su";
		channel.setCommand(command);

		// InputStream in = channel.getInputStream();
		channel.setInputStream(null, true);

		OutputStream out = channel.getOutputStream();
		// channel.setErrStream(System.err);
		channel.setOutputStream(System.out, true);
		channel.setExtOutputStream(System.err, true);
		// Test change
		channel.setPty(false);
		channel.connect();

		out.write((sudo_pass + "\n").getBytes());
		out.flush();

		return channel;
	}

	public static String execCmd(String command, String user, String passwd, String host) {
		BufferedReader reader = null;
		Channel channel = null;
		StringBuilder sb = new StringBuilder();
		try {
			connect(user, passwd, host);
			if (command != null) {
				channel = session.openChannel("exec");
				((ChannelExec) channel).setPty(true);
				((ChannelExec) channel).setCommand(command);
				channel.setInputStream(null);
				((ChannelExec) channel).setErrStream(System.err);
				channel.connect();
				System.out.println("exec comman for host:" + host);
				OutputStream out = channel.getOutputStream();
				out.write((passwd + "\n").getBytes());
				out.write(" ls -a \n".getBytes());
				out.flush();
				InputStream in = channel.getInputStream();
				reader = new BufferedReader(new InputStreamReader(in));
				String buf = null;
				while ((buf = reader.readLine()) != null) {
					System.out.println(buf);
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

	public static String shellCmd(String command, String user, String passwd, String host) {
		BufferedReader reader = null;
		Channel channel = null;
		StringBuilder sb = new StringBuilder();
		try {
			connect(user, passwd, host);
			if (command != null) {
				channel = session.openChannel("shell");
				Expect4j expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
				channel.connect();
				expect.send("pwd");
				 
			
			}
		} catch (Exception e) {

			sb.append("ERROR: no connect");
			e.printStackTrace();
		} finally {

		}
		return sb.length() == 0 ? "ERROR: no output" : sb.toString();
	}

	public static void main(String[] args) throws Exception {
		System.out.println("++++++++++" + shellCmd(" su \n ls -a", "zxh", "zxhaxr", "192.168.0.99"));
	}

}
