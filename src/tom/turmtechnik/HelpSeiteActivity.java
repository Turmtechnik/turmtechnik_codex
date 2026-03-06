package tom.turmtechnik;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class HelpSeiteActivity extends Activity {
    String fileName = Environment.getExternalStorageDirectory().toString() + "/Turmtechnik/LogError.txt";

    private StringBuffer outputStringBuffer;

    private boolean newDate = false;

    private String filePathAndName;

    private ExcelRead excelread;

    private ImageView image;

    private TextView outputView;

    private int displayWidth;
    private int displayHeight;

    int pfeilWidth;
    int pfeilHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.help_seite_layout);

//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);

        getDisplayParameter();
//        Log.i("displayWidth" , "=" + displayWidth) ;
//    	Log.i("displayHeight" , "=" + displayHeight) ;

        //pfeilWidth = displayWidth / 16 ;
        //pfeilHeight = displayHeight / 10 ;

        pfeilWidth = StaticVariable.tastenBreite;
        pfeilHeight = StaticVariable.tastenHoehe;

//        textView = (TextView) this.findViewById(R.id.outputText);

        Intent intent = getIntent();
        String helpFileName = intent.getStringExtra("help_file_name");


        image = (ImageView) this.findViewById(R.id.imageView1);

        //if(logErrorExists() )
        //{
        //	Log.e("logError" , "exists" ) ;
        //	setContentView(R.layout.text_layout);
        //	outputView = (TextView) this.findViewById(R.id.textView1);

        //	outputView.append("\nERROR LOG:\n\n");
        //	showLogErrors() ;
        //}
        //else
        //{
        Bitmap bmp = BitmapFactory.decodeFile(TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/" + helpFileName);
        image.setImageBitmap(bmp);
        //}


        //excelread = new ExcelRead() ;
        //String fileName = (TurmtechnikActivity.nebenUhrFileString);
        //excelread.openXls(fileName);


        // kann man besser machen!!  1.10.2013 17:46

        //if( ! logErrorExists() )
        //{
        Button btLinks = (Button) this.findViewById(R.id.buttonPfeilLinks);
        // neu ab 18.6.13 links Taste wird home Taste
        btLinks.setBackgroundDrawable(StaticVariable.btn_home_drawable);
        btLinks.setText(StaticVariable.btn_home_textstring);
        btLinks.setTextColor(getResources().getColor(R.color.white));

        btLinks.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //finish();
                endActivityHelpSeite();
            }
        });
        //}

    } // ende von onCreate

    private void getDisplayParameter() {
        Display display = this.getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
    }

    //private boolean logErrorExists()
    //{
    //	File logFile = new File(fileName) ;
    //	return logFile.exists() ;
    //}

    private void showLogErrors() {
        File logFile = new File(fileName);

        try {
            BufferedReader br = new BufferedReader(new FileReader(logFile));

            String line;

            while ((line = br.readLine()) != null) {
                outputView.append(" " + line + "\n");
            }
            br.close();
        } catch (IOException e) {
            // was nun ?
        }

    }

    private void endActivityHelpSeite() {
        // setStaticVariable();

        //  	setWartenLaufen();
        //  	excelread.closeWorkbook();
        //  	doRun = false ;
        //layout = null ;
        System.gc();
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            TurmtechnikActivity.showExitPasswordDialogFrom(this);
            return true;
        }
        return false;
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_turmtechnik, menu);
//        return true;
//    }


} // ende der Klasse
