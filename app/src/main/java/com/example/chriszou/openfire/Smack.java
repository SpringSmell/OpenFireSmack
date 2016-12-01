package com.example.chriszou.openfire;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
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
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.net.SocketFactory;

/**
 * @author Chris zou
 * @Date 2016/12/1
 * @modifyInfo1 Zuo-2016/12/1
 * @modifyContent 基于smack 4.1.4
 */

public class Smack {

    public static final int CONNECT = 0x001;
    public static final int LOGIN_IN = 0x002;
    private static final String defaultMaxUserCount = "9";
    private static XMPPTCPConnection mXMPPTCPConnection;
    private LoginThread mLoginThread;

    private Handler mHandler = new Handler () {

        @Override public void handleMessage ( android.os.Message msg ) {

            Bundle bundle;
            switch ( msg.what ) {
                case LOGIN_IN:
                    bundle = msg.getData ();
                    OnLoginListener onLoginListener = ( OnLoginListener ) bundle
                            .getSerializable ( "listener" );
                    String json = bundle.getString ( "data" );

                    onLoginListener.onLogin ( msg.getData ().getBoolean ( "isSuccess" ), json );
                    break;
                case CONNECT:
                    OnConnectListener onConnectListener = ( OnConnectListener ) msg.getData ()
                                                                                   .getSerializable (
                                                                                           "listener" );

                    onConnectListener.onConnect ( msg.getData ().getBoolean ( "isSuccess" ),
                                                  msg.getData ().getString ( "msg" ) );
                    break;
            }
        }
    };


    private static class ClassHolder {

        private static final Smack INSTANCE = new Smack ();
    }

    public static Smack getInstance ( String server, String serverName, int port ) {

        init ( server, serverName, port );
        return ClassHolder.INSTANCE;
    }

    private static void init ( String server, String serverName, int port ) {

        mXMPPTCPConnection = getConnection ( server, serverName, port );
    }

    private void sendMessage ( int what, Bundle bundle ) {

        android.os.Message msg = android.os.Message.obtain ();
        msg.what = what;
        msg.setData ( bundle );
        mHandler.sendMessage ( msg );
    }

    public boolean isConnected () {

        boolean flag;
        if ( mXMPPTCPConnection != null ) {
            flag = mXMPPTCPConnection.isConnected ();
        } else {
            flag = false;
        }

        return flag;
    }

    public void setConnection ( XMPPTCPConnection connection ) {

        this.mXMPPTCPConnection = connection;
    }

    /**
     * 创建一个连接
     *
     * @param server     服务器：一般为域名 ex:192.168.1.10
     * @param serverName 服务器名称：一般为主机名 ex:chrisZouPc-1
     * @param port       端口
     *
     * @return
     */
    private static XMPPTCPConnection getConnection ( String server, String serverName, int port ) {

        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder ();
        builder.setSocketFactory ( SocketFactory.getDefault () );
        builder.setServiceName ( serverName );
        builder.setHost ( server );
        builder.setPort ( port );
        builder.setCompressionEnabled ( false );//是否启用压缩
        builder.setDebuggerEnabled ( true );
        builder.setSendPresence ( true );//
        builder.setSecurityMode ( ConnectionConfiguration.SecurityMode.disabled );//
        XMPPTCPConnection connection = new XMPPTCPConnection ( builder.build () );

        return connection;
    }

    /**
     * 创建一个用户
     *
     * @param account  用户名
     * @param password 密码
     *
     * @throws SmackException.NotConnectedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NoResponseException
     */
    public void createUser ( String account, String password ) throws SmackException.NotConnectedException, XMPPException.XMPPErrorException, SmackException.NoResponseException {

        AccountManager accountManager = AccountManager.getInstance ( mXMPPTCPConnection );
        accountManager.createAccount ( account, password );
    }

    /**
     * 修改当前用户密码
     *
     * @param newPassword 新密码
     *
     * @throws SmackException.NotConnectedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NoResponseException
     */
    public void modifyUserPassword ( String newPassword ) throws SmackException.NotConnectedException, XMPPException.XMPPErrorException, SmackException.NoResponseException {

        AccountManager accountManager = AccountManager.getInstance ( mXMPPTCPConnection );
        accountManager.changePassword ( newPassword );
    }

    /**
     * 删除一个用户（没测试，待完善）
     *
     * @throws SmackException.NotConnectedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NoResponseException
     */
    public void deleteUserAccount () throws SmackException.NotConnectedException, XMPPException.XMPPErrorException, SmackException.NoResponseException {

        AccountManager accountManager = AccountManager.getInstance ( mXMPPTCPConnection );
        accountManager.deleteAccount ();
    }

