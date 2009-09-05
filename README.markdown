Problem: Your app processes jobs that take a long time.  You want an
easy way to check the progress of your app to see if it is still alive
and running.

Solution: status!

With status, your app can send a heartbeat to a log4j logger.  Users
can interact with the logger via IMs and listen in to that heartbeat
just when they choose.

Your app does not need to concern itself with throttling, per user
settings, or even the fact that IMs are being sent.

# Install

Status can currently be built with either `ant` or `mvn`.  If you use
`ant`, the jar will be output in the current directory.  If you use
`mvn`, the jar will be output to the target/ directory.

If you are using an app server like JBoss, drop status.jar into its
lib/ directory (the sibling of the deploy/ directory).  You will also
need to drop in any dependent jars, like smack.jar, if they are not
already provided by your app server.  These are all included in the
lib/ directory of this repo.

Add something like the following to your app's log4j.xml
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

# Usage

In your app, you need to actually output status.  This is done simply
by logging.  If you're already using log4j, your app doesn't need any
new dependencies and doesn't need to know anything specifically about
this project.

I'm showing here how to use a special "status" category/logger name so
that only messages intended for IMs will go to IMs.  Note, that the
category string used creating the logger must match what is in your
configuration file (above).  See the [log4j
manual](http://logging.apache.org/log4j/1.2/manual.html) for more
information.

    import org.apache.commons.logging.Log;
    import org.apache.commons.logging.LogFactory;
    
    ...
    
    private static final Log STATUS = LogFactory.getLog("status." + ThisClass.class.getCanonicalName());
    
    ...
    
    while (makingProgress()) {
      doWork();
      STATUS.info("Done " + percentDone() + "%");
    }

To get status updates sent to you, IM "myAppUsername@gmail.com".  Just
say "hi", or any greeting you feel like, and status will start
forwarding logged status messages to you as IMs.

myAppUsername will need to **confirm you as a contact in Gmail**.

# Throttling

Not all status messages will be sent as IMs.  Messages are
automatically dropped in order to not flood you with IMs.  At any
time, you can IM the app to adjust the number of IMs it sends you.
For example, sending the IM ...

    every 10 minutes

... or ...

    every 30 s

... will allow status messages to be sent as IMs at most every 10
minutes (or 30 seconds).  In other words, the first status message
will be sent as an IM.  And for the next 10 minutes, status messages
will be silently dropped.  The next status message after that will be
sent as an IM, and so on.  Status either forwards a logged message or
drops it; it does not store status messages until the time limit has
passed.

The time period is per IM address, so different people can monitor the
app at different intervals.

You can pause IMs at any time by IMing ...

    stop

... and resume them by IMing ...

    start
