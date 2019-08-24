package com.damaru.hotjava;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.cli.CommandLine;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;

public class SolaceSubscriber extends Solace {

    private XMLMessageConsumer consumer;
    private OscPlayer oscPlayer;
    private int messageCount;
    
    private PrintWriter writerc;
    private PrintWriter writer1;
    private PrintWriter writer6;
//    private PrintWriter writer10;
//    private PrintWriter writer60;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    // This doesn't work - it seems to hang on the cons.receive() call even when
    // there are messages.
    public void SolaceSubscriberSync(CommandLine cmd) throws Exception {
        // super(cmd);
        XMLMessageConsumer cons = null;

        try {
            cons = session.getMessageConsumer((XMLMessageListener) null);
            Main.log("Starting consumer.");
            cons.start();
            BytesXMLMessage msg = cons.receive();
            while (msg != null) {
                Main.log("Got " + msg);
                if (msg instanceof BytesMessage) {
                    BytesMessage message = (BytesMessage) msg;
                    byte[] data = message.getData();

                    if (data.length == 2) {
                        int id = data[0];
                        int temperature = data[1];
                        // Main.log("Got bytes.");
                    } else {
                        String text = new String(data);
                        processText(text);
                    }

                } else if (msg instanceof TextMessage) {
                    TextMessage message = (TextMessage) msg;
                    String json = message.getText();
                    Main.log("Got midi " + json);
                    processText(json);
                }
                msg = cons.receive();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Main.log("Closing Solace.");
            if (cons != null) {
                cons.close();
            }

            if (session != null) {
                session.closeSession();
            }
        }

    }

    private void processText(String text) {
    }

    public SolaceSubscriber(CommandLine cmd, OscPlayer oscPlayer) throws Exception {
        super(cmd);
        this.oscPlayer = oscPlayer;
        writer1 = new PrintWriter("data1.csv");
        writer6 = new PrintWriter("data6.csv");
        writerc = new PrintWriter("control.csv");

        consumer = session.getMessageConsumer(new XMLMessageListener() {

            @Override
            public void onReceive(BytesXMLMessage msg) {

                String topic = msg.getDestination().getName();
                //Main.log("got " + msg.getClass() + " " + topic);
                String tempStr = null;

                if (msg instanceof BytesMessage) {
                    BytesMessage message = (BytesMessage) msg;
                    byte[] data = message.getData();
                    tempStr = new String(data);
                } else if (msg instanceof TextMessage) {
                    TextMessage message = (TextMessage) msg;
                    tempStr = message.getText();
                } else {
                    Main.log("Unsupported message type: " + msg.getClass());
                }
                //Main.log("Got temp " + temp);
                processMessage(topic, tempStr);
            }

            @Override
            public void onException(JCSMPException e) {
                System.out.printf("Consumer received exception: %s%n", e);
            }

        });

        final Topic topic = JCSMPFactory.onlyInstance().createTopic("temp/>");
        session.addSubscription(topic);
        consumer.start();
    }

    private void processMessage(String topic, String message) {
        //String id = topic.substring(5);
        //Main.log("id: " + id + " temp: " + temp);
        Date dt = new Date();
        String dateString = dateFormat.format(dt);
        String dataLine;
        
        if (topic.contains("control")) {
            dataLine = String.format("%s,%s", dateString, message);
            writerc.println(dataLine);
        } else {
            double temp = Double.valueOf(message);
            dataLine = String.format("%s,%f", dateString, temp);
            writer1.println(dataLine);
            
            messageCount++;
            if (messageCount % 6 == 0) {
                writer6.println(dataLine);
            }
        }
        
        Main.log(dataLine);
        
//        if (messageCount % 10 == 0) {
//            writer10.println(dataLine);
//            
//            if (messageCount % 60 == 0) {
//                writer60.println(dataLine);
//            }
//        }
        try {
            //oscPlayer.handleMessage(id, temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        writer1.close();
        writer6.close();
        writerc.close();
            
        if (consumer != null) {
            consumer.close();
        }

        super.close();
    }
}
