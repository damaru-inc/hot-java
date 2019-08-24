package com.damaru.hotjava;

import org.apache.commons.cli.CommandLine;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.CacheLiveDataAction;
import com.solacesystems.jcsmp.CacheRequestListener;
import com.solacesystems.jcsmp.CacheRequestResult;
import com.solacesystems.jcsmp.CacheSession;
import com.solacesystems.jcsmp.CacheSessionProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class SolaceCacheSubscriber extends Solace {
    
    private static final String CACHE_NAME = "cache1";
    private XMLMessageConsumer consumer;
    private XMLMessageProducer producer;

    public SolaceCacheSubscriber(CommandLine cmd) throws Exception {
        super(cmd);
        
        CacheSessionProperties props = new CacheSessionProperties(CACHE_NAME);
        props.setMaxMsgs(100);
        props.setTimeout(11000);
        CacheSession cacheSession = session.createCacheSession(props);
        
        producer = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {

            @Override
            public void responseReceived(String messageID) {
                System.out.println("Producer received response for msg: " + messageID);
            }

            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                System.out.printf("Producer received error for msg: %s@%s - %s%n",
                                   messageID,timestamp,e);
             }
        });

        consumer = session.getMessageConsumer(new XMLMessageListener() {

            @Override
            public void onReceive(BytesXMLMessage msg) {

                String topic = msg.getDestination().getName();
                Main.log("got " + msg.getClass() + " " + topic);
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
                processTemp(topic, tempStr);
            }

            @Override
            public void onException(JCSMPException e) {
                System.out.printf("Consumer received exception: %s%n", e);
            }

        });
        
        consumer.start();
        Long requestId = 1L;
        Topic topic = JCSMPFactory.onlyInstance().createTopic("Test_Topic");
        CacheListener listener = new CacheListener();     
        cacheSession.sendCacheRequest(requestId, topic, false, CacheLiveDataAction.FLOW_THRU, listener);
    }
    
    private void processTemp(String topic, String message) {
        Main.log("topic: " + topic + " msg: " + message);
    }

    @Override
    public void close() {
        if (consumer != null) {
            consumer.close();
        }
        
        if (producer != null) {
            producer.close();
        }

        super.close();
    }

}

class CacheListener implements CacheRequestListener {

    @Override
    public void onComplete(Long arg0, Topic arg1, CacheRequestResult arg2) {
        Main.log("onComplete " + arg0 + " " + arg1.getName() + " " + arg2);
        
    }

    @Override
    public void onException(Long arg0, Topic arg1, JCSMPException arg2) {
        Main.log("onException " + arg0 + " " + arg1.getName() + " " + arg2);
        arg2.printStackTrace();
    }
    
}
