package com.example.iechat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class UserAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<ChatUser> users;

    public UserAdapter(Context context, ArrayList<ChatUser> users) {
        this.context = context;
        this.users = users;
    }

    @Override
    public int getCount() { return users.size(); }

    @Override
    public Object getItem(int position) { return users.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        }

        ImageView imgProfile = convertView.findViewById(R.id.imgProfile);
        TextView tvUsername = convertView.findViewById(R.id.tvUsername);
        TextView tvLastMsg = convertView.findViewById(R.id.tvLastMessage);

        ChatUser user = users.get(position);
        imgProfile.setImageResource(user.getProfileResId());
        tvUsername.setText(user.getUsername());
        tvLastMsg.setText(user.getLastMessage());

        return convertView;

    }
}
