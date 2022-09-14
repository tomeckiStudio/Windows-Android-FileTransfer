package com.tomecki.filetransfer_client;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    static MainActivity instance;

    ProgressBar pb_app_main, pb_choose_comp;
    ListView lv_ip_addresses;
    Thread connect_and_send_file;
    Thread connect_and_get_server_name;
    LinearLayout hsv_app_gallery_layout;
    RelativeLayout rl_choose_comp;
    TextView tv_app_header, tv_debug_window;
    Button btn_select_file;
    LockableScrollView hsv_app_gallery;
    ImageView iv_upload_icon;

    ArrayList<String> examples_images_to_send;
    HashMap<String, String> ipAddresses = new HashMap<>();
    ArrayList<String> listItems = new ArrayList<>();
    ArrayAdapter<String> adapter;
    Socket getServerNameSocket;

    float _yDelta;
    String hostname = "", target_comp = "", path = "";

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;

        tv_app_header = findViewById(R.id.tv_app_header);

        tv_debug_window = findViewById(R.id.tv_debug_window);
        tv_debug_window.setMovementMethod(new ScrollingMovementMethod());

        rl_choose_comp = findViewById(R.id.rl_choose_comp);
        pb_choose_comp = findViewById(R.id.pb_choose_comp);
        btn_select_file = findViewById(R.id.btn_select_file);

        pb_app_main = findViewById(R.id.pb_app_main);

        lv_ip_addresses = findViewById(R.id.lv_ipaddresses);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        lv_ip_addresses.setAdapter(adapter);

        iv_upload_icon = findViewById(R.id.iv_upload_icon);

        hsv_app_gallery = findViewById(R.id.hsv_app_gallery);

        hsv_app_gallery_layout = findViewById(R.id.hsv_app_gallery_layout);

        final AsyncTask<String, String, Void> get_all_ip_and_hostname = new getAllIPAndHostname().execute();

        lv_ip_addresses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                get_all_ip_and_hostname.cancel(true);

                hostname = adapter.getItem(i);
                target_comp = ipAddresses.get(hostname);
                tv_app_header.setText("Connection to " + hostname);
                rl_choose_comp.setVisibility(View.GONE);
                tv_debug_window.setText("Selected computer: " + hostname + "\n" + tv_debug_window.getText());
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            examples_images_to_send = getAllShownImagesPath(this);

            new getExamplesImagesToSend().execute();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                examples_images_to_send = getAllShownImagesPath(this);

                new getExamplesImagesToSend().execute();
            }
        }
    }

    class getAllIPAndHostname extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                InetAddress localhost = getCurrentIp();
                byte[] ip = localhost.getAddress();

                for (int i = 1; i <= 254; i++) {
                    if (isCancelled()) {
                        return null;
                    }
                    ip[3] = (byte) i;
                    final InetAddress address = InetAddress.getByAddress(ip);
                    String hostName = "", hostAddress = "";

                    getServerNameSocket = new Socket();

                    try {
                        getServerNameSocket.connect(new InetSocketAddress(address.getHostAddress(), 6869), 200);

                        MainActivity obj = MainActivity.getInstance();
                        Socket obj_client = getServerNameSocket;
                        DataInputStream din = new DataInputStream(obj_client.getInputStream());
                        DataOutputStream dout = new DataOutputStream(obj_client.getOutputStream());

                        dout.write(obj.CreateDataPacket("127".getBytes("UTF8"), "GetName".getBytes("UTF8")));
                        dout.flush();

                        boolean loop_break = false;
                        while (true) {
                            if (din.read() == 2) {
                                byte[] cmd_buff = new byte[3];
                                din.read(cmd_buff, 0, cmd_buff.length);
                                byte[] recv_buff = obj.ReadStream(din);
                                switch (Integer.parseInt(new String(cmd_buff))) {
                                    case 100:
                                        hostName = new String(recv_buff);
                                        hostAddress = address.getHostAddress();

                                        loop_break = true;
                                        break;
                                }
                            }
                            if (loop_break) {
                                dout.write(obj.CreateDataPacket("127".getBytes("UTF8"), "Close".getBytes("UTF8")));
                                dout.flush();
                                dout.close();
                                din.close();
                                obj_client.close();
                                break;
                            }
                        }

                    } catch (IOException exception) {
                        Log.e("siema2", ""+address);
                    } finally {
                        getServerNameSocket.close();
                    }

                    publishProgress(String.valueOf((int) ((i / 254.0) * 100)), hostName, hostAddress);
                }
            } catch (Exception e) {
                Log.e("TCPDATA", "Exception: " + e.toString());
            }

            return null;
        }

        protected void onProgressUpdate(final String... progress) {
            pb_choose_comp.setProgress(Integer.parseInt(progress[0]));
            if (!progress[1].equals("")) {
                adapter.add(progress[1]);
                ipAddresses.put(progress[1], progress[2]);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            pb_choose_comp.setVisibility(View.GONE);
        }
    }

    class getExamplesImagesToSend extends AsyncTask<String, String, Void> {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected Void doInBackground(String... params) {
            for (int i = 0; i < 10; i++) {
                try {
                    File imgFile = new File(examples_images_to_send.get(i));

                    if (imgFile.exists()) {

                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                        final ImageView image = new ImageView(MainActivity.this);

                        image.setImageBitmap(Bitmap.createScaledBitmap(myBitmap, myBitmap.getWidth() / 3, myBitmap.getHeight() / 3, false));

                        image.setTag(imgFile.getPath().toString());

                        image.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });

                        image.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                int Y = (int) event.getRawY();
                                LinearLayout.LayoutParams paramsnew = (LinearLayout.LayoutParams) v.getLayoutParams();

                                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                                    case MotionEvent.ACTION_DOWN:
                                        _yDelta = Y - paramsnew.topMargin;
                                        break;
                                    case MotionEvent.ACTION_UP:
                                    case MotionEvent.ACTION_CANCEL:
                                        hsv_app_gallery.setScrollingEnabled(true);
                                        LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) v.getLayoutParams();
                                        param.topMargin = 0;
                                        v.setLayoutParams(param);
                                        if ((int) (Y - _yDelta) < -200) {
                                            path = v.getTag().toString();
                                            sendFileToSocket();

                                            Toast.makeText(MainActivity.this, v.getTag().toString(), Toast.LENGTH_SHORT).show();
                                        }
                                        iv_upload_icon.setVisibility(View.GONE);
                                        break;
                                    case MotionEvent.ACTION_MOVE:
                                        int current_pos = (int) (Y - _yDelta);
                                        if (current_pos < -20) {
                                            hsv_app_gallery.setScrollingEnabled(false);
                                        }
                                        if (current_pos < 0) {
                                            LinearLayout.LayoutParams param3 = (LinearLayout.LayoutParams) v.getLayoutParams();
                                            param3.topMargin = current_pos;
                                            v.setLayoutParams(param3);
                                        }
                                        if (current_pos < -200) {
                                            iv_upload_icon.setVisibility(View.VISIBLE);
                                        }
                                        break;

                                }
                                hsv_app_gallery_layout.invalidate();
                                return false;
                            }
                        });


                        addView(image);

                    }
                } catch (Exception e) {

                }
            }

            return null;
        }

        protected void onProgressUpdate(final String... progress) {

        }

        @Override
        protected void onPostExecute(Void result) {

        }
    }


    public InetAddress getCurrentIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces
                        .nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                while (nias.hasMoreElements()) {
                    InetAddress ia = (InetAddress) nias.nextElement();
                    if (!ia.isLinkLocalAddress()
                            && !ia.isLoopbackAddress()
                            && ia instanceof Inet4Address) {
                        return ia;
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("TCP", "unable to get current IP " + e.getMessage(), e);
        }
        return null;
    }


    public void addView(final ImageView imageView2) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(500, 500);
                layoutParams.setMargins(0, 10, 0, 10);

                imageView2.setLayoutParams(layoutParams);
                hsv_app_gallery_layout.addView(imageView2);
            }
        });

    }

    public static ArrayList<String> getAllShownImagesPath(Activity activity) {
        Uri uri;
        Cursor cursor;
        int column_index_data;
        ArrayList<String> listOfAllImages = new ArrayList<>();
        String absolutePathOfImage = null;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

        cursor = activity.getContentResolver().query(uri, projection, null, null, null);

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);

            listOfAllImages.add(0, absolutePathOfImage);
        }
        return listOfAllImages;
    }


    public void SelectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 10);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 10) {
            Uri uri = data.getData();
            path = getPathFromUri(getApplicationContext(), uri);
            sendFileToSocket();
        }
    }

    public void sendFileToSocket(){
        connect_and_send_file = new Thread() {
            @Override
            public void run() {
                try {
                    MainActivity obj = MainActivity.getInstance();
                    Socket obj_client = new Socket(InetAddress.getByName(target_comp), 6868);
                    DataInputStream din = new DataInputStream(obj_client.getInputStream());
                    DataOutputStream dout = new DataOutputStream(obj_client.getOutputStream());

                    tv_debug_window.setText("Connected to "+hostname+"\n"+ tv_debug_window.getText());

                    File target_file = new File(path);
                    Log.e("TCP-test", target_file.toString());
                    dout.write(obj.CreateDataPacket("124".getBytes("UTF8"), target_file.getName().getBytes("UTF8")));
                    dout.flush();
                    RandomAccessFile rw = new RandomAccessFile(target_file, "r");
                    long current_file_pointer = 0;
                    boolean loop_break = false;

                    tv_debug_window.setText("I am initiating file transfer: " + target_file.getName() + "\n" + tv_debug_window.getText());
                    while (true) {
                        if (din.read() == 2) {
                            byte[] cmd_buff = new byte[3];
                            din.read(cmd_buff, 0, cmd_buff.length);
                            byte[] recv_buff = obj.ReadStream(din);
                            switch (Integer.parseInt(new String(cmd_buff))) {
                                case 125:
                                    current_file_pointer = Long.valueOf(new String(recv_buff));
                                    int buff_len = (int) (rw.length() - current_file_pointer < 20000 ? rw.length() - current_file_pointer : 20000);
                                    byte[] temp_buff = new byte[buff_len];
                                    if (current_file_pointer != rw.length()) {
                                        rw.seek(current_file_pointer);
                                        rw.read(temp_buff, 0, temp_buff.length);
                                        dout.write(obj.CreateDataPacket("126".getBytes("UTF8"), temp_buff));
                                        dout.flush();

                                        final int progress = Math.round(((float) current_file_pointer / rw.length()) * 100);

                                        pb_app_main.post(new Runnable() {
                                            public void run() {
                                                pb_app_main.setProgress(progress);
                                            }
                                        });
                                    } else {
                                        loop_break = true;

                                        tv_debug_window.setText("File transfer successfully completed\n" + tv_debug_window.getText());
                                    }
                                    break;
                            }
                        }
                        if (loop_break) {
                            dout.write(obj.CreateDataPacket("127".getBytes("UTF8"), "Close".getBytes("UTF8")));
                            dout.flush();
                            dout.close();
                            rw.close();
                            din.close();
                            obj_client.close();
                            tv_debug_window.setText("The connection has been closed\n"+ tv_debug_window.getText());
                            break;
                        }
                    }

                } catch (Exception ex) {
                    Log.e("Socket-error", ex.toString());
                    tv_debug_window.setText("Socket-error: " + ex.toString() + "\n"+ tv_debug_window.getText());
                }
            }
        };
        connect_and_send_file.start();
    }

    public static String getPathFromUri(final Context context, final Uri uri) {

        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    if (split.length > 1) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    } else {
                        return Environment.getExternalStorageDirectory() + "/";
                    }
                } else {
                    return "storage" + "/" + docId.replace(":", "/");
                }
            } else if (isDownloadsDocument(uri)) {
                String fileName = getFilePath(context, uri);
                if (fileName != null) {
                    return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                }

                String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith("raw:")) {
                    id = id.replaceFirst("raw:", "");
                    File file = new File(id);
                    if (file.exists())
                        return id;
                }

                if(id.split(":")[0].equals("msf")){
                    id = id.split(":")[1];
                }

                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }


        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    public static String getFilePath(Context context, Uri uri) {

        Cursor cursor = null;
        final String[] projection = {
                MediaStore.MediaColumns.DISPLAY_NAME
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private byte[] CreateDataPacket(byte[] cmd, byte[] data) {
        byte[] packet = null;
        try {
            byte[] initialize = new byte[1];
            initialize[0] = 2;
            byte[] separator = new byte[1];
            separator[0] = 4;
            byte[] data_length = String.valueOf(data.length).getBytes("UTF8");
            packet = new byte[initialize.length + cmd.length + separator.length + data_length.length + data.length];

            System.arraycopy(initialize, 0, packet, 0, initialize.length);
            System.arraycopy(cmd, 0, packet, initialize.length, cmd.length);
            System.arraycopy(data_length, 0, packet, initialize.length + cmd.length, data_length.length);
            System.arraycopy(separator, 0, packet, initialize.length + cmd.length + data_length.length, separator.length);
            System.arraycopy(data, 0, packet, initialize.length + cmd.length + data_length.length + separator.length, data.length);

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
        return packet;
    }

    private byte[] ReadStream(DataInputStream din) {
        byte[] data_buff = null;
        try {
            int b = 0;
            String buff_length = "";
            while ((b = din.read()) != 4) {
                buff_length += (char) b;
            }
            int data_length = Integer.parseInt(buff_length);
            data_buff = new byte[Integer.parseInt(buff_length)];
            int byte_read = 0;
            int byte_offset = 0;
            while (byte_offset < data_length) {
                byte_read = din.read(data_buff, byte_offset, data_length - byte_offset);
                byte_offset += byte_read;
            }
        } catch (IOException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data_buff;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_select_file:
                pb_app_main.setProgress(0);
                SelectFile();
                break;
            default:
                break;
        }
    }

}
