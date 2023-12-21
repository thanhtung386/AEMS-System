#include <EEPROM.h>

// Lora Normal
#include <SoftwareSerial.h>
#define LORA_TX 3
#define LORA_RX 2
SoftwareSerial LoraSerial(LORA_RX, LORA_TX); // Only Arduino

// A7680C
#define SERIAL_SIM_TX 10
#define SERIAL_SIM_RX 9
SoftwareSerial SimSerial(SERIAL_SIM_RX, SERIAL_SIM_TX); // RX, TX

// MQTT
String Publish = "collection-station";
String Publish_Status = "monitor-status";
String Subscriptions_1 = "control-water-pump";
String Subscriptions_2 = "control-oxygen-pump";
String Subscriptions_3 = "control-restore";
String Phone = "+84368201386";
// String Phone = "+84869098320";

void getMQTT();
void A7680C_Init();
void setupSubscriptions(String topic);
bool sim_at_cmd(String cmd, int Delay);
void ReceiveMessage();
void PublicMessage(String topic, String message);
void sendCmdSerial();
void requestDataLora();
void receiveDataLora();
void sendACKLora();
void sentSMS(String phone, String message);
void Control_Relay(String nameTopic, String SubMessage);

void requestDataLora()
{
  LoraSerial.println("START");
  if (!LoraSerial.isListening())
    LoraSerial.listen();
  while (LoraSerial.available())
  {
  }
  String data = LoraSerial.readString();
  Serial.println(data);
  delay(100);
}

void receiveDataLora()
{
  if (!LoraSerial.isListening())
    LoraSerial.listen();
  while (!LoraSerial.available())
  {
  }
  while (LoraSerial.available() > 0)
  {
    String PH = LoraSerial.readStringUntil(',');
    String temperature = LoraSerial.readStringUntil(',');
    String TSS = LoraSerial.readStringUntil(',');
    String acceleration = LoraSerial.readStringUntil(',');
    String water = LoraSerial.readStringUntil('\n');
    String statusCollection = water.toInt() > 500 ? "Unstable" : acceleration.toInt() > 5 ? "Unstable"
                                                                                          : "Stable";

    Serial.println("Receive successful");
    // if(PH=="" || temperature == "" || TSS == "" || acceleration == "" || water == "") return;

    String result = PH + "," + temperature + "," + TSS + "," + statusCollection;
    Serial.println(result);
    PublicMessage(Publish, result);

    if (statusCollection.indexOf("Unstable") != -1)
    {
      sentSMS(Phone, "Collection station has been unstable");
      Serial.println("SMS Done!!");
    }

    if (PH.toInt() > 8.5)
    {
      sentSMS(Phone, "PH exceeds the threshold with " + PH + " value");
      Serial.println("SMS Done!!");
    }

    if (temperature.toInt() > 30)
    {
      sentSMS(Phone, "Temperature exceeds the threshold with " + temperature + " value");
      Serial.println("SMS Done!!");
      delay(50);
    }

    if (TSS.toInt() < 450)
    {
      sentSMS(Phone, "TSS exceeds the threshold with " + TSS + " value");
      Serial.println("SMS Done!!");
    }

    delay(100);
  }
}

void setup()
{
  LoraSerial.begin(9600);
  SimSerial.begin(9600);
  Serial.begin(9600);
  EEPROM.begin();
  EEPROM.write(0, 0);
  EEPROM.write(1, 0);
  delay(2000);
  A7680C_Init();
  getMQTT();
  setupSubscriptions(Subscriptions_1);
  setupSubscriptions(Subscriptions_2);
  setupSubscriptions(Subscriptions_3);
  PublicMessage(Publish, "7.14,35.6,500,5.6,Stable");
  requestDataLora();
  delay(500);
}

void loop()
{
  ReceiveMessage();
  receiveDataLora();
}

