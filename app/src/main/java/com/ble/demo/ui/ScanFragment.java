package com.ble.demo.ui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ble.api.DataUtil;
import com.ble.ble.LeScanRecord;
import com.ble.ble.scan.LeScanResult;
import com.ble.ble.scan.LeScanner;
import com.ble.ble.scan.OnLeScanListener;
import com.ble.ble.scan.ScanPermissionRequest;
import com.ble.ble.scan.ScanRequestCallback;
import com.ble.demo.LeDevice;
import com.ble.demo.R;
import com.ble.demo.util.LeProxy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ScanFragment extends Fragment {
    private final static String TAG = "ScanFragment";

    private static final int MSG_SCAN_STARTED = 1;
    private static final int MSG_SCAN_DEVICE = 2;
    private static final int MSG_SCAN_STOPPED = 3;

    private LeProxy mLeProxy = LeProxy.getInstance();
    private LeDeviceListAdapter mLeDeviceListAdapter = new LeDeviceListAdapter();
    private Handler mHandler = new MyHandler(new WeakReference<ScanFragment>(this));

    private SwipeRefreshLayout mRefreshLayout;

    private static class MyHandler extends Handler {
        WeakReference<ScanFragment> reference;

        MyHandler(WeakReference<ScanFragment> reference) {
            this.reference = reference;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ScanFragment fragment = reference.get();
            if (fragment != null) {
                switch (msg.what) {
                    case MSG_SCAN_STARTED:
                        fragment.mLeDeviceListAdapter.clear();
                        fragment.mRefreshLayout.setRefreshing(true);
                        break;

                    case MSG_SCAN_DEVICE:
                        fragment.mLeDeviceListAdapter.addDevice((LeDevice) msg.obj);
                        break;

                    case MSG_SCAN_STOPPED:
                        fragment.mRefreshLayout.setRefreshing(false);
                        break;
                }
            }
        }
    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                if (state == BluetoothAdapter.STATE_ON) {
                    requestScan();
                }
            }
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refreshLayout);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //mLeDeviceListAdapter.clear();
                LeScanner.startScan(mOnLeScanListener);
            }
        });

        ListView listView = (ListView) view.findViewById(R.id.listView1);
        listView.setAdapter(mLeDeviceListAdapter);
        listView.setOnItemClickListener(mOnItemClickListener);
        listView.setOnItemLongClickListener(mOnItemLongClickListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        requestScan();
    }

    private void requestScan() {
        final Activity activity = requireActivity();
        LeScanner.requestScan(this, 111, new ScanRequestCallback() {
            @Override
            public void onBluetoothDisabled() {
                LeScanner.requestEnableBluetooth(activity);
            }

            @Override
            public void shouldShowPermissionRationale(final ScanPermissionRequest request) {
                showAlertDialog(
                        R.string.scan_tips_no_location_permission,
                        R.string.to_grant_permission,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                request.proceed();
                            }
                        });
            }

            @Override
            public void onPermissionDenied() {
                showAlertDialog(
                        R.string.scan_tips_no_location_permission,
                        R.string.to_grant_permission,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LeScanner.startAppDetailsActivity(activity);
                            }
                        });
            }

            @Override
            public void onLocationServiceDisabled() {
                showAlertDialog(
                        R.string.scan_tips_location_service_disabled,
                        R.string.to_enable,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LeScanner.requestEnableLocation(activity);
                            }
                        });
            }

            @Override
            public void onReady() {
                LeScanner.startScan(mOnLeScanListener);
            }
        });
    }

    private void showAlertDialog(int msgId, int okBtnTextId, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(getActivity())
                .setCancelable(false)
                .setMessage(msgId)
                .setPositiveButton(okBtnTextId, okListener)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                }).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        LeScanner.stopScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LeScanner.onRequestPermissionsResult(this, requestCode, permissions, grantResults);//todo 处理授权结果
    }

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //单击连接设备
            LeScanner.stopScan();
            LeDevice device = mLeDeviceListAdapter.getItem(position);
            mLeProxy.connect(device.getAddress(), false);
        }
    };


    private final OnItemLongClickListener mOnItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            //长按查看广播数据
            LeDevice device = mLeDeviceListAdapter.getItem(position);
            showAdvDetailsDialog(device);
            return true;
        }
    };

    //显示广播数据
    private void showAdvDetailsDialog(LeDevice device) {
        LeScanRecord record = device.getLeScanRecord();

        String message = device.getAddress()
                + "\n\n["
                + DataUtil.byteArrayToHex(record.getBytes())
                + "]\n\n"
                + record.toString();

        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }


    //蓝牙扫描监听
    private final OnLeScanListener mOnLeScanListener = new OnLeScanListener() {
        @Override
        public void onScanStart() {
            mHandler.sendEmptyMessage(MSG_SCAN_STARTED);
        }

        @Override
        public void onLeScan(LeScanResult leScanResult) {
            Log.e(TAG, "onLeScan() - " + leScanResult.getDevice() + " [" + leScanResult.getLeScanRecord().getLocalName() + "]");
            Message msg = new Message();
            msg.what = MSG_SCAN_DEVICE;
            msg.obj = new LeDevice(leScanResult);
            mHandler.sendMessage(msg);
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

        @Override
        public void onScanStop() {
            mHandler.sendEmptyMessage(MSG_SCAN_STOPPED);
        }
    };


    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<LeDevice> mLeDevices = new ArrayList<>();

        void addDevice(LeDevice device) {
            //if (TextUtils.isEmpty(device.getName())) return;
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                notifyDataSetChanged();
            }
        }

        void clear() {
            mLeDevices.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public LeDevice getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.item_device_list, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.connect = (TextView) view.findViewById(R.id.btn_connect);
                viewHolder.connect.setVisibility(View.VISIBLE);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            LeDevice device = mLeDevices.get(i);
            String deviceName = device.getName();
            if (!TextUtils.isEmpty(deviceName))
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView connect;
    }
}