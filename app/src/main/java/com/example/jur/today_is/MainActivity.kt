package com.example.jur.today_is

import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.github.bassaer.chatmessageview.model.ChatUser
import com.github.bassaer.chatmessageview.model.Message
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.json.responseJson
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import android.support.v7.app.AlertDialog
import android.widget.Toast
import android.support.v7.widget.Toolbar
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.google.firebase.auth.FirebaseAuth
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*


import android.content.Context
import android.content.Intent
import android.location.*
import android.net.Uri
import android.os.*
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.support.multidex.MultiDex
import android.view.*
import android.widget.Button
import com.google.android.gms.location.*
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Exception
import java.util.ArrayList
import java.util.Locale
import kotlin.concurrent.thread


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, EasyPermissions.PermissionCallbacks{


    //다이나모 디비 Mapper형식으로의 처리
    var ddbMapper: DynamoDBMapper? = null

    //다이나모 디비 데이터 모델
    @DynamoDBTable(tableName = "auth")
    class authDO {

        @DynamoDBHashKey(attributeName = "email")
        @DynamoDBAttribute(attributeName = "email")
        var email: String? = null

        @DynamoDBRangeKey(attributeName = "name")
        @DynamoDBAttribute(attributeName = "name")
        var name: String? = null

        var style1: String? =null

        var style2: String? = null

        var sex : String? =null

        var locate : String? = null

        var today : String ? =null

    }

    //음성출력의 초기화
    private var tts: TextToSpeech? = null
    lateinit var locationManager: LocationManager
    lateinit var fusedLocationClient : FusedLocationProviderClient
    var REQUEST_CODE_LOCATION = 2
    var geocoder: Geocoder = Geocoder(this)
    lateinit var first_style: String
    lateinit var second_style: String
    lateinit var sex_get: String
    lateinit var locationCallback : LocationCallback
    lateinit var locationRequest : LocationRequest
    lateinit var context : Context
    lateinit var test : Button


    //그림 출력을 위한 초기화작업
    internal lateinit var bitmap: Bitmap
    internal lateinit var all : Bitmap


    //구글 캘린더 사용


    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //텍스트 투 스피치 사용
        tts = TextToSpeech(this, this)
        //툴바에 메뉴 아이템 추가를 위한 작업
        val myToolbar : Toolbar? = findViewById<View>(R.id.mytool) as Toolbar
        setSupportActionBar(myToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_keyboard_voice_white_24dp)
        supportActionBar?.setTitle("")




        initLocation()// 위치 정보 및 초기값 데이터베이스에 저장하는 함수 호출


        //-----------------------------------------------------AWS데이터베이스

        //aws와 연동 위한 초기화
        AWSMobileClient.getInstance().initialize(this) {
            Log.d("MainActivity", "AWSMobileClient is initialized")
        }.execute()
        //다이나모 디비 사용을 위한 ddbMapper사용
        val client = AmazonDynamoDBClient(AWSMobileClient.getInstance().credentialsProvider)
        ddbMapper = DynamoDBMapper.builder()
                .dynamoDBClient(client)
                .awsConfiguration(AWSMobileClient.getInstance().configuration)
                .build()

        //-----------------------------------------------------firebase인증 및 로그인계정 확인

        //파이어베이스 현재 인증 정보 가져오기
        var mfirebaseAuth = FirebaseAuth.getInstance().currentUser
        var auth_name = mfirebaseAuth?.displayName
        var email = mfirebaseAuth?.email
        var icon = mfirebaseAuth?.photoUrl.toString()
        var iconurl = URL(icon)






        //로그인 정보가 없을 시 로그인 화면으로 옮기기(거의 실행되지 않음)
        if (mfirebaseAuth == null) {
            startActivity(Intent(this@MainActivity, AuthActivity::class.java))
            finish()
            return
        }else{

        }
        //-------------------------------------------------------------------여기서부터 채팅뷰 사용시작 및 처리시작

        //음성 인식 사용 권한
        val permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
        MultiDex.install(this)



        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
        //fuelfillment사용을 위한 작업
        FuelManager.instance.baseHeaders = mapOf(
                "Authorization" to "Bearer ${BuildConfig.ACCESS_TOKEN}"
        )

        FuelManager.instance.basePath =
                "https://api.dialogflow.com/v1/"

        FuelManager.instance.baseParams = listOf(
                "v" to "20190403",                  // latest protocol
                "sessionId" to UUID.randomUUID(),   // random ID
                "lang" to "ko"                      // Korea language
        )

        GetImage(iconurl)

        val human = ChatUser(
                1,
                "나",
                bitmap
        )
        val agent = ChatUser(
                2,
                "오늘은",
                BitmapFactory.decodeResource(resources,
                        R.mipmap.ic_launcher_today)
        )

        my_chat_view.setRightBubbleColor(ContextCompat.getColor(this, R.color.lightBlue500));
        Log.d("read" , email + auth_name)

        var welcome = "안녕하세요."+auth_name+"님\n오늘은 입니다.\n처음 사용하신다면 사용법 혹은 이거 어떻게 써 라고 물어보세요 상세히 알려드립니다.."

        //메세지뷰 처음 입장시 출력
        my_chat_view.send(Message.Builder()
                .setUser(agent)
                .setText(welcome)
                .build()
        )


        //보내기버튼 눌렀을때
        my_chat_view.setOnClickSendButtonListener(
                View.OnClickListener {
                    //new message
                    //Set to chat view
                    context = this@MainActivity
                    var calender : Intent? = null
                    calender = context.packageManager.getLaunchIntentForPackage("com.google.android.calendar")

                    try{
                        if (calender == null){
                            install_calender()
                        }else{
                            my_chat_view.send(Message.Builder()
                                    .setRight(true)
                                    .setUser(human)
                                    .setText(my_chat_view.inputText)
                                    .build()
                            )
                            Fuel.get("/query",
                                    listOf("query" to my_chat_view.inputText.toString() +"#"+ email + "#" + auth_name))
                                    .responseJson { _, _, result ->

                                        val reply = result.get().obj()
                                                .getJSONObject("result")
                                                .getJSONObject("fulfillment")
                                                .getString("speech")

                                        if (reply.contains("http")) {
                                            if (reply.contains("#")) {

                                                var urlset = reply.split("#").toTypedArray()
                                                if(urlset.size == 3){
                                                    my_chat_view.send(Message.Builder()
                                                            .setUser(agent)
                                                            .hideIcon(true)
                                                            .setText(urlset[0])
                                                            .build()
                                                    )
                                                    speakOut(urlset[0]);
                                                    Thread.sleep(6000);
                                                    try {
                                                        var url = URL(urlset[1])
                                                        GetImage_all(url)
                                                        my_chat_view.send(Message.Builder()
                                                                .setUser(agent)
                                                                .hideIcon(true)
                                                                .setPicture(all)
                                                                .setType(Message.Type.PICTURE)
                                                                .build()
                                                        )
                                                    } catch (e: IOException) {
                                                        e.printStackTrace()
                                                    }
                                                    my_chat_view.send(Message.Builder()
                                                            .setUser(agent)
                                                            .hideIcon(true)
                                                            .setText(urlset[2])
                                                            .build()
                                                    )
                                                    speakOut(urlset[2])
                                                }else if(urlset.size == 4){
                                                    my_chat_view.send(Message.Builder()
                                                            .setUser(agent)
                                                            .hideIcon(true)
                                                            .setText(urlset[0])
                                                            .build()
                                                    )
                                                    speakOut(urlset[0])
                                                    Thread.sleep(6000);
                                                    for(i in 1..2){
                                                        try {
                                                            var url = URL(urlset[i])
                                                            GetImage_all(url)
                                                            my_chat_view.send(Message.Builder()
                                                                    .setUser(agent)
                                                                    .hideIcon(true)
                                                                    .setPicture(all)
                                                                    .setType(Message.Type.PICTURE)
                                                                    .build()
                                                            )
                                                        } catch (e: IOException) {
                                                            e.printStackTrace()
                                                        }
                                                    }

                                                    my_chat_view.send(Message.Builder()
                                                            .setUser(agent)
                                                            .hideIcon(true)
                                                            .setText(urlset[3])
                                                            .build()
                                                    )
                                                    speakOut(urlset[3])
                                                }else{
                                                    my_chat_view.send(Message.Builder()
                                                            .setUser(agent)
                                                            .hideIcon(true)
                                                            .setText(urlset[0])
                                                            .build()
                                                    )

                                                    speakOut(urlset[0])

                                                    try {
                                                        var url = URL(urlset[urlset.size - 3])
                                                        GetImage_all(url)
                                                        my_chat_view.send(Message.Builder()
                                                                .setUser(agent)
                                                                .hideIcon(true)
                                                                .setPicture(all)
                                                                .setType(Message.Type.PICTURE)
                                                                .build()
                                                        )
                                                        my_chat_view.setOnBubbleClickListener(object : Message.OnBubbleClickListener{
                                                            override fun onClick(message: Message){
                                                                musinsa(urlset[urlset.size-2])                                               }
                                                        })
                                                    } catch (e: IOException) {
                                                        e.printStackTrace()
                                                    }
                                                    for (i in 1..urlset.size - 4) {
                                                        try {
                                                            var url = URL(urlset[i])
                                                            GetImage(url)
                                                            my_chat_view.send(Message.Builder()
                                                                    .setUser(agent)
                                                                    .hideIcon(true)
                                                                    .setPicture(bitmap)
                                                                    .setType(Message.Type.PICTURE)
                                                                    .build()
                                                            )

                                                        } catch (e: IOException) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                    my_chat_view.send(Message.Builder()
                                                            .setUser(agent)
                                                            .hideIcon(true)
                                                            .setText(urlset[urlset.size - 1])
                                                            .build()
                                                    )
                                                    Thread.sleep(5000)
                                                    speakOut(urlset[urlset.size - 1])
                                                }

                                            } else {
                                                try {
                                                    var url = URL(reply)
                                                    GetImage(url)
                                                    my_chat_view.send(Message.Builder()
                                                            .setUser(agent)
                                                            .hideIcon(true)
                                                            .setPicture(bitmap)
                                                            .setType(Message.Type.PICTURE)
                                                            .build()
                                                    )

                                                } catch (e: IOException) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        } else {
                                            if (reply.contains("#")) {
                                                var usrlet = reply.split("#").toTypedArray()
                                                for (i in 0..usrlet.size - 1) {
                                                    my_chat_view.send(Message.Builder()
                                                            .setUser(agent)
                                                            .setText(usrlet[i])
                                                            .build()
                                                    )

                                                }
                                            } else {
                                                my_chat_view.send(Message.Builder()
                                                        .setUser(agent)
                                                        .setText(reply)
                                                        .build()
                                                )
                                                speakOut(reply)

                                            }
                                        }
                                    }
                            //Reset edit text
                            my_chat_view.inputText = ""
                        }
                    }catch(e : Exception){
                        e.printStackTrace()
                    }

                }
        )


    }
    //음성인식을 했을때 처리
    protected fun makeRequest() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101)
    }
    //메뉴 옵션을 생성하고 적용
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //return super.onCreateOptionsMenu(menu)
        val menuInflater : MenuInflater = menuInflater
        menuInflater.inflate(R.menu.menu, menu)
        return true;
    }
    // 메뉴 옵션중 하나를 클릭했을 때 작업
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        if(item?.itemId == R.id.Logout){
            logout()
            return true
        }
        else if(item?.itemId == R.id.maker){
            Toast.makeText(this, "제작자 김가네 : 김영수 나두원 박재형 이민성 장주영", Toast.LENGTH_SHORT).show()
        }else if(item?.itemId == android.R.id.home){
            buttonClicked()
        }else{
            return super.onOptionsItemSelected(item)
        }
        return true
    }


    //url로 부터 이미지를 가져오는 작업
    private fun GetImage(url : URL){
        var mThead = object : Thread(){
            override fun run() {
                try{
                    var conn = url.openConnection() as HttpURLConnection
                    conn.doInput = true;
                    conn.connect();

                    var `is` = conn.inputStream;
                    bitmap = BitmapFactory.decodeStream(`is`)
                }catch(e: MalformedURLException){
                    e.printStackTrace()
                }catch (e: IOException){
                    e.printStackTrace()
                }
            }
        }
        mThead.start()
        try{
            mThead.join();
        }catch(e: InterruptedException){
            e.printStackTrace()
        }
    }
    private fun GetImage_all(url : URL){
        var mThead = object : Thread(){
            override fun run() {
                try{
                    var conn = url.openConnection() as HttpURLConnection
                    conn.doInput = true;
                    conn.connect();

                    var `is` = conn.inputStream;
                    all = BitmapFactory.decodeStream(`is`)
                }catch(e: MalformedURLException){
                    e.printStackTrace()
                }catch (e: IOException){
                    e.printStackTrace()
                }
            }
        }
        mThead.start()
        try{
            mThead.join();
        }catch(e: InterruptedException){
            e.printStackTrace()
        }
    }
    // 음성인식 버튼을 눌렀을 때 이벤트 처리
    fun buttonClicked() {
        context = this@MainActivity
        var calender : Intent? = null
        calender = context.packageManager.getLaunchIntentForPackage("com.google.android.calendar")
        try {
            if (calender == null){
                install_calender()
            }else{
                var intent : Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

                if(intent.resolveActivity(packageManager) != null){
                    startActivityForResult(intent,10);

                }else{

                }
            }
        }catch(e : Exception){

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 10){
            if(resultCode == Activity.RESULT_OK && data != null){
                var result : ArrayList<String> = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                var mfirebaseAuth = FirebaseAuth.getInstance().currentUser
                var email = mfirebaseAuth?.email
                var auth_name = mfirebaseAuth?.displayName
                var talk : String = result.get(0).toString() + "#" + email + "#" + auth_name
                var icon = mfirebaseAuth?.photoUrl.toString()
                var iconurl = URL(icon)
                GetImage(iconurl)
                val human = ChatUser(
                        1,
                        "나",
                        bitmap
                )
                val agent = ChatUser(
                        2,
                        "오늘은",
                        BitmapFactory.decodeResource(resources,
                                R.mipmap.ic_launcher_today)
                )
                my_chat_view.send(Message.Builder()
                        .setRight(true)
                        .setUser(human)
                        .setText(result.get(0).toString())
                        .build()
                )
                Fuel.get("/query",
                        listOf("query" to talk))
                        .responseJson { _, _, result ->

                            val reply = result.get().obj()
                                    .getJSONObject("result")
                                    .getJSONObject("fulfillment")
                                    .getString("speech")

                            if (reply.contains("http")) {
                                if (reply.contains("#")) {
                                    var urlset = reply.split("#").toTypedArray()
                                    if(urlset.size == 3){
                                        my_chat_view.send(Message.Builder()
                                                .setUser(agent)
                                                .hideIcon(true)
                                                .setText(urlset[0])
                                                .build()
                                        )
                                        speakOut(urlset[0]);
                                        Thread.sleep(6000);
                                        try {
                                            var url = URL(urlset[1])
                                            GetImage_all(url)
                                            my_chat_view.send(Message.Builder()
                                                    .setUser(agent)
                                                    .hideIcon(true)
                                                    .setPicture(all)
                                                    .setType(Message.Type.PICTURE)
                                                    .build()
                                            )
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                        my_chat_view.send(Message.Builder()
                                                .setUser(agent)
                                                .hideIcon(true)
                                                .setText(urlset[2])
                                                .build()
                                        )
                                        speakOut(urlset[2])
                                    }else if(urlset.size == 4){
                                        my_chat_view.send(Message.Builder()
                                                .setUser(agent)
                                                .hideIcon(true)
                                                .setText(urlset[0])
                                                .build()
                                        )
                                        speakOut(urlset[0])
                                        Thread.sleep(6000);
                                        for(i in 1..2){
                                            try {
                                                var url = URL(urlset[i])
                                                GetImage_all(url)
                                                my_chat_view.send(Message.Builder()
                                                        .setUser(agent)
                                                        .hideIcon(true)
                                                        .setPicture(all)
                                                        .setType(Message.Type.PICTURE)
                                                        .build()
                                                )
                                            } catch (e: IOException) {
                                                e.printStackTrace()
                                            }
                                        }

                                        my_chat_view.send(Message.Builder()
                                                .setUser(agent)
                                                .hideIcon(true)
                                                .setText(urlset[3])
                                                .build()
                                        )
                                        speakOut(urlset[3])
                                    }else{
                                        my_chat_view.send(Message.Builder()
                                                .setUser(agent)
                                                .hideIcon(true)
                                                .setText(urlset[0])
                                                .build()
                                        )

                                        speakOut(urlset[0])

                                        try {
                                            var url = URL(urlset[urlset.size - 3])
                                            GetImage_all(url)
                                            my_chat_view.send(Message.Builder()
                                                    .setUser(agent)
                                                    .hideIcon(true)
                                                    .setPicture(all)
                                                    .setType(Message.Type.PICTURE)
                                                    .build()
                                            )
                                            my_chat_view.setOnBubbleClickListener(object : Message.OnBubbleClickListener{
                                                override fun onClick(message: Message){
                                                    musinsa(urlset[urlset.size-2])                                               }
                                            })
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                        for (i in 1..urlset.size - 4) {
                                            try {
                                                var url = URL(urlset[i])
                                                GetImage(url)
                                                my_chat_view.send(Message.Builder()
                                                        .setUser(agent)
                                                        .hideIcon(true)
                                                        .setPicture(bitmap)
                                                        .setType(Message.Type.PICTURE)
                                                        .build()
                                                )

                                            } catch (e: IOException) {
                                                e.printStackTrace()
                                            }
                                        }
                                        my_chat_view.send(Message.Builder()
                                                .setUser(agent)
                                                .hideIcon(true)
                                                .setText(urlset[urlset.size - 1])
                                                .build()
                                        )
                                        Thread.sleep(5000)
                                        speakOut(urlset[urlset.size - 1])
                                    }

                                } else {
                                    try {
                                        var url = URL(reply)
                                        GetImage(url)
                                        my_chat_view.send(Message.Builder()
                                                .setUser(agent)
                                                .hideIcon(true)
                                                .setPicture(bitmap)
                                                .setType(Message.Type.PICTURE)
                                                .build()
                                        )

                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            } else {
                                if (reply.contains("#")) {
                                    var usrlet = reply.split("#").toTypedArray()
                                    for (i in 0..usrlet.size - 1) {
                                        my_chat_view.send(Message.Builder()
                                                .setUser(agent)
                                                .setText(usrlet[i])
                                                .build()
                                        )

                                    }
                                } else {
                                    my_chat_view.send(Message.Builder()
                                            .setUser(agent)
                                            .setText(reply)
                                            .build()
                                    )
                                    speakOut(reply)

                                }
                            }
                        }
            }

        }
    }
    fun musinsa (url : String){
        var buider : AlertDialog.Builder = AlertDialog.Builder(this)
        buider.setTitle("코디를 선택하셨습니다")
        buider.setMessage("해당 사이트로 이동하시겠습니까?")
        buider.setPositiveButton("예",
                DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                })
        buider.setNegativeButton("아니오",
                DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int ->
                    dialogInterface.dismiss()
                })
        buider.show()
    }

    //다이나모 디비의 데이터를 받아서 테이블을 생성하는 작업, 테이블의 모델에 따라 바뀌며 기본키, 정렬키가 바뀌지 않는 한 중복 입력은 안됨
    fun create(fb_email: String?, fb_name: String? , style1: String?, style2: String?, sex:String?, locate: String?, today: String?) {
        val Item = authDO().apply {
            this.email = fb_email
            this.name = fb_name
            this.style1 = style1
            this.style2 = style2
            this.sex = sex
            this.locate = locate
            this.today = today

        }
        Log.d("MainActivity", "item : " + Item.email)
        thread(start = true) {
            ddbMapper?.save(Item)
        }

    }

    fun updateNews(email : String?, name : String? ,locate : String?, today: String?){
        val Item = authDO().apply {
            this.email = email
            this.name = name
            this.locate = locate
            this.today = today
            // Do not set title - it will be removed from the item in DynamoDB
        }

        thread(start = true) {
            ddbMapper?.save(Item, DynamoDBMapperConfig(DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES))


        }.join()
    }

    override fun onInit(status : Int) {
        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            var result = tts!!.setLanguage(Locale.KOREAN)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            } else {
                Log.d("tts", "inited..")
                //음성 톤
                tts!!.setPitch(0.7f);
                //읽는 속도
                tts!!.setSpeechRate(1.2f);
            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }
    private fun speakOut(text : String?){
        val txt = text.toString()
        tts!!.speak(txt, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    public override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
    //다이나모 디비의 테이블 항목을 삭제하는 작업. 보통 데이터를 삭제하고 스타일을 다시 고르고 싶을때 사용된다.
    private fun delete(fb_email: String?, fb_name: String?){
        val item = authDO().apply{
            this.email = fb_email
            this.name = fb_name
        }
        thread(start = true) {
            ddbMapper?.delete(item)
        }
    }

    //메뉴 아이템 중 로그아웃 버튼을 눌렀을 때
    private fun logout() {
        var mfirebaseAuth = FirebaseAuth.getInstance()
        var user = mfirebaseAuth.currentUser
        var email = user?.email
        var name = user?.displayName
        delete(email, name)
        mfirebaseAuth.signOut()
        finish()

        Toast.makeText(this@MainActivity, "로그아웃 되었습니다.", Toast.LENGTH_LONG).show()
        startActivity(Intent(this@MainActivity, SplashActivity::class.java))
    }

    private fun initLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if(location == null) {
                        Log.e("main", "location get fail")
                    } else {
                        Log.d("main", "${location.latitude} , ${location.longitude}")
                    }
                }
                .addOnFailureListener {
                    Log.e("main", "location error is ${it.message}")
                    it.printStackTrace()
                }
        locationRequest = LocationRequest.create()
        locationRequest.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 60 * 1000
        }

        locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    for((i, location) in it.locations.withIndex()) {
                        Log.d("main", "#$i ${location.latitude} , ${location.longitude}")
                    }
                    geocoder = Geocoder(this@MainActivity, Locale.ENGLISH)

                    val addresses : List<Address>
                    addresses = geocoder.getFromLocation(it.lastLocation.latitude.toDouble(), it.lastLocation.longitude.toDouble(), 1)
                    val locate = addresses[0].getAddressLine(0).split(", ")
                    var location =  locate[2]
                    Log.d("Activity" , "location" + location)

                    val client = AmazonDynamoDBClient(AWSMobileClient.getInstance().credentialsProvider)
                    var mfirebaseAuth = FirebaseAuth.getInstance().currentUser
                    var name = mfirebaseAuth?.displayName
                    var email = mfirebaseAuth?.email

                    val intent : Intent = getIntent()
                    var first = intent.getStringExtra("first")
                    var second = intent.getStringExtra("second")
                    var sex = intent.getStringExtra("sex");
                    var today = intent.getStringExtra("todayis")
                    if(today == ""){
                        today = "없습니다."
                    }
                    Log.d("Activity", "todayis"+today)
                    ddbMapper = DynamoDBMapper.builder()
                            .dynamoDBClient(client)
                            .awsConfiguration(AWSMobileClient.getInstance().configuration)
                            .build()



                    if(first != null && second != null && sex != null){
                        create(email, name , first, second, sex, location, today)
                    }else{
                        updateNews(email, name, location, today)

                    }



                }
            }

        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

    }
    fun install_calender(){
        var buider : AlertDialog.Builder = AlertDialog.Builder(this)
        buider.setTitle("구글 캘린더 없음")
        buider.setMessage("원활한 사용을 위해서 구글 캘린더 설치페이지로 이동합니다.")
        buider.setPositiveButton("확인",
                DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.calendar")))
                })
        buider.show()
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
    }


}
