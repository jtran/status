package com.plpatterns.status;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

/**
 * @author Jonathan Tran
 */
public class XmppAppender extends AppenderSkeleton {

  private static final Log LOG = LogFactory.getLog(XmppAppender.class);
  
  private static final int MAX_CONVERSATIONS = 50;

  // TODO: {change this to a timestamp, and retry after a while. Ideally,
  // exponential back-off.}
  private boolean _notificationFailedRecently;

  private String _serverHostname;
  private int _serverPort;
  private String _serviceName;
  private ConnectionConfiguration _conConfig;
  private String _fromAddress;
  private String _password;
  private String _resource;
  private XMPPConnection _con;
  private LinkedList<Conversation> _conversations = new LinkedList<Conversation>();
  
  public XmppAppender() {
    // Default constructor for log4j.
    LOG.warn("XMPP Appender created");
  }
  
  public XmppAppender(String serverHostname, int serverPort,
          String serviceName, String fromAddress, String password,
          String resource) {
    this(new ConnectionConfiguration(serverHostname, serverPort, serviceName),
            fromAddress, password, resource);
  }
  
  public XmppAppender(ConnectionConfiguration conConfig, String fromAddress,
          String password, String resource) {
    _conConfig = conConfig;
    _fromAddress = fromAddress;
    _password = password;
    _resource = resource;
    LOG.warn("XMPP Appender created with good constructor!");
  }

