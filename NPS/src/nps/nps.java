package nps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.json.*;
import java.sql.*;
import java.util.Calendar;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;


/**
 * Servlet implementation class nps
 */
@WebServlet("/nps")
public class nps extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public nps() {
        // TODO Auto-generated constructor stub
    	System.out.println("Ready");
    }

    
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
		//Connection to MySQL server
		String myDriver = "com.mysql.jdbc.Driver";
		String myUrl = "jdbc:mysql://127.0.0.1/implementation";
		Class.forName(myDriver);
		Connection conn = DriverManager.getConnection(myUrl, "root","hva");
			
		//Generate random string that can be used as the uniqueId
		StringBuilder b = new StringBuilder();
		Random r = new Random();
		for (int i = 0; i < 3; i++) {
			b.append((int)r.nextInt(256));
			
		}
		System.out.println(b.toString());

		//Initialize variable
		String postData = null;
		String uniqueIdTemp = b.toString();
		String clientIpTemp = "127.0.0.1";
		String deviceId = null;
		String alreadyAnswered = null;
		String userChoice = "noChoiceYet";
		
		//Initialize requirements to extract date and time
		Calendar calendar = Calendar.getInstance();
	    java.util.Date currentDate = calendar.getTime();
	    java.sql.Date date = new java.sql.Date(currentDate.getTime());
	    Date currentDateForAndroid = new Date();
	    	      		
		//Grab JSON data from body (HTTP Post Request)
		if ("POST".equalsIgnoreCase(request.getMethod())) 
		{
		   postData = request.getReader().lines().collect(Collectors.joining());
		}
		
		//Print raw Json string just for debugging purpose
		System.out.println(postData);
		
		//Put JSON data in a Json Object to be able to extract the objects
		JSONObject obj = new JSONObject(postData);
		String function = obj.getString("function");
		
		//FUNCTION FOR RADIUS PLUGIN!!!!!!
		if (function.equals("radiusplugin"))
		{
			System.out.println("radiusPlugin Post Detected");
			
			//Objects into seperate variables
			String usernameDLL = obj.getString("user");
			String uniqueIdDLL = obj.getString("uniqueid");
			String clientIpDLL = obj.getString("clientip");
			
			//Remove weird character from Username (and later from other radius information)
			String username = usernameDLL.substring(0, usernameDLL.length() - 1);
			String uniqueId = uniqueIdDLL;
			String clientIp = clientIpDLL;
			
			//Write request to database so be able to identify it later when user responds
			Statement st = conn.createStatement();

		    String sqlCreateRequest = "INSERT INTO impl (uniqueId, username, remoteIp, alreadyAnswered, createDateTime, userChoice) VALUES ('"+uniqueIdTemp+"', '"+username+"', '"+clientIpTemp+"', '"+0+"', '"+date+"', '"+userChoice+"')";
		    st.executeUpdate(sqlCreateRequest);	      
	     		     	
	     	//Find Android Firebase Device ID in database
	     	String queryGetDeviceId = "SELECT * FROM devices WHERE username='"+username+"'";
		    ResultSet rs = st.executeQuery(queryGetDeviceId);
		    while (rs.next()) {
		    	deviceId = rs.getString("deviceId");
		    }
			System.out.println(deviceId);
			
			//Firebase - Send Notification with uniqueId, Username to user Device
			String SERVER_KEY_FCM = "AAAAQhGb-L4:APA91bH3frIvlA8EA53s7mxnzd1qh-ndK6NDU34svN13dtkV49kq-2LaRQBn5BTOp7zNSd7SXj-YnMj9ZKxYPiFmP7w2_l3mLyDhTtqgESLql1cR2KewRglNgYf5wd_3HnQj6eIXow5O";
			String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";
			String DeviceIdKey = deviceId;
			URL url = new URL(API_URL_FCM);
			HttpURLConnection connfcm = (HttpURLConnection) url.openConnection();

			connfcm.setUseCaches(false);
			connfcm.setDoInput(true);
			connfcm.setDoOutput(true);
			connfcm.setRequestMethod("POST");
			connfcm.setRequestProperty("Authorization", "key=" + SERVER_KEY_FCM);
			connfcm.setRequestProperty("Content-Type", "application/json");
			
			JSONObject data = new JSONObject();
			JSONObject body = new JSONObject();
			
			data.put("to", DeviceIdKey.trim());
			body.put("uniqueId", uniqueIdTemp);
			body.put("username", username);
			body.put("requestDateTime", currentDateForAndroid);
			data.put("data", body);

			OutputStreamWriter wr = new OutputStreamWriter(connfcm.getOutputStream());
			wr.write(data.toString());
			wr.flush();
			wr.close();
			int responseCode = connfcm.getResponseCode();
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(connfcm.getInputStream()));
			String inputLine;
			StringBuffer responsefcm = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				responsefcm.append(inputLine);
			}
			in.close();
			
			//Refresh and Loop trough SQL result for 1 minute, when user responds update SQL record and give response to DLL 
			System.out.println("Loop will start now");
			for (int i = 0; i < 10; i++) {
				System.out.println("Loop start");
				//Query DB to check if 'UserChoice' is updated
				String query2 = "SELECT * FROM impl WHERE uniqueId='"+uniqueIdTemp+"'";
			    ResultSet rs2 = st.executeQuery(query2);
			    while (rs2.next()) {
			    	 userChoice = rs2.getString("userChoice");
			    }
			    
			    //Check result of query and do proper action based on result (Nothing, accept or reject)
			   	if (userChoice.equals("noChoiceYet")) {
					System.out.println("User did not make a choice yet");
				} else if (userChoice.equals("accept")) {
					System.out.println("User Accept detected in DB for ID "+uniqueIdTemp);
					response.getOutputStream().println("accept");
					st.close();
					break;
				} else if (userChoice.equals("reject")) {
					System.out.println("User Reject detected in DB for ID "+uniqueIdTemp);
					response.getOutputStream().println("reject");
					st.close();
					break;
				}
				System.out.println("Loop end, will sleep for 10 sec");
				TimeUnit.SECONDS.sleep(10);
			}
			
			//Default response when no valid response received or within time
			if (userChoice.equals("accept") || userChoice.equals("reject")) {
				System.out.println("Geen reactie binnen gestelde tijd");
				response.getOutputStream().println("reject");
			} else {
				System.out.println("Geen geldige reactie ontvangen. Default Reject wordt verstuurd naar de NPS server");
				response.getOutputStream().println("reject");
			}
			
			
		//FUNCTION FOR ANDROID RESPONSE
		} else if (function.equals("androidresponse")){
			System.out.println("AndroidResponse Post Detected");
			
			//Extract JSON objects that were sended by the Android device
			String uniqueId = obj.getString("uniqueid");
			userChoice = obj.getString("userchoice");
			
			//Check if uniqueId sended by the android device is in the database by doing a query
			String query3 = "SELECT * FROM impl WHERE uniqueId='"+uniqueId+"'";
		    Statement st = conn.createStatement();
		    ResultSet rs = st.executeQuery(query3);
		    while (rs.next()) {
		    	alreadyAnswered = rs.getString("alreadyAnswered");
		    }
		    
		    //Check if request is still open and do proper action (Process or abort)
			if (alreadyAnswered.equals("0")) {
				System.out.println("Request is still open and will be processed");
				if (userChoice.equals("accept")) {
					System.out.println("User accepted request from App");
					String sql = "UPDATE impl SET userChoice='"+userChoice+"', alreadyAnswered=1 WHERE uniqueId='"+uniqueId+"'";
					st.executeUpdate(sql);
					st.close();
				} else if (userChoice.equals("reject")) {
					System.out.println("User rejected request from App");
					String sql = "UPDATE impl SET userChoice='"+userChoice+"', alreadyAnswered=1 WHERE uniqueId='"+uniqueId+"'";
					st.executeUpdate(sql);
					st.close();
				}
			} else if (alreadyAnswered.equals("1")) {
				System.out.println("Request is already processed. Not processing request.");
			}
		} else {
			System.out.println("No valid function was recognized. Abort!");
		}
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
		}
	}
}