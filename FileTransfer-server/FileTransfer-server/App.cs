using System;
using System.Diagnostics;
using System.Text;
using System.Threading;
using System.Windows.Forms;
using java.io;
using java.lang;
using java.net;

namespace FileTransfer_server
{
    public partial class App : Form
    {
        private ServerSocket server_socket;
        private Socket target_socket;
        private DataInputStream din;
        private DataOutputStream dout;
        System.Threading.Thread thread_receive_file = null;
        private RandomAccessFile rw = null;

        private string path = "D:/";
        private string file_name = "";
        private bool isServerRunning = false;
        private bool stopBackgroundService = false;

        public App()
        {
            InitializeComponent();
        }

        private void App_Load(object sender, EventArgs e)
        {
            
        }

        public void StartServer()
        {
            try
            {
                debug_window.Text = "Starting server..." + "\n" + debug_window.Text;
                server_socket = new ServerSocket(6869);
                debug_window.Text = "Waiting for file..." + "\n" + debug_window.Text;
                
                thread_receive_file = new System.Threading.Thread(new ThreadStart(ReceiveFile));
                thread_receive_file.Start();
            }
            catch (System.Exception ex)
            {
                Debug.WriteLine("StartServer: " + ex.ToString());
            }
        }

        private void StopServer()
        {
            stopBackgroundService = true;

            if(dout != null)
                dout.close();

            if(target_socket != null)
                target_socket.close();

            if(server_socket != null)
                server_socket.close();

            if(din != null)
                din.close();

            if(rw != null)
                rw.close();

            debug_window.Invoke(new Action(delegate ()
            {
                debug_window.Text = "========= Server stopped =========" + "\n" + debug_window.Text;
            }));

            pb_status.Invoke(new Action(delegate ()
            {
                pb_status.Value = 0;
            }));
        }

        public void ReceiveFile()
        {
            try
            {
                target_socket = server_socket.accept();
                din = new DataInputStream(target_socket.getInputStream());
                dout = new DataOutputStream(target_socket.getOutputStream());
            
                long current_file_pointer = 0;
                while (!stopBackgroundService)
                {
                    byte[] initilize = new byte[1];
                
                    din.read(initilize, 0, initilize.Length);

                    if (initilize[0] == 2)
                    {
                        byte[] cmd_buff = new byte[3];
                        din.read(cmd_buff, 0, cmd_buff.Length);
                        byte[] recv_data = ReadStream();

                        switch (Integer.parseInt(Encoding.UTF8.GetString(cmd_buff)))
                        {
                            case 124:
                                file_name = Encoding.UTF8.GetString(recv_data);
                                rw = new RandomAccessFile(path + file_name, "rw");
                                dout.write(CreateDataPacket(Encoding.UTF8.GetBytes("125"), Encoding.UTF8.GetBytes(java.lang.String.valueOf(current_file_pointer))));
                                dout.flush();
                                debug_window.Invoke(new Action(delegate ()
                                {
                                    debug_window.Text = "Receiving file: " + file_name + "..." + "\n" + debug_window.Text;
                                }));
                                break;
                            case 126:
                                rw.seek(current_file_pointer);
                                rw.write(recv_data);
                                current_file_pointer = rw.getFilePointer();
                                pb_status.Invoke(new Action(delegate ()
                                {
                                    pb_status.Value = Convert.ToInt32(((float)current_file_pointer / rw.length()) * 100);
                                }));

                                dout.write(CreateDataPacket(Encoding.UTF8.GetBytes("125"), Encoding.UTF8.GetBytes(java.lang.String.valueOf(current_file_pointer))));
                                dout.flush();
                                break;
                            case 127:
                                if ("Close".Equals(Encoding.UTF8.GetString(recv_data)))
                                {
                                    dout.close();
                                    target_socket.close();
                                    server_socket.close();
                                    din.close();
                                    if(rw != null)
                                        rw.close();

                                    current_file_pointer = 0;

                                    debug_window.Invoke(new Action(delegate ()
                                    {
                                        debug_window.Text = "File received and saved in: " + path + file_name + "\n" + debug_window.Text;
                                    }));

                                    pb_status.Invoke(new Action(delegate ()
                                    {
                                        pb_status.Value = 0;
                                    }));

                                    server_socket = new ServerSocket(6868);
                                    target_socket = server_socket.accept();
                                    din = new DataInputStream(target_socket.getInputStream());
                                    dout = new DataOutputStream(target_socket.getOutputStream());
                                }
                                else if("GetName".Equals(Encoding.UTF8.GetString(recv_data)))
                                {
                                    dout.write(CreateDataPacket(Encoding.UTF8.GetBytes("100"), Encoding.UTF8.GetBytes(InetAddress.getLocalHost().getHostName())));
                                    dout.flush();
                                }
                                break;
                        }
                    }
                }
            }
            catch (System.Exception ex)
            {
                Debug.WriteLine("ReceiveFile: " + ex.ToString());
            }
        }

        private byte[] ReadStream()
        {
            byte[] data_buff = null;
            try
            {
                int b = 0;
                System.String buff_length = "";
                while ((b = din.read()) != 4)
                {
                    buff_length += (char)b;
                }
                int data_length = Integer.parseInt(buff_length);
                data_buff = new byte[Integer.parseInt(buff_length)];
                int byte_read = 0;
                int byte_offset = 0;
                while (byte_offset < data_length)
                {
                    byte_read = din.read(data_buff, byte_offset, data_length - byte_offset);
                    byte_offset += byte_read;
                }
            }
            catch (System.Exception ex)
            {
                Debug.WriteLine("ReadStream: " + ex.ToString());
                debug_window.Invoke(new Action(delegate ()
                {
                    debug_window.Text = "ReadStream: " + ex.ToString() + "\n" + debug_window.Text;
                }));
            }
            return data_buff;
        }

        private byte[] CreateDataPacket(byte[] cmd, byte[] data)
        {
            byte[] packet = null;
            try
            {
                byte[] initialize = new byte[1];
                initialize[0] = 2;
                byte[] separator = new byte[1];
                separator[0] = 4;

                byte[] data_length = Encoding.UTF8.GetBytes(java.lang.String.valueOf(data.Length));
                packet = new byte[initialize.Length + cmd.Length + separator.Length + data_length.Length + data.Length];

                Array.Copy(initialize, 0, packet, 0, initialize.Length);
                Array.Copy(cmd, 0, packet, initialize.Length, cmd.Length);
                Array.Copy(data_length, 0, packet, initialize.Length + cmd.Length, data_length.Length);
                Array.Copy(separator, 0, packet, initialize.Length + cmd.Length + data_length.Length, separator.Length);
                Array.Copy(data, 0, packet, initialize.Length + cmd.Length + data_length.Length + separator.Length, data.Length);

            }
            catch (System.Exception ex)
            {
                Debug.WriteLine("CreateDataPacket: " + ex.ToString());
            }
            return packet;
        }

        private void Path_changed(object sender, EventArgs e)
        {
            path = tb_path.Text;
        }

        private void btn_browse_folder_Click(object sender, EventArgs e)
        {
            if (folderBrowserDialog.ShowDialog() == DialogResult.OK)
            {
                tb_path.Text = folderBrowserDialog.SelectedPath;
            }
        }

        private void btn_start_Server_Click(object sender, EventArgs e)
        {
            if (isServerRunning)
            {
                isServerRunning = false;
                btn_start_Server.Text = "Start server";
                StopServer();
            }
            else
            {
                isServerRunning = true;
                btn_start_Server.Text = "Stop server";
                StartServer();
            }
        }

    }
}
