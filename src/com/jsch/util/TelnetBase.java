package com.jsch.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * telnet 基本连接类
 * 
 * @description
 * @author weichaofan
 * @date 2013年10月25日
 */
public class TelnetBase {
	private static final byte SB = (byte) 250;// 子选项开始
	private static final byte SE = (byte) 240;// 子选项结束
	private static final byte WILL = (byte) 251;// 选项协商
	private static final byte WONT = (byte) 252;// 选项协商
	private static final byte DO = (byte) 253;// 选项协商
	private static final byte DONT = (byte) 254;// 选项协商
	private static final byte IAC = (byte) 255;// 数据字节255
	private static final byte ECHO = (byte) 1;// 回显
	private static final byte IS = (byte) 0;// 是
	private static final byte SUPPRESS = (byte) 3;// 抑制继续进行
	private static final byte TT = (byte) 24;// 终端类型
	private InputStream is;
	private OutputStream os;
	private Socket client;
	private byte[] readBuffer = new byte[20 * 1024];
	private int miniReadIntervalMillSec = 3000;// 最短read阻塞间隔时间-毫秒
	private int connectTimeout = 1000;// 连接超时时间
	private int maxReadTimeout = 5000;

	public static String[] failTags = { "Failed", "fail", "incorrect" };
	public static String[] loginTags = { "$", "#", ">", "ogin", "@" };
	public static String[] commondEndTags = { "$", "#", ">" };
	public static String[] allTags = { "Failed", "fail", "incorrect", "$", "#", ">", "ogin", "@" };

	private String ip;
	private int port = 23;

	/**
	 * 
	 * 打开telnet连接
	 * 
	 * @param ip
	 * @param port
	 *            23
	 * 
	 * @return
	 * 
	 * @throws CmdException
	 */

	public TelnetBase(String ip) {

		this(ip, 23);

	}

	/**
	 * 
	 * 打开telnet连接
	 * 
	 * @param ip
	 * @param port
	 * @return
	 * @throws CmdException
	 */

