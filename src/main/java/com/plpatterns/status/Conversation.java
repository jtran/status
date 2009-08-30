package com.plpatterns.status;

import java.util.Date;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;

public class Conversation {
  
  private static final long DEFAULT_MIN_MILLIS_BETWEEN_MESSAGES = 30 * 1000L;
  
  private Chat _chat;
  private boolean _paused;
  private Date _lastHeardFrom;
  private Date _lastSentTo;
  private long _minMillisecondsBetweenMessages;
  
  public Conversation(Chat chat) {
    _chat = chat;
    _paused = false;
    _lastHeardFrom = null;
    _lastSentTo = null;
    _minMillisecondsBetweenMessages = DEFAULT_MIN_MILLIS_BETWEEN_MESSAGES;
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

  public boolean shouldNotify() {
    return !isPaused() &&
      getLastSentTo().getTime() + getMinMillisecondsBetweenMessages() <= new Date().getTime();
  }
  
  /**
   * Sends an IM, ignoring any errors.  Updates last-sent-to timestamp.
   * TODO: Track errors.
   */
  public void sendIm(String msg) {
    try {
      setLastSentTo(new Date());
      getChat().sendMessage(msg);
    }
    catch (XMPPException e) {
      // ignore
    }
  }

}
