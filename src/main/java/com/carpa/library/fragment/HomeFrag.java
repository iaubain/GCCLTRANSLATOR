package com.carpa.library.fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.carpa.library.R;
import com.carpa.library.config.ExtraConfig;
import com.carpa.library.entities.Messages;
import com.carpa.library.services.RectifierService;
import com.carpa.library.utilities.DataFactory;
import com.carpa.library.utilities.DownloadTaskListener;
import com.carpa.library.utilities.Popup;
import com.carpa.library.utilities.Progress;
import com.carpa.library.utilities.adapter.MessageAdapter;
import com.carpa.library.utilities.loader.LocalMessageLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnHomeFrag} interface
 * to handle interaction events.
 * Use the {@link HomeFrag#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFrag extends Fragment implements MessageAdapter.OnMessageAdapter, LocalMessageLoader.OnLocalMessagesLoader {
    private OnHomeFrag mListener;

    private Popup popup;
    private Progress progress;
    private MessageAdapter adapter;
    private LocalMessageLoader messageLoader;
    private Intent alarmIntent;
    private PendingIntent pendingIntent;
    private AlarmManager alarm;

    private RecyclerView recycler;

    public HomeFrag() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFrag.
     */
    public static HomeFrag newInstance() {
        HomeFrag fragment = new HomeFrag();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.home_frag, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        popup = new Popup(getContext());
        progress = new Progress(getContext(), false, false);

        recycler = view.findViewById(R.id.recycler);
        progress.show("Loading messages");
        messageLoader = new LocalMessageLoader(HomeFrag.this);
        messageLoader.load();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnHomeFrag) {
            mListener = (OnHomeFrag) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnHomeFrag");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleAlarm();
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelAlarm();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMessageAdapter(boolean isClicked, View view, Object object, String action) {
        if (action == null)
            return;
        switch (action) {
            case ExtraConfig.MESSAGE_FAVORITE:
                Messages message = (Messages) object;
                message.setFavorite(true);
                long id = message.save();
                if (id < 0) {
                    popup.show("Oops", "Something went wrong and we couldn't save your favorite.");
                } else {
                    Snackbar.make(recycler, message.getMessageName() + " Added to favorites", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                }
                break;
            case ExtraConfig.MESSAGE_INFO:
                popup.show("INFO", ((Messages) object).details());
                break;
            case ExtraConfig.MESSAGE_PLAY:
                //navigate to play fragment
                try {
                    mListener.onNavigation(HomeFrag.this, PreviewFrag.newInstance(DataFactory.objectToString(object)), null);
                } catch (IOException e) {
                    e.printStackTrace();
                    popup.show("Oops", "Something went wrong and we couldn't view the message for the moment.");
                }
                break;
        }
    }

    @Override
    public void onLocalMessages(boolean isLoaded, String message, List<Messages> messages) {
        if (progress != null)
            progress.clear();

        if (!isLoaded) {
            popup.show("Oops!", message);
        } else {
            try {
                if (messages.isEmpty())
                    popup.show("Info", "There no messages yet. Let the device connected to the internet for as long you can, If no message come you may contact the language translator admin!");
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            List<Object> mMessages = new ArrayList<>();
            //if(adapter != null && messages.size() > adapter.getItemCount()){
            //  adapter.refreshAdapter(mLanguages);
            //adapter.notifyDataSetChanged();
            //return;
            //}
            if (adapter != null && messages.size() > adapter.getItemCount()) {
                mListener.onNewMessage();
            }
            mMessages.addAll(messages);
            //Initiate an adapter
            adapter = new MessageAdapter(HomeFrag.this, getContext(), mMessages);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
            recycler.setLayoutManager(mLayoutManager);
            recycler.setHasFixedSize(true);
            recycler.setItemAnimator(new DefaultItemAnimator());
            recycler.setAdapter(adapter);
        }
    }

    public void onNewMessage() {
        //update home ui on new Message downloaded
        messageLoader = new LocalMessageLoader(HomeFrag.this);
        messageLoader.load();

    }

    public void filter(String charSequence) {
        if (adapter != null) {
            adapter.filter(charSequence);
        }
    }

    public void scheduleAlarm() {
        DownloadTaskListener.setSchedule(true);
        Log.d("BOOT", "Scheduling download task");
        Calendar cal = Calendar.getInstance();
        alarmIntent = new Intent(getActivity(), RectifierService.class);
        alarmIntent.setAction(RectifierService.ACTION_RECT);
        pendingIntent = PendingIntent.getService(getActivity(),
                999,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarm = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), RectifierService.PERIOD, pendingIntent);
    }

    public void cancelAlarm() {
        if (alarm != null && pendingIntent != null) {
            alarm.cancel(pendingIntent);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnHomeFrag {
        void onNavigation(Fragment source, Fragment destination, Object extra);

        void onNewMessage();
    }
}
