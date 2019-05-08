package ru.spbhse.brainring.logic;

import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ru.spbhse.brainring.controllers.OnlineController;
import ru.spbhse.brainring.controllers.DatabaseController;
import ru.spbhse.brainring.network.messages.Message;
import ru.spbhse.brainring.utils.Question;

/** Realizes admin's logic in online mode */
public class OnlineGameAdminLogic {
    private UserScore user1;
    private UserScore user2;
    private Question currentQuestion;
    private String answeringUserId;
    private boolean readingTime;
    private boolean interrupted;

    private static final byte[] ALLOW_ANSWER = Message.generateMessage(Message.ALLOWED_TO_ANSWER, "");
    private static final byte[] FORBID_ANSWER = Message.generateMessage(Message.FORBIDDEN_TO_ANSWER, "");
    private static final byte[] OPPONENT_ANSWERING = Message.generateMessage(Message.OPPONENT_IS_ANSWERING, "");
    private static final byte[] TIME_START = Message.generateMessage(Message.TIME_START, "");
    private static final byte[] FALSE_START = Message.generateMessage(Message.FALSE_START, "");
    private static final byte[] TIME_OUT = Message.generateMessage(Message.TIME_TO_WRITE_ANSWER_IS_OUT, "");

    private static final int WINNER_SCORE = 5;
    private static final int FIRST_COUNTDOWN = 20;
    private static final int SECOND_COUNTDOWN = 20;
    private static final int SENDING_COUNTDOWN = 5;
    private static final int SECOND = 1000;
    private static final int TIME_TO_SHOW_ANSWER = 5;
    private static final int TIME_TO_READ_QUESTION = 10;
    private static final int TIME_TO_WRITE_ANSWER = 20;

    private final CountDownTimer firstGameTimer = new CountDownTimer(FIRST_COUNTDOWN * SECOND,
            SECOND) {
        @Override
        public void onTick(long millisUntilFinished) {
            if (interrupted) {
                return;
            }
            Log.d("BrainRing", "Tick first timer");
            if (millisUntilFinished <= SENDING_COUNTDOWN * SECOND) {
                OnlineController.NetworkController.sendMessageToAll(
                        Message.generateMessage(Message.TICK,
                                String.valueOf(millisUntilFinished / SECOND)));
            }
        }

        @Override
        public void onFinish() {
            if (interrupted) {
                return;
            }
            Log.d("BrainRing", "Finish first timer");
            if (answeringUserId == null) {
                showAnswer();
            }
        }
    };
    private final CountDownTimer secondGameTimer = new CountDownTimer(SECOND_COUNTDOWN * SECOND,
            SECOND) {
        @Override
        public void onTick(long millisUntilFinished) {
            if (interrupted) {
                return;
            }
            Log.d("BrainRing", "Tick second timer");
            if (millisUntilFinished <= SENDING_COUNTDOWN * SECOND) {
                OnlineController.NetworkController.sendMessageToAll(
                        Message.generateMessage(Message.TICK,
                                String.valueOf(millisUntilFinished / SECOND)));
            }
        }

        @Override
        public void onFinish() {
            if (interrupted) {
                return;
            }
            Log.d("BrainRing", "Finish second timer");
            if (answeringUserId == null) {
                showAnswer();
            }
        }
    };
    private volatile CountDownTimer timer;

    /** Returns UserScore object connected with given user */
    public OnlineGameAdminLogic() {
        user1 = new UserScore(OnlineController.NetworkController.getMyParticipantId());
        user2 = new UserScore(OnlineController.NetworkController.getOpponentParticipantId());
    }

    /** Returns UserScore object connected with given user */
    private UserScore getThisUser(String userId) {
        return user1.status.participantId.equals(userId) ? user1 : user2;
    }

    /** Returns UserScore object connected with opponent of given user */
    private UserScore getOtherUser(String userId) {
        return user1.status.participantId.equals(userId) ? user2 : user1;
    }

