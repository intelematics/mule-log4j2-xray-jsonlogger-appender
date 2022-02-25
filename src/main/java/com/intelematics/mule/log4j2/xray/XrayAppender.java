package com.intelematics.mule.log4j2.xray;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Plugin(name = "Xray", category = "Core", elementType = "appender", printObject = true)
public class XrayAppender extends AbstractAppender {

  static final boolean DEBUG_MODE = getDebugMode();
  static final boolean DISABLE_LOGGING = System.getProperty("log4j.xray.disable") != null;
  static String JsonLoggerClass = "org.mule.extension.jsonlogger.JsonLogger";
  final XrayLogReceiver xrayLogReceiver;

  private XrayAppender(final String name, final Layout<?> layout, final Filter filter, final boolean ignoreExceptions, final String awsRegion, Integer queueLength, Integer messagesBatchSize) {

    super(name, filter, layout, ignoreExceptions, null);
    String awsRegionValue = awsRegion == null ? System.getProperty("awsRegion") : awsRegion;
    this.xrayLogReceiver = XrayLogReceiver.getInstance(awsRegionValue);

    System.out.println("## Xray logging started  O.O"); // Can't use the logger here, as it is never setup right now.
  }

  @Override
  public void append(LogEvent event) {
    try {
      
      if (!DISABLE_LOGGING && JsonLoggerClass.equals(event.getLoggerName())) {
        xrayLogReceiver.processEvent(event);
        
        if (DEBUG_MODE)
          System.out.println("## Event Added");
      }
      
    } catch (Exception e) {
      log.error("## Couldn't send to Xray receiver", e);
    }
  }

  @Override
  public void start() {
    super.start();
  }

  @Override
  public void stop() {
    xrayLogReceiver.stop();
    super.stop();
  }

  @Override
  public String toString() {
    return XrayAppender.class.getSimpleName();

  }

  @PluginFactory
  public static XrayAppender createXrayAppender(@PluginAttribute(value = "queueLength") Integer queueLength, @PluginElement("Layout") Layout<?> layout,
      @PluginAttribute(value = "awsRegion") String awsRegion, @PluginAttribute(value = "name") String name,
      @PluginAttribute(value = "ignoreExceptions", defaultBoolean = false) Boolean ignoreExceptions,

      @PluginAttribute(value = "messagesBatchSize") Integer messagesBatchSize) {
    return new XrayAppender(name, layout, null, ignoreExceptions, awsRegion, queueLength, messagesBatchSize);
  }

  public static boolean getDebugMode() {
    return System.getProperty("log4j.debug") != null;
  }
}