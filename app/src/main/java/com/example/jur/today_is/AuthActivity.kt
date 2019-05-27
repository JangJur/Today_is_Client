package com.example.jur.today_is

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast

import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.concurrent.thread

class AuthActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener  {

    private var mfirebaseAuth: FirebaseAuth? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private val region = Region.getRegion(Regions.AP_NORTHEAST_2)
    var dynamoDBMapper: DynamoDBMapper? = null
    lateinit var locationManager: LocationManager
    private var REQUEST_CODE_LOCATION = 2
    lateinit var geocoder: Geocoder
    var f_style : String? = null
    var s_style : String? = null
    var location : String? = null



    //데이터베이스의 모델링
    @DynamoDBTable(tableName = "auth")
    class authDO {

        @DynamoDBHashKey(attributeName = "email")
        @DynamoDBAttribute(attributeName = "email")
        var email: String? = null

        @DynamoDBRangeKey(attributeName = "name")
        @DynamoDBAttribute(attributeName = "name")
        var name: String? = null

        var style1: String? = null
        var style2: String? = null


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val signInButton :ImageButton
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        signInButton = findViewById<View>(R.id.SignButton) as ImageButton
        mfirebaseAuth = FirebaseAuth.getInstance()
        //aws와 연결
        AWSMobileClient.getInstance().initialize(this) { Log.d("YourMainActivity", "AWSMobileClient is instantiated and you are connected to AWS!") }.execute()

        val credentialsProvider = AWSMobileClient.getInstance().credentialsProvider
        val configuration = AWSMobileClient.getInstance().configuration

        val dynamoDBClient = AmazonDynamoDBClient(credentialsProvider)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), this.REQUEST_CODE_LOCATION)

        }


        //다이나모 디비 mapper형식을 사용
        dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(configuration)
                .build()

        //파이어베이스 이용 구글 계정과 로그인
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        //구글 계정 초기화
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build()

        //로그인 버튼 클릭시 리스너
        signInButton.setOnClickListener {


            val intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
            startActivityForResult(intent, RC_SIGN_IN)
        }


    }

    //로그인 작업 시 결과 처리를 위한 작업
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            val account = result.signInAccount
            if (result.isSuccess) {
                firebaseWithGoogle(account!!)
            } else {
                Toast.makeText(this, "인증에 실패하였습니다", Toast.LENGTH_LONG).show()
            }

        } else {
            Toast.makeText(this, "인증에 실패하였습니다2", Toast.LENGTH_LONG).show()
        }
    }

    //파이어베이스와 구글이 연동하였을 때
    private fun firebaseWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val authResultTask = mfirebaseAuth!!.signInWithCredential(credential)
        authResultTask.addOnCompleteListener(this) { task ->
            if (!task.isSuccessful) {
                Toast.makeText(this@AuthActivity, "인증 실패", Toast.LENGTH_LONG).show()

            } else {
                Toast.makeText(this@AuthActivity, "인증 성공", Toast.LENGTH_LONG).show()

                val name = authResultTask.result!!.user.displayName
                val email = authResultTask.result!!.user.email

                readNews(email, name)

                if(f_style == null && s_style == null){
                    val intent : Intent = Intent(applicationContext, CalendarActivity::class.java)
                    startActivity(intent)
                    finish()
                }else{
                    val intent : Intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

    }

    //해당 데이터가 존재하는지 안하는지 판별하는 용도
    fun readNews(email: String?, name: String?) {
        thread(start = true) {
            val newsItem = dynamoDBMapper?.load(
                    AuthActivity.authDO::class.java,
                    email,
                    name)
        }.join()
    }

    //인증 실패시
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Toast.makeText(this, "인증에 실패하였습니다3", Toast.LENGTH_LONG).show()
    }



    companion object {
        val RC_SIGN_IN = 10
    }

}