void sim_at_wait(int Delay)
{
  if (!SimSerial.isListening())
    SimSerial.listen();
  long wtimer = millis();
  while (wtimer + Delay > millis())
  {
    while (SimSerial.available())
    {
      // Serial.write(SimSerial.read());

      String data = SimSerial.readString();
      Serial.println(data);
      if (data.indexOf("CMQTTRXTOPIC") != -1 && data.indexOf("CMQTTRXPAYLOAD") != -1)
      {

        String nameTopic = data.indexOf("water") != -1 ? "control-water-pump" : data.indexOf("oxygen") != -1 ? "control-oxygen-pump"
                                                                            : data.indexOf("restore") != -1  ? "control-restore"
                                                                                                             : "NONE";
        String SubMessage = data.indexOf("ON") != -1 ? "ON" : data.indexOf("OFF") != -1  ? "OFF"
                                                          : data.indexOf("ENABLE") != -1 ? "ENABLE"
                                                                                         : "NONE";
        Serial.print("** Topic: ");
        Serial.println(nameTopic);
        Serial.print("** Message: ");
        Serial.println(SubMessage);
        Control_Relay(nameTopic, SubMessage);
      }
    }
  }
}

void A7680C_Init()
{
  sim_at_cmd("AT+CREG?", 500);
  sim_at_cmd("AT+CGDCONT=1,\"IP\",\"m3-world\"", 500);
  sim_at_cmd("AT+NETOPEN", 500);
}

void getMQTT()
{
  sim_at_cmd("AT+CMQTTSTART", 500);
  sim_at_cmd("AT+CMQTTACCQ=0,\"thanhtung170520022\",1", 1000);
  sim_at_cmd("AT+CMQTTSSLCFG=0,1", 500);
  sim_at_cmd("AT+CMQTTCONNECT=0,\"tcp://broker.emqx.io:8883\",60,1", 1000);
}

bool sim_at_cmd(String cmd, int Delay)
{
  SimSerial.println(cmd);
  sim_at_wait(Delay);
}

static long temp = 0;

