package com.plpatterns.status;

import static com.plpatterns.status.Utils.formatPeriod;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * @author Jonathan Tran (jtran)
 */
public class Conversation {
  
  private static final Log LOG = LogFactory.getLog(Conversation.class);
  
  private static final long DEFAULT_MIN_MILLIS_BETWEEN_MESSAGES = 30 * 1000L;
  
  private Chat _chat;
  private boolean _paused;
  private Date _lastHeardFrom;
  private Date _lastSentTo;
  private long _minMillisecondsBetweenMessages;
  private Level _alwaysNotifyAtLevel;
  
  public Conversation(Chat chat) {
    _chat = chat;
    _paused = false;
    _lastHeardFrom = null;
    _lastSentTo = null;
    _minMillisecondsBetweenMessages = DEFAULT_MIN_MILLIS_BETWEEN_MESSAGES;
    _alwaysNotifyAtLevel = Level.WARN;
  }

  public Chat getChat() {
    return _chat;
  }

  public void setChat(Chat chat) {
    _chat = chat;
  }

  public boolean isPaused() {
    return _paused;
  }

  public void setPaused(boolean paused) {
    _paused = paused;
  }

  public Date getLastHeardFrom() {
    return _lastHeardFrom;
  }

  public void setLastHeardFrom(Date lastHeardFrom) {
    _lastHeardFrom = lastHeardFrom;
  }

  public Date getLastSentTo() {
    return _lastSentTo;
  }

  public void setLastSentTo(Date lastSentTo) {
    _lastSentTo = lastSentTo;
  }

  public long getMinMillisecondsBetweenMessages() {
    return _minMillisecondsBetweenMessages;
  }

  public void setMinMillisecondsBetweenMessages(
          long minMillisecondsBetweenMessages) {
    _minMillisecondsBetweenMessages = minMillisecondsBetweenMessages;
  }

  public Level getAlwaysNotifyAtLevel() {
    return _alwaysNotifyAtLevel;
  }

  public void setAlwaysNotifyAtLevel(Level alwaysNotifyAtLevel) {
    _alwaysNotifyAtLevel = alwaysNotifyAtLevel;
  }

  public boolean shouldNotify(LoggingEvent event) {
    return !isPaused() &&
      (// Level is high enough that we don't use throttling.
       event.getLevel().isGreaterOrEqual(getAlwaysNotifyAtLevel()) ||
       // We never sent to this user before.
       getLastSentTo() == null ||
       // It has been long enough since the last time we sent an IM to this user.
       getLastSentTo().getTime() + getMinMillisecondsBetweenMessages() <= new Date().getTime());
  }

  /**
   * Sends an IM, ignoring any errors.
   */
  public void sendIm(String msg) {
    sendIm(msg, false);
  }
  
  /**
   * Sends an IM, ignoring any errors.  Optionally updates
   * last-sent-to timestamp used for throttling.
   *
   * TODO: Track errors.
   */
  public void sendIm(String msg, boolean updateLastSentToTime) {
    try {
      LOG.info("trying to send IM to " + getChat().getParticipant() + ": " + msg);
      if (updateLastSentToTime) {
        setLastSentTo(new Date());
      }
      getChat().sendMessage(msg);
    }
    catch (XMPPException e) {
      // ignore
    }
  }
  
  public String toHumanReadableString() {
    return String.format("paused: %s, lastSentTo: %s, minInterval: %s",
            isPaused(),
            getLastSentTo(),
            Utils.formatPeriod(getMinMillisecondsBetweenMessages()));
  }
  
  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }


  /**
   * This function processes commands in an incoming message. The responses are
   * purposely lower-case and casual form to mimic the way people usually type
   * IMs. To a user, it shouldn't feel like they are talking to a bot, but
   * rather someone on the other end helping them.
   */
  public void reactToIm(boolean newConvo, Message message) {
    // Track when we've heard from this person.
    LOG.debug("setting last heard from...");
    setLastHeardFrom(new Date());
    
    String msg = message.getBody();
    LOG.debug("reacting to IM... convo: " + this + " newConvo: " + newConvo + " msg: " + msg);
    if (isBlank(msg)) return;
    
    if (msg.equalsIgnoreCase("pause") || msg.equalsIgnoreCase("stop")) {
      setPaused(true);
    }
    else if (msg.equalsIgnoreCase("resume") || msg.equalsIgnoreCase("start")) {
      sendIm("i'll start sending updates every " +
              formatPeriod(getMinMillisecondsBetweenMessages()));
      setPaused(false);
    }
    else if (msg.startsWith("every ")) {
      // Change the period at which IMs are sent.
      final String iDontUnderstand = "I don't understand.  If you'd rather I sent you IMs more often or less often, just let me know by saying \"every 5 minutes\", for example.";
      final Pattern everyPattern = Pattern.compile("every\\s+([\\w\\.]+)\\s+(\\w+)\\s*");
      
      // Try to match the basic format.
      LOG.debug("Trying to parse change in interval.");
      Matcher matcher = everyPattern.matcher(msg);
      if (!matcher.matches()) {
        sendIm(iDontUnderstand);
        return;
      }
      
      // Try to parse the number and units.
      LOG.debug("Basic structure found.  Trying to parse number and unit.");
      String strNumber = matcher.group(1).toLowerCase();
      String strUnits  = matcher.group(2).toLowerCase();
      Double num = Double.valueOf(strNumber);
      if (num == null || !strUnits.startsWith("s") && !strUnits.startsWith("m")) {
        sendIm(iDontUnderstand);
        return;
      }
      
      // Convert to milliseconds.
      LOG.debug("Interval found: " + num + " " + strUnits);
      if (strUnits.startsWith("m")) {
        num *= 60d;
      }
      num *= 1000d;
      long period = Math.round(num);
      
      // Prevent people from setting this too low.
      period = Math.max(period, 500);

      // Set new period.
      sendIm("ok, i'll send updates every " + formatPeriod(period));
      setMinMillisecondsBetweenMessages(period);
    }
    else if (msg.startsWith("set ")) {
      // Change the period at which IMs are sent.
      final String iDontUnderstand = "I don't understand.  If you'd rather I only sent you IMs above a certain log level, just let me know by saying \"set level info\", for example.";
      final Pattern setLevelPattern = Pattern.compile("set\\s+level\\s+(\\w+)\\s*");
      
      // Try to match the basic format.
      LOG.debug("Trying to parse log level.");
      Matcher matcher = setLevelPattern.matcher(msg);
      if (!matcher.matches()) {
        sendIm(iDontUnderstand);
        return;
      }
      
      // Try to parse the level.
      LOG.debug("Basic structure found.  Trying to parse the level.");
      String strLevel = matcher.group(1).toUpperCase();
      Level level = Level.toLevel(strLevel, getAlwaysNotifyAtLevel());
      if (level == null) {
        sendIm(iDontUnderstand);
        return;
      }

      // Set new log level.
      sendIm("ok, i'll send " + level + " updates");
      setAlwaysNotifyAtLevel(level);
    }
    else if (msg.equalsIgnoreCase("status") || msg.equalsIgnoreCase("st")) {
      sendIm(toHumanReadableString());
    }
    else if (msg.startsWith("echo")) {
      sendIm(msg);
    }
    else if (newConvo) {
      sendIm("hi, i'll start sending updates every " +
              formatPeriod(getMinMillisecondsBetweenMessages()));
    }
    else {
      sendIm("huh?  the commands I understand are: start, stop, every N s[econds], every N m[inutes], set level <log-level>.");
    }
  }

}
