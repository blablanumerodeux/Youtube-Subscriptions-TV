package tv.subscriptions.youtube.youtubesubscriptionstv;

import android.net.ParseException;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class HandleXML {

    private XmlPullParserFactory xmlFactoryObject;
    public volatile boolean parsingComplete = true;

    private ArrayList<Video> listVideos = new ArrayList<Video>();
    public ArrayList<Video> getListVideos() {
        return listVideos;
    }
    private synchronized void addVideo(Video video){
        this.listVideos.add(video);
    }

    /*
    Parsing the rss feed of a youtube channel
     */
    public void parseXMLAndStoreIt(XmlPullParser myParser) {

        int event;
        String text=null;
        String attributZero=null;
        boolean inEntry = false;
        boolean inMediaGroup = false;

        try {
            event = myParser.getEventType();
            Video v= new Video();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name=myParser.getName();
                switch (event){
                    case XmlPullParser.START_TAG:
                        if(name.equals("entry")){
                            inEntry = true;
                            v = new Video();
                        }
                        else if(name.equals("media:thumbnail")){
                            attributZero = myParser.getAttributeValue(0);
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = myParser.getText();
                        break;

                    case XmlPullParser.END_TAG:

                        if(name.equals("entry")){
                            inEntry = false;
                        }
                        else if(name.equals("name")){
                            v.setChannelTitle(text);
                        }else if(inEntry ==true && name.equals("yt:videoId")){
                            v.setIdYT(text);
                        }
                        else if(inEntry ==true && name.equals("title")){
                            v.setTitle(text);
                        }
                        else if(inEntry ==true && name.equals("published")){
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            try{
                                Date d = sdf.parse(text);
                                v.setDatePublished(d);
                                //Log.i("APP",d.toString());
                            }catch(ParseException e){
                                e.printStackTrace();
                            }
                        }
                        else if(name.equals("media:thumbnail")){
                            v.setThumbnailsUrl(attributZero);

                            //we take only the first video
                            this.addVideo(v);
                            return;
                        }

                        break;
                }
                event = myParser.next();
            }

            parsingComplete = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Thread fetchXML(final String urlString){
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                // Starts the query
                conn.connect();
                InputStream stream = conn.getInputStream();

                xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser myparser = xmlFactoryObject.newPullParser();

                myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                myparser.setInput(stream, null);

                parseXMLAndStoreIt(myparser);
                stream.close();
            } catch (Exception e) {
                Log.e(MainActivity.LOG_TAG, e.toString());
            }
            }
        });
        return thread;
    }
}
