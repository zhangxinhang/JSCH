package com.jsch.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.oro.text.regex.MalformedPatternException;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;

/**
 * Hello world!
 *
 */
public class SSHClient {
	private static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
	private static String ENTER_CHARACTER = "\r";
	private static final int SSH_PORT = 22;
	private List<String> lstCmds = new ArrayList<String>();
	private static String[] linuxPromptRegEx = new String[] { "/home/sshtest#", "$", "~$", "Password:",
			"[sudo] password for sshtest:", "Do you want to continue [Y/n]?" };

	private Expect4j expect = null;
	private StringBuilder buffer = new StringBuilder();
	private String userName;
	private String password;
	private String host;
	ChannelShell channel = null;
	Session session = null;
	private String tempBuffer;

	/**
	 *
	 * @param host
	 * @param userName
	 * @param password
	 */
	public SSHClient(String host, String userName, String password) {
		super();
		this.userName = userName;
		this.password = password;
		this.host = host;
	}

	public String execute(List<String> cmdsToExecute, String password2) {
		this.lstCmds = cmdsToExecute;

		Closure closure = new Closure() {
			public void run(ExpectState expectState) throws Exception {
				tempBuffer = expectState.getBuffer();
				buffer.append(tempBuffer);
			}
		};
		List<Match> lstPattern = new ArrayList<Match>();
		for (String regexElement : linuxPromptRegEx) {
			try {
				Match mat = new RegExpMatch(regexElement, closure);
				lstPattern.add(mat);
			} catch (MalformedPatternException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {

			expect = SSH();

			boolean isSuccess = true;
			// root login
			System.out.println("root login:" + isSuccess(lstPattern, "su", 2000));
			System.out.println(isSuccess(lstPattern, password2, 2000));
			for (String strCmd : lstCmds) {
				isSuccess = isSuccess(lstPattern, strCmd, 2000);
				if (!isSuccess) {
					isSuccess = isSuccess(lstPattern, strCmd, 2000);
				}
			}

			checkResult(expect.expect(lstPattern));

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			closeConnection();
		}
		return buffer.toString();

	}

	/**
	 *
	 * @param objPattern
	 * @param strCommandPattern
	 * @return
	 */
	private boolean isSuccess(List<Match> objPattern, String strCommandPattern, int sleepTime) {
		sleepTime = 1000;
		try {
			boolean isFailed = checkResult(expect.expect(objPattern));
			System.out.println("tempBuffer: " + tempBuffer);
			if (!isFailed) {
				expect.send(strCommandPattern);
				expect.send(ENTER_CHARACTER);

				Thread.sleep(sleepTime);

				return true;
			}
			return false;
		} catch (MalformedPatternException ex) {
			ex.printStackTrace();
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 *
	 * @param hostname
	 * @param username
	 * @param password
	 * @param port
	 * @return
	 * @throws Exception
	 */
	private Expect4j SSH() throws Exception {
		JSch jsch = new JSch();
		session = jsch.getSession(userName, host, SSH_PORT);
		if (password != null) {
			session.setPassword(password);
		}

		Hashtable<String, String> config = new Hashtable<String, String>();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.connect(3000);
		channel = (ChannelShell) session.openChannel("shell");
		Expect4j expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
		channel.connect(3000);
		return expect;
	}

	/**
	 *
	 * @param intRetVal
	 * @return
	 */
	private boolean checkResult(int intRetVal) {
		if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
			return true;
		}
		return false;
	}

	/**
	 *
	 */
	private void closeConnection() {
		if (expect != null) {
			expect.close();
		}
		if (channel != null) {
			channel.disconnect();
		}
		if (session != null) {
			session.disconnect();
		}
	}

	public static String execute(String host, String userName, String password, String... cmd) {
		SSHClient ssh = new SSHClient(host, userName, password);
		List<String> cmdsToExecute = new ArrayList<String>();
		for (int i = 0; i < cmd.length; i++) {
			cmdsToExecute.add(cmd[i]);
		}

		return ssh.execute(cmdsToExecute, password);
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * SSHClient ssh = new SSHClient("192.168.0.99", "zxh", "zxhaxr");
		 * List<String> cmdsToExecute = new ArrayList<String>();
		 * cmdsToExecute.add("pwd"); String outputLog =
		 * ssh.execute(cmdsToExecute, "zxhaxr");
		 */
		System.out.println("outputlog: " + execute("192.168.0.99", "zxh", "zxhaxr", "pwd"));
	}
}
