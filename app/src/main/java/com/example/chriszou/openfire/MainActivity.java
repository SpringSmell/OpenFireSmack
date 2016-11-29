package com.example.chriszou.openfire;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Handler;
import android.provider.SyncStateContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;

import javax.net.SocketFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private XMPPTCPConnection connection;
    private final String HOST = "@196server-2";

    private TextView resultTv;
    private EditText ipEt;
    private EditText portEt;
    private EditText inputTxt;
    private EditText accountEt;
    private EditText passwordEt;
    private Button startConnectionBtn;
    private Button sendBtn;

    private ImageView imgIv;

    private EditText userNameEt;

    private String ipStr = "dev.cq196.cn";
    private int port = 5222;

    private Handler mHandler = new Handler () {

        @Override
        public void handleMessage ( android.os.Message msg ) {

            switch ( msg.what ) {
                case 1:
                    Toast.makeText ( getApplicationContext (), msg.obj + "", Toast.LENGTH_SHORT )
                         .show ();
                    resultTv.setText ( msg.obj + "" );
                    break;
            }
            super.handleMessage ( msg );
        }
    };

    @Override
    protected void onCreate ( Bundle savedInstanceState ) {

        super.onCreate ( savedInstanceState );
        setContentView ( R.layout.activity_main );
        initView ();
    }

    private void initView () {

        connection = getConnection ( ipStr, port );
        resultTv = ( TextView ) findViewById ( R.id.resultTv );
        ipEt = ( EditText ) findViewById ( R.id.ip );
        portEt = ( EditText ) findViewById ( R.id.port );
        startConnectionBtn = ( Button ) findViewById ( R.id.startConnectionBtn );
        inputTxt = ( EditText ) findViewById ( R.id.inputMsg );
        sendBtn = ( Button ) findViewById ( R.id.sendBtn );
        imgIv = ( ImageView ) findViewById ( R.id.imgIv );
        userNameEt = ( EditText ) findViewById ( R.id.userName );
        accountEt = ( EditText ) findViewById ( R.id.accountEt );
        passwordEt = ( EditText ) findViewById ( R.id.passwordEt );

        ipEt.setText ( ipStr );
        portEt.setText ( port + "" );
        sendBtn.setOnClickListener ( this );
        startConnectionBtn.setOnClickListener ( this );
        findViewById ( R.id.disconnectBtn ).setOnClickListener ( this );
        findViewById ( R.id.loadImg ).setOnClickListener ( this );
        findViewById ( R.id.loginBtn ).setOnClickListener ( this );
    }

    @Override public void onClick ( View view ) {

        switch ( view.getId () ) {
            case R.id.sendBtn:
                sendMsg ();
                break;

            case R.id.loginBtn:
                login ();
                break;
            case R.id.startConnectionBtn:
                startConnection ();
                break;

            case R.id.disconnectBtn:
                disconnect ();
                break;
            case R.id.loadImg:
                loadImg ();
                break;
        }
    }

    private void startConnection () {

        try {
            ipStr = ipEt.getText ().toString ();
            port = Integer.parseInt ( this.portEt.getText ().toString () );
            connection = getConnection ( ipStr, port );
        } catch ( Exception ex ) {
            ex.printStackTrace ();
            sendMessage ( ex.getMessage (), "连接" );
        }
    }

    private void login () {

        final String account  = accountEt.getText ().toString ();
        final String password = passwordEt.getText ().toString ();
        new Thread ( new Runnable () {

            @Override
            public void run () {

                try {
//                    if(connection.isConnected ()){
//                        connection.connect ();
//                    }
//                    SASLAuthentication.unBlacklistSASLMechanism ( "PLAIN" );
//                    SASLAuthentication.blacklistSASLMechanism ( "DIGEST-MD5" );
                    connection.connect ();
                    connection.login ( account, password );
                    Presence presence = new Presence ( Presence.Type.available );
                    presence.setStatus ( "我登陆了" );
                    connection.sendStanza ( presence );
                    ChatManager chatmanager = ChatManager.getInstanceFor ( connection );
                    chatmanager.addChatListener ( new ChatManagerListener () {

                        @Override
                        public void chatCreated ( Chat chat, boolean createdLocally ) {

                            chat.addMessageListener ( new ChatMessageListener () {

                                @Override
                                public void processMessage ( Chat chat, Message message ) {

                                    String content = message.getBody ();
                                    if ( content != null ) {
                                        Log.e ( "TAG",
                                                "from:" + message.getFrom () + " to:" +
                                                        message.getTo () + " message:" +
                                                        message.getBody () );
                                        sendMessage ( message.getBody (),
                                                      message.getFrom () );
                                    }
                                }
                            } );
                        }
                    } );
                } catch ( SmackException e ) {
                    e.printStackTrace ();
                    sendMessage ( e.getMessage (), "登陆" );
                } catch ( IOException e ) {
                    e.printStackTrace ();
                    sendMessage ( e.getMessage (), "登陆" );
                } catch ( XMPPException e ) {
                    sendMessage ( e.getMessage (), "登陆" );
                    e.printStackTrace ();
                }

            }
        } ).start ();
    }

    private void sendMsg () {

        if ( TextUtils.isEmpty ( inputTxt.getText ().toString () ) ) {
            String hint = "消息不能为空";
            resultTv.setText ( hint );
            Toast.makeText ( this, hint, Toast.LENGTH_SHORT ).show ();
            return;
        }
        String msgStr = inputTxt.getText ().toString ();

        try {
            ChatManager chatManager = ChatManager.getInstanceFor ( connection );
            Chat chat = chatManager.createChat ( userNameEt.getText ().toString () + HOST,
                                                 new ChatMessageListener () {

                                                     @Override
                                                     public void processMessage ( Chat chat, Message message ) {

                                                         String content = message.getBody ();
                                                         if ( content != null ) {
                                                             Log.e ( "TAG",
                                                                     "from:" + message.getFrom () +
                                                                             " to:" +
                                                                             message.getTo () +
                                                                             " message:" +
                                                                             message.getBody () );
                                                             sendMessage ( message.getBody (),
                                                                           message.getFrom () );
                                                         }
                                                     }
                                                 } );
//            Message msg = new Message ();
//            msg.setBody ( msgStr );
//            msg.setFrom ( "chris" );
//            msg.setTo ( accountEt.getText ().toString ()+HOST );
            chat.sendMessage ( msgStr );
        } catch ( SmackException.NotConnectedException e ) {
            e.printStackTrace ();
            sendMessage ( e.getMessage (), "发送消息" );
        } catch ( NullPointerException e ) {
            e.printStackTrace ();
            sendMessage ( e.getMessage (), "发送消息" );
        }
    }

    private void loadImg () {

        VCardManager vCardManager = VCardManager.getInstanceFor ( connection );
        try {
            VCard                vCard = vCardManager.loadVCard ( "admin" );
            ByteArrayInputStream bais  = new ByteArrayInputStream ( vCard.getAvatar () );
//                    imgIv.setImageDrawable ( Drawable.createFromStream ( bais, "image" ) );
            imgIv.setImageBitmap ( BitmapFactory.decodeStream ( bais ) );
        } catch ( SmackException.NoResponseException e ) {
            e.printStackTrace ();
            sendMessage ( e.getMessage (), "加载图片" );
        } catch ( XMPPException.XMPPErrorException e ) {
            e.printStackTrace ();
            sendMessage ( e.getMessage (), "加载图片" );
        } catch ( SmackException.NotConnectedException e ) {
            e.printStackTrace ();
            sendMessage ( e.getMessage (), "加载图片" );
        } catch ( IllegalArgumentException e ) {
            e.printStackTrace ();
            sendMessage ( e.getMessage (), "加载图片" );
        }
    }


    private XMPPTCPConnection getConnection ( String server, int port ) {

        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder ();
        builder.setSocketFactory ( SocketFactory.getDefault () );
        builder.setServiceName ( server );
        builder.setHost ( server );
        builder.setPort ( port );
        builder.setCompressionEnabled ( false );
        builder.setDebuggerEnabled ( true );
        builder.setSendPresence ( true );
        builder.setSecurityMode ( ConnectionConfiguration.SecurityMode.disabled );
        XMPPTCPConnection connection = new XMPPTCPConnection ( builder.build () );

        return connection;
    }

    private void disconnect () {

        if ( connection != null ) {
            connection.disconnect ();
            connection = null;
        }
    }

    private void checkRoster () {

        Presence presence = new Presence ( Presence.Type.unavailable );
        presence.setStatus ( "Gone fishing" );
        try {
            getConnection ( ipStr, port ).sendPacket ( presence );
        } catch ( SmackException.NotConnectedException e ) {
            e.printStackTrace ();
        }
    }

    private void sendMessage ( String content, String from ) {

        android.os.Message message1 = android.os.Message
                .obtain ();
        message1.what = 1;
        message1.obj = "收到消息：" + content +
                " 来自:" + from;
        mHandler.sendMessage ( message1 );
    }


//    @Override protected void onDestroy () {
//
//        disconnect ();
//    }
}
