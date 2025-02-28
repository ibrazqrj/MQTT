package com.mqtt;

import org.eclipse.paho.client.mqttv3.*;

public class App {
    public static void main(String[] args) {
        String broker = "tcp://localhost:1883"; // MQTT-Broker
        String clientId = "JavaSinusClient_" + System.currentTimeMillis(); 

        // PUB- und SUB-Topics per Argument oder Umgebungsvariable setzen
        String pubTopic = args.length > 0 ? args[0] : System.getenv("MQTT_PUB_TOPIC");
        String subTopic = args.length > 1 ? args[1] : System.getenv("MQTT_SUB_TOPIC");

        // Standardwerte setzen, falls keine Parameter übergeben wurden
        if (pubTopic == null) pubTopic = "sensoren/java1";
        if (subTopic == null) subTopic = "feedback/java1";

        System.out.println("Senden auf: " + pubTopic);
        System.out.println("Abonniert auf: " + subTopic);

        try {
            MqttClient client = new MqttClient(broker, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);

            // Feedback-Topic abonnieren
            client.subscribe(subTopic, (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("Feedback erhalten: " + payload);
                if ("stop".equalsIgnoreCase(payload.trim())) {
                    System.out.println("Stop-Befehl empfangen. Beende das Programm.");
                    client.disconnect();
                    System.exit(0);
                }
            });

            double counter = Math.PI / 2; // Starte bei 90°
            while (true) {
                double sinValue = Math.sin(counter) * 10; // Werte skalieren
                String payload = String.format("%.2f", sinValue);
                MqttMessage msg = new MqttMessage(payload.getBytes());
                msg.setQos(0);
                msg.setRetained(false);

                client.publish(pubTopic, msg);
                System.out.println("Gesendet an [" + pubTopic + "]: " + payload);

                counter += 0.1;
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
