package com.test.bidon.domain;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.test.bidon.dto.LiveBidCostDTO;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiveBidRoom {
    private final Long roomId;
    private final Set<WebSocketSession> sessions = new HashSet<>();
    private final Set<LiveBidRoomUser> roomUsers = new HashSet<>();
    private LiveBidRoomUser highestBidder = null;
    private LiveBidCostDTO highestBidCost = null;
    private Integer highestBidPrice = 0;
    private Timer timer = new Timer();
    private Integer remainingSeconds;
    private Boolean isTimerRunning = false;
    private static final int TOTAL_SECONDS = 60;
    private final Object lock = new Object();



    @Builder
    public LiveBidRoom(Long roomId) {
        this.roomId = roomId;
    }

    public void sendMessageExclude(TextMessage message, WebSocketSession excludeSession) {
        this.getSessions()
                .parallelStream()
                .filter(WebSocketSession::isOpen)
                .filter(session -> !session.equals(excludeSession))
                .forEach(session -> sendMessageToSession(session, message));
    }

    public void sendMessageAll(TextMessage message) {
        this.getSessions()
                .parallelStream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> sendMessageToSession(session, message));
    }

    public void sendMessageToSession(WebSocketSession session, TextMessage message) {
        synchronized (lock) {
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public LiveBidRoomUser findRoomUser(Long userId) {
        for (LiveBidRoomUser user : roomUsers) {
            if (user.getUserId().equals(userId)) {
                return user;
            }
        }

        return null;
    }

    public LiveBidRoomUser updateHighestBidder(Long userId) {
        LiveBidRoomUser highestBidder = null;

        for (LiveBidRoomUser user : roomUsers) {
            if (user.getUserId().equals(userId)) {
                highestBidder = user;
                user.setIsHighestBidder(true);
            } else {
                user.setIsHighestBidder(false);
            }
        }

        return highestBidder;
    }

    public void startTimer(TimerTask task) {
        this.remainingSeconds = TOTAL_SECONDS;
        this.timer.scheduleAtFixedRate(task, 0, 1000L);
    }

    public void resetTimer(TimerTask task) {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
        }

        this.timer = new Timer();

        startTimer(task);
    }

    public void enter(WebSocketSession session, LiveBidRoomUser roomUser) {
        sessions.add(session);
        roomUsers.add(roomUser);
    }

    public void leave(WebSocketSession session, Long userInfoId) {
        sessions.remove(session);
        roomUsers.remove(new LiveBidRoomUser(userInfoId));
    }

    public static LiveBidRoom of(Long roomId) {
        return LiveBidRoom.builder()
                .roomId(roomId)
                .build();
    }

}
