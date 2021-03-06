package ru.spbhse.brainring.network;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

import ru.spbhse.brainring.managers.LocalAdminGameManager;
import ru.spbhse.brainring.network.messages.Message;
import ru.spbhse.brainring.network.messages.messageTypes.HandshakeMessage;
import ru.spbhse.brainring.network.messages.messageTypes.InitialHandshakeMessage;
import ru.spbhse.brainring.utils.Constants;

/**
 * Class with methods to interact with network
 * Used by admin in a local network mode
 */
public class LocalNetworkAdmin extends LocalNetwork {
    private LocalAdminGameManager manager;
    private String redId;
    private String greenId;
    private ServerSocket serverSocket;
    private static final Message HANDSHAKE = new HandshakeMessage();
    private static final int HANDSHAKE_DELAY = 1000;

    /**
     * Creates new instance. Fills {@code mRoomUpdateCallback} with an instance that
     *      on connected room starts game
     */
    public LocalNetworkAdmin(LocalAdminGameManager manager) {
        super(manager);
        this.manager = manager;
    }

    /** Decodes byte message received by server and calls needed functions in LocalController */
    @Override
    protected void onMessageReceived(@NonNull byte[] buf, @NonNull String userId) {
        if (gameIsFinished) {
            return;
        }
        Log.d(Constants.APP_TAG, "Received message as admin!");
        try {
            Message message = Message.readMessage(buf);
            manager.getProcessor().process(message, userId);
        } catch (IOException e) {
            Log.e(Constants.APP_TAG, "Error while reading message");
            e.printStackTrace();
        }
    }

    /** Sets green player id. If both players shared their ids starts game cycle */
    public void setGreenPlayer(@NonNull String userId) {
        if (handshaked) {
            Log.d(Constants.APP_TAG, "Handshake is done");
            return;
        }
        greenId = userId;
        if (redId != null) {
            handshaked = true;
            manager.getLogic().startGameCycle(greenId, redId);
        }
    }

    /** Sets red player id. If both players shared their ids starts game cycle */
    public void setRedPlayer(@NonNull String userId) {
        if (handshaked) {
            Log.d(Constants.APP_TAG, "Handshake is done");
            return;
        }
        redId = userId;
        if (greenId != null) {
            handshaked = true;
            manager.getLogic().startGameCycle(greenId, redId);
        }
    }

    /**
     * Sends empty message to players in order to determine which of them is green/red
     * Waits while the answer isn't received
     * After execution starts game cycle
     */
    @Override
    public void handshake() {
        if (handshaked) {
            return;
        }
        Log.d(Constants.APP_TAG, "Start handshake");
        sendMessageToUsers(new InitialHandshakeMessage());
        /*// Sometimes first message doesn't reach opponent for some reason
        // so we have to send it one more time
        uiHandler.postDelayed(this::handshake, HANDSHAKE_DELAY);*/
    }

    public void sendMessageToUsers(@NonNull Message message) {
        for (String key : contacts.keySet()) {
            sendMessageToConcreteUser(key, message);
        }
    }

    /**
     * Sends {@code HANDSHAKE} message to others to check that both players are connected
     * Called before each question
     */
    public void regularHandshake() {
        sendMessageToUsers(HANDSHAKE);
    }

    public void getIp() {
        executor.submit(() -> {
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            String ip = inetAddress.getHostAddress();
                            Log.d(Constants.APP_TAG, "ip is " + ip + " " + inetAddress.isAnyLocalAddress() + " " + inetAddress.isLinkLocalAddress() + " " + inetAddress.isLoopbackAddress() + " " + (inetAddress instanceof Inet4Address));
                            manager.getActivity().runOnUiThread(() -> manager.getActivity().onIpReceived(ip));
                            return;
                        }
                    }
                }
            } catch (SocketException ex) {
                Log.e(Constants.APP_TAG, ex.toString());
            }
        });
    }

    public void startServer() {
        executor.submit(() -> {
            try {
                serverSocket = new ServerSocket(Constants.LOCAL_PORT);
                while (!Thread.interrupted()) {
                    Socket socket = serverSocket.accept();
                    if (socket != null) {
                        String senderId = String.valueOf(new Random().nextInt());
                        LocalMessageDealing messageDealing = new LocalMessageDealing(socket,
                                LocalNetworkAdmin.this, senderId);
                        contacts.put(senderId, socket);
                        executor.submit(messageDealing);
                        if (contacts.size() == 2) {
                            handshake();
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                Log.wtf(Constants.APP_TAG, "IO exception starting server");
                e.printStackTrace();
                getUiHandler().post(() -> manager.finishGame());
            }
        });
    }

    @Override
    public void finish() {
        if (!gameIsFinished) {
            super.finish();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDisconnected(String disconnectedId) {
        manager.finishGame();
    }

    public LocalAdminGameManager getManager() {
        return manager;
    }
}
