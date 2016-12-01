package com.example.chriszou.openfire;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Handler;
import android.provider.ContactsContract;
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
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.sasl.provided.SASLPlainMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

    private EditText registerAccount;
    private EditText registerPassword;

    private ImageView imgIv;

    private EditText userNameEt;

    private EditText chatRoomEt;

    private MultiUserChat muc;
    private EditText inputChatRoomEt;

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
        registerAccount = ( EditText ) findViewById ( R.id.registerAccountEt );
        registerPassword = ( EditText ) findViewById ( R.id.registerPasswordEt );
        chatRoomEt = ( EditText ) findViewById ( R.id.chatRoomNickname );
        inputChatRoomEt = ( EditText ) findViewById ( R.id.inputChatRoomMsg );

        ipEt.setText ( ipStr );
        portEt.setText ( port + "" );
        sendBtn.setOnClickListener ( this );
        startConnectionBtn.setOnClickListener ( this );
        findViewById ( R.id.disconnectBtn ).setOnClickListener ( this );
        findViewById ( R.id.loadImg ).setOnClickListener ( this );
        findViewById ( R.id.loginBtn ).setOnClickListener ( this );
        findViewById ( R.id.registerBtn ).setOnClickListener ( this );
        findViewById ( R.id.createChatRoom ).setOnClickListener ( this );
        findViewById ( R.id.joinChatRoom ).setOnClickListener ( this );
        findViewById ( R.id.sendChatRoomMsg ).setOnClickListener ( this );
        findViewById ( R.id.destroyChatRoom ).setOnClickListener ( this );
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
                loadHeadImg ();
                break;
            case R.id.registerBtn:
                new Thread () {

                    @Override public void run () {

                        try {
                            createUser ( registerAccount.getText ().toString (),
                                         registerPassword.getText ().toString () );
                        } catch ( SmackException.NotConnectedException e ) {
                            e.printStackTrace ();
                            sendMessage ( e.getMessage (), "注册" );
                        } catch ( XMPPException.XMPPErrorException e ) {
                            e.printStackTrace ();
                            sendMessage ( e.getMessage (), "注册" );
                        } catch ( SmackException.NoResponseException e ) {
                            e.printStackTrace ();
                            sendMessage ( e.getMessage (), "注册" );
                        } catch ( XMPPException e ) {
                            e.printStackTrace ();
                            sendMessage ( e.getMessage (), "注册" );
                        } catch ( IOException e ) {
                            e.printStackTrace ();
                            sendMessage ( e.getMessage (), "注册" );
                        } catch ( SmackException e ) {
                            e.printStackTrace ();
                            sendMessage ( e.getMessage (), "注册" );
                        }
                        super.run ();
                    }
                }.start ();
                break;
            case R.id.createChatRoom:
                try {
                    createGroupChat ( chatRoomEt.getText ().toString () );
                } catch ( SmackException e ) {
                    e.printStackTrace ();
                    sendMessage ( e.getMessage (), "创建聊天群" );
                } catch ( XMPPException.XMPPErrorException e ) {
                    e.printStackTrace ();
                    sendMessage ( e.getMessage (), "创建聊天群" );
                }
                break;
            case R.id.joinChatRoom:
                muc = joinChatRoom ( chatRoomEt.getText ().toString (),
                                     accountEt.getText ().toString (),
                                     "" );
                muc.addMessageListener (
                        new MessageListener () {

                            @Override public void processMessage ( Message message ) {

                                if ( !TextUtils.isEmpty ( message.getBody () ) ) {
                                    sendMessage ( message.getBody (), message.getFrom () );
                                }
                            }
                        } );
                break;
            case R.id.sendChatRoomMsg:
                try {
                    if ( muc != null ) {
                        muc.sendMessage ( "Hello client" );
                    } else {
                        sendMessage ( "加入房间失败", "发送聊天室消息" );
                    }
                } catch ( SmackException.NotConnectedException e ) {
                    sendMessage ( e.getMessage (), "发送聊天室消息" );
                    e.printStackTrace ();
                }
                break;
            case R.id.destroyChatRoom:
                try {
                    muc.destroy ( chatRoomEt.getText ().toString (),null );//需设置权限
                } catch ( SmackException.NoResponseException e ) {
                    e.printStackTrace ();
                } catch ( XMPPException.XMPPErrorException e ) {
                    e.printStackTrace ();
                } catch ( SmackException.NotConnectedException e ) {
                    e.printStackTrace ();
                }
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
//                    SASLAuthentication.unBlacklistSASLMechanism("PLAIN");
//                    SASLAuthentication.unBlacklistSASLMechanism("SCRAM-SHA-1");
//
//                    SASLAuthentication.blacklistSASLMechanism("PLAIN");
//                    SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
//                    SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");
//                    SASLPlainMechanism saslPlainMechanism=new SASLPlainMechanism ();
//                    saslPlainMechanism.
//                    SASLAuthentication.registerSASLMechanism (  );
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
            Message msg = new Message ();
            msg.setBody ( msgStr );
            msg.setSubject ( "Test call " );
            chat.sendMessage ( msg );
        } catch ( SmackException.NotConnectedException e ) {
            e.printStackTrace ();
            sendMessage ( e.getMessage (), "发送消息" );
        } catch ( NullPointerException e ) {
            e.printStackTrace ();
            sendMessage ( e.getMessage (), "发送消息" );
        }
    }


    private void createUser ( String account, String password ) throws SmackException, XMPPException, IOException {

        if ( !connection.isConnected () ) {
            connection.connect ();
        }
        AccountManager accountManager = AccountManager.getInstance ( connection );
        accountManager.createAccount ( account, password );
        accountManager.changePassword ( "1234" );
//        accountManager.deleteAccount ();
    }

    /**
     * 聊天并加入
     *
     * @param chatGroupNickname
     *
     * @throws SmackException
     * @throws XMPPException.XMPPErrorException
     */
    private void createGroupChat ( String chatGroupNickname ) throws SmackException, XMPPException.XMPPErrorException {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager
                .getInstanceFor ( connection );

        List< HostedRoom > hostedRooms = multiUserChatManager
                .getHostedRooms ( HOST.substring ( 1, HOST.length () ) );

        MultiUserChat muc = multiUserChatManager
                .getMultiUserChat (
                        chatGroupNickname + "@conference." + connection.getServiceName () );
        muc.create ( chatGroupNickname );
        Form form = muc.getConfigurationForm ();
        //克隆一个新表单
        Form submitForm = form.createAnswerForm ();
        // 向要提交的表单添加默认答复
        List< FormField > fields = form.getFields ();
        for ( int i = 0 ; fields != null && i < fields.size () ; i++ ) {
            if ( FormField.Type.hidden != fields.get ( i ).getType () &&
                    fields.get ( i ).getVariable () != null ) {
                // 设置默认值作为答复
                submitForm.setDefaultAnswer ( fields.get ( i ).getVariable () );
            }
        }
        // 设置聊天室的新拥有者
        List owners = new ArrayList ();
        owners.add ( connection.getUser () );//用户JID
        submitForm.setAnswer ( "muc#roomconfig_roomowners", owners );
        // 设置房间名称
        submitForm.setAnswer ( FormField.FORM_TYPE, "http://jabber.org/protocol/muc#roomconfig" );
        //设置房间名称
        submitForm.setAnswer ( "muc#roomconfig_roomname", "新的名称" );
        //设置房间描述
        submitForm.setAnswer ( "muc#roomconfig_roomdesc", "房间描述" );
        //是否允许修改主题
        submitForm.setAnswer ( "muc#roomconfig_changesubject", true );
        //设置最大人数
        List< String > maxusers = new ArrayList<> ();
        maxusers.add ( "9" );
        maxusers.add ( "19" );
        submitForm.setAnswer ( "muc#roomconfig_maxusers", maxusers );
        // 设置聊天室是持久聊天室，即将要被保存下来
        submitForm.setAnswer ( "muc#roomconfig_persistentroom", true );
        // 房间仅对成员开放
        submitForm.setAnswer ( "muc#roomconfig_membersonly", true );
        // 允许占有者邀请其他人
        submitForm.setAnswer ( "muc#roomconfig_allowinvites", true );
        // 能够发现占有者真实 JID 的角色
        // submitForm.setAnswer("muc#roomconfig_whois", "anyone");
        // 登录房间对话
        submitForm.setAnswer ( "muc#roomconfig_enablelogging", true );
        // 仅允许注册的昵称登录
        submitForm.setAnswer ( "x-muc#roomconfig_reservednick", true );
        // 允许使用者修改昵称
        submitForm.setAnswer ( "x-muc#roomconfig_canchangenick", true );
        // 允许用户注册房间
        submitForm.setAnswer ( "x-muc#roomconfig_registration", true );
        // 发送已完成的表单（有默认值）到服务器来配置聊天室
        muc.sendConfigurationForm ( submitForm );
        muc.join ( chatGroupNickname );
    }

    /**
     * 加入一个群聊聊天室
     *
     * @param roomName 聊天室名字
     * @param nickName 用户在聊天室中的昵称
     * @param password 聊天室密码
     *
     * @return
     */
    public MultiUserChat joinChatRoom ( String roomName, String nickName, String password ) {

        try {
            // 使用XMPPConnection创建一个MultiUserChat窗口
            MultiUserChat muc = MultiUserChatManager.getInstanceFor ( connection ).
                    getMultiUserChat ( roomName + "@conference." + connection.getServiceName () );
            // 聊天室服务将会决定要接受的历史记录数量
            DiscussionHistory history = new DiscussionHistory ();
            history.setMaxChars ( 0 );
            // history.setSince(new Date());
            // 用户加入聊天室
            muc.join ( nickName, password );
            return muc;
        } catch ( XMPPException | SmackException e ) {
            e.printStackTrace ();
            sendMessage ( e.getMessage (), "加入聊天室" );
            return null;
        }
    }

    /**
     * 加载头像图片
     */
    private void loadHeadImg () {

        VCardManager vCardManager = VCardManager.getInstanceFor ( connection );
        try {
            VCard vCard = vCardManager
                    .loadVCard ( accountEt.getText ().toString () );
            ByteArrayInputStream bais = new ByteArrayInputStream ( vCard.getAvatar () );
//            imgIv.setImageDrawable ( Drawable.createFromStream ( bais, "image" ) );
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
        builder.setServiceName ( HOST.substring ( 1, HOST.length () ) );
        builder.setHost ( server );
        builder.setPort ( port );
        builder.setCompressionEnabled ( false );//是否启用压缩
        builder.setDebuggerEnabled ( true );
        builder.setSendPresence ( true );//
        builder.setSecurityMode ( ConnectionConfiguration.SecurityMode.disabled );//
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
        message1.obj = content +
                " 来自:" + from;
        mHandler.sendMessage ( message1 );
    }


//    @Override protected void onDestroy () {
//
//        disconnect ();
//    }
}