void ReceiveMessage()
{
  String SubMessage = "";
  String nameTopic = "";
  if (!SimSerial.isListening())
    SimSerial.listen();
  while (!SimSerial.available())
  {
    if (temp < 100000)
      temp++;
    else
    {
      temp = 0;
      break;
    }
  }
  while (SimSerial.available() > 0)
  {
    String receivedString_1 = SimSerial.readString();
    String receivedString_2 = receivedString_1;
    if (receivedString_1.indexOf("TOPIC") != -1)
    {
      // Lấy thông tin topic
      int index1 = receivedString_1.indexOf("TOPIC");
      String string1 = receivedString_1.substring(index1);
      int index2 = string1.indexOf('\n');
      String string2 = string1.substring(index2 + 1);
      int index3 = string2.indexOf('\n');
      nameTopic = string2.substring(0, index3);
    }

    if (nameTopic.indexOf("control-water-pump") == -1 && nameTopic.indexOf("control-oxygen-pump") == -1 && nameTopic.indexOf("control-restore") == -1)
      break;

    Serial.print("Topic is: ");
    Serial.println(nameTopic);

    if (receivedString_2.indexOf("PAYLOAD") != -1)
    {
      // Lấy nội dung message
      int new1 = receivedString_2.indexOf("PAYLOAD");
      String neww = receivedString_2.substring(new1);
      int new2 = neww.indexOf('\n');
      String new3 = neww.substring(new2 + 1);
      int new4 = new3.indexOf('\n');
      SubMessage = new3.substring(0, new4);
    }

    if (SubMessage.indexOf("ON") == -1 && SubMessage.indexOf("OFF") == -1 && SubMessage.indexOf("ENABLE") == -1)
      break;
    Serial.print("Message is: ");
    Serial.println(SubMessage);

    if (nameTopic.indexOf("control-water-pump") != -1 && SubMessage.indexOf("ON") != -1)
    {
      LoraSerial.println("READ?");
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      String result_READ = LoraSerial.readStringUntil('\n');
      while (result_READ.indexOf("READ_OK") == -1)
      {
        LoraSerial.println("READ?");
        if (!LoraSerial.isListening())
          LoraSerial.listen();
        while (!LoraSerial.available())
        {
        }
        result_READ = LoraSerial.readStringUntil('\n');
      }
      LoraSerial.println("water,ON");
      Serial.println(result_READ);
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      String result_ACK = LoraSerial.readStringUntil('\n');
      Serial.println("Sucessful");
      Serial.println(result_ACK);
      EEPROM.write(0, 1);
    }
    else if (nameTopic.indexOf("control-water-pump") != -1 && SubMessage.indexOf("OFF") != -1)
    {
      LoraSerial.println("READ?");
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      String result_READ = LoraSerial.readStringUntil('\n');
      while (result_READ.indexOf("READ_OK") == -1)
      {
        LoraSerial.println("READ?");
        if (!LoraSerial.isListening())
          LoraSerial.listen();
        while (!LoraSerial.available())
        {
        }
        result_READ = LoraSerial.readStringUntil('\n');
      }
      LoraSerial.println("water,OFF");
      Serial.println(result_READ);
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      String result_ACK = LoraSerial.readStringUntil('\n');
      Serial.println("Sucessful");
      Serial.println(result_ACK);
      EEPROM.write(0, 0);
    }
    else if (nameTopic.indexOf("control-oxygen-pump") != -1 && SubMessage.indexOf("ON") != -1)
    {
      LoraSerial.println("READ?");
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      String result_READ = LoraSerial.readStringUntil('\n');
      while (result_READ.indexOf("READ_OK") == -1)
      {
        LoraSerial.println("READ?");
        if (!LoraSerial.isListening())
          LoraSerial.listen();
        while (!LoraSerial.available())
        {
        }
        result_READ = LoraSerial.readStringUntil('\n');
      }
      LoraSerial.println("oxygen,ON");
      Serial.println(result_READ);
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      String result_ACK = LoraSerial.readStringUntil('\n');
      Serial.println("Sucessful");
      Serial.println(result_ACK);
      EEPROM.write(1, 1);
    }
    else if (nameTopic.indexOf("control-oxygen-pump") != -1 && SubMessage.indexOf("OFF") != -1)
    {
      LoraSerial.println("READ?");
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      String result_READ = LoraSerial.readStringUntil('\n');
      while (result_READ.indexOf("READ_OK") == -1)
      {
        LoraSerial.println("READ?");
        if (!LoraSerial.isListening())
          LoraSerial.listen();
        while (!LoraSerial.available())
        {
        }
        result_READ = LoraSerial.readStringUntil('\n');
      }
      LoraSerial.println("oxygen,OFF");
      Serial.println(result_READ);
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      String result_ACK = LoraSerial.readStringUntil('\n');
      Serial.println("Sucessful");
      Serial.println(result_ACK);
      EEPROM.write(1, 0);
    }

    else if (nameTopic.indexOf("control-restore") != -1 && SubMessage.indexOf("ENABLE") != -1)
    {
      PublicMessage(Subscriptions_1, EEPROM.read(0) == 1 ? "ON" : "OFF");
      PublicMessage(Subscriptions_2, EEPROM.read(1) == 1 ? "ON" : "OFF");
    }
    SubMessage.remove(SubMessage.length() - 1);
    nameTopic.remove(nameTopic.length() - 1);
  }
  LoraSerial.println("SEND");
}

void sendCmdSerial()
{
  sim_at_wait(100);
  if (Serial.available() > 0)
  {
    String c = Serial.readStringUntil('\n');
    sim_at_cmd(c, 100);
  }
}

void setupSubscriptions(String topic)
{
  sim_at_cmd("AT+CMQTTSUB=0," + String(topic.length()) + ",1", 1000);
  sim_at_cmd(topic, 100);
}

void PublicMessage(String topic, String message)
{
  sim_at_cmd("AT+CMQTTTOPIC=0," + String(topic.length()), 1000);
  sim_at_cmd(topic, 100);
  sim_at_cmd("AT+CMQTTPAYLOAD=0," + String(message.length()), 1000);
  sim_at_cmd(message, 100);
  sim_at_cmd("AT+CMQTTPUB=0,1,60", 1000);
}

void sentSMS(String phone, String message)
{
  sim_at_cmd("AT+CMGS=\"" + phone + "\"", 1000);
  sim_at_cmd(message + "\x1A", 100);
}

