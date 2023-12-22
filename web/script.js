let sendStatus = true;
let sendStatus_2 = true;
let sendStatus_3 = true;

var  topicwater =  "control-water-pump";
var  topicoxy =  "control-oxygen-pump";
var  topiccollection =  "collection-station";
var  topicstatus =  "monitor-status";

clientID = "clientID - "+parseInt(Math.random() * 100);/*Tạo ID tự động */
var host = "broker.emqx.io"; // host
var port = "8083";  // web socket là 8000

client = new Paho.MQTT.Client(host,Number(port),clientID); // Paho tên đề cập đến thư viện JavaScript Paho MQTT và Clientlà hàm tạo để tạo các phiên bản máy khách MQTT.

function startConnect(){
    client.onConnectionLost = onConnectionLost;
    client.onMessageArrived = onMessageArrived;
  
    client.connect({
        onSuccess: onConnect
    });
  
  }

  function onConnect(){

     client.subscribe(topicwater);
     client.subscribe(topicoxy);
     client.subscribe (topiccollection);

     publishMessageRestore();
    //  if (sendStatus_2) {
    //   setTimeout(function (){
    //     publishMessageRestore();
    // }, 100);
    // }
  
  }
  
  function onConnectionLost(responseObject){
    if(responseObject !=0){
    }
  }
  
  function onMessageArrived({ payloadString, destinationName }) {
    console.log("OnMessageArrived: " + payloadString);
    //const currentTime = new Date().toLocaleTimeString('en-US', {day: '2-digit', month: '2-digit', hour: 'numeric', minute: 'numeric', hour12: true });
    const currentTime = new Date().toLocaleTimeString('en-US', {hour: 'numeric', minute: 'numeric', hour12: true });
    if (destinationName === "collection-station") {
        var messcollection = payloadString;
        // Split the string into an array using ',' as the delimiter
        var values = messcollection.split(',');
        // Assign values to variables
        var ph = isNaN(parseFloat(values[0])) ? "N/A" : parseFloat(values[0]);
        var temperature = isNaN(parseFloat(values[1])) ? "N/A" : parseFloat(values[1]);
        var turbidity = isNaN(parseFloat(values[2])) ? "N/A" : parseFloat(values[2]);
        var status = values[3] === "Stable" ? "Stable" : values[3] === "Unstable" ? "Unstable" : "N/A";
  
        console.log(ph);
  
  
  
        document.getElementById("ph").innerHTML = ph;
        if(ph < 6.5 || ph > 8.5)
        {
          document.getElementById("ph").style.color = "red";
          document.getElementById("ph").style.fontWeight = "bold";
        }
        else
        {
          document.getElementById("ph").style.color = "#6e0101";
          document.getElementById("ph").style.fontWeight = "normal";
        }
        document.getElementById("temperature").innerHTML = temperature;

        if(temperature > 31)
        {
          document.getElementById("temperature").style.color = "red";
          document.getElementById("temperature").style.fontWeight = "bold";
        }
        else
        {
          document.getElementById("temperature").style.color = "#6e0101";
          document.getElementById("temperature").style.fontWeight = "normal";
        }

        document.getElementById("turbidity").innerHTML = turbidity;

        if(turbidity < 500)
        {
          document.getElementById("turbidity").style.color = "red";
          document.getElementById("turbidity").style.fontWeight = "bold";
        }
        else
        {
          document.getElementById("turbidity").style.color = "#6e0101";
          document.getElementById("turbidity").style.fontWeight = "normal";
        }

        document.getElementById("status").innerHTML = status;

        if(status === 'Unstable')
        {
          document.getElementById("status").style.color = "red";
        }
        else
        {
          document.getElementById("status").style.color = "#6e0101";
        }

        // Loại bỏ phần tử đầu tiên
        // PH
        PH_chart_var.data.labels.shift();
        PH_chart_var.data.datasets[0].backgroundColor.shift();
        PH_chart_var.data.datasets[0].data.shift();

        PH_chart_var.data.labels.push(currentTime);
        PH_chart_var.data.datasets[0].backgroundColor.push(ph > 8.5 || ph < 6.5 ? 'red' : 'rgba(0,0,255,1.0)');
        PH_chart_var.data.datasets[0].data.push(ph);


        // Temperature
        Temperature_chart_var.data.labels.shift();
        Temperature_chart_var.data.datasets[0].backgroundColor.shift();
        Temperature_chart_var.data.datasets[0].data.shift();

        Temperature_chart_var.data.labels.push(currentTime);
        Temperature_chart_var.data.datasets[0].backgroundColor.push(temperature > 31 ? 'red' : 'rgba(0,0,255,1.0)');
        Temperature_chart_var.data.datasets[0].data.push(temperature);
  

  
        // Turbidity
        Turbidity_chart_var.data.labels.shift();
        Turbidity_chart_var.data.datasets[0].backgroundColor.shift();
        Turbidity_chart_var.data.datasets[0].data.shift();

        Turbidity_chart_var.data.labels.push(currentTime);
        Turbidity_chart_var.data.datasets[0].backgroundColor.push(turbidity < 500 ? 'red' : 'rgba(0,0,255,1.0)');
        Turbidity_chart_var.data.datasets[0].data.push(turbidity);
  

  

  
  
    
        // Cập nhật biểu đồ
        Turbidity_chart_var.update();
        PH_chart_var.update();
        Temperature_chart_var.update();
  
  
  
    } else if (destinationName === "control-water-pump") {
        var messwater = payloadString;
        if (messwater === "ON") {
            document.getElementById("water-switch").checked = true;
        } else if (messwater === "OFF") {
            document.getElementById("water-switch").checked = false;
        }
    } else if (destinationName === "control-oxygen-pump") {
        var messoxy = payloadString;
        if (messoxy === "ON") {
            document.getElementById("oxygen-switch").checked = true;
        } else if (messoxy === "OFF") {
            document.getElementById("oxygen-switch").checked = false;
        }
    }
  }

