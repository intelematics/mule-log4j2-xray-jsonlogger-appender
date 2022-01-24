What?
====================
Attaching to the documentation a custom appender that uses the amazon default credentials provider chain and that utilizes the AWS SDK to publish logs to Cloudwatch . 



Usuage?
==========================
* Build this application using the following command.

```mvn clean install```

Use this dependency in your Java/Mule Applications

```
<dependency>
	<groupId>d4a849c2-b859-405b-b0dc-04a08f5d6eb5</groupId>
    <artifactId>log4j2-cloudwatch-appender</artifactId>
    <version>1.0.0</version>
	<type>jar</type>
</dependency>
```

* Modify Application's log4j2.xml to add the below appender custom appender config.

```
<Appenders>
	<CLOUDWATCH name="CloudWatch" logGroupName="<your CloudWatch group name>"
		logStreamName="<your Mule app name>-${sys:environment}"
		awsRegion="<your AWS region>" 
		messagesBatchSize="5"
		queueLength="100">
		<PatternLayout
			pattern="%-5p %d [%t] %X{correlationId}%c: %m%n" /> 
	</CLOUDWATCH>
</Appenders>
```
Add this java package in your top level log4j2 configuration element

```
<Configuration packages="com.mulesoft.log4j2.cloudwatch">
```

Add this custom appender to your Root logger in log4j2.xml.

```
<Root level="INFO">
    <AppenderRef ref="CloudWatch" />
</Root>
     
        (or)

<AsyncRoot level="INFO">
    <AppenderRef ref="CloudWatch" />
</AsyncRoot>
```

* That's it!

