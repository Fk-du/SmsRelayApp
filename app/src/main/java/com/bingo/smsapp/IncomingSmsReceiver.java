package com.bingo.smsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IncomingSmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsAppReceiver";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) {
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();
        String sender = null;

        for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, "3gpp");
            if (smsMessage == null) {
                continue;
            }
            if (sender == null) {
                sender = smsMessage.getOriginatingAddress();
            }
            messageBuilder.append(smsMessage.getMessageBody());
        }

        if (sender == null) {
            return;
        }

        String text = messageBuilder.toString();
        Log.d(TAG, "SMS from " + sender + ": " + text);

        if (!looksLikeBankMessage(sender, text)) {
            return;
        }

        sendToBackend(sender, text);
    }

    private boolean looksLikeBankMessage(String sender, String text) {
        String normalizedSender = (sender == null ? "" : sender.toLowerCase(Locale.ROOT));
        String normalizedText = (text == null ? "" : text.toLowerCase(Locale.ROOT));

        boolean knownBankSender = normalizedSender.contains("127")
                || normalizedSender.contains("cbe")
                || normalizedSender.contains("telebirr")
                || normalizedSender.contains("ethio")
                || normalizedSender.contains("bank");

        boolean bankKeywords = normalizedText.contains("received")
                || normalizedText.contains("credited")
                || normalizedText.contains("transaction")
                || normalizedText.contains("receipt")
                || normalizedText.contains("ብር")
                || normalizedText.contains("etb")
                || normalizedText.contains("የሂሳብ እንቅስቃሴ");

        return knownBankSender || bankKeywords;
    }

    private void sendToBackend(String sender, String text) {
        OkHttpClient client = new OkHttpClient();
        String payload = String.format(Locale.US,
                "{\"from\":\"%s\",\"text\":\"%s\",\"receivedAt\":\"%s\"}",
                sender,
                text.replace("\\", "\\\\").replace("\"", "\\\""),
                java.time.Instant.now().toString());

        Request request = new Request.Builder()
                .url("http://192.168.1.15:8080/api/v1/payments/sms-webhook")
                .header("X-API-KEY", "dev-sms-key")
                .post(RequestBody.create(payload, JSON))
                .build();

        try {
            Response response = client.newCall(request).execute();
            Log.d(TAG, "Webhook response: " + response.code() + " " + response.message());
            response.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to forward SMS to backend", e);
        }
    }
}
