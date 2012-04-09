/*
 * JBoss, Home of Professional Open Source. Copyright 2011, Red Hat, Inc., and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.jboss.web.comet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Random;

/**
 * {@code CometServletClientTest}
 * <p/>
 * 
 * Created on Oct 12, 2011 at 4:46:13 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class CometServletClientTest extends Thread {

	public static final String DEFAULT_URL = "http://localhost:8080/comet/CometServletTest";
	public static final String CRLF = "\r\n";
	public static final int MAX = 1000;
	public static final int N_THREADS = 100;
	public static final int DEFAULT_DELAY = 1000; // default wait delay 1000ms
	private long max_time = Long.MIN_VALUE;
	private long min_time = Long.MAX_VALUE;
	private double avg_time = 0;
	// private Exception ex = null;
	// boolean failed = false;
	private Socket socket;
	private URL url;
	private int max;
	private int delay;
	private String lastPartialSess = null;

	/**
	 * Create a new instance of {@code CometServletClientTest}
	 * 
	 * @param url
	 *            the service URL
	 * @param d_max
	 *            the maximum number of requests
	 * @param delay
	 * @throws Exception
	 *             if the URL is MalFormed
	 */
	public CometServletClientTest(URL url, int d_max, int delay) throws Exception {
		this.url = url;
		// this.max = 55 * 1000 / delay;
		this.max = d_max;
		this.delay = delay;
	}

	/**
	 * Create a new instance of {@code CometServletClientTest}
	 * 
	 * @param max
	 *            the maximum number of requests
	 * @throws Exception
	 */
	public CometServletClientTest(int max) throws Exception {
		this(new URL(DEFAULT_URL), max, DEFAULT_DELAY);
	}

	@Override
	public void run() {
		try {
			// wait for 2 seconds until all threads are ready
			sleep(2 * DEFAULT_DELAY);
			socket = new Socket(this.url.getHost(), this.url.getPort());
			runit();
		} catch (Exception exp) {
			System.err.println("Exception: " + exp.getMessage());
			exp.printStackTrace();
		} finally {
			System.out.println("[Thread-" + getId() + "] terminated -> "
					+ System.currentTimeMillis());
			try {
				close();
			} catch (IOException ioex) {
				System.err.println("Exception: " + ioex.getMessage());
			}
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		this.socket.close();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void runit() throws Exception {
		OutputStream os = this.socket.getOutputStream();
		os.write(("POST " + this.url.getPath() + " HTTP/1.1\n").getBytes());
		os.write(("User-Agent: " + CometServletClientTest.class.getName() + " (chunked-test)\n")
				.getBytes());
		os.write(("Host: " + this.url.getHost() + "\n").getBytes());
		os.write("Transfer-Encoding: chunked\n".getBytes());
		os.write("\n".getBytes());
		os.flush();

		InputStream is = this.socket.getInputStream();
		BufferedReader in = new BufferedReader(new InputStreamReader(is));

		String line = null;
		while ((line = in.readLine()) != null && !line.matches("\\s*")) {
			System.out.println(line);
		}
		System.out.println(line);

		// Wait a delay to ensure that all threads are ready
		Random random = new Random();
		sleep(DEFAULT_DELAY + random.nextInt(1000));
		long time = 0;
		String response = null;
		int counter = 0;

		int min_count = 10 * 1000 / delay;
		int max_count = 50 * 1000 / delay;
		int n = 1;
		while ((this.max--) > 0) {
			//sleep(this.delay);
			time = System.currentTimeMillis();
			System.out.println("Write to server #" + n);
			writechunk(os, "Testing...");
			System.out.println("Read from server #" + (n++));
			response = readchunk(in);
			if (response == null) {
				// Reach the end of the stream
				break;
			}
			System.out.println("Server Response -> " + response);
			time = System.currentTimeMillis() - time;
			// update the maximum response time
			if (time > max_time) {
				max_time = time;
			}
			// update the minimum response time
			if (time < min_time) {
				min_time = time;
			}
			// update the average response time
			if (counter >= min_count && counter <= max_count) {
				avg_time += time;
			}
			counter++;
			sleep(this.delay);
		}

		avg_time /= (max_count - min_count + 1);
		// For each thread print out the maximum, minimum and average response
		// times
		System.out.println(max_time + " \t " + min_time + " \t " + avg_time);
	}

	/**
	 * 
	 * @param input
	 * @return
	 */
	public String readsess(String input) {

		System.out.println("Read Session -> input: " + input);

		String data = null;
		String in = input;

		data = getsess(in);
		if (data == null) {
			/*
			 * A small chunk without a complete sessionid
			 */
			if (this.lastPartialSess == null) {
				this.lastPartialSess = input;
			} else {
				this.lastPartialSess += input;
			}
			in = this.lastPartialSess;
			data = getsess(in);
		}
		/*
		 * Store the last part of session (for the next "small" chunk)
		 */
		if (data != null) {
			int start = in.lastIndexOf("[" + data + "]");
			if (start >= 0) {
				this.lastPartialSess = in.substring(start);
			} else {
				this.lastPartialSess = null;
			}
		}
		if (data == null) {
			System.out.println("SESSION not found in: " + in);
		}
		return data;
	}

	/**
	 * Write a chunk to the output stream
	 * 
	 * @param os
	 * @param data
	 * @throws Exception
	 */
	protected static void writechunk(OutputStream os, String data) throws Exception {
		String chunkSize = Integer.toHexString(data.length());
		os.write((chunkSize + CRLF + data + CRLF).getBytes());
		os.flush();
	}

	/**
	 * Read a chunk and return it as a String
	 * 
	 * @param in
	 * @return
	 * @throws Exception
	 */
	protected static String readchunk(BufferedReader in) throws Exception {
		String data = null;
		int len = -1;
		while (len == -1) {
			try {
				data = in.readLine();
				System.out.println("DATA (len): " + data);
				len = Integer.valueOf(data, 16);
			} catch (NumberFormatException ex) {

				break;
			} catch (Exception ex) {
				System.err.println("Ex: " + ex.getMessage());
				ex.printStackTrace();
			} finally {
				if (len == 0) {
					System.out.println("End chunk");
					throw new Exception("End chunk");
				}
			}
		}

		len += 2; // For the CR & LF chars
		char buf[] = new char[len];
		int offset = 0;
		int recv = 0;
		while (recv != len) {
			int i = in.read(buf, offset, len - offset);
			recv += i;
			// System.out.println("DATA: " + recv + ":" + len);
			offset = recv;
		}
		data = new String(buf);
		// System.out.println("DATA: " + recv + " : " + data);
		// System.out.println("DATA: " + recv);
		return data;
	}

	/**
	 * 
	 * @param in
	 * @return
	 */
	protected static String getsess(String in) {
		String data = null;
		int start = in.indexOf('[');
		if (start != -1) {
			int end = in.indexOf(']');
			if (end != -1) {
				if (end > start) {
					data = in.substring(start + 1, end);
				} else {
					start = in.indexOf('[', end);
					if (start != -1) {
						end = in.indexOf(']', start);
						if (end != -1) {
							data = in.substring(start + 1, end);
						}
					}
				}
			}
		}
		return data;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			// System.err.println("Usage: java " +
			// CometServletClientTest.class.getName() +
			// " URL [n] [max] [delay]");
			System.err.println("Usage: java " + CometServletClientTest.class.getName()
					+ " URL [n] [max] [delay]");
			System.err.println("\tURL: The url of the service to test.");
			System.err.println("\tn: The number of threads. (default is 100)");
			// System.err.println("\tmax: The maximum number of requests. (default is 1000)");
			System.err.println("\tdelay: The delay between writes. (default is 1000ms)");
			System.exit(1);
		}

		URL strURL = new URL(args[0]);
		int n = 100, max = 1000, delay = DEFAULT_DELAY;
		if (args.length > 1) {
			try {
				n = Integer.parseInt(args[1]);
				if (args.length > 2) {
					max = Integer.parseInt(args[2]);
					if (max < 1) {
						throw new IllegalArgumentException("Negative number: max");
					}
				}
				if (args.length > 3) {
					delay = Integer.parseInt(args[3]);
					if (delay < 1) {
						throw new IllegalArgumentException("Negative number: delay");
					}
				}
			} catch (Exception exp) {
				System.err.println("Error: " + exp.getMessage());
				System.exit(1);
			}
		}

		System.out.println("\nRunning test with parameters:");
		System.out.println("\tURL: " + strURL);
		System.out.println("\tn: " + n);
		System.out.println("\tmax: " + max);
		System.out.println("\tdelay: " + delay);

		CometServletClientTest comet[] = new CometServletClientTest[n];
		for (int i = 0; i < comet.length; i++) {
			comet[i] = new CometServletClientTest(strURL, max, delay);
		}
		for (int i = 0; i < comet.length; i++) {
			comet[i].start();
		}
		for (int i = 0; i < comet.length; i++) {
			comet[i].join();
		}
	}
}
