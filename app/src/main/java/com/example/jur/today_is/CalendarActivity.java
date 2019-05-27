package com.example.jur.today_is;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class CalendarActivity extends Activity implements EasyPermissions.PermissionCallbacks {


    FirebaseAuth mfirebaseAuth;
    FirebaseUser firebaseUser;
    DynamoDBMapper dynamoDBMapper;

    @DynamoDBTable(tableName = "auth")

    static public class NewsDO {
        private String email;
        private String name;


        @DynamoDBHashKey(attributeName = "email")
        @DynamoDBAttribute(attributeName = "email")
        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }
        @DynamoDBRangeKey(attributeName = "name")
        @DynamoDBAttribute(attributeName = "name")
        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }


        // setters and getters for other attributes ...

    }
    /**
     * Google Calendar API에 접근하기 위해 사용되는 구글 캘린더 API 서비스 객체
     */

    private com.google.api.services.calendar.Calendar mService = null;

    /**
     * Google Calendar API 호출 관련 메커니즘 및 AsyncTask을 재사용하기 위해 사용
     */
    private  int mID = 0;


    GoogleAccountCredential mCredential;
    private TextView mStatusText;
    private TextView mResultText;
    private Button mGetEventButton;
    ProgressDialog mProgress;


    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;


    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};

    private Context tcontext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_calendar);

        AWSMobileClient.getInstance().initialize(this).execute();

        tcontext = CalendarActivity.this;
        Intent calendar = new Intent();
        calendar = tcontext.getPackageManager().getLaunchIntentForPackage("com.google.android.calendar");

        if(calendar == null){

            install_calender();

        }else{
            mProgress = new ProgressDialog(this);
            mProgress.setMessage("Google 캘린더를 가져오는 중입니다.");


            // Google Calendar API 사용하기 위해 필요한 인증 초기화( 자격 증명 credentials, 서비스 객체 )
            // OAuth 2.0를 사용하여 구글 계정 선택 및 인증하기 위한 준비
            mCredential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(),
                    Arrays.asList(SCOPES)
            ).setBackOff(new ExponentialBackOff()); // I/O 예외 상황을 대비해서 백오프 정책 사용

            mID = 3;        //이벤트 가져오기
            getResultsFromApi();

        }



        // Google Calendar API의 호출 결과를 표시하는 TextView를 준비


        // Google Calendar API 호출중에 표시되는 ProgressDialog


    }
    public void install_calender(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("구글 캘린더 없음");
        builder.setMessage("원활한 사용을 위해서 구글 캘린더 설치페이지로 이동합니다.");
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.calendar")));
                        finish();
                    }
                }
        );
        builder.show();
    }



    /**
     * 다음 사전 조건을 모두 만족해야 Google Calendar API를 사용할 수 있다.
     *
     * 사전 조건
     *     - Google Play Services 설치
     *     - 유효한 구글 계정 선택
     *     - 안드로이드 디바이스에서 인터넷 사용 가능
     *
     * 하나라도 만족하지 않으면 해당 사항을 사용자에게 알림.
     */
    private String getResultsFromApi() {

        if (!isGooglePlayServicesAvailable()) { // Google Play Services를 사용할 수 없는 경우

            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) { // 유효한 Google 계정이 선택되어 있지 않은 경우

            chooseAccount();
        } else if (!isDeviceOnline()) {    // 인터넷을 사용할 수 없는 경우

            mStatusText.setText("No network connection available.");
        } else {

            // Google Calendar API 호출
            new MakeRequestTask(this, mCredential).execute();
        }
        return null;
    }



    /**
     * 안드로이드 디바이스에 최신 버전의 Google Play Services가 설치되어 있는지 확인
     */
    private boolean isGooglePlayServicesAvailable() {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();

        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }



    /*
     * Google Play Services 업데이트로 해결가능하다면 사용자가 최신 버전으로 업데이트하도록 유도하기위해
     * 대화상자를 보여줌.
     */
    private void acquireGooglePlayServices() {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {

            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }



    /*
     * 안드로이드 디바이스에 Google Play Services가 설치 안되어 있거나 오래된 버전인 경우 보여주는 대화상자
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode
    ) {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();

        Dialog dialog = apiAvailability.getErrorDialog(
                CalendarActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES
        );
        dialog.show();
    }



    /*
     * Google Calendar API의 자격 증명( credentials ) 에 사용할 구글 계정을 설정한다.
     *
     * 전에 사용자가 구글 계정을 선택한 적이 없다면 다이얼로그에서 사용자를 선택하도록 한다.
     * GET_ACCOUNTS 퍼미션이 필요하다.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {

        // GET_ACCOUNTS 권한을 가지고 있다면
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {


            // SharedPreferences에서 저장된 Google 계정 이름을 가져온다.
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {

                // 선택된 구글 계정 이름으로 설정한다.
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {


                // 사용자가 구글 계정을 선택할 수 있는 다이얼로그를 보여준다.
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }



            // GET_ACCOUNTS 권한을 가지고 있지 않다면
        } else {


            // 사용자에게 GET_ACCOUNTS 권한을 요구하는 다이얼로그를 보여준다.(주소록 권한 요청함)
            EasyPermissions.requestPermissions(
                    (Activity)this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }



    /*
     * 구글 플레이 서비스 업데이트 다이얼로그, 구글 계정 선택 다이얼로그, 인증 다이얼로그에서 되돌아올때 호출된다.
     */

    @Override
    protected void onActivityResult(
            int requestCode,  // onActivityResult가 호출되었을 때 요청 코드로 요청을 구분
            int resultCode,   // 요청에 대한 결과 코드
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {

            case REQUEST_GOOGLE_PLAY_SERVICES:

                if (resultCode != RESULT_OK) {

                    mStatusText.setText( " 앱을 실행시키려면 구글 플레이 서비스가 필요합니다."
                            + "구글 플레이 서비스를 설치 후 다시 실행하세요." );
                } else {

                    getResultsFromApi();
                }
                break;


            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;


            case REQUEST_AUTHORIZATION:

                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }


    /*
     * Android 6.0 (API 23) 이상에서 런타임 권한 요청시 결과를 리턴받음
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,  //requestPermissions(android.app.Activity, String, int, String[])에서 전달된 요청 코드
            @NonNull String[] permissions, // 요청한 퍼미션
            @NonNull int[] grantResults    // 퍼미션 처리 결과. PERMISSION_GRANTED 또는 PERMISSION_DENIED
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    /*
     * EasyPermissions 라이브러리를 사용하여 요청한 권한을 사용자가 승인한 경우 호출된다.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> requestPermissionList) {

        // 아무일도 하지 않음
    }


    /*
     * EasyPermissions 라이브러리를 사용하여 요청한 권한을 사용자가 거부한 경우 호출된다.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> requestPermissionList) {

        // 아무일도 하지 않음
    }


    /*
     * 안드로이드 디바이스가 인터넷 연결되어 있는지 확인한다. 연결되어 있다면 True 리턴, 아니면 False 리턴
     */
    private boolean isDeviceOnline() {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }


    /*
     * 캘린더 이름에 대응하는 캘린더 ID를 리턴
     */
    private String getCalendarID(String calendarTitle){

        String id = null;

        // Iterate through entries in calendar list
        String pageToken = null;
        do {
            CalendarList calendarList = null;
            try {
                calendarList = mService.calendarList().list().setPageToken(pageToken).execute();
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            }catch (IOException e) {
                e.printStackTrace();
            }
            List<CalendarListEntry> items = calendarList.getItems();


            for (CalendarListEntry calendarListEntry : items) {

                if ( calendarListEntry.getSummary().toString().equals(calendarTitle)) {

                    id = calendarListEntry.getId().toString();
                }
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

        return id;
    }


    /*
     * 비동기적으로 Google Calendar API 호출
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, String> {

        private Exception mLastError = null;
        private CalendarActivity mActivity;
        List<String> eventStrings = new ArrayList<String>();


        public MakeRequestTask(CalendarActivity activity, GoogleAccountCredential credential) {

            mActivity = activity;

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            mService = new com.google.api.services.calendar.Calendar
                    .Builder(transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }


        @Override
        protected void onPreExecute() {
            // mStatusText.setText("");
            mProgress.show();

        }


        /*
         * 백그라운드에서 Google Calendar API 호출 처리
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                if (mID == 3) {

                    return getEvent();
                }


            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }

            return null;
        }


        /*
         * CalendarTitle 이름의 캘린더에서 10개의 이벤트를 가져와 리턴
         */
        private String getEvent() throws IOException {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);

            DateTime now = new DateTime(System.currentTimeMillis());


            String format_time1 = format.format(System.currentTimeMillis());

            Events events = mService.events().list("primary")
                    .setMaxResults(1)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();



            for (Event event : items) {

                DateTime start = event.getStart().getDateTime();
                if (start == null) {

                    // 모든 이벤트가 시작 시간을 갖고 있지는 않다. 그런 경우 시작 날짜만 사용
                    start = event.getStart().getDate();
                }

                Log.d("calendar", "위치 : "+event.getLocation());
                Log.d("calendar : ", "시간1 : "+format_time1 + "시간2 : " + start);
                if(start.toString().contains(format_time1)){
                        if(event.getLocation() == null){
                            eventStrings.add(String.format("%s/%s/%s", event.getSummary()+"있습니다."," ", start));

                        }else{
                            eventStrings.add(String.format("%s/%s/%s", event.getSummary()+"있습니다.", event.getLocation(), start));
                        }
                    }else{
                    eventStrings.add("");
                }



            }


            return eventStrings.size() + "개의 데이터를 가져왔습니다.";
        }

        @Override
        protected void onPostExecute(String output) {

            mProgress.hide();


            if ( mID == 3 )   {
                AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance().getCredentialsProvider();
                AWSConfiguration configuration = AWSMobileClient.getInstance().getConfiguration();

                AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);
                mfirebaseAuth = FirebaseAuth.getInstance();
                firebaseUser = mfirebaseAuth.getCurrentUser();
                String email_c= firebaseUser.getEmail();
                String name_c = firebaseUser.getDisplayName();
                dynamoDBMapper = DynamoDBMapper.builder()
                        .dynamoDBClient(dynamoDBClient)
                        .awsConfiguration(configuration)
                        .build();
                readNews(email_c, name_c, TextUtils.join("\n\n", eventStrings));

                Log.d("Result", TextUtils.join("\n\n", eventStrings));
                try{
                    Thread.sleep(5000);
                    finish();
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }


        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            CalendarActivity.REQUEST_AUTHORIZATION);
                } else {
                    Log.d("calendar","MakeRequestTask The following error occurred:\n" + mLastError.getMessage());
                }
            } else {
                mStatusText.setText("요청 취소됨.");
            }
        }
    }

    public void readNews(final String c_email, final String c_name, final String todayis) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                NewsDO newsItem = dynamoDBMapper.load(
                        CalendarActivity.NewsDO.class,
                        c_email,
                        c_name);
                if(newsItem == null){
                        Intent intent = new Intent(getApplicationContext(), InitActivity.class);
                        intent.putExtra("todayis", todayis);
                        startActivity(intent);

                        }else{
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("todayis", todayis);
                        startActivity(intent);

                    }


            }
        }).start();

    }

}
