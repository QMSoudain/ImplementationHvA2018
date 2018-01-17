package implementation.implappv6;

import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


    public class MainActivity extends AppCompatActivity {
        private static final String TAG = "MyFirebaseIIDService";
        private Button rejectButton;
        private Button acceptButton;
        private TextView username;
        private TextView uniqueId;
        private TextView date;
        private final int WAIT_TIME = 2500;
//    Intent intent = getIntent();
        //  String uniqueId = intent.getStringExtra("messageBody");

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            final Bundle bundle = getIntent().getExtras();
            username = (TextView) findViewById(R.id.textView2);
            uniqueId = (TextView) findViewById(R.id.textView3);
            date = (TextView) findViewById(R.id.textView5);
            //date.setText("17-01-2017");
            if(bundle != null) {
                username.setText("Uw username is: " + bundle.getString("username"));
                uniqueId.setText("Uw unieke ID is: " + bundle.getString("uniqueid"));
                date.setText(bundle.getString("requestdatetime"));
            }

            acceptButton = (Button) findViewById(R.id.button);

            acceptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("Accept");
                    String uniqueId = bundle.getString("uniqueid");
                    System.out.println(uniqueId);
                    String responsePostRequest = callServlet(uniqueId, "accept");

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    if (responsePostRequest.equals("200")) {
                        builder.setMessage(R.string.popup_success_message)
                                .setTitle(R.string.popup_success_title);
                    } else {
                        builder.setMessage(R.string.popup_fail_message)
                                .setTitle(R.string.popup_fail_title);
                    }

                    AlertDialog alertDialog = builder.create();

                    alertDialog.show();
                }
            });

            rejectButton = (Button) findViewById(R.id.button2);

            rejectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("Reject");
                    String uniqueId = bundle.getString("uniqueid");
                    System.out.println(uniqueId);
                    String responsePostRequest = callServlet(uniqueId, "reject");

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    if (responsePostRequest.equals("200")) {
                        builder.setMessage(R.string.popup_success_message)
                                .setTitle(R.string.popup_success_title);
                    } else {
                        builder.setMessage(R.string.popup_fail_message)
                                .setTitle(R.string.popup_fail_title);
                    }

                    AlertDialog alertDialog = builder.create();

                    alertDialog.show();
                }
            });

        }

        public void onTokenRefresh(View view) {
            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            Log.d(TAG, "Refreshed token: " + refreshedToken);
        }

        String responseCode = "404";
        public String callServlet(final String id, final String userChoice) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String urlAdress = "http://192.168.33.2:8080/NPS/nps";
                        URL url = new URL(urlAdress);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setDoOutput(true);
                        conn.setDoInput(true);

                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("function", "androidresponse");
                        jsonParam.put("uniqueid", id);
                        jsonParam.put("userchoice", userChoice);

                        Log.i("JSON", jsonParam.toString());
                        DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                        //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                        os.writeBytes(jsonParam.toString());

                        os.flush();
                        os.close();

                        int responseCodeInt = conn.getResponseCode();
                        responseCode = String.valueOf(responseCodeInt);
                        System.out.println(responseCode);

                        Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                        Log.i("MSG", conn.getResponseMessage());

                        conn.disconnect();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            try {
                thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return responseCode;
        }


        public void sendPost(final String id, final String userChoice) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String urlAdress = "http://192.168.33.2:8080/NPS/nps";
                        URL url = new URL(urlAdress);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        conn.setRequestProperty("Accept","application/json");
                        conn.setDoOutput(true);
                        conn.setDoInput(true);

                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("function", "androidresponse");
                        jsonParam.put("uniqueid", id);
                        jsonParam.put("userchoice", userChoice);

                        Log.i("JSON", jsonParam.toString());
                        DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                        //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                        os.writeBytes(jsonParam.toString());

                        os.flush();
                        os.close();

                        Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                        Log.i("MSG" , conn.getResponseMessage());

                        conn.disconnect();

                        String responseCode = String.valueOf(conn.getResponseCode());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }
