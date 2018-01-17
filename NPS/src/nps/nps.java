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
    }

    
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		try {
			System.out.println("GET Request Now");
		} catch (Exception e){
			System.out.println(e.getLocalizedMessage());
		}
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
			
		//Generate random string
		StringBuilder b = new StringBuilder();
		Random r = new Random();
		for (int i = 0; i < 3; i++) {
			b.append((int)r.nextInt(256));
			
		}
		System.out.println(b.toString());

		//Initialize variable
		String dataFromDLL = null;
		String uniqueIdTemp = b.toString();
		String clientIpTemp = "192.168.0.30";
		String deviceId = null;
		String alreadyAnswered = null;
		String userChoice = "noChoiceYet";
		
		Calendar calendar = Calendar.getInstance();
	    java.util.Date currentDate = calendar.getTime();
	    java.sql.Date date = new java.sql.Date(currentDate.getTime());
	    
	    Date currentDateForAndroid = new Date();
	    
	    System.out.println(currentDate.getTime());
	   		
		//Grab JSON data from body (HTTP Post from DLL)
		if ("POST".equalsIgnoreCase(request.getMethod())) 
		{
		   dataFromDLL = request.getReader().lines().collect(Collectors.joining());
		}
		
		//Just for debugging purpose
		System.out.println(dataFromDLL);
		
		//Parse JSON by putting every object in a seperate variable
		JSONObject obj = new JSONObject(dataFromDLL);
		String function = obj.getString("function");
		
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
			
			//Write request to database
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
			
			//Firebase - Send Notification with uniqueId, Username, DeviceId
			String SERVER_KEY_FCM = "XXXXXXXXIvlA8EA53s7mxnzd1qh-ndK6NDU34svN13dtkV49kq-2LaRQBn5BTOp7zNSd7SXj-YnMj9ZKxYPiFmP7w2_l3mLyDhTtqgESLql1cR2KewRglNgYf5wd_3HnQj6eIXow5O";
			String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";
			//String DEVICE_TOKEN="c8l2bgFzg4I:APA91bF7MhhZIBK-XMrH8JaKh29NQt1a_4qK8kno159HMF-6aGaIz-swb6cWVbtVP4oB33OkCbGtEbVv-AGhe8RAVhuV0CG6gGIw3LV4LleKhLCo166A-1Vb6KpMECmy-N4g0wZla2ZB";
			String DeviceIdKey= deviceId;
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
			//info.put("title", "Login Request"); // Notification title
			//info.put("body", "Login Request: OpenVPN Server"); // Notification body
			body.put("uniqueId", uniqueIdTemp);
			body.put("username", username);
			body.put("requestDateTime", currentDateForAndroid);
			data.put("data", body);
			//data.put("notification", body);
			//System.out.println(data.toString());
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
			
			//Refresh and Loop trough SQL result for 1 minute, update SQL record and give response to DLL 
			System.out.println("Loop will start now");
			for (int i = 0; i < 10; i++) {
				System.out.println("loop");
				String query2 = "SELECT * FROM impl WHERE uniqueId='"+uniqueIdTemp+"'";
			    ResultSet rs2 = st.executeQuery(query2);
			    while (rs2.next()) {
			    	 userChoice = rs2.getString("userChoice");
			    }
			    
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
			
			//Default response when no valid response received
			if (userChoice.equals("accept")|| userChoice.equals("reject")) {
				
			} else {
			System.out.println("Geen geldige reactie ontvangen. Default Reject wordt verstuurd naar de NPS server");
			response.getOutputStream().println("reject");
			}
		} else if (function.equals("androidresponse")){
			System.out.println("radiusPlugin Post Detected");
			
			String uniqueId = obj.getString("uniqueid");
			userChoice = obj.getString("userchoice");
			
			String query3 = "SELECT * FROM impl WHERE uniqueId='"+uniqueId+"'";
		    Statement st = conn.createStatement();
		    ResultSet rs = st.executeQuery(query3);
		    while (rs.next()) {
		    	alreadyAnswered = rs.getString("alreadyAnswered");
		    }
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
				System.out.println("Request is already answered. Not processing request.");
			}
		} else {
			System.out.println("No valid function was recognized. Abort!");
		}
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
		}
	}
}