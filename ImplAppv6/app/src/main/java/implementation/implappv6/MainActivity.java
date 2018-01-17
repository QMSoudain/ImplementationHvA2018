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


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            //Get bundle data from intent
            final Bundle bundle = getIntent().getExtras();

            //Define TextView's that we are going to use to fill with custom data from Firebase
            username = (TextView) findViewById(R.id.textView2);
            uniqueId = (TextView) findViewById(R.id.textView3);
            date = (TextView) findViewById(R.id.textView5);

            //Place data from intent into textviews
            if(bundle != null) {
                username.setText("Uw username is: " + bundle.getString("username"));
                uniqueId.setText("Uw unieke ID is: " + bundle.getString("uniqueid"));
                date.setText(bundle.getString("requestdatetime"));
            }

            //Accept button and onClick action
            acceptButton = (Button) findViewById(R.id.button);
            acceptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Get uniqueId from intent bundle
                    String uniqueId = bundle.getString("uniqueid");

                    //Call HTTP post method
                    String responsePostRequest = callServlet(uniqueId, "accept");

                    //Show AlertDialog with status information about sending data to Servlet
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

            //Reject button and onClick action
            rejectButton = (Button) findViewById(R.id.button2);
            rejectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Get uniqueId from intent bundle
                    String uniqueId = bundle.getString("uniqueid");

                    //Call HTTP Post method
                    String responsePostRequest = callServlet(uniqueId, "reject");

                    //Show AlertDialog with status information about sending data to Servlet
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

        //Print token method for button in app
        public void onTokenRefresh(View view) {
            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            Log.d(TAG, "Refreshed token: " + refreshedToken);
        }


        String responseCode = "0";
        //Method to send HTTP post
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
    }
