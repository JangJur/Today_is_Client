package com.example.jur.today_is;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class InitActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    Button btn;
    RadioGroup first, second, sex;
    ImageView classic, casual, spoty, street;
    AlertDialog.Builder builder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        //라디오 버튼의 리턴값을 확인하기 위한 라디오 그룹 초기화
        first = (RadioGroup)findViewById(R.id.f_selc);
        second = (RadioGroup)findViewById(R.id.s_selc);
        sex = (RadioGroup)findViewById(R.id.f_sex);


        //확인 눌렀을 때 초기화와 이벤트리스너
        btn = (Button)findViewById(R.id.submit);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = getIntent();
                    String todayis = intent.getStringExtra("todayis");
                    Log.d("main", todayis);
                    RadioButton f_rd = (RadioButton)findViewById(first.getCheckedRadioButtonId());
                    String f_style = f_rd.getText().toString();
                    RadioButton s_rd =  (RadioButton)findViewById(second.getCheckedRadioButtonId());
                    String s_style = s_rd.getText().toString();
                    RadioButton f_sex =  (RadioButton)findViewById(sex.getCheckedRadioButtonId());
                    String s_sex = f_sex.getText().toString();
                    String convert;

                    //같은 스타일을 선택 하였을 때 처리 및 각각의 스타일을 선택하였을 때 처리

                    if(f_style == s_style) {
                        Toast.makeText(InitActivity.this, "같은 스타일은 선택할 수 없습니다.", Toast.LENGTH_LONG).show();
                    }else if(f_style == null && s_style == null && s_sex == null){
                        Toast.makeText(InitActivity.this, "선택해주세요", Toast.LENGTH_LONG).show();
                    }else if(f_style == null){
                        Toast.makeText(InitActivity.this, "첫번째 스타일이 선택되지 않았습니다. 선택해주세요.", Toast.LENGTH_LONG).show();
                    }else if(s_style == null){
                        Toast.makeText(InitActivity.this, "두번째 스타일이 선택되지 않았습니다. 선택해주세요.", Toast.LENGTH_LONG).show();
                    }else if(s_sex == null){
                        Toast.makeText(InitActivity.this, "성별이 선택되지 않았습니다. 선택해주세요.", Toast.LENGTH_LONG).show();
                    }else if(f_style == null && s_style == null){
                        Toast.makeText(InitActivity.this, "스타일 두개가 선택되지 않았습니다. 선택해주세요.", Toast.LENGTH_LONG).show();
                    }else if(s_style == null && s_sex == null){
                        Toast.makeText(InitActivity.this, "두번째 스타일과 성별이 선택되지 않았습니다. 선택해주세요.", Toast.LENGTH_LONG).show();
                    }else if(f_style == null && s_sex == null){
                        Toast.makeText(InitActivity.this, "첫번째 스타일과 성별이 선택되지 않았습니다. 선택해주세요.", Toast.LENGTH_LONG).show();
                    }else{
                        intent = new Intent(getApplicationContext(), MainActivity.class);

                        Log.d("sex", ": " + s_sex);

                        if(s_sex.equals("남성")){
                            convert = "male";
                        }else{
                            convert = "female";
                        }

                        intent.putExtra("first", f_style);
                        intent.putExtra("second", s_style);
                        intent.putExtra("sex", convert);
                        intent.putExtra("todayis", todayis);
                        startActivity(intent);
                        finish();
                    }


                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                //라디오 버튼 초기화 및 선택한 라디오를 문자열로 초기화
            }
        });
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }
    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
