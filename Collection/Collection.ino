void sendData();
void receiveData();

// Relay
#define oxygen_pump_switch 12
#define water_pump_switch 11

// ds18b20 by Miles Burton
#include <OneWire.h>
#include <DallasTemperature.h>

// Lora Normal
#include <SoftwareSerial.h>
SoftwareSerial LoraSerial(3, 4); //Only Arduino 

// Water sensor
#define water_sensorPin A1

// MPU6050
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>

// Some define
// DS1820B
#define ONE_WIRE_BUS 2
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);

// MPU6050
Adafruit_MPU6050 mpu;

// PH
#define analogInPin A0            //pH meter Analog output to Arduino Analog Input 0
#define Offset 0.70            //deviation compensate Bù TRỪ PH
unsigned long int avgValue;     //Store the average value of the sensor feedback

// Độ đục
#define TSSPin A3

void setup() {
  pinMode(oxygen_pump_switch,OUTPUT);
  pinMode(water_pump_switch,OUTPUT);
  Serial.begin(9600);
  LoraSerial.begin(9600);
  
  // DS1820B
  sensors.begin();
  mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);
  
  //MPU6050
  if (!mpu.begin()) {
  Serial.println("Failed to find MPU6050 chip");
  while (1) {
  delay(10);
  }
  }
  Serial.println("MPU6050 Found!");

  while(!LoraSerial.available())
  {
    Serial.println(".");
  }
  String data = LoraSerial.readStringUntil('\n');
  Serial.println(data);
  if(data.indexOf("START") != -1) {
    LoraSerial.println("OKE");
    Serial.println("OKE");
  }
}

void loop() {
  //sendData();
  receiveData();
}

void sendData()
{
      // ds1820b
  sensors.requestTemperatures();
  float temperature = sensors.getTempCByIndex(0);
  
  // Water sensor
  int water = analogRead(water_sensorPin); 
  
  //MPU6050
  sensors_event_t a, g, temp1;
  mpu.getEvent(&a, &g, &temp1);
  int mpu6050_value = abs(a.acceleration.x) > 5 ? abs(a.acceleration.x) : abs(a.acceleration.y);
  
  // PH
    int buf[10];                //buffer for read analog
    for(int i=0;i<10;i++)       //Get 10 sample value from the sensor for smooth the value
    { 
      buf[i]=analogRead(analogInPin);
      delay(10);
    }
    for(int i=0;i<9;i++)        //sort the analog from small to large
    {
      for(int j=i+1;j<10;j++)
      {
        if(buf[i]>buf[j])
        {
          int temp=buf[i];
          buf[i]=buf[j];
          buf[j]=temp;
        }
      }
    }
    avgValue=0;
    for(int i=2;i<8;i++)                      //take the average value of 6 center sample
      avgValue+=buf[i];
    float phValue=(float)avgValue*5.0/1024/6; //convert the analog into millivolt
    phValue=2.0*phValue+Offset; 
  
   //TSS
   int TSS = analogRead(TSSPin);
  
  // Lora Normal
  String value_frame = "";
  value_frame += String(phValue);
  value_frame += ",";
  value_frame += String(temperature);
  value_frame += ",";
  value_frame += String(TSS);
  value_frame += ",";
  value_frame += String(mpu6050_value);
  value_frame += ",";
  value_frame += String(water);
    
    LoraSerial.println(value_frame); 
    Serial.println("Send successful");
    Serial.println(value_frame);
    Serial.println("1");
//  }
}

void receiveData()
{
  while(LoraSerial.available())
  {
      String data = LoraSerial.readStringUntil('\n');
      if(data.indexOf("READ?") != -1) LoraSerial.println("READ_OK");
      else if(data.indexOf("SEND") != -1)
        {
          sendData();
        }
      else
      {
        if(data.indexOf("water") != -1)
        {
          String water_value = data.substring(data.indexOf("water"));
          if(water_value.indexOf("ON") != -1) digitalWrite(water_pump_switch,HIGH);
          else if(water_value.indexOf("OFF") != -1) digitalWrite(water_pump_switch,LOW);
          LoraSerial.println("ACK");
          Serial.println("ACK");
        }
        else if(data.indexOf("oxygen") != -1)
        {
          String oxygen_value = data.substring(data.indexOf("oxygen"));
          if(oxygen_value.indexOf("ON") != -1) digitalWrite(oxygen_pump_switch,HIGH);
          else if(oxygen_value.indexOf("OFF") != -1) digitalWrite(oxygen_pump_switch,LOW);
          LoraSerial.println("ACK");
          Serial.println("ACK");
        }
      }
  }
}