    public void loginIn ( String account, String password, OnLoginListener onLoginListener ) {

        if ( mLoginThread == null && !mLoginThread.isInterrupted () ) {
            mLoginThread = new LoginThread ( account, password, onLoginListener );
            mLoginThread.start ();
        } else {
            onLoginListener.onLogin ( false, "连接中" );
        }
    }

    /**
     * 创建一个聊天室
     *
     * @param chatRoomNickname 聊天室名称
     * @param password         密码
     *
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException
     */
    public void createChatRoom ( String chatRoomNickname, String password ) throws XMPPException.XMPPErrorException, SmackException {

        createChatRoom ( chatRoomNickname, password, "", defaultMaxUserCount );
    }

    /**
     * 创建一个聊天室
     *
     * @param chatRoomNickname 聊天室名称
     * @param password         密码
     * @param maxUserCount     最大房间容量
     *
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException
     */
    public void createChatRoom ( String chatRoomNickname, String password, String maxUserCount ) throws XMPPException.XMPPErrorException, SmackException {

        createChatRoom ( chatRoomNickname, password, "", maxUserCount );
    }

    /**
     * 创建一个聊天室
     *
     * @param chatRoomNickname    聊天室名称
     * @param password            密码
     * @param chatRoomDescription 描述
     * @param maxUserCount        最大房间容量
     *
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException
     */
    public void createChatRoom ( String chatRoomNickname, String password, String chatRoomDescription, String maxUserCount ) throws XMPPException.XMPPErrorException, SmackException {

        MultiUserChatManager multiUserChatManager = MultiUserChatManager
                .getInstanceFor ( mXMPPTCPConnection );

        MultiUserChat muc = multiUserChatManager
                .getMultiUserChat (
                        chatRoomNickname + "@conference." + mXMPPTCPConnection.getServiceName () );
        muc.create ( chatRoomNickname );
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
        owners.add ( mXMPPTCPConnection.getUser () );//用户JID
        submitForm.setAnswer ( "muc#roomconfig_roomowners", owners );
        // 设置房间名称
        submitForm.setAnswer ( FormField.FORM_TYPE, "http://jabber.org/protocol/muc#roomconfig" );
        //设置房间名称
        submitForm.setAnswer ( "muc#roomconfig_roomname", chatRoomNickname );
        // 设置进入密码
        submitForm.setAnswer ( "muc#roomconfig_roomsecret", password );
        //设置房间描述
        submitForm.setAnswer ( "muc#roomconfig_roomdesc", chatRoomDescription );
        //是否允许修改主题
        submitForm.setAnswer ( "muc#roomconfig_changesubject", true );
        //设置最大人数
        List< String > maxusers = new ArrayList<> ();
        maxusers.add ( maxUserCount );
        submitForm.setAnswer ( "muc#roomconfig_maxusers", maxusers );
        // 设置聊天室是持久聊天室，即将要被保存下来
        submitForm.setAnswer ( "muc#roomconfig_persistentroom", true );
        // 房间仅对成员开放
        submitForm.setAnswer ( "muc#roomconfig_membersonly", false );
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
        muc.join ( chatRoomNickname );
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
    public MultiUserChat joinChatRoom ( String roomName, String nickName, String password, MessageListener listener ) throws XMPPException.XMPPErrorException, SmackException {

        // 使用XMPPConnection创建一个MultiUserChat窗口
        MultiUserChat muc = MultiUserChatManager.getInstanceFor ( mXMPPTCPConnection ).
                getMultiUserChat (
                        roomName + "@conference." + mXMPPTCPConnection.getServiceName () );
        // 聊天室服务将会决定要接受的历史记录数量
        DiscussionHistory history = new DiscussionHistory ();
        history.setMaxChars ( 0 );
        // history.setSince(new Date());
        // 用户加入聊天室
        muc.join ( nickName, password );
        muc.addMessageListener ( listener );
        return muc;
    }

    /**
     * 给其他用户发送消息
     *
     * @param toUser   接收者名称
     * @param msgStr   消息内容
     * @param listener 回复监听
     *
     * @throws SmackException.NotConnectedException
     */
    public void sendChatMsg ( String toUser, String msgStr, ChatMessageListener listener ) throws SmackException.NotConnectedException {

        ChatManager chatManager = ChatManager.getInstanceFor ( mXMPPTCPConnection );
        Chat chat = chatManager
                .createChat ( toUser + "@" + mXMPPTCPConnection.getServiceName (), listener );
        Message msg = new Message ();
        msg.setBody ( msgStr );
        chat.sendMessage ( msg );
    }

    /**
     * 给聊天室发送消息
     *
     * @param muc    聊天室实例
     * @param msgStr 消息内容
     *
     * @throws SmackException.NotConnectedException
     */
    public void sendChatRoomMsg ( MultiUserChat muc, String msgStr ) throws SmackException.NotConnectedException {

        Message msg = new Message ();
        msg.setBody ( msgStr );
        muc.sendMessage ( msg );
    }

    /**
     * 断开连接
     */
    public void disconnect () {

        if ( mXMPPTCPConnection != null ) {
            mXMPPTCPConnection.disconnect ();
        }
    }

    /**
     * 销毁
     */
    public void destroy () {

        disconnect ();
        mXMPPTCPConnection = null;
    }

    /**
     * 获取发送文件的发送器
     *
     * @param jid 一个完整的jid(如：laohu@192.168.0.108/Smack
     *            后面的Smack应该客户端类型，不加这个会出错)
     *
     * @return
     */
    public OutgoingFileTransfer getSendFileTransfer ( String jid ) {

        if ( isConnected () ) {
            return FileTransferManager.getInstanceFor ( mXMPPTCPConnection )
                                      .createOutgoingFileTransfer ( jid );
        }
        throw new NullPointerException ( "服务器连接失败，请先连接服务器" );
    }

    public void sendFile () {

    }

    public interface OnLoginListener < T > extends Serializable {

        void onLogin ( boolean isSuccess, T t );
    }

    public interface OnConnectListener extends Serializable {

        void onConnect ( boolean isConnect, String msg );
    }

    public class ConnectThread extends Thread {

        private OnConnectListener listener;
        private String server;
        private String serverName;
        private int port;

        public ConnectThread ( String server, String serverName, int port, OnConnectListener listener ) {

            this.listener = listener;
            this.server = server;
            this.serverName = serverName;
            this.port = port;
        }

        @Override public void run () {

            super.run ();
            Exception ex     = null;
            boolean   isSuccess;
            String    msg;
            Bundle    bundle = new Bundle ();
            bundle.putSerializable ( "listener", listener );

            if ( mXMPPTCPConnection == null ) {
                mXMPPTCPConnection = getConnection ( server, serverName, port );
            }
            try {
                mXMPPTCPConnection.connect ();
                sendMessage ( CONNECT, bundle );
                return;
            } catch ( SmackException e ) {
                ex = e;
            } catch ( IOException e ) {
                ex = e;
            } catch ( XMPPException e ) {
                ex = e;
            } finally {
                if ( ex == null || isConnected () ) {
                    isSuccess = true;
                    msg = "连接成功";
                } else {
                    isSuccess = false;
                    msg = ex.getMessage ();
                }
            }
            bundle.putBoolean ( "isSuccess", isSuccess );
            bundle.putString ( "msg", msg );
            sendMessage ( CONNECT, bundle );
        }
    }

    public class LoginThread extends Thread {

        private String account;
        private String password;
        private OnLoginListener onLoginListener;

        public LoginThread ( String account, String password, OnLoginListener onLoginListener ) {

            this.account = account;
            this.password = password;
            this.onLoginListener = onLoginListener;
        }

        @Override public void run () {

            super.run ();
            try {
                if ( !isConnected () ) {
                    mXMPPTCPConnection.connect ();
                }
                mXMPPTCPConnection.login ( account, password );
                //出席信息
                Presence presence = new Presence ( Presence.Type.available );
                presence.setStatus ( "我登陆了" );

                mXMPPTCPConnection.sendStanza ( presence );
                ChatManager chatmanager = ChatManager.getInstanceFor ( mXMPPTCPConnection );
                chatmanager.addChatListener ( new ChatManagerListener () {

                    @Override
                    public void chatCreated ( Chat chat, boolean createdLocally ) {

                        chat.addMessageListener ( new ChatMessageListener () {

                            @Override
                            public void processMessage ( Chat chat, Message message ) {

                                String  content = message.getBody ();
                                boolean isSuccess;
                                Bundle  bundle  = new Bundle ();
                                bundle.putSerializable ( "listener", onLoginListener );

                                if ( content != null ) {
                                    isSuccess = true;
                                } else {
                                    content = "";
                                    isSuccess = false;
                                }
                                bundle.putString ( "data", content );
                                bundle.putBoolean ( "isSuccess", isSuccess );
                                sendMessage ( LOGIN_IN, bundle );
                            }
                        } );
                    }
                } );
            } catch ( SmackException e ) {
                e.printStackTrace ();
            } catch ( IOException e ) {
                e.printStackTrace ();
            } catch ( XMPPException e ) {
                e.printStackTrace ();
            }
        }
    }
}