function publishMessageRestore(){
  var topic = "control-restore";
  Message = new Paho.MQTT.Message("ENABLE");
  Message.destinationName = topic;
  client.send(Message);
  }
  
function publishMessageWater(messwater){
    var topic = "control-water-pump";
    Message = new Paho.MQTT.Message(messwater);
    Message.destinationName = topic;
    client.send(Message);
  }
  
function publishMessageOxy(messoxy){
  var topic = "control-oxygen-pump";
  Message = new Paho.MQTT.Message(messoxy);
  Message.destinationName = topic;
  client.send(Message);
}

var ctx_PH = document.getElementById('PH-chart').getContext('2d');
var ctx_Temperature = document.getElementById('Temperature-chart').getContext('2d');
var ctx_Turbidity = document.getElementById('Turbidity-chart').getContext('2d');

var PH_chart_var = new Chart(ctx_PH , {
type: "line",
data: {
    labels: ['N/A','N/A','N/A','N/A','N/A','N/A','N/A','N/A','N/A','N/A'],
    datasets: [{
    fill: false,
    lineTension: 0,
    backgroundColor: ["rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)"],
    borderColor: "rgba(0,0,255,0.1)",
    data: [0,0,0,0,0,0,0,0,0,0],
    }]
},
options: {
    title: {
        display: true,
        text: 'PH'
    },
    legend: {display: false},
    scales: {
    yAxes: [{ticks: {min: 0, max:14}}],
    },
}
});

