package com.zinhao.chtholly;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.collection.SimpleArrayMap;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.zinhao.chtholly.databinding.ActivityScanNetBinding;
import org.jetbrains.annotations.NotNull;
import personal.kola.net_scaner.*;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanNetActivity extends AppCompatActivity implements MainAsyncResponse, HostAsyncResponse, AdapterView.OnItemClickListener {
    private ActivityScanNetBinding binding;
    SimpleArrayMap<String, Host> simpleArrayMap = new SimpleArrayMap<>();
    List<RemoteServer> servers = new ArrayList<>();
    private int startPort = 0;
    private int endPort = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanNetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent input = getIntent();
        startPort = input.getIntExtra("start_port",80);
        endPort = input.getIntExtra("end_port",444);
        ssidAccess(this);
        checkListEmpty();
    }

    private void checkListEmpty(){
        if(servers.isEmpty()){
            binding.listview.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.listview.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void ssidAccess(Context context) {

        String perm = Manifest.permission.ACCESS_COARSE_LOCATION;
        int request = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perm = Manifest.permission.ACCESS_FINE_LOCATION;
            request = 2;
        }

        if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{perm}, request);
        }else {
            scanIp();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode== 1 || requestCode == 2){
            for (int i = 0; i < permissions.length; i++) {
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    scanIp();
                    break;
                }
            }
        }
    }

    public void scanIp() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        Wireless wifi = new Wireless(this);

        //Endianness can be a potential issue on some hardware
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ip = Integer.reverseBytes(ip);
        }

        byte[] ipByteArray = BigInteger.valueOf(ip).toByteArray();
        int _ip = 0;
        try {
            _ip = new BigInteger(InetAddress.getByAddress(ipByteArray).getAddress()).intValue();
            new ScanHostsAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,_ip, wifi.getInternalWifiSubnet(), 25);
        } catch (Wireless.NoWifiManagerException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void scanPorts(String ip, int startPort, int stopPort, int timeout, HostAsyncResponse delegate) {
        new ScanPortsAsyncTask(delegate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ip, startPort, stopPort, timeout);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        RemoteServer remoteServer = servers.get(position);
        Intent dataIntent = new Intent();
        dataIntent.putExtra("host",remoteServer.getHttpUrl());
        setResult(RESULT_OK,dataIntent);
        finish();
    }

    @Override
    public <T extends Throwable> void processFinish(T t) {

    }

    @Override
    public void processFinish(SparseArray<String> output) {
        int scannedPort = output.keyAt(0);
        String key = output.get(scannedPort);
        servers.add(new RemoteServer(simpleArrayMap.get(key),scannedPort));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RemoteServerAdapter adapter = new RemoteServerAdapter(ScanNetActivity.this,servers);
                binding.listview.setOnItemClickListener(ScanNetActivity.this);
                binding.listview.setAdapter(adapter);
                checkListEmpty();
            }
        });
    }

    @Override
    public void processFinish(int i) {

    }

    @Override
    public void processFinish(boolean b) {

    }

    @Override
    public void processFinish(Exception e) {

    }

    @Override
    public void processFinish(Host host, AtomicInteger atomicInteger) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                simpleArrayMap.put(host.getIp(),host);
                scanPorts(host.getIp(),startPort,endPort,15,ScanNetActivity.this);
            }
        });
    }
}