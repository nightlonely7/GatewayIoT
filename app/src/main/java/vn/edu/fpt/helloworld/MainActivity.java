package vn.edu.fpt.helloworld;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.jjoe64.graphview.GraphView;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import lombok.Data;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {

    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int REQ_CODE_SPEECH_INPUT = 1;

    private static final String CLOUD_SERVER_URL = "http://192.168.1.10:8080";

    UsbSerialPort port;

    private static final Gson GSON = new Gson();

    private String buffer = "";
    private List<Execution> executionList;

    private static GraphView GRAPH_VIEW_TEMPERATURE;
    private static GraphView GRAPH_VIEW_MOISTURE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        GRAPH_VIEW_TEMPERATURE = findViewById(R.id.graphTemperature);
//        GRAPH_VIEW_TEMPERATURE.getViewport().setMinY(0);
//        GRAPH_VIEW_TEMPERATURE.getViewport().setMaxY(10);
//        GRAPH_VIEW_TEMPERATURE.getViewport().setYAxisBoundsManual(true);
//        GRAPH_VIEW_MOISTURE = findViewById(R.id.graphMoisture);
//        GRAPH_VIEW_MOISTURE.getViewport().setMinY(0);
//        GRAPH_VIEW_MOISTURE.getViewport().setMaxY(10);
//        GRAPH_VIEW_MOISTURE.getViewport().setYAxisBoundsManual(true);
//
//        Random r = new Random();
        Timer timer = new Timer();

        TimerTask executionTask = new TimerTask() {
            @Override
            public void run() {
                getExecutionList().stream().filter(execution -> !execution.getExecuted()).forEach(execution -> execute(execution));
            }
        };
        timer.schedule(executionTask, 1000, 5000);

        TimerTask getMoistureTask = new TimerTask() {
            @Override
            public void run() {
                writeSerial("GET_MOISTURE");
            }
        };
        timer.schedule(getMoistureTask, 2000, 5000);

        Button buttonOn = findViewById(R.id.btn_on);
        buttonOn.setOnClickListener(v -> writeSerial("GET_MOISTURE"));

        Button buttonVoice = findViewById(R.id.btn_voice);
        buttonVoice.setOnClickListener(v -> writeSerial("EXECUTE:5000"));

        Button buttonOff = findViewById(R.id.btn_off);
        buttonOff.setOnClickListener(v -> {
            SensorData sensorData = new SensorData();
            sensorData.setSensorId("1");
            sensorData.setSensorValue("1");
            sensorData.setMeasureTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")));
            this.uploadDataToCloud(sensorData);
        });

//
//        Button buttonOff = (Button) findViewById(R.id.btn_off);
//        buttonOff.setOnClickListener(v -> openUART("OFF"));
//
//        Button buttonVoice = (Button) findViewById(R.id.btn_voice);
//        buttonVoice.setOnClickListener(v -> promptSpeechInput());

//        openUART();
//
//        SensorData sensorData = new SensorData();
//        sensorData.setSensorId("1");
//        sensorData.setSensorValue("1");
//        sensorData.setMeasureTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")));
//        this.uploadDataToCloud(sensorData);
    }

//    private void sendDataToThingSpeak(Map<String, String> valuesMap) {
//        OkHttpClient okHttpClient = new OkHttpClient();
//        Request.Builder builder = new Request.Builder();
//        StringBuilder valuesStr = new StringBuilder();
//        for (String key : valuesMap.keySet()) {
//            valuesStr.append("&").append(key).append("=").append(valuesMap.get(key));
//        }
//
//        String apiURL = "https://api.thingspeak.com/update?api_key=RPJ1QF0BP5ENCMLX"
//                + valuesStr.toString();
//        Request request = builder.url(apiURL).build();
//
//        okHttpClient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Request request, IOException e) {
//
//            }
//
//            @Override
//            public void onResponse(Response response) throws IOException {
//
//                System.out.println(response.body().string());
//
//            }
//        });
//
//    }

