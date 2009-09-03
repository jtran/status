To use this, add something like the following to your app's log4j.xml
configuration, specifying the account your app should send IMs from.

    <appender name="GTALK" class="com.plpatterns.status.GtalkAppender">
      <param name="FromAddress" value="myAppUsername@gmail.com"/>
      <param name="Password"    value="myAppPassword"/>
    </appender>
    
    <!-- Send any log messages in the category "status" over IM -->
    <category name="status" additivity="true">
      <priority value="DEBUG"/>
      <appender-ref ref="GTALK"/>
    </category>

Then, in your app, log status.

    import org.apache.commons.logging.Log;
    import org.apache.commons.logging.LogFactory;
    
    ...
    
    private static final Log STATUS = LogFactory.getLog("status." + ThisClass.class);
    
    STATUS.info("Done " + percent + "%");

To get status updates sent to you, IM "myAppUsername@gmail.com".
myAppUsername will need to **confirm you as a contact in Gmail**.

Not all status messages will be sent as IMs.  Messages are
automatically dropped in order to not flood you with IMs.  At any
time, you can IM the app to adjust the number of IMs it sends you.
For example, sending the IM...

    every 10 minutes

... will allow status messages to be sent as IMs at most every 10
minutes.  In other words, the first status message will be sent as an
IM.  And for the next 10 minutes, status messages will be silently
dropped.  The next status message after that will be sent as an IM,
and so on.  The time period is per user, so different people can
monitor the app at different intervals.