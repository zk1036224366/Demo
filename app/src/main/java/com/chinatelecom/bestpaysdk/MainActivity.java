package com.chinatelecom.bestpaysdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bestpay.app.PaymentTask;
import com.chinatelecom.bestpaysdk.util.ParamsUtil;
import com.chinatelecom.bestpaysdk.util.StreamUtil;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    @SuppressLint("HandlerLeak")
    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISSMISS_PROGRESSBAR:
                    mProgressbar.setVisibility(View.GONE);
                    break;
                case GOTO_PAY:
                    gotoPay();
                    break;
            }
        }
    };

    /**
     * 商户key,为防止泄漏,建议通过服务端获取,不要放在本地
     */
    private final static String KEY = "344C4FB521F5A52EA28FB7FC79AEA889478D4343E4548C02";
    private final static String riskControlInfo="Json字符串，2016.8.30（不包含）以后新商户必填)\n" +
            "翼支付风控组提供（在商户入网的时候会给出）";
    private final static int DISSMISS_PROGRESSBAR = 1;
    private final static int GOTO_PAY = 2;

    private EditText mEtMerchantId;
    private EditText mEtMerchantPwd;
    private EditText mEtAmt;
    private EditText mEtAccount;
    private EditText mEtBusiness;
    private Model mModel;
    private ProgressBar mProgressbar;
    private PaymentTask mPaymentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEtMerchantId = (EditText) findViewById(R.id.et_merchant_id);
        mEtMerchantPwd = (EditText) findViewById(R.id.et_merchant_pwd);
        mEtAmt = (EditText) findViewById(R.id.et_amt);
        mEtAccount = (EditText) findViewById(R.id.et_account);
        mEtBusiness = (EditText) findViewById(R.id.et_business);
        mProgressbar = (ProgressBar) findViewById(R.id.progressbar);

        mPaymentTask = new PaymentTask(this);

        findViewById(R.id.btn_pay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                gotoPay();
            }
        });
    }

    private void initData() {
        long time = System.currentTimeMillis();
        mModel = new Model();
        mModel.setMERCHANTID(mEtMerchantId.getText().toString());
        mModel.setMERCHANTPWD(mEtMerchantPwd.getText().toString());
        //下单单位为分
        mModel.setORDERAMOUNT(yuan2Fen(mEtAmt.getText().toString()));
        mModel.setACCOUNTID(mEtAccount.getText().toString());
        mModel.setBUSITYPE(mEtBusiness.getText().toString());
        mModel.setORDERSEQ(String.valueOf(time));
        mModel.setORDERTIME(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(time)));

        mModel.setCUSTOMERID("12345678901");
        mModel.setPRODUCTAMOUNT(mEtAmt.getText().toString());
        mModel.setPRODUCTDESC("流程测试");
        mModel.setATTACHAMOUNT("0");
        mModel.setCURTYPE("RMB");
        mModel.setBACKMERCHANTURL("http://127.0.0.1:8040/wapBgNotice.action");
        mModel.setPRODUCTID(mEtBusiness.getText().toString());
        mModel.setUSERIP("192.168.11.130");
        mModel.setDIVDETAILS("");
        mModel.setORDERREQTRANSEQ(time + "00001");
        mModel.setSERVICE("mobile.security.pay");
        mModel.setSIGNTYPE("MD5");
        mModel.setSUBJECT("商品测试");
        mModel.setSWTICHACC("true");
        mModel.setOTHERFLOW("01");
    }

    private String yuan2Fen(String yuan) {
        BigDecimal decimal = new BigDecimal(yuan);
        BigDecimal decimalMultiply = new BigDecimal("100");
        BigDecimal minuteAmt = decimal.multiply(decimalMultiply).setScale(0);
        return minuteAmt.toString();
    }

    /**
     * 下单
     */
    private void order() {
        mProgressbar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL orderUrl = new URL("https://webpaywg.bestpay.com.cn/order.action");
                    HttpURLConnection urlConnection = (HttpURLConnection) orderUrl.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Charset", "UTF-8");
                    //请求入参
                    urlConnection.getOutputStream().write(ParamsUtil.buildOrderParams(mModel, KEY, riskControlInfo).getBytes());
                    InputStream is = urlConnection.getInputStream();
                    String responseCode = StreamUtil.stream2String(is).split("&")[0];

                    mHandle.sendEmptyMessage(DISSMISS_PROGRESSBAR);
                    if (TextUtils.equals(responseCode, "00")) {
                        mHandle.sendEmptyMessage(GOTO_PAY);
                    } else {
                        Toast.makeText(MainActivity.this, "下单失败", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 调用sdk支付
     */
    private void gotoPay() {
        //调用sdk支付，订单金额单位为元
String s="SERVICE=mobile.security.pay&MERCHANTID=01310101110472528&MERCHANTPWD=797643&SUBMERCHANTID=&BACKMERCHANTURL=http://180.166.2.4/sdop_eai/sdop/bestpay/acceptBestPay&ORDERSEQ=1069938126360222019&ORDERREQTRANSEQ=17061210411069938126360222019&ORDERTIME=20170612104132&ORDERVALIDITYTIME=&CURTYPE=RMB&ORDERAMOUNT=0.01&SUBJECT=上海电信智能家居商城&PRODUCTID=04&PRODUCTDESC=智饮水&CUSTOMERID=17721038498&SWTICHACC=false&SIGN=49FCDBD8B29D3EDD7BC388EAC7735FAC&BUSITYPE=04&PRODUCTAMOUNT=0.01&ATTACHAMOUNT=0.00&ATTACH=&SIGNTYPE=MD5";
        mPaymentTask.pay(s);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;

        String result = data.getStringExtra("result");
        result = TextUtils.isEmpty(result) ? "" : result;
        String showStr = String.format("resultCode:%s;result：%s", resultCode, result);
        Toast.makeText(MainActivity.this, showStr, Toast.LENGTH_SHORT).show();
    }
}
