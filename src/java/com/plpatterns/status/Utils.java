package com.plpatterns.status;

import java.io.Closeable;

public class Utils {
  
  private Utils() {}

  /**
   * Formats the given milliseconds into a human-readable amount.
   * 
   * @param millis must be greater than zero.
   */
  public static String formatPeriod(long millis) {
    double seconds = millis / 1000.0d;
    double minutes = seconds / 60.0d;
    double hours   = minutes / 60.0d;
    
    if (hours   >= 10d) return String.format("%d hours", Math.round(hours));
    if (hours   >   1d) return String.format("%.1f hours", hours);
    if (hours   ==  1d) return "hour";
    if (minutes >=  2d) return String.format("%d minutes", Math.round(minutes));
    if (minutes >   1d) return String.format("%.1f minutes", minutes);
    if (minutes ==  1d) return "minute";
    if (seconds >=  2d) return String.format("%d seconds", Math.round(seconds));
    if (seconds >   1d) return String.format("%.1f seconds", seconds);
    if (seconds ==  1d) return "second";
    if (millis  >   1 ) return String.format("%d milliseconds", millis);
    return "millisecond";
  }
  
  public static void closeQuietly(Closeable... cs) {
    if (cs == null) return;
    
    for (int i = 0; i < cs.length; i++) {
      if (cs[i] == null) continue;
      try {
        cs[i].close();
      }
      catch (Throwable t) {}
    }
  }
  
}
