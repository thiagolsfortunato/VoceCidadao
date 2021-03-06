package br.gov.sp.fatec.vocecidadao.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import br.gov.sp.fatec.vocecidadao.adapter.PlacesAutoCompleteAdapter;
import br.gov.sp.fatec.vocecidadao.model.DetalheSugestao;
import br.gov.sp.fatec.vocecidadao.service.SugestaoService;
import br.gov.sp.fatec.vocecidadao.util.GeocodeJSONParser;
import fatec.sp.gov.br.vocecidadao.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap;
    private static final String TAG = "MainActivity";
    private DisplayMetrics displayMetrics;
    private ImageButton ibSearch;
    private LatLng latLng;
    private ProgressDialog progDailog;
    private ProgressBar progressBar;
    private final static int PHOTO = 2;
    private Double latitude, longitude;
    private MarkerOptions markerOptions;
    private final DetalheSugestao detalheSugestao = new DetalheSugestao();

    private AutoCompleteTextView myLocation;
    private GoogleApiClient mGoogleApiClient;
    private PlacePicker.IntentBuilder builder;
    private PlacesAutoCompleteAdapter mPlacesAdapter;
    private static final int PLACE_PICKER_FLAG = 1;
    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));
    private AlertDialog alerta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Bem vindo ao Você Cidadão. Digite o endereço do local que você gostaria de mudar.");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            }
        });
        alerta = builder.create();
        alerta.show();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .build();
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        //builder = new PlacePicker.IntentBuilder();
        myLocation = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView1);
        mPlacesAdapter = new PlacesAutoCompleteAdapter(this, android.R.layout.simple_list_item_1,
                mGoogleApiClient, BOUNDS_GREATER_SYDNEY, null);
        myLocation.setOnItemClickListener(mAutocompleteClickListener);
        myLocation.setAdapter(mPlacesAdapter);


        ibSearch = (ImageButton) findViewById(R.id.btnSearch);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        ibSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String location = myLocation.getText().toString();
                Log.i("Localizacao ", location);

                if (location == null || location.equals("")) {
                    Toast.makeText(MapsActivity.this, "No place found",
                            Toast.LENGTH_SHORT).show();
                    setProgress(false);
                    return;
                }
                locationOnMap(location);

            }
        });


        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            public void onMapLongClick(LatLng latLng) {
                mMap.clear();

                latitude = latLng.latitude;
                longitude = latLng.longitude;

                mMap.animateCamera(CameraUpdateFactory
                        .newLatLng(latLng));
                markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                mMap.addMarker(markerOptions);

                //---------------
                try {
                    Geocoder geocoder;
                    List<Address> addresses;
                    geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());

                    addresses = geocoder.getFromLocation(latitude, longitude, 1);

                    String address = addresses.get(0).getAddressLine(0);
                    String city = addresses.get(0).getLocality();
                    String state = addresses.get(0).getAdminArea();
                    String country = addresses.get(0).getCountryName();
                    String postalCode = addresses.get(0).getPostalCode();
                    String knownName = addresses.get(0).getFeatureName();


                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MapsActivity.this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.i("Blah", "blah3");

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
                alertDialog.setTitle("Digite sua sugestão");

                final EditText input = new EditText(MapsActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);
                //alertDialog.setIcon(R.drawable.key);

                alertDialog.setPositiveButton("Prosseguir",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String comentario = input.getText().toString();
                                Intent intent = new Intent();
                                intent.setType("image/*");
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(intent, PHOTO);

                                Toast.makeText(MapsActivity.this, "Escolha uma foto do local.", Toast.LENGTH_SHORT).show();
                            }
                        });

                alertDialog.setNegativeButton("Cancelar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
                return false;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Uri uri = data.getData();
        ContentResolver contentResolver = this.getContentResolver();
        try{
            Bitmap bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri));
            if(bitmap.getWidth()>bitmap.getHeight())
                ScalePic(bitmap,displayMetrics.heightPixels);
            else
                ScalePic(bitmap,displayMetrics.widthPixels);

            //converting bitmap in Bae64 and set on the object
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream .toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            detalheSugestao.setImagem(encoded);

            sendData();
        }catch (FileNotFoundException e){
            Log.d(TAG,e.toString());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void ScalePic(Bitmap bitmap,int phone) {
        float mScale = 1;

        if (bitmap.getWidth() > phone) {
            mScale = (float) phone / (float) bitmap.getWidth();

            Matrix mMat = new Matrix();
            mMat.setScale(mScale, mScale);

            Bitmap mScaleBitmap = Bitmap.createBitmap(bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    mMat,
                    false);
        }
    }

    public void sendData(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Deseja enviar sua sugestão?");
        builder.setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {

                SugestaoService service = SugestaoService.retrofit.create(SugestaoService.class);
                final Call<DetalheSugestao> call =
                        service.inserirSugestao(detalheSugestao);// repoContributors("square", "retrofit");
                call.enqueue(
                        new Callback<DetalheSugestao>() {

                            @Override
                            public void onResponse(Call<DetalheSugestao> call, Response<DetalheSugestao> response) {
                                Log.i("Success ",response.body().toString());
                                Toast.makeText(MapsActivity.this, "Sua sugestão foi envida com sucesso..", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Call<DetalheSugestao> call, Throwable t) {
                                Log.i("Something went wrong: ", t.getMessage());
                                Toast.makeText(MapsActivity.this, "Ops, aconteceu algo de errado. Tente novamente.", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.cancel();
            }
        });
        alerta = builder.create();
        alerta.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }
    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlacesAutoCompleteAdapter.PlaceAutocomplete item = mPlacesAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e("place", "Place query did not complete. Error: " +
                        places.getStatus().toString());
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);
        }
    };

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(-23.162611, -45.795177)).title("Fatec São José dos Campos"));
    }

    private void locationOnMap(String location) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json?";
        setProgress(true);
        try {
            // encoding special characters like space in the user input
            // place
            location = URLEncoder.encode(location, "utf-8");
        } catch (UnsupportedEncodingException e) {
            setProgress(false);
            e.printStackTrace();
        }

        String address = "address=" + location;

        String sensor = "sensor=false";

        // url , from where the geocoding data is fetched
        url = url + address + "&" + sensor;

        // Instantiating DownloadTask to get places from Google
        // Geocoding service
        // in a non-ui thread
        DownloadTask downloadTask = new DownloadTask();

        // Start downloading the geocoding places
        downloadTask.execute(url);
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                setProgress(false);
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result) {

            // Instantiating ParserTask which parses the json data from
            // Geocoding webservice
            // in a non-ui thread
            ParserTask parserTask = new ParserTask();

            // Start parsing the places in JSON format
            // Invokes the "doInBackground()" method of the class ParseTask
            parserTask.execute(result);
        }
    }

    @SuppressLint("LongLogTag")
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            br.close();

        } catch (Exception e) {
            Log.d("Exception while downloading url", e.toString());
        } finally {
            setProgress(false);
            iStream.close();
            urlConnection.disconnect();
        }

        return data;
    }

    // ** A class to parse the Geocoding Places in non-ui thread *//*
    class ParserTask extends
            AsyncTask<String, Integer, List<HashMap<String, String>>> {

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String, String>> doInBackground(
                String... jsonData) {

            List<HashMap<String, String>> places = null;
            GeocodeJSONParser parser = new GeocodeJSONParser();

            try {
                jObject = new JSONObject(jsonData[0]);

                // ** Getting the parsed data as a an ArrayList *//*
                places = parser.parse(jObject);

            } catch (Exception e) {
                setProgress(false);
                Log.d("Exception", e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(List<HashMap<String, String>> list) {

            // Clears all the existing markers
            mMap.clear();
            setProgress(false);
            for (int i = 0; i < list.size(); i++) {
                if (i == 0) {
                    HashMap<String, String> hmPlace = list.get(i);
                    double lat = Double.parseDouble(hmPlace.get("lat"));
                    double lng = Double.parseDouble(hmPlace.get("lng"));
                    String name = hmPlace.get("formatted_address");
                    plotMarker(lat, lng, name);
                    break;
                }
            }
        }
    }

    private void plotMarker(double lati, double longi, String name) {
        MarkerOptions markerOptions = new MarkerOptions();
        LatLng latLng = new LatLng(lati, longi);
        markerOptions.position(latLng);
        markerOptions.title(name);
        mMap.addMarker(markerOptions);
        // googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f));
        latitude = lati;
        longitude = longi;

        detalheSugestao.setEndereco(name);
        detalheSugestao.setLatitude(latitude.toString());
        detalheSugestao.setLongitude(longitude.toString());
    }

    private void setProgress(final boolean bol) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (bol) {
                    ibSearch.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    myLocation.setEnabled(false);
                } else {
                    ibSearch.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    myLocation.setEnabled(true);
                }
            }
        });
    }
}
