/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.jms.example;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;

import org.apache.activemq.common.example.ActiveMQExample;

/**
 * A simple example that shows a JMS Topic clustered across two nodes of a cluster.
 * Messages are sent on one node and received by consumers on both nodes.
 */
public class ClusteredTopicExample extends ActiveMQExample
{
   public static void main(final String[] args)
   {
      new ClusteredTopicExample().run(args);
   }

   @Override
   public boolean runExample() throws Exception
   {
      Connection connection0 = null;

      Connection connection1 = null;

      InitialContext ic0 = null;

      InitialContext ic1 = null;

      try
      {
         // Step 1. Get an initial context for looking up JNDI from server 0
         Hashtable<String, Object> properties = new Hashtable<String, Object>();
         properties.put("java.naming.factory.initial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
         properties.put("connectionFactory.ConnectionFactory", args[0]);
         properties.put("topic.topic/exampleTopic", "exampleTopic");
         ic0 = new InitialContext(properties);

         // Step 2. Look-up the JMS Topic object from JNDI
         Topic topic = (Topic)ic0.lookup("topic/exampleTopic");

         // Step 3. Look-up a JMS Connection Factory object from JNDI on server 0
         ConnectionFactory cf0 = (ConnectionFactory)ic0.lookup("ConnectionFactory");

         // Step 4. Get an initial context for looking up JNDI from server 1
         properties = new Hashtable<String, Object>();
         properties.put("java.naming.factory.initial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
         properties.put("connectionFactory.ConnectionFactory", args[1]);
         ic1 = new InitialContext(properties);

         // Step 5. Look-up a JMS Connection Factory object from JNDI on server 1
         ConnectionFactory cf1 = (ConnectionFactory)ic1.lookup("ConnectionFactory");

         // Step 6. We create a JMS Connection connection0 which is a connection to server 0
         connection0 = cf0.createConnection();

         // Step 7. We create a JMS Connection connection1 which is a connection to server 1
         connection1 = cf1.createConnection();

         // Step 8. We create a JMS Session on server 0
         Session session0 = connection0.createSession(false, Session.AUTO_ACKNOWLEDGE);

         // Step 9. We create a JMS Session on server 1
         Session session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);

         // Step 10. We start the connections to ensure delivery occurs on them
         connection0.start();

         connection1.start();

         // Step 11. We create JMS MessageConsumer objects on server 0 and server 1
         MessageConsumer consumer0 = session0.createConsumer(topic);

         MessageConsumer consumer1 = session1.createConsumer(topic);

         Thread.sleep(1000);

         // Step 12. We create a JMS MessageProducer object on server 0
         MessageProducer producer = session0.createProducer(topic);

         // Step 13. We send some messages to server 0

         final int numMessages = 10;

         for (int i = 0; i < numMessages; i++)
         {
            TextMessage message = session0.createTextMessage("This is text message " + i);

            producer.send(message);

            System.out.println("Sent message: " + message.getText());
         }

         // Step 14. We now consume those messages on *both* server 0 and server 1.
         // We note that all messages have been consumed by *both* consumers.
         // JMS Topics implement *publish-subscribe* messaging where all consumers get a copy of all messages

         for (int i = 0; i < numMessages; i++)
         {
            TextMessage message0 = (TextMessage)consumer0.receive(5000);

            System.out.println("Got message: " + message0.getText() + " from node 0");

            TextMessage message1 = (TextMessage)consumer1.receive(5000);

            System.out.println("Got message: " + message1.getText() + " from node 1");
         }

         return true;
      }
      finally
      {
         // Step 15. Be sure to close our JMS resources!
         if (connection0 != null)
         {
            connection0.close();
         }

         if (connection1 != null)
         {
            connection1.close();
         }

         if (ic0 != null)
         {
            ic0.close();
         }

         if (ic1 != null)
         {
            ic1.close();
         }
      }
   }

}
