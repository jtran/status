To use this, add something like the following to your app's log4j.xml configuration.

    <appender name="GTALK" class="com.plpatterns.status.GtalkAppender">
      <param name="FromAddress" value="myUsername@gmail.com"/>
      <param name="Password"    value="myPassword"/>
    </appender>
    
    <!-- Send any log messages in the category "status" over IM -->
    <category name="status" additivity="true">
      <priority value="DEBUG"/>
      <appender-ref ref="GTALK"/>
    </category>

