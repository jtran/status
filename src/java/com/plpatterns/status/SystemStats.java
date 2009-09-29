package com.plpatterns.status;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

public class SystemStats {
  
  private static final String TOP_CMD = "top -n 2 -b -d 0.2";
  private static final int DEFAULT_LINES = 12;
  
  private SystemStats() {}

  public static void main(String[] args) {
    System.out.println(getTopOutput());
  }

  /**
   * Execs Linux top and returns the first {@value #DEFAULT_LINES} lines of
   * output.
   */
  public static String getTopOutput() {
    return getTopOutput(DEFAULT_LINES);
  }

  public static String getTopOutput(int numLines) {
    InputStreamReader in = null;
    BufferedReader reader = null;

    try {
      Process child = Runtime.getRuntime().exec(TOP_CMD);

      in = new InputStreamReader(child.getInputStream());
      reader = new BufferedReader(in);

      // Skip blank lines.
      String line;
      int emptyLines = 0;
      while (emptyLines < 3) {
        line = reader.readLine();
        if (line.length() < 1) emptyLines++;
      }
      
      // Accumulate output.
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < numLines; i++) {
        line = reader.readLine();
        if (line == null) break;
        
        if (i > 0) sb.append("\n");
        sb.append(line);
      }
      
      return sb.toString();
    }
    catch (Throwable t) {
      return null;
    }
    finally {
      Utils.closeQuietly(reader, in);
    }
  }
  
  /**
   * Get the hostname of the machine this thread is running on.
   */
  public static String getHostname() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    }
    catch (UnknownHostException e) {
      return null;
    }
  }

}
