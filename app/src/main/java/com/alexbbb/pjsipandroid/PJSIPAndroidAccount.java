package com.alexbbb.pjsipandroid;

import android.content.Context;
import android.util.Log;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.HashMap;

/**
 * Wrapper around PJSUA2 Account object.
 * @author alexbbb (Aleksandar Gotev)
 */
public class PJSIPAndroidAccount extends Account {

    private static final String LOG_TAG = "PJSIPAndroidAccount";

    private Context appContext;
    private HashMap<Integer, PJSIPAndroidCall> activeCalls = new HashMap<>();
    private PJSIPAccountData data;

    protected PJSIPAndroidAccount(Context context, PJSIPAccountData data) {
        super();
        appContext = context;
        this.data = data;
    }

    public PJSIPAccountData getData() {
        return data;
    }

    public void create() throws Exception {
        create(data.getAccountConfig());
    }

    protected void removeCall(int callId) {
        PJSIPAndroidCall call = activeCalls.get(callId);

        if (call != null) {
            Log.i(LOG_TAG, "Removing call with ID: " + callId);
            activeCalls.remove(callId);
        }
    }

    public PJSIPAndroidCall getCall(int callId) {
        return activeCalls.get(callId);
    }

    public PJSIPAndroidCall addIncomingCall(int callId) {

        PJSIPAndroidCall call = new PJSIPAndroidCall(appContext, this, callId);
        activeCalls.put(callId, call);

        return call;
    }

    public PJSIPAndroidCall addOutgoingCall(final String numberToDial , final String realm) {
        PJSIPAndroidCall call = new PJSIPAndroidCall(appContext, this);

        CallOpParam callOpParam = new CallOpParam();
        try {
            call.makeCall("sip:" + numberToDial + "@" + realm, callOpParam);

            Log.d(LOG_TAG, "New outgoing call with ID: " + call.getId());
            activeCalls.put(call.getId(), call);

            return call;

        } catch (Exception exc) {
            Log.e(LOG_TAG, "Error while making outgoing call", exc);
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PJSIPAndroidAccount that = (PJSIPAndroidAccount) o;

        return data.equals(that.data);

    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public void onRegState(OnRegStateParam prm) {
        PJSIPAndroid.getBroadcastEmitter()
                    .registrationState(data.getIdUri(), prm.getCode().swigValue());
    }

    @Override
    public void onIncomingCall(OnIncomingCallParam prm) {

        PJSIPAndroidCall call = addIncomingCall(prm.getCallId());

        if (activeCalls.size() > 1) {
            call.declineIncomingCall();
            Log.i(LOG_TAG, "Sending busy");
            //TODO: notification of missed call
            return;
        }

        try {
            // Answer with 180 Ringing
            CallOpParam callOpParam = new CallOpParam();
            callOpParam.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
            call.answer(callOpParam);

            PJSIPAndroid.startRingTone();

            PJSIPCallerInfo contactInfo = new PJSIPCallerInfo(call.getInfo());

            PJSIPAndroid.getBroadcastEmitter()
                        .incomingCall(data.getIdUri(), prm.getCallId(),
                                      contactInfo.getDisplayName(), contactInfo.getRemoteUri());

        } catch (Exception exc) {
            Log.e(LOG_TAG, "Error while getting call info", exc);
        }
    }
}