//    private void getDataFromThingSpeak() {
//        Map<String, String> valuesMap = new HashMap<>();
//        OkHttpClient okHttpClient = new OkHttpClient();
//        Request.Builder builder = new Request.Builder();
//        String apiURL = "http://192.168.1.47:8080/sensorData";
//        Request request = builder.url(apiURL).build();
//        okHttpClient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Request request, IOException e) {
//                System.out.println(e.getMessage());
//                System.out.println(request.body());
//            }
//
//            @RequiresApi(api = Build.VERSION_CODES.N)
//            @Override
//            public void onResponse(Response response) throws IOException {
//
//                ThingSpeakResponse thingSpeakResponse = GSON.fromJson(response.body().string(), ThingSpeakResponse.class);
//                List<Feed> feeds = new ArrayList<>(thingSpeakResponse.getFeedList());
//                System.out.println(response.body().string());
//
//                GRAPH_VIEW_TEMPERATURE.removeAllSeries();
//                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
//                List<DataPoint> dataPointTemperature = feeds.stream().map(feed -> new DataPoint(
//                        LocalDateTime.parse(feed.getCreatedAt(), dateTimeFormatter).get(ChronoField.SECOND_OF_DAY),
//                        Integer.parseInt(feed.getField1()))).collect(Collectors.toList());
//                DataPoint[] dataPointTemperatureArr = new DataPoint[dataPointTemperature.size()];
//                for (int i = 0; i < dataPointTemperatureArr.length; i++) {
//                    dataPointTemperatureArr[i] = dataPointTemperature.get(i);
//                }
//                LineGraphSeries<DataPoint> seriesTemperature = new LineGraphSeries<>(dataPointTemperatureArr);
//                GRAPH_VIEW_TEMPERATURE.addSeries(seriesTemperature);
//
//                GRAPH_VIEW_MOISTURE.removeAllSeries();
//                List<DataPoint> dataPointMoisture = feeds.stream().map(feed -> new DataPoint(
//                        LocalDateTime.parse(feed.getCreatedAt(), dateTimeFormatter).get(ChronoField.SECOND_OF_DAY),
//                        Integer.parseInt(feed.getField2()))).collect(Collectors.toList());
//                DataPoint[] dataPointMoistureArr = new DataPoint[dataPointMoisture.size()];
//                for (int i = 0; i < dataPointMoistureArr.length; i++) {
//                    dataPointMoistureArr[i] = dataPointMoisture.get(i);
//                }
//                LineGraphSeries<DataPoint> seriesMoisture = new LineGraphSeries<>(dataPointMoistureArr);
//                GRAPH_VIEW_MOISTURE.addSeries(seriesMoisture);
//            }
//        });
//    }

    @Override
    public void onNewData(byte[] data) {
        buffer += new String(data);
        Log.d("BUFFER---", buffer);
        // buffer ="!test A123#";
        int startIndex = buffer.indexOf("!");
        int endIndex = buffer.indexOf("#");
        if (startIndex >= 0 && endIndex >= 0 && endIndex > startIndex) {
            String value = buffer.substring(startIndex + 1, endIndex);
            Log.d("VALUE+++", value);
            TextView textView = findViewById(R.id.txt_serial_value);
            textView.setText(value);
            buffer = "";
            if (value.startsWith("MOISTURE_VALUE:")) {
                String moistureValue = value.replace("MOISTURE_VALUE:", "");
                SensorData sensorData = new SensorData();
                sensorData.setSensorId("1");
                sensorData.setSensorValue(moistureValue);
                sensorData.setMeasureTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")));
                uploadDataToCloud(sensorData);
            } else if (value.startsWith("EXECUTE_DONE")) {
                getExecutionList().forEach(execution -> {
                    execution.setExecuted(Boolean.TRUE);
                    execution.setExecutionTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")));
                    updateExecution(execution);
                });
            }
        }
        if (buffer.length() >= 256) {
            buffer = buffer.substring(1);
        }
    }

    private void uploadDataToCloud(SensorData sensorData) {

        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String apiURL = CLOUD_SERVER_URL + "/sensorData";
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), GSON.toJson(sensorData));
        Request request = builder.url(apiURL).post(body).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
//                TextView textView2 = findViewById(R.id.txt_voice);
//                textView2.setText(response.body().string());
            }
        });
    }

    private List<Execution> getExecutionList() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String apiURL = CLOUD_SERVER_URL + "/execution";
        Request request = builder.url(apiURL).get().build();
        String responseBody = null;
        try {
            Response response = okHttpClient.newCall(request).execute();
            responseBody = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Execution> executionList = GSON.fromJson(responseBody, ExecutionGet.class).getData();
        System.out.println(executionList);
        return executionList;
    }

    private List<SensorData> getSensorDataList() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String apiURL = CLOUD_SERVER_URL + "/sensorData";
        Request request = builder.url(apiURL).get().build();
        String responseBody = null;
        try {
            Response response = okHttpClient.newCall(request).execute();
            responseBody = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<SensorData> sensorDataList = GSON.fromJson(responseBody, SensorDataGet.class).getData();
        System.out.println(sensorDataList);
        return sensorDataList;
    }

    private List<SensorAutomation> getSensorAutomationFromServer() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String apiURL = CLOUD_SERVER_URL + "/sensorAutomation";
        Request request = builder.url(apiURL).get().build();
        String responseBody = null;
        try {
            Response response = okHttpClient.newCall(request).execute();
            responseBody = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<SensorAutomation> sensorAutomationList = GSON.fromJson(responseBody, SensorAutomationGet.class).getData();
        System.out.println(sensorAutomationList);
        return sensorAutomationList;
    }

    private void sendExecutionToServer(Execution execution) {

        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String apiURL = CLOUD_SERVER_URL + "/execution";
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), GSON.toJson(execution));
        Request request = builder.url(apiURL).post(body).build();
        try {
            okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateExecution(Execution execution) {

        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String apiURL = CLOUD_SERVER_URL + "/execution/" + execution.getExecutionId();
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), GSON.toJson(execution));
        Request request = builder.url(apiURL).put(body).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {

            }
        });
    }

    @Override
    public void onRunError(Exception e) {

    }

    private static final class ThingSpeakResponse {
        @SerializedName("feeds")
        private final List<Feed> feedList;

        public List<Feed> getFeedList() {
            return feedList;
        }

        private ThingSpeakResponse(List<Feed> feedList) {
            this.feedList = feedList;
        }
    }

    @Data
    private static final class Feed {
        @SerializedName("entry_id")
        private final String entryId;
        @SerializedName("created_at")
        private final String createdAt;
        @SerializedName("field1")
        private final String field1;
        @SerializedName("field2")
        private final String field2;
    }


    private void writeSerial(String writeValue) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.d("UART", "UART is not available");

        } else {
            Log.d("UART", "UART is available");

            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {

                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);

                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));

            } else {

                port = driver.getPorts().get(0);
                try {
                    port.open(connection);
                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                    port.write((writeValue + "#").getBytes(), 1000);

                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
                    Executors.newSingleThreadExecutor().submit(usbIoManager);

                } catch (Exception ignored) {

                }
            }
        }

    }

