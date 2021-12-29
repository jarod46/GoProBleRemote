package com.lazyjarod.goproremote;

import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class CustomOutputStream extends OutputStream {

    private StringBuilder buffer;
    private EditText editText;
    private MainActivity activity;

    public CustomOutputStream(MainActivity activity, EditText editText) {
        buffer = new StringBuilder(128);
        this.editText = editText;
        this.activity = activity;
    }

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
    Runnable action = new Runnable() {
        @Override
        public void run() {
            LocalDateTime localDateTime = LocalDateTime.now();
            editText.append("[" + dtf.format(localDateTime) + "] " + buffer);
            buffer.delete(0, buffer.length());
        }
    };


    @Override
    public void write(int b) throws IOException {

            buffer.append(Character.toChars((b + 256) % 256));
            if ((char) b == '\n') {
                Log.d("log", "log : " + buffer);
                activity.runOnUiThread(action);
                //editText.setCaretPosition(textArea.getDocument().getLength());

            }

    }
}
