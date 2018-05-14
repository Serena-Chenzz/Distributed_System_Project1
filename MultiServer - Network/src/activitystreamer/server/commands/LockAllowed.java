package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Settings;
import activitystreamer.models.*;

public class LockAllowed {
	private final Logger log = LogManager.getLogger();
	private boolean closeConnection=false;


	public LockAllowed(String msg, Connection con) {
		try{
		    //First, I need to check whether this connection is authenticated.
            //If not authenticated, it will return a invalid message
            if (!Control.getInstance().containsServer(con.getRemoteId())) {
                String info = "Lock_Request is not from an authenticated server";
                con.writeMsg(Command.createInvalidMessage(info));
                closeConnection = true;
            }
            else{
                JSONParser parser = new JSONParser();
                JSONObject message = (JSONObject)parser.parse(msg);
                
                String username = message.get("username").toString();
                String secret = message.get("secret").toString();
                
                //First check whether it has received all lock_allowed from its neighbors
                if (Control.getInstance().checkAllLocks(con,msg)){
                    //Writing the user info in local storage
                    Control.getInstance().addLocalUser(username, secret);
                    //If it has received all lock_allowed from its neighbors, it will continue to check whether it is inside 
                    //local register pending list.
                    if (Control.getInstance().changeInPendingList(username, secret)){
                        //If the client is registered in this server, it will return back the message
                        closeConnection = false;
                    }
                    else{
                        //If the client is not registerd in this server, it will broadcast this lock_allowed message.
                        Control.getInstance().broadcast(msg,con.getRemoteId());
                        closeConnection = false;
                    }
                }
                
            }
        }catch(ParseException e){
            e.printStackTrace();
        }
	}
	
	public boolean getCloseCon() {
		return closeConnection;
	}
}
