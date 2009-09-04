package com.plpatterns.status;

/**
 * Convenience class for using {@link XmppAppender} with Google Talk/gchat.
 * <p>
 * Note: You must invite to chat the sender of this message and they must
 * confirm in Gmail -- the message won't be delivered unless you do this!
 * 
 * @author Jonathan Tran (jtran)
 */
public class GtalkAppender extends XmppAppender {

  public GtalkAppender() {
    // Default constructor for log4j.
    setServerHostname("talk.google.com");
    setServerPort(5222);
    setServiceName("gmail.com");
    setResource("resource");
  }
  
  public GtalkAppender(String gmailAddress, String password) {
    super("talk.google.com", 5222, "gmail.com", gmailAddress, password, "resource");
  }
  
}