	public TelnetBase(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	/**
	 * 连接
	 * 
	 * @return
	 * @throws Exception
	 */
	public String connect() throws Exception {
		try {

			client = new Socket();
			client.connect(new InetSocketAddress(ip, port), connectTimeout);
			client.setSoTimeout(miniReadIntervalMillSec);// 设置is的read方法阻塞时间
			is = client.getInputStream();
			os = client.getOutputStream();
		} catch (Exception e) {
			this.close();
			throw new Exception(e);
		}
		return readKeyWords("ogin:");
	}

	/**
	 * 
	 * 读取回显，并进行telnet协商
	 * 
	 * @return
	 * 
	 * @throws IOException
	 */

	public String recieveEcho() throws IOException {

		int len = is.read(this.readBuffer);

		ArrayList<Byte> bsList = new ArrayList<Byte>();
		ArrayList<Byte> cmdList = new ArrayList<Byte>();
		for (int i = 0; i < len; i++) {
			int b = this.readBuffer[i] & 0xff;// &0xff是为了防止byte的255溢出,java中byte的取值是-128~127
			if (b != 255) {
				if (b == '\n' || b == '\0') {// NVT中行结束符以'\r\n'表示，回车以'\r\0表示'
					continue;
				}
				bsList.add((byte) b);
				continue;
			}
			cmdList.add(IAC);
			switch (this.readBuffer[++i] & 0xff) {
			case 251:// 服务器想激活某选项
				if ((readBuffer[++i] & 0xff) == 1) {// 同意回显
					cmdList.add(DO);
					cmdList.add(ECHO);
				} else if ((readBuffer[i] & 0xff) == 3) {// 同意抑制继续执行
					cmdList.add(DO);
					cmdList.add(SUPPRESS);
					// cmdList.add(GA);
				} else {// 不同意其他类型协商
					cmdList.add(DONT);
					cmdList.add(readBuffer[i]);
				}
				break;
			case 253:// 服务器想让客户端发起激活某选项
				if ((readBuffer[++i] & 0xff) == 24) {// 终端类型
					cmdList.add(WONT);// 同意激活终端类型协商
					cmdList.add(TT);
				} else if ((readBuffer[i] & 0xff) == 1) {
					cmdList.add(WILL);
					cmdList.add(ECHO);
				} else {
					cmdList.add(WONT);// 不同意其他类型协商
					cmdList.add(readBuffer[i]);
				}
				break;
			case 250:// 子选项开始
				cmdList.add(SB);
				if ((readBuffer[++i] & 0xff) == 24 && (readBuffer[++i] & 0xff) == 1) {// 发送你的终端类型
					cmdList.add(TT);
					cmdList.add(IS);// 我的终端类型是
					cmdList.add((byte) 'V');
					cmdList.add((byte) 'T');
					cmdList.add((byte) '1');
					cmdList.add((byte) '0');
					cmdList.add((byte) '0');
				}
				break;
			case 240:// 子选项结束
				cmdList.add(SE);
				break;
			case 252:// 必须同意
				cmdList.add(DONT);
				cmdList.add(readBuffer[++i]);
				break;
			case 254:// 必须同意
				cmdList.add(WONT);
				cmdList.add(readBuffer[++i]);
				break;
			}
		}
		// 如果有协商则向服务端发送协商选项
		if (cmdList.size() > 0) {
			byte[] writeBuffer = new byte[cmdList.size()];
			for (int i = 0; i < cmdList.size(); i++) {
				writeBuffer[i] = cmdList.get(i);
			}
			os.write(writeBuffer);
		}

		// 组织回显字符
		int size = bsList.size();
		String str = "";
		if (size > 0) {
			byte[] bs = new byte[size];
			for (int i = 0; i < size; i++) {
				bs[i] = bsList.get(i).byteValue();
			}
			str = new String(bs, "gbk");
		} else {
			// 如果是协商，则回传协商信息
			if (cmdList.size() > 0) {
				str = recieveEcho();
			}
		}
		// log(len, cmdList);
		return str;
	}

	private void log(int len, ArrayList<Byte> cmdList) {
		System.out.println("read===== ");
		for (int i = 0; i < len; i++) {
			System.out.println(readBuffer[i] & 0xff);
			System.out.println(" ");
		}

		if (cmdList.size() > 0) {
			System.out.println("write==== ");
			for (int i = 0; i < cmdList.size(); i++) {
				System.out.println(cmdList.get(i) & 0xff);
				System.out.println(" ");
			}

		}
	}

	/**
	 * 用户名 命令中不要包括回车、换行
	 * 
	 * @param cmd
	 * @param keyWords
	 * @return
	 */
	public String sendUserName(String name) throws Exception {
		name += "\r\n";
		os.write(name.getBytes());

		return readKeyWords("assword");
	}

	/**
	 * 密码 命令中不要包括回车、换行
	 * 
	 * @param cmd
	 * @param keyWords
	 * @return
	 */
	public String sendUserPwd(String pwd) throws Exception {
		pwd += "\r\n";
		os.write(pwd.getBytes());

		return readKeyWords(allTags);
	}

	/**
	 * 命令中不要包括回车、换行
	 * 
	 * @param cmd
	 * @param keyWords
	 * @return
	 */
	public String sendCmd(String cmd, String... keyWords) throws Exception {

		return sendCmd(cmd, false, keyWords);
	}

	/**
	 * 命令中不要包括回车、换行
	 * 
	 * @param cmd
	 * @param keyWords
	 * @return
	 */
	public String sendCmd(String cmd, boolean excludeCommandCheck, String... keyWords) throws Exception {
		os.write((cmd + "\r\n").getBytes());

		if (!excludeCommandCheck) {
			return readKeyWords(cmd, maxReadTimeout, keyWords);
		} else {
			return readKeyWords(keyWords);
		}
	}

	/**
	 * 命令中不要包括回车、换行 默认搜索条件为$、#、> 不包含执行命令中的关键字
	 * 
	 * @param cmd
	 * @param keyWords
	 * @return
	 */
	public String sendCommand(String cmd) throws Exception {

		return sendCommand(cmd, false);
	}

	/**
	 * 命令中不要包括回车、换行 默认搜索条件为$、#、> 是否包含执行命令中的关键字
	 * 
	 * @param cmd
	 * @param keyWords
	 * @return
	 */
	public String sendCommand(String cmd, boolean excludeCommandCheck) throws Exception {

		os.write((cmd + "\r\n").getBytes());
		if (!excludeCommandCheck) {
			return readKeyWords(cmd, maxReadTimeout, commondEndTags);
		} else {
			return readKeyWords(commondEndTags);
		}
	}

	/**
	 * 命令中不要包括回车、换行 默认搜索条件为$、#、> 不包含执行命令中的关键字
	 * 
	 * @param cmd
	 * @param timeOut
	 * @param keyWords
	 * @return
	 */
	public String sendCommand(String cmd, long timeOut) throws Exception {

		return sendCommand(cmd, timeOut, false);
	}

	/**
	 * 命令中不要包括回车、换行 默认搜索条件为$、#、> 是否包含执行命令中的关键字
	 * 
	 * @param cmd
	 * @param timeOut
	 * @param keyWords
	 * @return
	 */
	public String sendCommand(String cmd, long timeOut, boolean excludeCommandCheck) throws Exception {
		os.write((cmd + "\r\n").getBytes());

		if (!excludeCommandCheck) {
			return readKeyWords(cmd, timeOut, commondEndTags);
		} else {
			return readKeyWords(timeOut, commondEndTags);
		}

	}

	/**
	 * 命令中不要包括回车、换行
	 * 
	 * @param cmd
	 * @param timeOut
	 * @param keyWords
	 * @return
	 */
	public String sendCmd(String cmd, long timeOut, String... keyWords) throws Exception {

		return sendCmd(cmd, false, timeOut, keyWords);
	}

	/**
	 * 命令中不要包括回车、换行
	 * 
	 * @param cmd
	 * @param timeOut
	 * @param keyWords
	 * @return
	 */
	public String sendCmd(String cmd, boolean excludeCommandCheck, long timeOut, String... keyWords) throws Exception {
		os.write((cmd + "\r\n").getBytes());
		if (!excludeCommandCheck) {
			return readKeyWords(cmd, timeOut, keyWords);
		} else {
			return readKeyWords(timeOut, keyWords);
		}

	}

	/**
	 * 
	 * 关闭telnet连接
	 */

	public void close() {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (os != null) {
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (client != null) {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * 读取期望值，使用默认超时时间5秒
	 * 
	 * @param keyWords
	 * 
	 * @return
	 */

	public String readKeyWords(String... keyWords) {

		return this.readKeyWords(maxReadTimeout, keyWords);

	}

	/**
	 * 
	 * 读取期望值
	 * 
	 * @param timeOut
	 *            超时时间
	 * 
	 * @param keyWords
	 * 
	 * @return
	 * 
	 * @throws CmdException
	 */

	public String readKeyWords(long timeOut, String... keyWords) {
		String rv = "";
		long nextTime = 0;
		long endTime = System.currentTimeMillis() + timeOut;
		do {
			try {
				String _rv = this.recieveEcho();
				rv += _rv;
			} catch (IOException e) {

				nextTime = endTime - System.currentTimeMillis();
			}
		} while (!this.findKeyWord(keyWords, rv) && nextTime >= 0);
		if (nextTime < 0)
			System.err.println("Read TimeOut...Echo:\n" + rv);
		return rv;

	}

	/**
	 * 
	 * 读取期望值 排除command中含有的关键字
	 * 
	 * @param timeOut
	 *            超时时间
	 * 
	 * @param keyWords
	 * 
	 * @return
	 * 
	 * @throws CmdException
	 */

	public String readKeyWords(String command, long timeOut, String... keyWords) {
		String rv = "";
		long nextTime = 0;
		long endTime = System.currentTimeMillis() + timeOut;
		do {
			try {
				String _rv = this.recieveEcho();
				rv += _rv;
			} catch (IOException e) {

				nextTime = endTime - System.currentTimeMillis();
			}
		} while (!this.findKeyWord(command, keyWords, rv) && nextTime >= 0);
		if (nextTime < 0)
			System.err.println("Read TimeOut...Echo:\n" + rv);
		return rv;

	}

	/**
	 * 
	 * 查找关键字
	 * 
	 * @param keyWords
	 * 
	 * @param str
	 * 
	 * @return
	 */

	public boolean findKeyWord(String[] keyWords, String str) {
		if (str == null || "".equals(str))
			return false;
		if (keyWords == null || keyWords.length == 0)
			return true;
		for (int i = 0; i < keyWords.length; i++) {
			if (str.indexOf(keyWords[i]) != -1)
				return true;
		}
		return false;
	}

	/**
	 * 
	 * 查找关键字 排除command中含有的关键字
	 * 
	 * @param keyWords
	 * 
	 * @param str
	 * 
	 * @return
	 */

	public boolean findKeyWord(String command, String[] keyWords, String str) {
		if (str == null || "".equals(str))
			return false;
		if (keyWords == null || keyWords.length == 0)
			return true;
		System.out.println(str);
		if (-1 != str.indexOf(command)) {
			str = str.substring(str.indexOf(command) + command.length());
			for (int i = 0; i < keyWords.length; i++) {
				if (str.indexOf(keyWords[i]) != -1)
					return true;
			}
		}

		return false;
	}

	/**
	 * 最短读阻塞时间
	 * 
	 * @return
	 */
	public int getMiniReadIntervalMillSec() {
		return miniReadIntervalMillSec;
	}

	public void setMiniReadIntervalMillSec(int miniReadIntervalMillSec) {
		this.miniReadIntervalMillSec = miniReadIntervalMillSec;
	}

	/**
	 * 连接超时时间
	 * 
	 * @return
	 */
	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 最大读阻塞时间
	 * 
	 * @return
	 */
	public int getMaxReadTimeout() {
		return maxReadTimeout;
	}

	public void setMaxReadTimeout(int maxReadTimeout) {
		this.maxReadTimeout = maxReadTimeout;
	}

	public static void main(String[] args) throws Exception {
		TelnetBase tb = new TelnetBase("192.168.0.99");
		tb.setConnectTimeout(3000);
		System.out.println(tb.connect());
		System.out.println(tb.sendUserName("zxh"));
		System.out.println(tb.sendUserPwd("zxhaxr"));
		System.out.println("---------------"+tb.sendCmd("pwd")+"-------------");
		System.out.println("****"+tb.recieveEcho()+"********");
		tb.close();
		
	}
}