    /**
     * Allows or forbids to answer team that pushed answer button
     * Determines false starts
     */
    public void onAnswerIsReady(String userId) {
        System.out.println("THREAD ID" + Thread.currentThread().getId());
        if (readingTime) {
            getThisUser(userId).status.alreadyAnswered = true;
            OnlineController.NetworkController.sendMessageToConcreteUser(userId, FALSE_START);
            return;
        }
        timer.cancel();
        UserScore user = getThisUser(userId);
        if (user.status.alreadyAnswered || answeringUserId != null) {
            OnlineController.NetworkController.sendMessageToConcreteUser(userId, FORBID_ANSWER);
        } else {
            answeringUserId = userId;
            user.status.alreadyAnswered = true;
            OnlineController.NetworkController.sendReliableMessageToConcreteUser(userId, ALLOW_ANSWER);
            final String currentUser = userId;
            timer = new CountDownTimer(TIME_TO_WRITE_ANSWER * SECOND,
                    TIME_TO_WRITE_ANSWER * SECOND) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    if (answeringUserId != null && answeringUserId.equals(currentUser)) {
                        stopAnswering();
                    }
                }
            };
            timer.start();
            OnlineController.NetworkController.sendMessageToConcreteUser(
                    getOtherUser(userId).status.participantId, OPPONENT_ANSWERING);
        }
    }

    private void restartTime(String previousUserId, String previousAnswer) {
        if (bothAnswered()) {
            showAnswer();
            return;
        }
        OnlineController.NetworkController.sendReliableMessageToConcreteUser(
                getOtherUser(previousUserId).status.participantId,
                Message.generateMessage(Message.SENDING_INCORRECT_OPPONENT_ANSWER, previousAnswer));
        timer = secondGameTimer;
        timer.start();
    }

    // called by writing timer. answeringUserId != null
    private void stopAnswering() {
        String userId = answeringUserId;
        answeringUserId = null;
        OnlineController.NetworkController.sendReliableMessageToConcreteUser(userId, TIME_OUT);
        restartTime(userId, "");
    }

    /** Rejects or accepts answer written by user */
    public void onAnswerIsWritten(String writtenAnswer, String id) {
        System.out.println("THREAD ID" + Thread.currentThread().getId());
        Log.d("BrainRing","GOT ANSWER: " + writtenAnswer + " from user " + id);
        if (!id.equals(answeringUserId)) {
            return;
        }
        timer.cancel();
        String userId = answeringUserId;
        answeringUserId = null;
        if (!currentQuestion.checkAnswer(writtenAnswer)) {
            if (!getOtherUser(userId).status.alreadyAnswered) {
                restartTime(userId, writtenAnswer);
                return;
            }
        } else {
            ++getThisUser(userId).score;
        }

        showAnswer();
    }

    /** Sends answer and shows it for {@code TIME_TO_SHOW_ANSWER} seconds */
    private void showAnswer() {
        System.out.println("THREAD ID" + Thread.currentThread().getId());
        OnlineController.NetworkController.sendReliableMessageToAll(generateAnswer());
        new Handler().postDelayed(this::newQuestion, TIME_TO_SHOW_ANSWER * SECOND);
    }

    /** Determines if game is finished. If not, generates new question and sends it */
    public void newQuestion() {
        if (user1.score >= WINNER_SCORE || user2.score >= WINNER_SCORE) {
            OnlineController.finishOnlineGame();
            return;
        }

        Log.d("BrainRing", "New question");
        user1.status.onNewQuestion();
        user2.status.onNewQuestion();

        currentQuestion = DatabaseController.getRandomQuestion();
        readingTime = true;
        byte[] message = Message.generateMessage(Message.SENDING_QUESTION, currentQuestion.getQuestion());
        OnlineController.NetworkController.sendQuestion(message);
    }

    public void publishing() {
        new Handler().postDelayed(this::publishQuestion, TIME_TO_READ_QUESTION * SECOND);
    }

    private boolean bothAnswered() {
        return user1.status.alreadyAnswered && user2.status.alreadyAnswered;
    }

    private void publishQuestion() {
        System.out.println("THREAD ID" + Thread.currentThread().getId());
        if (interrupted) {
            return;
        }
        readingTime = false;
        if (!bothAnswered()) {
            OnlineController.NetworkController.sendReliableMessageToAll(TIME_START);
            timer = firstGameTimer;
            timer.start();
        } else {
            showAnswer();
        }
    }

    public void finishGame() {
        interrupted = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private byte[] generateAnswer() {
        String answer = currentQuestion.getAllAnswers();
        byte[] buf;
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
             DataOutputStream dout = new DataOutputStream(bout)) {
            dout.writeInt(Message.SENDING_CORRECT_ANSWER_AND_SCORE);
            dout.writeInt(user1.score);
            dout.writeInt(user2.score);
            dout.writeChars(answer);
            buf = bout.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            buf = null;
        }
        return buf;
    }

    private static class UserScore {
        private int score;
        private UserStatus status;

        private UserScore(String userId) {
            status = new UserStatus(userId);
        }
    }
}
