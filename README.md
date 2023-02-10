## What is it

Runs within a Mule runtime and converts any JSON Logger nodes in Mule and into XRay events that can be shown in AWS.

This is capable of processing `START`, `END` events, `[BEFORE|AFTER]_REQUEST`, `[BEFORE|AFTER]_TRANSFORM`, as well as EXCEPTION.

If a `traceId` variable is recorded in any `AFTER_REQUEST` logs, this will also be able to link back to the original request

|![image](https://user-images.githubusercontent.com/6788278/217944270-7d2f9376-004f-443c-89d5-a52bc526c634.png)|
| - |

To:

|![image](https://user-images.githubusercontent.com/6788278/217944097-f7d7ff1a-d284-4c63-af0e-8516d50a55fa.png)|
| - |
* In order to get full flows a few modifications are required.




## Usage

* Build this application using the following command.

```mvn clean install```

Use this dependency in your Java/Mule Applications

```
<dependency>
	<groupId>com.intelematics.mule</groupId>
	<artifactId>log4j2-xray-jsonlogger-appender</artifactId>
	<version>1.0.37</version>
	<type>jar</type>
</dependency>
```

* Modify Application's log4j2.xml to add the below appender custom appender config.

```
<Appenders>
	<Xray name="xray"
          awsRegion="ap-southeast-2"
          messagesBatchSize="5"
          queueLength="100">
          <PatternLayout pattern="%m" /> 
        </Xray>
</Appenders>
```
Add this java package in your top level log4j2 configuration element

```
<Configuration packages="com.intelematics.mule.log4j2.xray">
```

Add this custom appender to your Root logger in log4j2.xml.

```
<Root level="INFO">
     <AppenderRef ref="xray" />
</Root>
     
        (or)

<AsyncRoot level="INFO">
     <AppenderRef ref="xray" />
</AsyncRoot>
```

In your properties you will need to specify a `aws.accessKeyId` and `aws.secretKey`, these are used to login to your AWS account and send the entries. These credentials will need to have the correct permissions within AWS to store the logs.

The xray appender will now run when your mule starts up, and start sending any relevant logs to Xray.

After this initial setup, you need to be able to link your API calls together. This is achieved by using the correlationID and the traceId. The correlationId is used to identify the request chain between APIs. Mule by default uses it's own format of correlationId, this must be setup to use an AWS correlation ID:
|![image](https://user-images.githubusercontent.com/6788278/217948044-bc6a74b5-c95d-44f0-b123-fb9cb8071e68.png)|
| ------- |

The content of this should look like this:
```
import toHex from dw::core::Numbers import * from dw::core::Strings --- "1-$(toHex(now() as Number))-$(replaceAll(last(uuid(),27),"-",""))"
```

This correlationId will now be used automatically within the JSON loggers and will be sent automatically via your connectors. This enables the requests to be put in a single trace. If you run your application now you will see the requests in the same trace, BUT NOT CONNECTED. In order to connect the traces a `traceId` must be recorded in the end of the JSON logger. This must be generated and stored by an `END` of the child request, and recorded in the parent's `AFTER_REQUEST`.

Your `AFTER_REQUEST` JSON loggers should look like this:

| ![image](https://user-images.githubusercontent.com/6788278/217951454-32974835-4114-4987-b359-9158ec947b42.png) |
| - |

Any child Mule APIs will need to record outbound headers we suggest a variable after your APIKit like:

| ![image](https://user-images.githubusercontent.com/6788278/217952072-790702e1-8a7c-4d47-a76e-b6973f2b6fd3.png) |
| - |

with content:

```
import toHex from dw::core::Numbers import leftPad from dw::core::Strings --- (vars.outboundHeaders default {}) ++ {traceId: leftPad(toHex(randomInt((16 pow 16) - 1)),16, "0")}
```
This will generate an AWS compatible trace Id

You will also need to configure your HTTP Listner to return these outbound headers:

| ![image](https://user-images.githubusercontent.com/6788278/217952406-2b894eb0-cb6a-4e0f-a3a1-abfc87265962.png) |
| - |

That's it! You will now get X-Ray messages automatically populating into your APIs. Hope this helps trace down tricky problems!