void Control_Relay(String nameTopic, String SubMessage)
{
  Serial.println("Controlling");
  if (nameTopic.indexOf("control-water-pump") != -1 && SubMessage.indexOf("ON") != -1)
  {
    LoraSerial.println("READ?");
    if (!LoraSerial.isListening())
      LoraSerial.listen();
    while (!LoraSerial.available())
    {
    }
    String result_READ = LoraSerial.readStringUntil('\n');
    while (result_READ.indexOf("READ_OK") == -1)
    {
      LoraSerial.println("READ?");
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      result_READ = LoraSerial.readStringUntil('\n');
    }
    LoraSerial.println("water,ON");
    Serial.println(result_READ);
    if (!LoraSerial.isListening())
      LoraSerial.listen();
    while (!LoraSerial.available())
    {
    }
    String result_ACK = LoraSerial.readStringUntil('\n');
    Serial.println("Sucessful");
    Serial.println(result_ACK);
    EEPROM.write(0, 1);
  }
  else if (nameTopic.indexOf("control-water-pump") != -1 && SubMessage.indexOf("OFF") != -1)
  {
    LoraSerial.println("READ?");
    if (!LoraSerial.isListening())
      LoraSerial.listen();
    while (!LoraSerial.available())
    {
    }
    String result_READ = LoraSerial.readStringUntil('\n');
    while (result_READ.indexOf("READ_OK") == -1)
    {
      LoraSerial.println("READ?");
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      result_READ = LoraSerial.readStringUntil('\n');
    }
    LoraSerial.println("water,OFF");
    Serial.println(result_READ);
    if (!LoraSerial.isListening())
      LoraSerial.listen();
    while (!LoraSerial.available())
    {
    }
    String result_ACK = LoraSerial.readStringUntil('\n');
    Serial.println("Sucessful");
    Serial.println(result_ACK);
    EEPROM.write(0, 0);
  }
  else if (nameTopic.indexOf("control-oxygen-pump") != -1 && SubMessage.indexOf("ON") != -1)
  {
    LoraSerial.println("READ?");
    if (!LoraSerial.isListening())
      LoraSerial.listen();
    while (!LoraSerial.available())
    {
    }
    String result_READ = LoraSerial.readStringUntil('\n');
    while (result_READ.indexOf("READ_OK") == -1)
    {
      LoraSerial.println("READ?");
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      result_READ = LoraSerial.readStringUntil('\n');
    }
    LoraSerial.println("oxygen,ON");
    Serial.println(result_READ);
    if (!LoraSerial.isListening())
      LoraSerial.listen();
    while (!LoraSerial.available())
    {
    }
    String result_ACK = LoraSerial.readStringUntil('\n');
    Serial.println("Sucessful");
    Serial.println(result_ACK);
    EEPROM.write(1, 1);
  }
  else if (nameTopic.indexOf("control-oxygen-pump") != -1 && SubMessage.indexOf("OFF") != -1)
  {
    LoraSerial.println("READ?");
    if (!LoraSerial.isListening())
      LoraSerial.listen();
    while (!LoraSerial.available())
    {
    }
    String result_READ = LoraSerial.readStringUntil('\n');
    while (result_READ.indexOf("READ_OK") == -1)
    {
      LoraSerial.println("READ?");
      if (!LoraSerial.isListening())
        LoraSerial.listen();
      while (!LoraSerial.available())
      {
      }
      result_READ = LoraSerial.readStringUntil('\n');
    }
    LoraSerial.println("oxygen,OFF");
    Serial.println(result_READ);
    if (!LoraSerial.isListening())
      LoraSerial.listen();
    while (!LoraSerial.available())
    {
    }
    String result_ACK = LoraSerial.readStringUntil('\n');
    Serial.println("Sucessful");
    Serial.println(result_ACK);
    EEPROM.write(1, 0);
  }

  else if (nameTopic.indexOf("control-restore") != -1 && SubMessage.indexOf("ENABLE") != -1)
  {
    PublicMessage(Subscriptions_1, EEPROM.read(0) == 1 ? "ON" : "OFF");
    PublicMessage(Subscriptions_2, EEPROM.read(1) == 1 ? "ON" : "OFF");
  }
}
