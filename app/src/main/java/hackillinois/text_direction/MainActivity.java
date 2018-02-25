package hackillinois.text_direction;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.BoolRes;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  private static final int TRANS_CAR = 1;
  private static final int TRANS_TRAIN = 2;
  private static final int TRANS_WALKING = 3;
  private static final int ALL_PERMISSIONS = 101;
  private static final String TWILIO_PHONE_NUMBER = "12175744278"; // temporary number
  private static final String AWS_SNS_PHONE_NUMBER = "37083";

  // Different maneuver and corresponding image resource ID
  public static final Map<String, Integer> MANEUVER = new HashMap<String, Integer>() {{
    put("straight", R.drawable.straight);
    put("merge",  R.drawable.merge);
    put("right",  R.drawable.turn_right);
    put("left",  R.drawable.turn_left);
  }};

  private Button goButton;
  private EditText toEditText;
  private EditText fromEditText;
  private ImageView carImage;
  private ImageView trainImage;
  private ImageView walkingImage;
  private String geoLocation;
  private static CustomList adapter;
  private static boolean lastMessage = true;

  // Current transportation mode; initialized to driving
  private int transMode = TRANS_CAR;

  // ArrayList for ListView
  private static ArrayList<String> directionList;
  private static ArrayList<String> distanceList;
  private static ArrayList<Integer> imageID;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Check permissions
    int sendReq = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
    int receiveReq = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
    int readReq = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
    int locReq = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

    // If any of them is not approved by the user, dynamically request permission
    if (sendReq != PackageManager.PERMISSION_GRANTED ||
        receiveReq != PackageManager.PERMISSION_GRANTED ||
        readReq != PackageManager.PERMISSION_GRANTED ||
        locReq != PackageManager.PERMISSION_GRANTED) {
      requestPermission();
    } else {
      // Else, enable GPS tracker
      setGPSTracker();
    }

    toEditText = (EditText) findViewById(R.id.to_text);
    fromEditText = (EditText) findViewById(R.id.from_text);
    goButton = (Button) findViewById(R.id.go_button);
    carImage = (ImageView) findViewById(R.id.car_image);
    trainImage = (ImageView) findViewById(R.id.train_image);
    walkingImage = (ImageView) findViewById(R.id.walking_image);

    carImage.setOnClickListener(this);
    trainImage.setOnClickListener(this);
    walkingImage.setOnClickListener(this);
    goButton.setOnClickListener(this);

    directionList = new ArrayList<>();
    distanceList = new ArrayList<>();
    imageID = new ArrayList<>();
    adapter = new CustomList(MainActivity.this, directionList, distanceList, imageID);

    ListView listView = (ListView) findViewById(R.id.list_view);
    listView.setAdapter(adapter);

    onTransModeChange();
  }

  // Update ImageViews' images accordingly
  private void onTransModeChange() {
    if (transMode == TRANS_CAR) {
      carImage.setImageResource(R.drawable.car_clicked);
      trainImage.setImageResource(R.drawable.train);
      walkingImage.setImageResource(R.drawable.walking);
    } else if (transMode == TRANS_WALKING) {
      walkingImage.setImageResource(R.drawable.walking_clicked);
      carImage.setImageResource(R.drawable.car);
      trainImage.setImageResource(R.drawable.train);
    } else if (transMode == TRANS_TRAIN) {
      trainImage.setImageResource(R.drawable.train_clicked);
      walkingImage.setImageResource(R.drawable.walking);
      carImage.setImageResource(R.drawable.car);
    }
  }

  @Override
  public void onClick(View view) {
    if (view == carImage) {
      Log.d("TAG", "Driving is selected");
      transMode = TRANS_CAR;
    } else if (view == trainImage) {
      Log.d("TAG", "Transit is selected");
      transMode = TRANS_TRAIN;
    } else if (view == walkingImage) {
      Log.d("TAG", "Walking is selected");
      transMode = TRANS_WALKING;
    } else if (view == goButton) {
      Log.d("TAG", "Go button pressed");
      sendSMS();
    }

    // Update transMode
    onTransModeChange();
  }

  private void setGPSTracker() {
    LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    // Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
      public void onLocationChanged(Location location) {
        // Called when a new location is found by the network location provider
        // It passes x and y in "x, y" format
        geoLocation = Double.toString(location.getLatitude()) + ',' + Double.toString(location.getLongitude());
      }

      public void onStatusChanged(String provider, int status, Bundle extras) {
      }
      public void onProviderEnabled(String provider) {
      }
      public void onProviderDisabled(String provider) {
      }
    };

    // Register the listener with the Location Manager to receive location updates
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED) {
      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }
  }

  // Android App ==SMS==> Twilio Server ==HTTP==> AWS Lambda ==> Google Maps ==> AWS SQS & SNS
  private void sendSMS() {
    String origin = fromEditText.getText().toString();
    String destination = toEditText.getText().toString();

    // Use GPS location by default
    if (origin.contains("Your Location")) {
      Log.d("TAG", "origin equals your location");
      origin = geoLocation;
    }

    // Form up url to Google API
    String strMessage = "https://maps.googleapis.com/maps/api/directions/json?" +
        "origin=" + origin + '&' +
        "destination=" + destination + '&' +
        "mode=";

    // String for mode of transportation; initially driving
    switch (transMode) {
      case TRANS_CAR:
        strMessage += "driving";
        break;
      case TRANS_TRAIN:
        strMessage += "transit";
        break;
      case TRANS_WALKING:
        strMessage += "walking";
        break;
      default:
        strMessage += "driving";
        break;
    }

    // Send
    SmsManager sms = SmsManager.getDefault();
    sms.sendTextMessage(TWILIO_PHONE_NUMBER, null, strMessage, null, null);
    Log.d("TAG", strMessage);
  }

  // When the app is running on foreground, we could use this function to receive on-going SMS
  public static class SmsReceiver extends BroadcastReceiver {
    private static String buffer = "";

    @Override
    public void onReceive(Context context, Intent intent) {
      Bundle intentExtras = intent.getExtras();

      if (intentExtras != null) {
        Log.d("TAG", "smsReceiver");
        Object[] sms = (Object[]) intentExtras.get("pdus");

        assert sms != null;

        if (lastMessage) {
          Log.d("lastMessage: ", "true in addToLists so update");
          directionList.clear();
          distanceList.clear();
          imageID.clear();
        }

        // Note: When a long message is sent, it is divided into multiple messages
        for (Object sm : sms) {
          SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) sm);
          String phone = smsMessage.getOriginatingAddress();
          String message = smsMessage.getMessageBody();
          String mssg = String.copyValueOf(message.toCharArray());

          // Filter out unnecessary SMS using their phone numbers
          //if (phone.contains(TWILIO_PHONE_NUMBER)) {
        if (phone.contains(AWS_SNS_PHONE_NUMBER)) {
            Log.d("TAG", "Twilio");
            Log.d("lastMessage: ", Boolean.toString(lastMessage));

//            This is for when we are using Twilio free texting service
//            mssg = mssg.replace("Sent from your Twilio trial account - ", "");
//            mssg = mssg.replace(" - Lambda", "");

            // Cut the message every time we see a new line
            while (mssg.contains("\n")) {
              String substring =  mssg.substring(0, mssg.indexOf("\n"));

              // String is saved on buffer if there a leftover string at the end of paragraph
              if (!buffer.isEmpty()) {
                substring = buffer + substring;
                buffer = "";
              }

              lastMessage = !substring.contains("PART2");

              // Process substring and populate ListView
              addToLists(substring);
              mssg = mssg.substring(mssg.indexOf("\n") + 1, mssg.length());
            }

            if (mssg.length() > 1) {
              buffer = mssg;
              Log.d("TAG", "buffer: " + buffer);
            } else {
              buffer = "";
            }
          } else {
            Log.d("TAG", phone + ": " + message);
          }
        }

        // Last one could be in buffer
        if (buffer.length() > 0) {
          Log.d("TAG", buffer);
          addToLists(buffer);
          buffer = "";
        }

        // Update whoever is listening to adapter's data set changing
        if (lastMessage) {
          Log.d("lastMessage: ", "true, so update");
          adapter.notifyDataSetChanged();
        }
      }
    }

    // Add direction, distance, and imageID for every currStr
    // FORMAT of string: "Go there and here and there:100mi;straight
    private void addToLists(String currStr) {
      if (!currStr.contains(":"))
        return;

      String currDir = currStr.substring(0, currStr.indexOf(":"));
      String currDis = currStr.substring(currStr.indexOf(":") + 1, currStr.indexOf(";"));
      String currMan = currStr.substring(currStr.indexOf(";") + 1, currStr.length());

      // Add to the lists
      directionList.add(currDir);
      distanceList.add(currDis);
      if (currMan.contains("right")) {
        imageID.add(MANEUVER.get("right"));
      } else if (currMan.contains("left")) {
        imageID.add(MANEUVER.get("left"));
      } else {
        if (currMan.equals("straight"))
          imageID.add(MANEUVER.get("straight"));
        else if (currMan.equals("merge"))
          imageID.add(MANEUVER.get("merge"));
      }
    }
  }

  // Read past SMS
  // Not sure where to use this for our project
  private void readSMS() {
    Cursor cur = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

    if (cur != null && cur.moveToFirst()) { /* false = no sms */
      String msgInfo = "";
      for (int i = 0; i < cur.getColumnCount(); i++) {
        if (cur.getColumnName(i).contains("body")) {
          msgInfo = cur.getString(i);
        }
      }

      Log.d("TAG", msgInfo);
      cur.close();
    }
  }

  // Delete SMS with given SMS ID
  private boolean deleteSMS(String smsId) {
    boolean isSMSDeleted;

    try {
      getContentResolver().delete(Uri.parse("content://sms/inbox" + smsId), null, null);
      isSMSDeleted = true;

    } catch (Exception ex) {
      isSMSDeleted = false;
    }
    return isSMSDeleted;
  }

  // Requests permissions for the app
  private void requestPermission() {
    final String[] permissions = new String[]{Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION};

    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]) ||
        !ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[1]) ||
        !ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[2]) ||
        !ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[3])) {
      ActivityCompat.requestPermissions(this, permissions, ALL_PERMISSIONS);
    }
  }

  // When the user interacted with request permission approval
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    boolean smsSendPermResult = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
    boolean smsReceivePermResult = (grantResults[1] == PackageManager.PERMISSION_GRANTED);
    boolean smsReadPermResult = (grantResults[2] == PackageManager.PERMISSION_GRANTED);
    boolean locPermResult = (grantResults[3] == PackageManager.PERMISSION_GRANTED);

    if (smsSendPermResult && smsReceivePermResult && smsReadPermResult && locPermResult) {
      Log.d("TAG", "All permission approved");
      setGPSTracker();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