//    private void automate() {
//        List<SensorData> sensorDataList = getSensorDataList();
//        List<Execution> executionList = getExecutionList();
//        List<SensorAutomation> sensorAutomationList = getSensorAutomationFromServer();
//        sensorAutomationList.forEach(sensorAutomation -> {
//            if (sensorAutomation.getAutomationEnabled()) {
//                Integer currentHumidity = Integer.MAX_VALUE;
//                if (!sensorDataList.isEmpty()) {
//                    currentHumidity = Integer.parseInt(sensorDataList.get(0).getSensorValue());
//                }
//                if (sensorAutomation.getMinHumidity() >= currentHumidity
//                        && executionList.stream().allMatch(Execution::getExecuted)) {
//                    Execution execution = new Execution();
//                    execution.setExecutionId("EXECUTION_" + System.currentTimeMillis());
//                    execution.setSensorId("1");
//                    execution.setCurrentHumidity(currentHumidity);
//                    execution.setTargetHumidity(sensorAutomation.getMaxHumidity());
//                    execution.setRequestTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")));
//                    execution.setExecuted(Boolean.FALSE);
//                    sendExecutionToServer(execution);
//                }
//            }
//        });
//    }

    private void execute(Execution execution) {
        writeSerial("EXECUTE:" + (execution.getTargetHumidity() - execution.getCurrentHumidity()));
    }

//    private void openUART() {
//        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
//
//        if (availableDrivers.isEmpty()) {
//            Log.d("UART", "UART is not available");
//
//        } else {
//            Log.d("UART", "UART is available");
//
//            UsbSerialDriver driver = availableDrivers.get(0);
//            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
//            if (connection == null) {
//
//                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
//                manager.requestPermission(driver.getDevice(), usbPermissionIntent);
//
//                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));
//
//            } else {
//
//                port = driver.getPorts().get(0);
//                try {
//                    port.open(connection);
//                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
//
//                    port.write(("init#").getBytes(), 1000);
//
//                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
//                    Executors.newSingleThreadExecutor().submit(usbIoManager);
//
//                } catch (Exception ignored) {
//
//                }
//            }
//        }
//
//    }

//    private void promptSpeechInput() {
//        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...");
//        try {
//            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
//        } catch (ActivityNotFoundException a) {
//            Toast.makeText(getApplicationContext(), "Not supported.",
//                    Toast.LENGTH_SHORT).show();
//        }
//    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent
//            data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        switch (requestCode) {
//            case REQ_CODE_SPEECH_INPUT: {
//                if (resultCode == RESULT_OK && null != data) {
//                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//                    Log.d("VOICEEEEE", Arrays.toString(result.toArray()));
//                    this.writeSerial(result.get(0).toUpperCase());
//                    TextView textView = findViewById(R.id.txt_voice);
//                    textView.setText(result.get(0).toUpperCase());
//                }
//                break;
//            }
//
//        }
//    }


    @Data
    private static final class SensorDataGet {
        private String code;
        private String message;
        private List<SensorData> data;
    }

    @Data
    private static final class SensorData {
        private String sensorId;
        private String sensorValue;
        private String measureTime;
    }

    @Data
    private static final class ExecutionGet {
        private String code;
        private String message;
        private List<Execution> data;
    }

    @Data
    private static final class Execution {
        private String executionId;
        private String sensorId;
        private Integer currentHumidity;
        private Integer targetHumidity;
        private String executionTime;
        private String requestTime;
        private Boolean executed;
    }

    @Data
    private static final class SensorAutomationGet {
        private String code;
        private String message;
        private List<SensorAutomation> data;
    }

    @Data
    private static final class SensorAutomation {
        private String sensorId;
        private String sensorAutomationId;
        private String sensorAutomationName;
        private Integer minHumidity;
        private Integer maxHumidity;
        private Integer minPeriod;
        private Integer maxPeriod;
        private Boolean automationEnabled;
        private Boolean applied;
    }
}