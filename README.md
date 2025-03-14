### **README.md - Projektdokumentation**

# **MQTT-Datenvisualisierung mit Java und Grafana**

Dieses Projekt wurde erstellt, um Sensordaten über **MQTT mit Java** zu simulieren und die Werte in **Grafana** als Graphen darzustellen.  
Ziel war es, mehrere **"Sensoren"** zu starten, die Sinuswerte berechnen und diese über den Mosquitto-MQTT-Broker senden.

Während der Einrichtung gab es einige kleinere Probleme, z. B. fehlende Abhängigkeiten in **Maven** oder das **shaded JAR** wurde nicht korrekt erstellt. Nach mehreren Tests funktionierte jedoch alles wie erwartet.

---

## **1. Voraussetzungen**
Bevor das Projekt starten konnte, musste sichergestellt werden, dass die folgenden Tools installiert sind:

- **Java 11 oder höher**
- **Maven** für das Bauen des Java-Projekts
- **Mosquitto MQTT-Broker** (läuft lokal auf `localhost:1883`)
- **Docker** (falls Mosquitto und Grafana über Container laufen sollen)
- **Grafana mit MQTT-Plugin** für die Visualisierung

Da bereits **Mosquitto** installiert war, wurde es einfach mit Docker gestartet:
```bash
docker run -d --name mosquitto -p 1883:1883 eclipse-mosquitto
```
### **🔻 Screenshot: Mosquitto-Container gestartet**
![image](https://github.com/user-attachments/assets/d94e87f7-cab4-4d19-958b-402e96738810)


Für **Grafana**:
```bash
docker run -d --name grafana -p 3000:3000 grafana/grafana
```
Danach konnte Grafana über `http://localhost:3000` im Browser aufgerufen werden.

---

## **2. Java-Programm für Sensordaten**
### **2.1 Maven-Projekt erstellen**
Das Projekt wurde mit **Maven** eingerichtet. Falls noch nicht geschehen, wurde ein neues Projekt erstellt:
```bash
mvn archetype:generate -DgroupId=com.mqtt -DartifactId=MqttJava -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
cd MqttJava
```
Um die MQTT-Funktionalität zu nutzen, musste die **Paho MQTT-Bibliothek** in `pom.xml` hinzugefügt werden:
```xml
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.2.5</version>
</dependency>
```
Dann wurden die Abhängigkeiten mit:
```bash
mvn clean install
```
heruntergeladen.

### **🔻 Screenshot: Erfolgreicher Maven-Build**
![image](https://github.com/user-attachments/assets/12d72ced-8804-4ab1-8dcb-8ca5fb0d04b7)


---

### **2.2 Java-Code für die Sensoren**
In **`App.java`** wurde die Hauptlogik geschrieben, um Sinuswerte zu senden.  
Dabei wurde eine Möglichkeit eingebaut, dass die **PUB- und SUB-Topics über die Kommandozeile übergeben** werden können.

```java
package com.mqtt;

import org.eclipse.paho.client.mqttv3.*;

public class App {
    public static void main(String[] args) {
        String broker = "tcp://localhost:1883";
        String clientId = "JavaSinusClient_" + System.currentTimeMillis();

        String pubTopic = args.length > 0 ? args[0] : "sensoren/java1";
        String subTopic = args.length > 1 ? args[1] : "feedback/java1";

        System.out.println("Senden auf: " + pubTopic);
        System.out.println("Abonniert auf: " + subTopic);

        try {
            MqttClient client = new MqttClient(broker, clientId);
            client.connect();
            client.subscribe(subTopic, (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("Feedback erhalten: " + payload);
                if ("stop".equalsIgnoreCase(payload.trim())) {
                    System.out.println("Programm wird beendet.");
                    client.disconnect();
                    System.exit(0);
                }
            });

            double counter = Math.PI / 2;
            while (true) {
                double sinValue = Math.sin(counter) * 10;
                String payload = String.format("%.2f", sinValue);
                client.publish(pubTopic, new MqttMessage(payload.getBytes()));
                System.out.println("Gesendet an [" + pubTopic + "]: " + payload);

                counter += 0.1;
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

Beim ersten Start des Programms gab es einen Fehler mit **"NoClassDefFoundError"**, da die MQTT-Bibliothek nicht ins JAR gepackt wurde.  
Dies wurde behoben, indem das **maven-shade-plugin** in `pom.xml` hinzugefügt wurde:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.2.4</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>com.mqtt.App</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## **3. Sensordaten testen**
Nach dem Build wurde das Programm für mehrere Sensoren gestartet:

```bash
java -jar target/MqttJava-1.0-SNAPSHOT.jar "sensoren/sensor1" "feedback/sensor1"
java -jar target/MqttJava-1.0-SNAPSHOT.jar "sensoren/sensor2" "feedback/sensor2"
java -jar target/MqttJava-1.0-SNAPSHOT.jar "sensoren/sensor3" "feedback/sensor3"
```
Um zu prüfen, ob die Werte ankommen:
```bash
mosquitto_sub -h localhost -t "sensoren/+"
```
Die Ausgabe zeigte korrekt **Sinuswerte**, die jede Sekunde aktualisiert wurden.

---

## **4. Grafana-Visualisierung**
Grafana wurde über `http://localhost:3000` geöffnet.

### **4.1 MQTT-Datenquelle hinzufügen**
1. **Configuration → Data Sources**  
2. **"Add data source" → "MQTT"**  
3. **Broker:** `tcp://localhost:1883`  
4. **Datenquelle speichern und testen**

### **4.2 Dashboard mit Sensor-Daten**
1. **"Create" → "Dashboard"**  
2. **"Add new panel" → Datenquelle: MQTT**  
3. **Topic z. B. `sensoren/sensor1` eintragen**  
4. **Visualisierung auf "Time Series" setzen**  
5. **Speichern und für weitere Sensoren wiederholen**

### **🔻 Screenshot: Grafana mit mehreren Sensorkurven**
![image](https://github.com/user-attachments/assets/06ec6d3a-4afd-4c08-b318-022622ffceb1)


---
## 5. 📌 Testplan: Smart-Home Umgebung

### **Testumgebung**
- Docker-basierte Umgebung mit **Mosquitto (MQTT-Broker)**, **Java-Sensoren** und **Grafana**.
- Sensoren senden Daten an MQTT, die in Grafana angezeigt werden.
- Sensoren können über `steuerung/shutdown` gestoppt werden.

### **Testfälle**
| **Testfall-ID** | **Beschreibung** | **Erwartetes Ergebnis** | **Status** |
|---------------|----------------|---------------------|-----------|
| **TC-01** | Sensor1 sendet Daten an MQTT | Mosquitto empfängt die Nachricht | ✅ |
| **TC-02** | Sensor2 sendet Daten an MQTT | Grafana zeigt die Daten live an | ✅ |
| **TC-03** | Sensor3 sendet Temperaturwerte | MQTT-Plugin in Grafana empfängt die Werte | ✅ |
| **TC-04** | MQTT-Broker speichert Nachrichten | `mosquitto_sub` zeigt die letzten Nachrichten | ✅ |
| **TC-05** | Grafana zeigt Live-Daten an | Dashboard aktualisiert sich automatisch | ✅ |
| **TC-06** | `steuerung/shutdown` wird gesendet | Alle Sensoren beenden sich | ✅ |



---

## **6. Mitwirkende**

Die Arbeit habe ich mit Florian R. erledigt.