var Turbidity_chart_var = new Chart(ctx_Turbidity , {
  type: "line",
  data: {
      labels: ['N/A','N/A','N/A','N/A','N/A','N/A','N/A','N/A','N/A','N/A'],
      datasets: [{
      fill: false,
      lineTension: 0,
      backgroundColor: ["rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)"],
      borderColor: "rgba(0,0,255,0.1)",
      data: [0,0,0,0,0,0,0,0,0,0],
      }]
  },
  options: {
      title: {
          display: true,
          text: 'Turbidity'
      },
      legend: {display: false},
      scales: {
      yAxes: [{ticks: {min: 0, max:1000}}],
      },
  }
  });

  var Temperature_chart_var = new Chart(ctx_Temperature , {
    type: "line",
    data: {
        labels: ['N/A','N/A','N/A','N/A','N/A','N/A','N/A','N/A','N/A','N/A'],
        datasets: [{
        fill: false,
        lineTension: 0,
        backgroundColor: ["rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)","rgba(0,0,255,1.0)"],
        borderColor: "rgba(0,0,255,0.1)",
        data: [0,0,0,0,0,0,0,0,0,0],
        }]
    },
    options: {
        title: {
            display: true,
            text: 'Temperature'
        },
        legend: {display: false},
        scales: {
        yAxes: [{ticks: {min: 0, max:100}}],
        },
    }
    });

  var messageCount = 0;
  var messageCount_2 = 0;
    
  function updateSwitchLabel(labelId) {
  
  var switchElement = document.getElementById(labelId === 'water-state' ? 'water-switch' : 'oxygen-switch');
  var labelElement = document.getElementById(labelId);
  
  if (labelId === 'water-state') {
    console.log(switchElement.checked);
      switchElement.checked = !switchElement.checked;
      console.log(switchElement.checked);
  
      if (!switchElement.checked) {
          var mess = 'ON';
          let count = 0;

          function runIteration() {
            if (count < 3) {
              publishMessageWater(mess);
                count++;
                setTimeout(runIteration, 1000); // Đợi 1 giây trước khi chạy lần tiếp theo
            }
          }   
          runIteration();

          labelElement.innerText = 'ON';
          console.log('Switch 1 is ON');
          //publishMessageWater(mess);

      } else {
          var mess = 'OFF';
          let count = 0;

          function runIteration() {
            if (count < 3) {
              publishMessageWater(mess);
                count++;
                setTimeout(runIteration, 1000); // Đợi 1 giây trước khi chạy lần tiếp theo
            }
          }   
          runIteration();          

          labelElement.innerText = 'OFF';
          console.log('Switch 1 is OFF');          
          //publishMessageWater(mess);
      }
  } else if (labelId === 'oxygen-state') {
      switchElement.checked = !switchElement.checked;
      if (!switchElement.checked) {
          var mess = 'ON';

          let count = 0;

          function runIteration() {
            if (count < 3) {
              publishMessageOxy(mess);
                count++;
                setTimeout(runIteration, 1000); // Đợi 1 giây trước khi chạy lần tiếp theo
            }
          }   
          runIteration();

          labelElement.innerText = 'ON';
          console.log('Switch 2 is ON');          
          //publishMessageOxy(mess);
      } else {
          var mess = 'OFF';

          let count = 0;

          function runIteration() {
            if (count < 3) {
              publishMessageOxy(mess);
                count++;
                setTimeout(runIteration, 1000); // Đợi 1 giây trước khi chạy lần tiếp theo
            }
          }   
          runIteration();          

          labelElement.innerText = 'OFF';
          console.log('Switch 2 is OFF');
          //publishMessageOxy(mess);
      }
  }
  }

function switchPage(pageName) {
  var i, sections;
  sections = document.getElementsByTagName("section");
  for (i = 0; i < sections.length; i++) {
    sections[i].style.display = "none";
  }

  var targetSection = document.getElementById(pageName);
  targetSection.style.display = "flex";
}

var elements = document.querySelectorAll(".menu-button");

function handleElementClick(event) {
  for (var i = 0; i < elements.length; i++) {
    elements[i].classList.remove("hover");
  }
  event.currentTarget.classList.add("hover");
}

for (var i = 0; i < elements.length; i++) {
  elements[i].addEventListener("click", handleElementClick);
}