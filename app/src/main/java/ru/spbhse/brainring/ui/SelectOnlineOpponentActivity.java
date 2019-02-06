package ru.spbhse.brainring.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import ru.spbhse.brainring.Controller;
import ru.spbhse.brainring.R;

public class SelectOnlineOpponentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_online_oppenent);

        Button searchOpponentButton = findViewById(R.id.searchOpponentButton);
        searchOpponentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Controller.createOnlineGame();

                Intent intent = new Intent(SelectOnlineOpponentActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });
    }


}