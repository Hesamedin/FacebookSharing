package com.kamalan.facebooksharing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.socialnetworks.facebook.BaseDialogListener;
import com.socialnetworks.facebook.SessionEvents;
import com.socialnetworks.facebook.SessionEvents.AuthListener;
import com.socialnetworks.facebook.SessionEvents.LogoutListener;
import com.socialnetworks.facebook.SessionStore;

public class MainActivity extends Activity {

	private static final String TAG = "FacebookActivity";
	private static final String FACEBOOK_APPID = "213308992128590"; // <===== This key should be changed depends of application.
	private static final String[] PERMISSIONS = new String[] {"publish_stream"};
	private static final int MSG_AUTH_SUCCEED = 1;
    
	private Facebook mFacebook;
    private SessionListener mSessionListener;
	private Context mContext;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Try to create activity...");
        
        // Close activity if app id is not specified.
        if (FACEBOOK_APPID == null  ||  FACEBOOK_APPID == "") {
            Log.e(TAG, "Facebook Applicaton ID must be specified before running this screen.");
            closeActivity();
        }
        
        // Assign layout to activity
        setContentView(R.layout.activity_main);
        
        // Save context in order to use it later
	   	mContext = this;
	   	
        // Create Facebook object
        mFacebook = new Facebook(FACEBOOK_APPID);
        
        // Create session object
        mSessionListener = new SessionListener();
	   	SessionStore.restore(mFacebook, this);
		SessionEvents.addAuthListener(mSessionListener);
	    SessionEvents.addLogoutListener(mSessionListener);
		
	    if(!mFacebook.isSessionValid()) {
	    	Log.i(TAG, "Session is not valid. Try to get token...");
	    	mFacebook.authorize(this, PERMISSIONS, Facebook.FORCE_DIALOG_AUTH, new LoginDialogListener());
	    } else {
	    	Log.i(TAG, "Session is valid.");
	    	postFacebookMessage();
	    }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private void closeActivity() {
		this.finish();
	}
    
    Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_AUTH_SUCCEED:
					postFacebookMessage();
					break;
			}
		}
	};
    
    private class SessionListener implements AuthListener, LogoutListener {

		public void onAuthSucceed() {
			SessionStore.save(mFacebook, mContext);
			Message message = new Message();
			message.what = MSG_AUTH_SUCCEED;
			handler.sendMessage(message);
		}

		public void onAuthFail(String error) {
			closeActivity();
		}

		public void onLogoutBegin() {
		}

		public void onLogoutFinish() {
			SessionStore.clear(mContext);
		}
	}
    
    public class LoginDialogListener implements DialogListener {

		public void onComplete(Bundle values) {
			SessionEvents.onLoginSuccess();
		}

		public void onFacebookError(FacebookError error) {
			SessionEvents.onLoginError(error.getMessage());
			closeActivity();
		}

		public void onError(DialogError error) {
			SessionEvents.onLoginError(error.getMessage());
			closeActivity();
		}

		public void onCancel() {
			SessionEvents.onLoginError("Action Canceled");
			closeActivity();
		}
	}

    public class PostDialogListener extends BaseDialogListener {
    	
        public void onComplete(Bundle values) {
            final String postId = values.getString("post_id");
            if (postId != null) {
                Log.d(TAG, "Successfully posted, post_id=> " + postId);
            } else {
            	Log.d(TAG, "Failed to post :(");
            }
            
            closeActivity();
        }

		@Override
		public void onFacebookError(FacebookError e) {
			super.onFacebookError(e);
			Log.e(TAG, "Facebook Error: " + e.toString());
		}

		@Override
		public void onError(DialogError e) {
			super.onError(e);
			Log.e(TAG, e.toString());
		}

		@Override
		public void onCancel() {
			super.onCancel();
			closeActivity();
		}
        
    }


    private void postFacebookMessage() {
		Log.i(TAG, "Try to post message...");
		
		try {
			JSONObject attachment = new JSONObject();
		    attachment.put("name", "Facebook Integration");
		    attachment.put("href", "http://developer.blog.appxtream.com/?p=34");
		    attachment.put("description", "This code shows how we can customize this dialog based on our needs.");
				
		    JSONObject media = new JSONObject();
		    media.put("type", "image");
		    media.put("src",  "http://i45.tinypic.com/29ol6pl.png");
		    media.put("href", "http://developer.blog.appxtream.com/?p=34");
		    attachment.put("media", new JSONArray().put(media));
		        
		    JSONObject properties = new JSONObject();
		    JSONObject prop1 = new JSONObject();
		    prop1.put("text", "Hope you like it :)");
		    prop1.put("href", "http://developer.blog.appxtream.com/?p=34");
		    properties.put("Finally", prop1);
				
		    attachment.put("properties", properties);
		        
		    Bundle params = new Bundle();
		    params.putString("attachment", attachment.toString());
		    mFacebook.dialog(mContext, "stream.publish", params, new PostDialogListener());      
	
		} catch (JSONException e) {
		    Log.e(TAG, e.getLocalizedMessage(), e);
		}
	}   
}
