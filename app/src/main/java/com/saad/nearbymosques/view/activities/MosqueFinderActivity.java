package com.saad.nearbymosques.view.activities;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.saad.nearbymosques.R;
import com.saad.nearbymosques.controller.networks.CustomHttpClient;
import com.saad.nearbymosques.controller.utilities.ConstantsClass;
import com.saad.nearbymosques.model.MosqueViewHolder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MosqueFinderActivity extends AppCompatActivity implements  OnItemClickListener {

	public static final long MAX_RADIUS_LIMIT = 1000000;
	private static final int MAP_ACTIVITY_REQUEST_CODE = 1;;
	final int REQUEST_ID_MULTIPLE_PERMISSIONS = 10;
	public String TAG = "Mosques";
	private ListView listViewMosques;
	private MosquesListAdaptor mosquesListAdaptor;
	private ArrayList<JSONObject> mosquesJsonArrayList;
	private Object googleApiKey;
	public String nextPageToken = null;
	private ProgressDialog loadingPrgresBAr;
	public long defaultRadusToSearch = 1000;
	Context context;
	public Location mLastKnownLocation = null;
	private JSONObject location;
	private String location_string;
	private String KEY_LOCATION = "location";
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mosque_finder);
		if (savedInstanceState != null) {
			mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
		}
        getSupportActionBar ().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		context=this;
		googleApiKey = getResources().getString(R.string.google_maps_key);
		mosquesJsonArrayList = new ArrayList<JSONObject>();
		setScreenViews();
		loadingPrgresBAr = new ProgressDialog(MosqueFinderActivity.this);
		loadingPrgresBAr.setMessage("Loading...");
		loadingPrgresBAr.setCancelable(false);
		loadingPrgresBAr.show();
		if(Build.VERSION.SDK_INT>22) {
			if (checkAndRequestPermissions()) {
				getCurrentLocation();
			}
		}
		else{
			getCurrentLocation();
		}
	}

	private  boolean checkAndRequestPermissions() {
		int permissionSendMessage = ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_COARSE_LOCATION);
		int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
		List<String> listPermissionsNeeded = new ArrayList<>();
		if (locationPermission != PackageManager.PERMISSION_GRANTED) {
			listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
		}
		if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
			listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
		}
		if (!listPermissionsNeeded.isEmpty()) {
			ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
			return false;
		}
		return true;
	}
	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		Log.d(TAG, "Permission callback called-------");
		switch (requestCode) {
			case REQUEST_ID_MULTIPLE_PERMISSIONS: {

				Map<String, Integer> perms = new HashMap<>();
				// Initialize the map with both permissions
				perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
				perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
				// Fill with actual results from user
				if (grantResults.length > 0) {
					for (int i = 0; i < permissions.length; i++)
						perms.put(permissions[i], grantResults[i]);
					// Check for both permissions
					if (perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
							&& perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
						Log.d(TAG, "location services permission granted");
						// process the normal flow
						//else any one or both the permissions are not granted
					} else {
						Log.d(TAG, "Some permissions are not granted ask again ");
						//permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
//                        // shouldShowRequestPermissionRationale will return true
						//show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
						if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
							showDialogOK("Location Services Permission required for this app",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											switch (which) {
												case DialogInterface.BUTTON_POSITIVE:
													checkAndRequestPermissions();
													break;
												case DialogInterface.BUTTON_NEGATIVE:
													// proceed with logic by disabling the related features or quit the app.
													break;
											}
										}
									});
						}
						//permission is denied (and never ask again is  checked)
						//shouldShowRequestPermissionRationale will return false
						else {
							Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG)
									.show();
							//                            //proceed with logic by disabling the related features or quit the app.
						}
					}
				}
			}
		}

	}

	private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
		new AlertDialog.Builder(this)
				.setMessage(message)
				.setPositiveButton("OK", okListener)
				.setNegativeButton("Cancel", okListener)
				.create()
				.show();
	}
	public void getCurrentLocation(){
		try {
				// carry on the normal flow, as the case of  permissions  granted.
				FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
				Task locationResult = mFusedLocationProviderClient.getLastLocation();
				locationResult.addOnCompleteListener(this, new OnCompleteListener() {
					@Override
					public void onComplete(@NonNull Task task) {
						if (task.isSuccessful()) {
							// Set the map's camera position to the current location of the device.
							mLastKnownLocation = (Location) task.getResult();
							mosquesJsonArrayList.clear();

							new MosquesDataLoadingThread("").execute();
						} else {
						}
					}
				});
		} catch(SecurityException e)  {
			Log.e("Exception: %s", e.getMessage());
		}
	}

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void setScreenViews() {
		listViewMosques = (ListView)findViewById(R.id.listview_mosque);
		mosquesListAdaptor = new MosquesListAdaptor(context);
		listViewMosques.setAdapter(mosquesListAdaptor);
		listViewMosques.setOnItemClickListener(this);
 	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
	private class MosquesListAdaptor extends BaseAdapter
	{
		private LayoutInflater layoutInflator;

		MosquesListAdaptor(Context context)
		{
			layoutInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		@Override
		public int getCount() {
			if(mosquesJsonArrayList!=null)
				return mosquesJsonArrayList.size();
			else
				return 0;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int location, View convertedView, ViewGroup viewGroup) {
			MosqueViewHolder mosqueViewHolder;
			if(convertedView == null)
			{
				convertedView = layoutInflator.inflate(R.layout.rowitem_mosque, null);
				mosqueViewHolder = new MosqueViewHolder();
				mosqueViewHolder.textViewMosqueName = (TextView) convertedView.findViewById(R.id.textview_mosque_name);
				mosqueViewHolder.textViewMosqueDistance = (TextView) convertedView.findViewById(R.id.textview_mosque_distance);
				convertedView.setTag(mosqueViewHolder);
			}
			else
				mosqueViewHolder = (MosqueViewHolder) convertedView.getTag();
			
			try {
				mosqueViewHolder.textViewMosqueName.setText(mosquesJsonArrayList.get(location).getString("name"));
				mosqueViewHolder.textViewMosqueDistance.setText(
						mosquesJsonArrayList.get(location).getJSONObject("distance").getString("text"));
			} catch (JSONException e) {
			}
			return convertedView;
		}
	}
	public JSONObject getLocationInfo() {
		InputStream is = null;
		JSONObject jObject = null;
		HttpGet httpGet = new HttpGet("http://maps.google.com/maps/api/geocode/json?latlng=" + mLastKnownLocation.getLatitude() + "," + mLastKnownLocation.getLongitude() + "&sensor=true");
		HttpClient client = new DefaultHttpClient();
		HttpResponse response;
		StringBuilder stringBuilder = new StringBuilder();

		try {
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			InputStream stream = entity.getContent();
			int b;
			while ((b = stream.read()) != -1) {
				stringBuilder.append((char) b);
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = new JSONObject(stringBuilder.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return jsonObject;
	}
	private class MosquesDataLoadingThread extends AsyncTask<Void, Void, ArrayList<JSONObject>>
	{
		private String nextPageV;

		public MosquesDataLoadingThread(String nextPageTkn)
		{
			this.nextPageV = nextPageTkn;
		}
		@Override
		protected ArrayList<JSONObject> doInBackground(Void... arg0) {
			
			String apiUrl = "https://maps.googleapis.com/maps/api/place/search/json?location="
			+mLastKnownLocation.getLatitude()+","+mLastKnownLocation.getLongitude()+"&radius="+ String.valueOf(defaultRadusToSearch) +"&sensor=true&types=mosque&key="
			+googleApiKey+"&pagetoken="+nextPageV;
			JSONObject ret = getLocationInfo();

			JSONArray tempJsonArry = null;
			ArrayList<JSONObject> tempJsonArrayList = new ArrayList<JSONObject>();
			String result;
			try {
				location = ret.getJSONArray("results").getJSONObject(0);
				location_string = location.getString("formatted_address");
				result  = CustomHttpClient.executeGet(apiUrl);
				if(result!=null)
				{
					try {
							JSONObject tempRsultJson = new JSONObject(result);
							tempJsonArry = tempRsultJson.getJSONArray("results");
							for (int i = 0; i < tempJsonArry.length(); i++) 
							{
								try {
									String distanceApiUrl = "http://maps.googleapis.com/maps/api/distancematrix/json?origins="+mLastKnownLocation.getLatitude()+","+mLastKnownLocation.getLongitude()
											+"&destinations="+tempJsonArry.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lat")
											+","+tempJsonArry.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lng")+"&sensor=false";
									
									String distanceResult = CustomHttpClient.executeGet(distanceApiUrl);
									tempJsonArry.getJSONObject(i).put("distance", new JSONObject(distanceResult).getJSONArray("rows")
											.getJSONObject(0).getJSONArray("elements").getJSONObject(0).getJSONObject("distance"));
								} catch (Exception e) {
								}

							}
							if(tempRsultJson.has("next_page_token"))
								nextPageToken = tempRsultJson.getString("next_page_token");
							else
								nextPageToken = null;
							
							if(tempJsonArry!=null && tempJsonArry.length()>0)
							{
								for (int i = 0; i < tempJsonArry.length(); i++) {
									tempJsonArrayList.add(tempJsonArry.getJSONObject(i));
								}
							}
						} catch (Exception e) {
						Toast.makeText(MosqueFinderActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
					}
				}
			} catch (Exception e) {

			}
			return tempJsonArrayList;
		}
		
		@Override
		protected void onPostExecute(ArrayList<JSONObject> resultArry) {
				loadingPrgresBAr.dismiss();
			if(resultArry!=null)
			{
				if(mosquesJsonArrayList.size()==0 && resultArry.size()==0 && defaultRadusToSearch<MAX_RADIUS_LIMIT )
				{
						loadingPrgresBAr.show();
					nextPageToken = "";
					defaultRadusToSearch *= 10;
					mosquesJsonArrayList.clear();
					new MosquesDataLoadingThread("").execute();
				}
				else
				{
					mosquesJsonArrayList.addAll(resultArry);
					Collections.sort(mosquesJsonArrayList,new JsonListComparator());
				}
			}else{
				Toast.makeText(context, "No Mosques Found!", Toast.LENGTH_SHORT).show();
			}
			mosquesListAdaptor.notifyDataSetChanged();
			super.onPostExecute(resultArry);
		}
	}
	
	private class JsonListComparator implements Comparator<JSONObject>
	{
		public int compare(JSONObject lhs, JSONObject rhs) {
			try {
				float lhsDist = Float.valueOf(lhs.getJSONObject("distance").getString("text").replace("km", "").trim());
				float rhsDist = Float.valueOf(rhs.getJSONObject("distance").getString("text").replace("km", "").trim());
				if(lhsDist>rhsDist)
					return 1;
				else if(lhsDist<rhsDist)
					return -1;
				else
					return 0;
			} catch (JSONException e) {
				return  0;
			}
			
		}
}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		
		Intent mapActivityIntent = new Intent(context,MapsActivity.class);
		
		mapActivityIntent.putExtra("point_one_lat", mLastKnownLocation.getLatitude());
		mapActivityIntent.putExtra("point_one_lon", mLastKnownLocation.getLongitude());
		try {
			mapActivityIntent.putExtra("point_location_one_title", location_string);
			mapActivityIntent.putExtra("point_location_two_title", mosquesJsonArrayList.get(position).getString("name"));
			mapActivityIntent.putExtra("point_two_lat", mosquesJsonArrayList.get(position).getJSONObject("geometry").getJSONObject("location").getDouble("lat"));
			mapActivityIntent.putExtra("point_two_lon", mosquesJsonArrayList.get(position).getJSONObject("geometry").getJSONObject("location").getDouble("lng"));
		} catch (JSONException e) {
		}
		startActivity(mapActivityIntent);
	}

}
