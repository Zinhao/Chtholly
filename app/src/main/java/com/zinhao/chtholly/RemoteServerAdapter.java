package com.zinhao.chtholly;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import personal.kola.net_scaner.Host;
import personal.kola.net_scaner.RemoteServer;

import java.util.List;

public final class RemoteServerAdapter extends ArrayAdapter<RemoteServer> {
    private final List<RemoteServer> data;

    public RemoteServerAdapter(Context context, List<RemoteServer> data) {
        super(context, 0, data);
        this.data = data;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        ViewHolder view;
        Context context = getContext();

        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            rowView = inflater.inflate(R.layout.host_list_item, parent, false);

            view = new ViewHolder();
            view.hostname = rowView.findViewById(R.id.hostname);
            view.hostIp = rowView.findViewById(R.id.hostIp);
            view.hostMac = rowView.findViewById(R.id.hostMac);
            view.hostMacVendor = rowView.findViewById(R.id.hostMacVendor);

            rowView.setTag(view);
        } else {
            view = (ViewHolder) rowView.getTag();
        }

        RemoteServer item = data.get(position);
        if(item.getHost()!=null){
            view.hostname.setText(item.getHost().getHostname());
            view.hostIp.setText(item.getHost().getIp());
            view.hostMac.setText(String.valueOf(item.getPort()));
            view.hostMacVendor.setText(item.getHttpUrl());
        }


        return rowView;
    }

    private static class ViewHolder {
        private TextView hostname;
        private TextView hostIp;
        private TextView hostMac;
        private TextView hostMacVendor;
    }
}
