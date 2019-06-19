package ru.spbhse.brainring.logic.timers;

import android.os.CountDownTimer;

import ru.spbhse.brainring.logic.OnlineGameUserLogic;
import ru.spbhse.brainring.utils.Constants;

public class OnlineShowingAnswerTimer extends CountDownTimer {
    private OnlineGameUserLogic logic;

    public OnlineShowingAnswerTimer(int showingAnswerTimeSec, OnlineGameUserLogic logic) {
        super(showingAnswerTimeSec * Constants.SECOND,
                showingAnswerTimeSec * Constants.SECOND);
        this.logic = logic;
    }

    @Override
    public void onTick(long millisUntilFinished) {
    }

    @Override
    public void onFinish() {
        if (logic.getTimer() == this) {
            logic.readyForQuestion();
        }
    }
}
