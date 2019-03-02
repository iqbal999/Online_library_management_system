package com.example.iqbal.olms.activities;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.iqbal.olms.R;
import com.example.iqbal.olms.api.DownloadFileService;
import com.example.iqbal.olms.api.RetrofitClient;
import com.example.iqbal.olms.api.ServiceGenerator;
import com.example.iqbal.olms.model.DefaultResponse;
import com.example.iqbal.olms.model.StudentInfo;
import com.example.iqbal.olms.storage.SharedPrefManager;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookDetails extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private final String CHANNEL_ID = "personal_notfications";
    private final int NOTIFICATION_ID = 001;

    TextView tv_book_name, tv_author_name, tv_book_edition, tv_avail_copies, tv_shelf, tv_position;
    Button pdf_download, issue_book;
    String base_url = "http://192.168.1.6/library/upload/";
    DownloadManager downloadManager;
    public long downloadId;
    StudentInfo stu_info;
    String id, book_name, book_edition, author_name, avail_copies, shelf, pos, pdf, full_url;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_details);

        stu_info = SharedPrefManager.getInstance(this).getStudentInfo();


        tv_book_name = findViewById(R.id.tv_book_name);
        tv_author_name = findViewById(R.id.tv_author_name);
        tv_book_edition = findViewById(R.id.tv_book_edition);
        tv_avail_copies = findViewById(R.id.tv_aval_copies);
        tv_shelf = findViewById(R.id.tv_shelf_no);
        tv_position = findViewById(R.id.tv_book_position);
        pdf_download = findViewById(R.id.pdf_download);
        issue_book = findViewById(R.id.issue_book);


        // Received data from intent

        Intent i = this.getIntent();
        id = i.getStringExtra("id");
        book_name = i.getStringExtra("book");
        book_edition = i.getStringExtra("edition");
        author_name = i.getStringExtra("author");
        avail_copies = i.getStringExtra("ava_cop");
        shelf = i.getStringExtra("shelf");
        pos = i.getStringExtra("pos");
        pdf = i.getStringExtra("pdf");

        if(pdf.equals("")){
            pdf_download.setEnabled(false);
        }

        // Show data to text view

        tv_book_name.setText("Book Name: "+book_name);
        tv_book_edition.setText("Edition: "+book_edition);
        tv_author_name.setText("Author Name: "+author_name);
        tv_avail_copies.setText("Available copies: "+avail_copies);
        tv_shelf.setText("Shelf No: "+shelf);
        tv_position.setText("Book Positon: "+pos);
        //tv_pdf.setText("PDF: "+pdf);



        full_url = base_url + pdf;
        Log.d("AAA",""+full_url);

        pdf_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission()){
                    downloadFile(full_url);
                } else {
                    requestPermission();
                }
               
            }
        });

        issue_book.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                Log.d("AAA",""+stu_info.getId()+ "\t"+Integer.parseInt(id));
                if(SharedPrefManager.getInstance(getApplicationContext()).isLoggedIn()){
                    Call<DefaultResponse> call = RetrofitClient
                            .getInstance()
                            .getApi()
                            .issueBook(stu_info.getId(), Integer.parseInt(id));

                    call.enqueue(new Callback<DefaultResponse>() {
                        @Override
                        public void onResponse(Call<DefaultResponse> call, Response<DefaultResponse> response) {
                            DefaultResponse dr = response.body();
                            Toast.makeText(BookDetails.this, dr.getMsg(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<DefaultResponse> call, Throwable t) {
                            Toast.makeText(BookDetails.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }else{
                    String[] all_info_of_books =  {
                            id,
                            book_name,
                            book_edition,
                            author_name,
                            avail_copies,
                            shelf,
                            pos,
                            pdf,
                            "ib"
                            };

                    openDetailsActivity(all_info_of_books);
                }

            }
        });



    }
    private void openDetailsActivity(String[] data) {
        Intent intent = new Intent(this, StudentLogin.class);
        intent.putExtra("id", data[0]);
        intent.putExtra("book", data[1]);
        intent.putExtra("edition", data[2]);
        intent.putExtra("author", data[3]);
        intent.putExtra("ava_cop", data[4]);
        intent.putExtra("shelf", data[5]);
        intent.putExtra("pos", data[6]);
        intent.putExtra("pdf", data[7]);
        intent.putExtra("flag", data[8]);

        startActivity(intent);
    }

    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED){

            return true;

        } else {

            return false;
        }
    }
    private void requestPermission() {
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    downloadFile(full_url);
                    //displayNotification();
                } else {

                    //Snackbar.make(findViewById(R.id.coordinatorLayout),"Permission Denied, Please allow to proceed !", Snackbar.LENGTH_LONG).show();

                }
                break;
        }
    }

    private void displayNotification(String pdfTitle) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_file_download);
        builder.setContentTitle("File Download");
        builder.setContentText(pdfTitle);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);


        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
    }


    private void downloadFile(String url){

        final DownloadFileService downloadService =
                ServiceGenerator.createService(DownloadFileService.class);

        Call<ResponseBody> call = downloadService.downloadFileUrl(url);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    //Log.d(TAG, "server contacted and has file");

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            boolean writtenToDisk = writeResponseBodyToDisk(response.body());
                            //Toast.makeText(BookDetails.this, "Download Successfull", Toast.LENGTH_SHORT).show();
                            //Log.d("AAA", "file download was a success? " + writtenToDisk);
                            return null;
                        }
                    }.execute();

                    displayNotification(pdf);
                    Toast.makeText(BookDetails.this, "yes", Toast.LENGTH_SHORT).show();
                }
                else {
                    //Log.d(TAG, "server contact failed");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                //Log.e(TAG, "error");
            }
        });



    }

    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            // todo change the file location/name according to your needs
            File path = Environment.getExternalStorageDirectory();
            File futureStudioIconFile = new File(path+"/Download/", pdf);
            Log.d("AAA",""+futureStudioIconFile);
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d("AAA" , fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }


}