  public void connect() {
    LOG.warn("Trying to connect...");

    // Must synchronize since this gets called from multiple threads.
    synchronized (this) {
      XMPPConnection con = getConnection();
      if (con == null) {
        LOG.debug("created new XMPPConnection object");
        con = new XMPPConnection(getConnectionConfiguration());
        setConnection(con);
      }

      LOG.warn("Authenticated? " + con.isAuthenticated() + " con: " + _con);
      if (con.isAuthenticated()) return;

      try {
        // Connect to server.
        LOG.warn("Connecting...");
        con.connect();

        // Add listener to capture IMs sent to us.
        LOG.warn("Adding chat listener...");
        con.getChatManager().addChatListener(new XmppChatManagerListener());

        // Login.
        LOG.warn("Logging in...");
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);
        con.login(getFromAddress(), getPassword(), getResource());

        LOG.warn("Logged in!  Authenticated? " + con.isAuthenticated());
      }
      catch (Throwable t) {
        LOG.warn("connecting and logging in to XMPP server failed using connection configuration host: " + _conConfig.getHost()
                + " port: " + _conConfig.getPort() + " serviceName: " + _conConfig.getServiceName(),
                t);
        setNotificationFailedRecently(true);
      }
    }
  }

  @Override
  public void close() {
    if (getConnection() != null) {
      LOG.debug("Disconnecting...");
      getConnection().disconnect();
    }
  }

  @Override
  protected void append(LoggingEvent event) {
    if (!shouldNotify()) return;

    String msg = getMessage(event);
    if (isBlank(msg)) return;

    // Return if we have not connected or logged in yet.
    if (getConnection() == null || !getConnection().isAuthenticated()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format(
                "Dropping notification since we are not authenticated.  Did you remember to call connect()?  If you did call connect(), this could be a connection problem with the server.  msg: %s, conConfig",
                msg,
                _conConfig));
      }
      return;
    }

    // TODO: Should the failure of one message prevent the others from being attempted?
    try {
      // Send notification to all conversations.
      for (Conversation conversation : getConversations()) {
        if (!conversation.shouldNotify(event)) continue;
        
        // Send the IM.
        conversation.sendIm(msg, true);
      }
    }
    catch (Throwable t) {
      // We had an error in sending the message.  Don't try to send again.
      setNotificationFailedRecently(true);
    }
  }


  /**
   * Gets the message of an event. If the message is a {@link Callable}, calls
   * it and returns {@code toString()} of the result. Otherwise simply calls
   * {@code toString()} of the message.
   */
  private static String getMessage(LoggingEvent event) {
    Object msg = event.getMessage();
    
    if (msg instanceof Callable<?>) {
      try {
        return ObjectUtils.toString(((Callable<?>)msg).call());
      }
      catch (Throwable t) {
        return "";
      }
    }
    
    return ObjectUtils.toString(msg);
  }


  /**
   * As far as I understand, there is no way using log4j to know when all the
   * properties have been set and there is no initialize method that is called.
   * So we have to require that all properties be set and when they are, then
   * connect.
   */
  private void attemptToConnect() {
    LOG.warn("Attempting to connect....");
    
    if (// No connection configuration
        getConnectionConfiguration() == null &&
        (getServerHostname() == null || getServerPort() == 0 ||
         getServiceName() == null || getFromAddress() == null ||
         getPassword() == null || getResource() == null)
        
        ||
        
        // Already logged in.
        getConnection() != null && getConnection().isAuthenticated()) {
      
      return;
    }
    
    try {
      if (getConnectionConfiguration() == null) {
        setConnectionConfiguration(new ConnectionConfiguration(
                getServerHostname(), getServerPort(), getServiceName()));
      }

      connect();
    }
    catch (Throwable t) {
      LOG.error(t);
    }
  }
  
  public LinkedList<Conversation> getConversations() {
    return _conversations;
  }
  
  public XMPPConnection getConnection() {
    return _con;
  }
  
  private void setConnection(XMPPConnection con) {
    _con = con;
  }
  
  public ConnectionConfiguration getConnectionConfiguration() {
    return _conConfig;
  }
  
  public void setConnectionConfiguration(ConnectionConfiguration conConfig) {
    _conConfig = conConfig;
    
    // Refresh connection.
    close();
    setConnection(null);
    attemptToConnect();
  }
  
  public boolean getNotificationFailedRecently() {
    return _notificationFailedRecently;
  }
  
  public void setNotificationFailedRecently(boolean notificationFailedRecently) {
    _notificationFailedRecently = notificationFailedRecently;
  }
  
  public String getServerHostname() {
    return _serverHostname;
  }

  public void setServerHostname(String serverHostname) {
    _serverHostname = serverHostname;
    attemptToConnect();
  }

  public int getServerPort() {
    return _serverPort;
  }

  public void setServerPort(int serverPort) {
    _serverPort = serverPort;
    attemptToConnect();
  }

  public String getServiceName() {
    return _serviceName;
  }

  public void setServiceName(String serviceName) {
    _serviceName = serviceName;
    attemptToConnect();
  }

  public String getFromAddress() {
    return _fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    _fromAddress = fromAddress;
    attemptToConnect();
  }

  public String getPassword() {
    return _password;
  }

  public void setPassword(String password) {
    _password = password;
    attemptToConnect();
  }

  public String getResource() {
    return _resource;
  }

  public void setResource(String resource) {
    _resource = resource;
    attemptToConnect();
  }

  public boolean shouldNotify() {
    return !getNotificationFailedRecently();
  }

  @Override
  public boolean requiresLayout() {
    return false;
  }
  
  /**
   * Assumes conversations has been locked and is safe to modify.
   */
  public Conversation removeConversation(String participant) {
    Iterator<Conversation> it = getConversations().iterator();
    while (it.hasNext()) {
      Conversation convo = it.next();
      if (participant.equals(convo.getChat().getParticipant())) {
        it.remove();
        return convo;
      }
    }
    return null;
  }
  
  public List<String> getParticipants() {
    LinkedList<Conversation> convos = getConversations();
    List<String> participants;

    // Lock conversations before iterating.
    synchronized(convos) {
      // Copy participants.
      participants = new ArrayList<String>(convos.size());
      for (Conversation c : convos) {
        participants.add(c.getChat().getParticipant());
      }
    }
    
    return participants;
  }
  
  private class XmppChatManagerListener implements ChatManagerListener {

    /**
     * Event fired when a new chat is created.  We must capture this and add
     * our {@link MessageListener} so that we can handle incoming IMs.
     *
     * @param chat the chat that was created.
     * @param createdLocally true if the chat was created by the local user and
     *                       false if it wasn't.
     */
    public void chatCreated(Chat chat, boolean createdLocally) {
      LOG.debug("chatCreated chat.participant: " + chat.getParticipant() +
              " chat.threadID: " + chat.getThreadID() + " createdLocally: " + createdLocally);
      chat.addMessageListener(new XmppMessageListener());
    }
    
  }
  
  private class XmppMessageListener implements MessageListener {

    /** I wanted this to last over a long weekend, so it is {@value} */
    private static final int MAX_DAYS_TO_NOTIFY_OF_EVICTION = 4;

    /**
     * Event fired whenever we receive an IM.
     */
    public void processMessage(Chat chat, Message message) {
      LOG.debug("processMessage chat.threadID: " + chat.getThreadID()
              + " message.from: " + message.getFrom()
              + " message.body: " + message.getBody()
              + " message.subject: " + message.getSubject()
              + " message: " + message);
      Conversation convo;
      boolean newConvo = false;
      Conversation oldConvo = null;
      
      try {
        // To allow this to be called from multiple threads, we must lock the
        // conversations when we muck with them.
        LOG.debug("getting lock on conversations...");
        synchronized (getConversations()) {
          LOG.debug("got lock on conversations");

          // Get the conversation.
          convo = removeConversation(chat.getParticipant());
          if (convo == null) {
            LOG.debug("new conversation with participant " + chat.getParticipant());
            convo = new Conversation(chat);
            newConvo = true;
          }

          // Add this conversation to the front of the list.
          getConversations().addFirst(convo);

          // Evict old conversations.
          LOG.debug("number of conversations: " + getConversations().size());
          if (getConversations().size() > MAX_CONVERSATIONS) {
            oldConvo = getConversations().removeLast();
          }
        }

        // Process the message itself.
        convo.reactToIm(newConvo, message, XmppAppender.this);

        // Let the person know we're evicting them.
        if (oldConvo != null) {
          // Don't send this if we haven't heard from this person in a long time.
          Calendar cal = Calendar.getInstance();
          cal.add(Calendar.DATE, -MAX_DAYS_TO_NOTIFY_OF_EVICTION);
          if (oldConvo.getLastHeardFrom().after(cal.getTime())) {
            oldConvo.sendIm("I'm holding a conversation with " + MAX_CONVERSATIONS +
            " other people, so I'll talk to you later.");
          }
        }
      }
      catch (Throwable t) {
        LOG.error(t);
      }
    }
    
  }

}
