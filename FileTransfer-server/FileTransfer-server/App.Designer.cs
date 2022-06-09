namespace FileTransfer_server
{
    partial class App
    {
        /// <summary>
        /// Wymagana zmienna projektanta.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Wyczyść wszystkie używane zasoby.
        /// </summary>
        /// <param name="disposing">prawda, jeżeli zarządzane zasoby powinny zostać zlikwidowane; Fałsz w przeciwnym wypadku.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Kod generowany przez Projektanta formularzy systemu Windows

        /// <summary>
        /// Metoda wymagana do obsługi projektanta — nie należy modyfikować
        /// jej zawartości w edytorze kodu.
        /// </summary>
        private void InitializeComponent()
        {
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(App));
            this.pb_status = new System.Windows.Forms.ProgressBar();
            this.tb_path = new System.Windows.Forms.TextBox();
            this.folderBrowserDialog = new System.Windows.Forms.FolderBrowserDialog();
            this.btn_browse_folder = new System.Windows.Forms.Button();
            this.btn_start_Server = new System.Windows.Forms.Button();
            this.debug_window = new System.Windows.Forms.Label();
            this.panel_debug_window = new System.Windows.Forms.Panel();
            this.panel_debug_window.SuspendLayout();
            this.SuspendLayout();
            // 
            // pb_status
            // 
            this.pb_status.Location = new System.Drawing.Point(15, 165);
            this.pb_status.Name = "pb_status";
            this.pb_status.Size = new System.Drawing.Size(451, 23);
            this.pb_status.TabIndex = 0;
            // 
            // tb_path
            // 
            this.tb_path.Location = new System.Drawing.Point(12, 75);
            this.tb_path.Name = "tb_path";
            this.tb_path.Size = new System.Drawing.Size(451, 20);
            this.tb_path.TabIndex = 3;
            this.tb_path.Text = "D:/";
            this.tb_path.TextChanged += new System.EventHandler(this.Path_changed);
            // 
            // btn_browse_folder
            // 
            this.btn_browse_folder.Location = new System.Drawing.Point(15, 101);
            this.btn_browse_folder.Name = "btn_browse_folder";
            this.btn_browse_folder.Size = new System.Drawing.Size(106, 23);
            this.btn_browse_folder.TabIndex = 4;
            this.btn_browse_folder.Text = "Browse folder";
            this.btn_browse_folder.UseVisualStyleBackColor = true;
            this.btn_browse_folder.Click += new System.EventHandler(this.btn_browse_folder_Click);
            // 
            // btn_start_Server
            // 
            this.btn_start_Server.Location = new System.Drawing.Point(141, 12);
            this.btn_start_Server.Name = "btn_start_Server";
            this.btn_start_Server.Size = new System.Drawing.Size(172, 23);
            this.btn_start_Server.TabIndex = 5;
            this.btn_start_Server.Text = "Start server";
            this.btn_start_Server.UseVisualStyleBackColor = true;
            this.btn_start_Server.Click += new System.EventHandler(this.btn_start_Server_Click);
            // 
            // debug_window
            // 
            this.debug_window.AutoSize = true;
            this.debug_window.Location = new System.Drawing.Point(-3, 0);
            this.debug_window.MinimumSize = new System.Drawing.Size(454, 223);
            this.debug_window.Name = "debug_window";
            this.debug_window.Size = new System.Drawing.Size(454, 223);
            this.debug_window.TabIndex = 7;
            // 
            // panel_debug_window
            // 
            this.panel_debug_window.AutoScroll = true;
            this.panel_debug_window.Controls.Add(this.debug_window);
            this.panel_debug_window.Location = new System.Drawing.Point(15, 215);
            this.panel_debug_window.Name = "panel_debug_window";
            this.panel_debug_window.Size = new System.Drawing.Size(451, 223);
            this.panel_debug_window.TabIndex = 8;
            // 
            // App
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(478, 450);
            this.Controls.Add(this.panel_debug_window);
            this.Controls.Add(this.btn_start_Server);
            this.Controls.Add(this.btn_browse_folder);
            this.Controls.Add(this.tb_path);
            this.Controls.Add(this.pb_status);
            this.Icon = ((System.Drawing.Icon)(resources.GetObject("$this.Icon")));
            this.Name = "App";
            this.Text = "File Transfer";
            this.Load += new System.EventHandler(this.App_Load);
            this.panel_debug_window.ResumeLayout(false);
            this.panel_debug_window.PerformLayout();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.ProgressBar pb_status;
        private System.Windows.Forms.TextBox tb_path;
        private System.Windows.Forms.FolderBrowserDialog folderBrowserDialog;
        private System.Windows.Forms.Button btn_browse_folder;
        private System.Windows.Forms.Button btn_start_Server;
        private System.Windows.Forms.Label debug_window;
        private System.Windows.Forms.Panel panel_debug_window;
    }
}

