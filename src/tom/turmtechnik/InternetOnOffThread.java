package tom.turmtechnik;

import android.util.Log;

import java.util.Calendar;

/**
 * Created by afred on 15.01.17.
 */

public class InternetOnOffThread extends Thread
{
    Calendar calendar;

    public void run()
    {
        while (StaticVariable.serial_io_ThreadsRun == true)
        {
            //Log.e("internetOnOff" , "internetOnStunden=" + StaticVariable.internetOnStunden) ;
            //Log.e("internetOnOff" , "internetOnMinuten=" + StaticVariable.internetOnMinuten) ;
            //Log.e("internetOnOff" , "internetOffStunden=" + StaticVariable.internetOffStunden) ;
            //Log.e("internetOnOff" , "internetOffMinuten=" + StaticVariable.internetOffMinuten) ;
            if((StaticVariable.internetOnStunden == StaticVariable.internetOffStunden)
                    && (StaticVariable.internetOnMinuten == StaticVariable.internetOffMinuten))
            {
                StaticVariable.internetOnOffFlag = true ;
            }
            else
            {
                StaticVariable.internetOnOffFlag = checkInTimeRange() ;
            }

            //Log.e("internetOnOff" , "=" + StaticVariable.internetOnOffFlag) ;

            sleepMs(1200);
        }
    } // ende von run

    private boolean checkInTimeRange()
    {
        boolean onOffFlag = true ;

        calendar = Calendar.getInstance();

        int momentaneStunde = calendar.get(Calendar.HOUR_OF_DAY);
        int momentaneMinute = calendar.get(Calendar.MINUTE);

        //Log.e("internetOnOff" , "momentaneStunde=" + momentaneStunde) ;
        //Log.e("internetOnOff" , "momentaneMinute=" + momentaneMinute) ;

        int startStundeTemp = StaticVariable.internetOnStunden ;
        int startMinuteTemp = StaticVariable.internetOnMinuten ;
        int stopStundeTemp = StaticVariable.internetOffStunden ;
        int stopMinuteTemp = StaticVariable.internetOffMinuten ;

        //Log.e("internetOnOff" , "startStundeTemp=" + startStundeTemp) ;
        //Log.e("internetOnOff" , "startMinuteTemp=" + startMinuteTemp) ;
        //Log.e("internetOnOff" , "stopStundeTemp=" + stopStundeTemp) ;
        //Log.e("internetOnOff" , "stopMinuteTemp=" + stopMinuteTemp) ;
        //Log.e("internetOnOff" , "vor while") ;

        while ( ! ((startStundeTemp == stopStundeTemp) && (startMinuteTemp == stopMinuteTemp)) )
        {
            //Log.e("internetOnOff" , "startStundeTemp=" + startStundeTemp) ;
            //Log.e("internetOnOff" , "startMinuteTemp=" + startMinuteTemp) ;
            //Log.e("internetOnOff" , "stopStundeTemp=" + stopStundeTemp) ;
            //Log.e("internetOnOff" , "stopMinuteTemp=" + stopMinuteTemp) ;

            if((momentaneStunde == startStundeTemp) && (momentaneMinute == startMinuteTemp))
            {
                Log.e("internetOnOff" , "in Range") ;
                onOffFlag = false ; // in time range on/off ; internet ist ausgeschaltet
                return onOffFlag ;
            }
            else
            {
                startMinuteTemp++ ;
                if(startMinuteTemp > 59)
                {
                    startMinuteTemp=0 ;
                    startStundeTemp++ ;
                    if(startStundeTemp > 23)
                    {
                        startStundeTemp = 0 ;
                    }
                }
            }
            //sleepMs(100);
        }


        return onOffFlag ;
    }

    private void sleepMs(long ms) {
        long time = ms / 10;
        for (int i = 0; i < time; i++) {
            if ( StaticVariable.serial_io_ThreadsRun == false) {
                break;
            }
            try {
                Thread.sleep(10);     //Thread.sleep(time);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}


